/**
 * Copyright (c) 2010-2016 Yahoo! Inc., 2017 YCSB contributors All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */

package com.leo.ycsb;

import com.leo.ycsb.job.core.context.XxlJobHelper;
import com.leo.ycsb.measurements.Measurements;
import com.leo.ycsb.measurements.exporter.MeasurementsExporter;
import com.leo.ycsb.measurements.exporter.XxlJobMeasurementsExporter;
import org.apache.htrace.core.HTraceConfiguration;
import org.apache.htrace.core.TraceScope;
import org.apache.htrace.core.Tracer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;

/**
 * @author leojie
 *
 * Main class for executing YCSB.
 */
public final class YcsbDbClientThreadThread extends AbstractYcsbDbClientThread {
    private final String[] ycsbArgs;

    public YcsbDbClientThreadThread(String[] ycsbArgs) {
        this.ycsbArgs = ycsbArgs;
    }

    /**
     * ycsb 执行客户端请求的线程数
     */
    private Map<Thread, ClientThread> threads = new HashMap<>();

    private Workload workload = null;

    /**
     * An optional thread used to track progress and measure JVM stats.
     */
    private StatusThread statusthread = null;

    /**
     * 终止任务的线程
     */
    private Thread terminator = null;

    private volatile boolean toStop = false;

    /**
     * Exports the measurements to either sysout or a file using the exporter
     * loaded from conf.
     *
     * @throws IOException Either failed to write to output stream or failed to close it.
     */
    private void exportMeasurements(Properties props, int opCount, long runtime)
            throws IOException {
        MeasurementsExporter exporter = null;
        OutputStream out = null;
        try {
            // if no destination file is provided the results will be written to stdout
            String exportFile = props.getProperty(EXPORT_FILE_PROPERTY);
            if (exportFile == null) {
                out = System.out;
            } else {
                out = new FileOutputStream(exportFile);
            }
            // if no exporter is provided the default text one will be used
            String exporterStr = props.getProperty(EXPORTER_PROPERTY, "com.leo.ycsb.measurements.exporter.XxlJobMeasurementsExporter");
            try {
                exporter = (MeasurementsExporter) Class.forName(exporterStr).getConstructor(OutputStream.class).newInstance(out);
            } catch (Exception e) {
                XxlJobHelper.log("Could not find exporter " + exporterStr + ", will use the default text reporter "
                        + "com.leo.ycsb.measurements.exporter.XxlJobMeasurementsExporter" + ".");
                exporter = new XxlJobMeasurementsExporter(out);
            }

            exporter.write("OVERALL", "RunTime(ms)", runtime);
            double throughput = 1000.0 * (opCount) / (runtime);
            exporter.write("OVERALL", "Throughput(ops/sec)", throughput);

            final Map<String, Long[]> gcs = Utils.getGCStatst();
            long totalGCCount = 0;
            long totalGCTime = 0;
            for (final Entry<String, Long[]> entry : gcs.entrySet()) {
                exporter.write("TOTAL_GCS_" + entry.getKey(), "Count", entry.getValue()[0]);
                exporter.write("TOTAL_GC_TIME_" + entry.getKey(), "Time(ms)", entry.getValue()[1]);
                exporter.write("TOTAL_GC_TIME_%_" + entry.getKey(), "Time(%)",
                        ((double) entry.getValue()[1] / runtime) * (double) 100);
                totalGCCount += entry.getValue()[0];
                totalGCTime += entry.getValue()[1];
            }
            exporter.write("TOTAL_GCs", "Count", totalGCCount);

            exporter.write("TOTAL_GC_TIME", "Time(ms)", totalGCTime);
            exporter.write("TOTAL_GC_TIME_%", "Time(%)", ((double) totalGCTime / runtime) * (double) 100);
            if (statusthread != null && statusthread.trackJVMStats()) {
                exporter.write("MAX_MEM_USED", "MBs", statusthread.getMaxUsedMem());
                exporter.write("MIN_MEM_USED", "MBs", statusthread.getMinUsedMem());
                exporter.write("MAX_THREADS", "Count", statusthread.getMaxThreads());
                exporter.write("MIN_THREADS", "Count", statusthread.getMinThreads());
                exporter.write("MAX_SYS_LOAD_AVG", "Load", statusthread.getMaxLoadAvg());
                exporter.write("MIN_SYS_LOAD_AVG", "Load", statusthread.getMinLoadAvg());
            }

            Measurements.getMeasurements().exportMeasurements(exporter);
        } finally {
            if (out != null) {
                out.close();
            }
            if (exporter != null) {
                exporter.close();
            }
        }
    }

    private static List<ClientThread> initDb(String dbname, Properties props, int threadcount,
                                             double targetperthreadperms, Workload workload, Tracer tracer,
                                             CountDownLatch completeLatch) {
        boolean initFailed = false;
        boolean dotransactions = Boolean.parseBoolean(props.getProperty(DO_TRANSACTIONS_PROPERTY, String.valueOf(true)));

        final List<ClientThread> clients = new ArrayList<>(threadcount);
        try (final TraceScope span = tracer.newScope(CLIENT_INIT_SPAN)) {
            int opcount;
            if (dotransactions) {
                opcount = Integer.parseInt(props.getProperty(OPERATION_COUNT_PROPERTY, "0"));
            } else {
                if (props.containsKey(INSERT_COUNT_PROPERTY)) {
                    opcount = Integer.parseInt(props.getProperty(INSERT_COUNT_PROPERTY, "0"));
                } else {
                    opcount = Integer.parseInt(props.getProperty(RECORD_COUNT_PROPERTY, DEFAULT_RECORD_COUNT));
                }
            }
            if (threadcount > opcount) {
                threadcount = opcount;
                XxlJobHelper.log("Warning: the threadcount is bigger than recordcount, the threadcount will be recordcount!");
            }
            for (int threadid = 0; threadid < threadcount; threadid++) {
                DB db;
                try {
                    db = DBFactory.newDB(dbname, props, tracer);
                } catch (UnknownDBException e) {
                    XxlJobHelper.log("Unknown DB " + dbname);
                    initFailed = true;
                    break;
                }

                int threadopcount = opcount / threadcount;

                // ensure correct number of operations, in case opcount is not a multiple of threadcount
                if (threadid < opcount % threadcount) {
                    ++threadopcount;
                }

                ClientThread t = new ClientThread(db, dotransactions, workload, props, threadopcount, targetperthreadperms, completeLatch);
                t.setThreadId(threadid);
                t.setThreadCount(threadcount);
                clients.add(t);
            }

            if (initFailed) {
                XxlJobHelper.log("Error initializing datastore bindings.");
                throw new YcsbException("Error initializing datastore bindings.");
            }
        }
        return clients;
    }

    private Tracer getTracer(Properties props, Workload workload) {
        return new Tracer.Builder("YCSB " + workload.getClass().getSimpleName())
                .conf(getHTraceConfiguration(props))
                .build();
    }

    private void initWorkload(Properties props, Thread warningthread, Workload workload, Tracer tracer) {
        try {
            try (final TraceScope span = tracer.newScope(CLIENT_WORKLOAD_INIT_SPAN)) {
                workload.init(props);
                warningthread.interrupt();
            }
        } catch (WorkloadException e) {
            XxlJobHelper.log(e);
            throw new YcsbException(e);
        }
    }

    private HTraceConfiguration getHTraceConfiguration(Properties props) {
        final Map<String, String> filteredProperties = new HashMap<>();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith(HTRACE_KEY_PREFIX)) {
                filteredProperties.put(key.substring(HTRACE_KEY_PREFIX.length()), props.getProperty(key));
            }
        }
        return HTraceConfiguration.fromMap(filteredProperties);
    }

    private Thread setupWarningThread() {
        //show a warning message that creating the workload is taking a while
        //but only do so if it is taking longer than 2 seconds
        //(showing the message right away if the setup wasn't taking very long was confusing people)
        return new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return;
            }
            XxlJobHelper.log(" (might take a few minutes for large data sets)");
        });
    }

    public void toStop() {
        this.terminator = new TerminatorThread(2, threads.keySet(), workload);
        this.terminator.start();
        try {
            this.terminator.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        toStop = true;
    }

    @Override
    public void run() {
        while (!toStop) {
            Properties props = parseArguments(this.ycsbArgs);

            boolean status = Boolean.parseBoolean(props.getProperty(STATUS_PROPERTY, String.valueOf(false)));
            String label = props.getProperty(LABEL_PROPERTY, "");

            long maxExecutionTime = Integer.parseInt(props.getProperty(MAX_EXECUTION_TIME, "0"));

            //get number of threads, target and db
            int threadCount = Integer.parseInt(props.getProperty(THREAD_COUNT_PROPERTY, "1"));
            String dbname = props.getProperty(DB_PROPERTY, "com.leo.ycsb.BasicDB");
            int target = Integer.parseInt(props.getProperty(TARGET_PROPERTY, "0"));

            //compute the target throughput
            double targetPerThreadPerms = -1;
            if (target > 0) {
                double targetPerThread = ((double) target) / ((double) threadCount);
                targetPerThreadPerms = targetPerThread / 1000.0;
            }

            Thread warningThread = setupWarningThread();
            warningThread.start();

            Measurements.setProperties(props);

            workload = getWorkload(props);

            final Tracer tracer = getTracer(props, workload);
            initWorkload(props, warningThread, workload, tracer);

            final CountDownLatch completeLatch = new CountDownLatch(threadCount);

            final List<ClientThread> clients = initDb(dbname, props, threadCount, targetPerThreadPerms, workload, tracer, completeLatch);

            if (status) {
                boolean standardStatus = false;
                if (props.getProperty(Measurements.MEASUREMENT_TYPE_PROPERTY, "").compareTo("timeseries") == 0) {
                    standardStatus = true;
                }
                int statusIntervalSeconds = Integer.parseInt(props.getProperty("status.interval", "10"));
                boolean trackJVMStats = "true".equals(props.getProperty(Measurements.MEASUREMENT_TRACK_JVM_PROPERTY,
                        Measurements.MEASUREMENT_TRACK_JVM_PROPERTY_DEFAULT));
                statusthread = new StatusThread(completeLatch, clients, label, standardStatus, statusIntervalSeconds, trackJVMStats);
                statusthread.start();
            }

            long st;
            long en;
            int opsDone;
            try (final TraceScope span = tracer.newScope(CLIENT_WORKLOAD_SPAN)) {

                this.threads = new HashMap<>(threadCount);
                for (ClientThread client : clients) {
                    threads.put(new Thread(tracer.wrap(client, "ClientThread")), client);
                }

                st = System.currentTimeMillis();

                for (Thread t : threads.keySet()) {
                    t.start();
                }


                if (maxExecutionTime > 0) {
                    terminator = new TerminatorThread(maxExecutionTime, threads.keySet(), workload);
                    terminator.start();
                }

                opsDone = 0;

                for (Entry<Thread, ClientThread> entry : threads.entrySet()) {
                    try {
                        entry.getKey().join();
                        opsDone += entry.getValue().getOpsDone();
                    } catch (InterruptedException ignored) {
                        // ignored
                    }
                }

                en = System.currentTimeMillis();
            }

            try {
                try (final TraceScope span = tracer.newScope(CLIENT_CLEANUP_SPAN)) {

                    if (terminator != null && !terminator.isInterrupted()) {
                        terminator.interrupt();
                    }

                    if (status) {
                        // wake up status thread if it's asleep
                        statusthread.interrupt();
                        // at this point we assume all the monitored threads are already gone as per above join loop.
                        try {
                            statusthread.join();
                        } catch (InterruptedException ignored) {
                            // ignored
                        }
                    }

                    workload.cleanup();
                }
            } catch (WorkloadException e) {
                XxlJobHelper.log(e);
                throw new YcsbException(e);
            }

            try {
                try (final TraceScope span = tracer.newScope(CLIENT_EXPORT_MEASUREMENTS_SPAN)) {
                    exportMeasurements(props, opsDone, en - st);
                }
            } catch (IOException e) {
                XxlJobHelper.log("Could not export measurements, error: " + e.getMessage());
                throw new YcsbException("Could not export measurements, error: " + e.getMessage());
            }

        }

    }

}
