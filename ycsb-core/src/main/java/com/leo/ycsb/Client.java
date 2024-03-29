///**
// * Copyright (c) 2010-2016 Yahoo! Inc., 2017 YCSB contributors All rights reserved.
// * <p>
// * Licensed under the Apache License, Version 2.0 (the "License"); you
// * may not use this file except in compliance with the License. You
// * may obtain a copy of the License at
// * <p>
// * http://www.apache.org/licenses/LICENSE-2.0
// * <p>
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
// * implied. See the License for the specific language governing
// * permissions and limitations under the License. See accompanying
// * LICENSE file.
// */
//
//package com.leo.ycsb;
//
//import com.leo.ycsb.job.core.context.XxlJobHelper;
//import com.leo.ycsb.measurements.exporter.XxlJobMeasurementsExporter;
//import org.apache.htrace.core.HTraceConfiguration;
//import org.apache.htrace.core.TraceScope;
//import org.apache.htrace.core.Tracer;
//import com.leo.ycsb.measurements.Measurements;
//import com.leo.ycsb.measurements.exporter.MeasurementsExporter;
//
//import java.io.*;
//import java.util.*;
//import java.util.Map.Entry;
//import java.util.concurrent.CountDownLatch;
//
///**
// * Main class for executing YCSB.
// */
//public final class Client {
//    private Client() {
//    }
//
//    /**
//     * 默认记录数
//     */
//    public static final String DEFAULT_RECORD_COUNT = "0";
//
//    /**
//     * The target number of operations to perform.
//     */
//    public static final String OPERATION_COUNT_PROPERTY = "operationcount";
//
//    /**
//     * The number of records to load into the database initially.
//     */
//    public static final String RECORD_COUNT_PROPERTY = "recordcount";
//
//    /**
//     * The workload class to be loaded.
//     */
//    public static final String WORKLOAD_PROPERTY = "workload";
//
//    /**
//     * The database class to be used.
//     */
//    public static final String DB_PROPERTY = "db";
//
//    /**
//     * The exporter class to be used. The default is
//     * com.leo.ycsb.measurements.exporter.XxlJobMeasurementsExporter.
//     */
//    public static final String EXPORTER_PROPERTY = "exporter";
//
//    /**
//     * If set to the path of a file, YCSB will write all output to this file
//     * instead of STDOUT.
//     */
//    public static final String EXPORT_FILE_PROPERTY = "exportfile";
//
//    /**
//     * The number of YCSB client threads to run.
//     */
//    public static final String THREAD_COUNT_PROPERTY = "threadcount";
//
//    /**
//     * Indicates how many inserts to do if less than recordcount.
//     * Useful for partitioning the load among multiple servers if the client is the bottleneck.
//     * Additionally workloads should support the "insertstart" property which tells them which record to start at.
//     */
//    public static final String INSERT_COUNT_PROPERTY = "insertcount";
//
//    /**
//     * Target number of operations per second.
//     */
//    public static final String TARGET_PROPERTY = "target";
//
//    /**
//     * The maximum amount of time (in seconds) for which the benchmark will be run.
//     */
//    public static final String MAX_EXECUTION_TIME = "maxexecutiontime";
//
//    /**
//     * Whether or not this is the transaction phase (run) or not (load).
//     */
//    public static final String DO_TRANSACTIONS_PROPERTY = "dotransactions";
//
//    /**
//     * Whether or not to show status during run.
//     */
//    public static final String STATUS_PROPERTY = "status";
//
//    /**
//     * Use label for status (e.g. to label one experiment out of a whole batch).
//     */
//    public static final String LABEL_PROPERTY = "label";
//
//    /**
//     * An optional thread used to track progress and measure JVM stats.
//     */
//    private static StatusThread statusthread = null;
//
//    private static volatile boolean toStop = false;
//
//    // HTrace integration related constants.
//
//    /**
//     * All keys for configuring the tracing system start with this prefix.
//     */
//    private static final String HTRACE_KEY_PREFIX = "htrace.";
//    private static final String CLIENT_WORKLOAD_INIT_SPAN = "Client#workload_init";
//    private static final String CLIENT_INIT_SPAN = "Client#init";
//    private static final String CLIENT_WORKLOAD_SPAN = "Client#workload";
//    private static final String CLIENT_CLEANUP_SPAN = "Client#cleanup";
//    private static final String CLIENT_EXPORT_MEASUREMENTS_SPAN = "Client#export_measurements";
//
//    public static void usageMessage() {
//        XxlJobHelper.log("Options:");
//        XxlJobHelper.log("  -threads n: execute using n threads (default: 1) - can also be specified as the \n" +
//                "        \"threadcount\" property using -p");
//        XxlJobHelper.log("  -target n: attempt to do n operations per second (default: unlimited) - can also\n" +
//                "       be specified as the \"target\" property using -p");
//        XxlJobHelper.log("  -load:  run the loading phase of the workload");
//        XxlJobHelper.log("  -t:  run the transactions phase of the workload (default)");
//        XxlJobHelper.log("  -db dbname: specify the name of the DB to use (default: com.leo.ycsb.BasicDB) - \n" +
//                "        can also be specified as the \"db\" property using -p");
//        XxlJobHelper.log("  -P propertyfile: load properties from the given file. Multiple files can");
//        XxlJobHelper.log("           be specified, and will be processed in the order specified");
//        XxlJobHelper.log("  -p name=value:  specify a property to be passed to the DB and workloads;");
//        XxlJobHelper.log("          multiple properties can be specified, and override any");
//        XxlJobHelper.log("          values in the propertyfile");
//        XxlJobHelper.log("  -s:  show status during run (default: no status)");
//        XxlJobHelper.log("  -l label:  use label for status (e.g. to label one experiment out of a whole batch)");
//        XxlJobHelper.log("");
//        XxlJobHelper.log("Required properties:");
//        XxlJobHelper.log("  " + WORKLOAD_PROPERTY + ": the name of the workload class to use (e.g. " +
//                "com.leo.ycsb.workloads.CoreWorkload)");
//        XxlJobHelper.log("");
//        XxlJobHelper.log("To run the transaction phase from multiple servers, start a separate client on each.");
//        XxlJobHelper.log("To run the load phase from multiple servers, start a separate client on each; additionally,");
//        XxlJobHelper.log("use the \"insertcount\" and \"insertstart\" properties to divide up the records " +
//                "to be inserted");
//    }
//
//    public static boolean checkRequiredProperties(Properties props) {
//        if (props.getProperty(WORKLOAD_PROPERTY) == null) {
//            throw new YcsbException("Missing property: " + WORKLOAD_PROPERTY);
//        }
//        return true;
//    }
//
//
//    /**
//     * Exports the measurements to either sysout or a file using the exporter
//     * loaded from conf.
//     *
//     * @throws IOException Either failed to write to output stream or failed to close it.
//     */
//    private static void exportMeasurements(Properties props, int opcount, long runtime)
//            throws IOException {
//        MeasurementsExporter exporter = null;
//        try {
//            // if no destination file is provided the results will be written to stdout
//            OutputStream out;
//            String exportFile = props.getProperty(EXPORT_FILE_PROPERTY);
//            if (exportFile == null) {
//                out = System.out;
//            } else {
//                out = new FileOutputStream(exportFile);
//            }
//
//            // if no exporter is provided the default text one will be used
//            String exporterStr = props.getProperty(EXPORTER_PROPERTY, "com.leo.ycsb.measurements.exporter.XxlJobMeasurementsExporter");
//            try {
//                exporter = (MeasurementsExporter) Class.forName(exporterStr).getConstructor(OutputStream.class).newInstance(out);
//            } catch (Exception e) {
//                XxlJobHelper.log("Could not find exporter " + exporterStr + ", will use default text reporter.");
//                exporter = new XxlJobMeasurementsExporter(out);
//            }
//
//            exporter.write("OVERALL", "RunTime(ms)", runtime);
//            double throughput = 1000.0 * (opcount) / (runtime);
//            exporter.write("OVERALL", "Throughput(ops/sec)", throughput);
//
//            final Map<String, Long[]> gcs = Utils.getGCStatst();
//            long totalGCCount = 0;
//            long totalGCTime = 0;
//            for (final Entry<String, Long[]> entry : gcs.entrySet()) {
//                exporter.write("TOTAL_GCS_" + entry.getKey(), "Count", entry.getValue()[0]);
//                exporter.write("TOTAL_GC_TIME_" + entry.getKey(), "Time(ms)", entry.getValue()[1]);
//                exporter.write("TOTAL_GC_TIME_%_" + entry.getKey(), "Time(%)",
//                        ((double) entry.getValue()[1] / runtime) * (double) 100);
//                totalGCCount += entry.getValue()[0];
//                totalGCTime += entry.getValue()[1];
//            }
//            exporter.write("TOTAL_GCs", "Count", totalGCCount);
//
//            exporter.write("TOTAL_GC_TIME", "Time(ms)", totalGCTime);
//            exporter.write("TOTAL_GC_TIME_%", "Time(%)", ((double) totalGCTime / runtime) * (double) 100);
//            if (statusthread != null && statusthread.trackJVMStats()) {
//                exporter.write("MAX_MEM_USED", "MBs", statusthread.getMaxUsedMem());
//                exporter.write("MIN_MEM_USED", "MBs", statusthread.getMinUsedMem());
//                exporter.write("MAX_THREADS", "Count", statusthread.getMaxThreads());
//                exporter.write("MIN_THREADS", "Count", statusthread.getMinThreads());
//                exporter.write("MAX_SYS_LOAD_AVG", "Load", statusthread.getMaxLoadAvg());
//                exporter.write("MIN_SYS_LOAD_AVG", "Load", statusthread.getMinLoadAvg());
//            }
//
//            Measurements.getMeasurements().exportMeasurements(exporter);
//        } finally {
//            if (exporter != null) {
//                exporter.close();
//            }
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    public static void mainWork(String[] args) {
//        Properties props = parseArguments(args);
//
//        boolean status = Boolean.parseBoolean(props.getProperty(STATUS_PROPERTY, String.valueOf(false)));
//        String label = props.getProperty(LABEL_PROPERTY, "");
//
//        long maxExecutionTime = Integer.parseInt(props.getProperty(MAX_EXECUTION_TIME, "0"));
//
//        //get number of threads, target and db
//        int threadCount = Integer.parseInt(props.getProperty(THREAD_COUNT_PROPERTY, "1"));
//        String dbname = props.getProperty(DB_PROPERTY, "com.leo.ycsb.BasicDB");
//        int target = Integer.parseInt(props.getProperty(TARGET_PROPERTY, "0"));
//
//        //compute the target throughput
//        double targetPerThreadPerms = -1;
//        if (target > 0) {
//            double targetPerThread = ((double) target) / ((double) threadCount);
//            targetPerThreadPerms = targetPerThread / 1000.0;
//        }
//
//        Thread warningThread = setupWarningThread();
//        warningThread.start();
//
//        Measurements.setProperties(props);
//
//        Workload workload = getWorkload(props);
//
//        final Tracer tracer = getTracer(props, workload);
//
//        initWorkload(props, warningThread, workload, tracer);
//
//        final CountDownLatch completeLatch = new CountDownLatch(threadCount);
//
//        final List<ClientThread> clients = initDb(dbname, props, threadCount, targetPerThreadPerms,
//                workload, tracer, completeLatch);
//
//        if (status) {
//            boolean standardStatus = false;
//            if (props.getProperty(Measurements.MEASUREMENT_TYPE_PROPERTY, "").compareTo("timeseries") == 0) {
//                standardStatus = true;
//            }
//            int statusIntervalSeconds = Integer.parseInt(props.getProperty("status.interval", "10"));
//            boolean trackJVMStats = "true".equals(props.getProperty(Measurements.MEASUREMENT_TRACK_JVM_PROPERTY,
//                    Measurements.MEASUREMENT_TRACK_JVM_PROPERTY_DEFAULT));
//            statusthread = new StatusThread(completeLatch, clients, label, standardStatus, statusIntervalSeconds, trackJVMStats);
//            statusthread.start();
//        }
//
//        Thread terminator = null;
//        // Thread toStopThread = null;
//        long st;
//        long en;
//        int opsDone;
//
//        try (final TraceScope span = tracer.newScope(CLIENT_WORKLOAD_SPAN)) {
//
//            final Map<Thread, ClientThread> threads = new HashMap<>(threadCount);
//            for (ClientThread client : clients) {
//                threads.put(new Thread(tracer.wrap(client, "ClientThread")), client);
//            }
//
//            st = System.currentTimeMillis();
//
//            for (Thread t : threads.keySet()) {
//                t.start();
//            }
//
//          /*  toStopThread = new StopYcsbProcessThread(threads.keySet(), workload);
//            while (toStop) {
//                toStopThread.start();
//            }*/
//
//            if (maxExecutionTime > 0) {
//                terminator = new TerminatorThread(maxExecutionTime, threads.keySet(), workload);
//                terminator.start();
//            }
//
//            opsDone = 0;
//
//            for (Entry<Thread, ClientThread> entry : threads.entrySet()) {
//                try {
//                    entry.getKey().join();
//                    opsDone += entry.getValue().getOpsDone();
//                } catch (InterruptedException ignored) {
//                    // ignored
//                }
//            }
//
//            en = System.currentTimeMillis();
//        }
//
//        try {
//            try (final TraceScope span = tracer.newScope(CLIENT_CLEANUP_SPAN)) {
//
//                if (terminator != null && !terminator.isInterrupted()) {
//                    terminator.interrupt();
//                }
//
//                if (status) {
//                    // wake up status thread if it's asleep
//                    statusthread.interrupt();
//                    // at this point we assume all the monitored threads are already gone as per above join loop.
//                    try {
//                        statusthread.join();
//                    } catch (InterruptedException ignored) {
//                        // ignored
//                    }
//                }
//
//                workload.cleanup();
//            }
//        } catch (WorkloadException e) {
//            XxlJobHelper.log(e);
//            throw new YcsbException(e);
//        }
//
//        try {
//            try (final TraceScope span = tracer.newScope(CLIENT_EXPORT_MEASUREMENTS_SPAN)) {
//                exportMeasurements(props, opsDone, en - st);
//            }
//        } catch (IOException e) {
//            XxlJobHelper.log("Could not export measurements, error: " + e.getMessage());
//            throw new YcsbException("Could not export measurements, error: " + e.getMessage());
//        }
//
//    }
//
//    private static List<ClientThread> initDb(String dbname, Properties props, int threadcount,
//                                             double targetperthreadperms, Workload workload, Tracer tracer,
//                                             CountDownLatch completeLatch) {
//        boolean initFailed = false;
//        boolean dotransactions = Boolean.parseBoolean(props.getProperty(DO_TRANSACTIONS_PROPERTY, String.valueOf(true)));
//
//        final List<ClientThread> clients = new ArrayList<>(threadcount);
//        try (final TraceScope span = tracer.newScope(CLIENT_INIT_SPAN)) {
//            int opcount;
//            if (dotransactions) {
//                opcount = Integer.parseInt(props.getProperty(OPERATION_COUNT_PROPERTY, "0"));
//            } else {
//                if (props.containsKey(INSERT_COUNT_PROPERTY)) {
//                    opcount = Integer.parseInt(props.getProperty(INSERT_COUNT_PROPERTY, "0"));
//                } else {
//                    opcount = Integer.parseInt(props.getProperty(RECORD_COUNT_PROPERTY, DEFAULT_RECORD_COUNT));
//                }
//            }
//            if (threadcount > opcount) {
//                threadcount = opcount;
//                XxlJobHelper.log("Warning: the threadcount is bigger than recordcount, the threadcount will be recordcount!");
//            }
//            for (int threadid = 0; threadid < threadcount; threadid++) {
//                DB db;
//                try {
//                    db = DBFactory.newDB(dbname, props, tracer);
//                } catch (UnknownDBException e) {
//                    XxlJobHelper.log("Unknown DB " + dbname);
//                    initFailed = true;
//                    break;
//                }
//
//                int threadopcount = opcount / threadcount;
//
//                // ensure correct number of operations, in case opcount is not a multiple of threadcount
//                if (threadid < opcount % threadcount) {
//                    ++threadopcount;
//                }
//
//                ClientThread t = new ClientThread(db, dotransactions, workload, props, threadopcount, targetperthreadperms, completeLatch);
//                t.setThreadId(threadid);
//                t.setThreadCount(threadcount);
//                clients.add(t);
//            }
//
//            if (initFailed) {
//                XxlJobHelper.log("Error initializing datastore bindings.");
//                throw new YcsbException("Error initializing datastore bindings.");
//            }
//        }
//        return clients;
//    }
//
//    private static Tracer getTracer(Properties props, Workload workload) {
//        return new Tracer.Builder("YCSB " + workload.getClass().getSimpleName())
//                .conf(getHTraceConfiguration(props))
//                .build();
//    }
//
//    private static void initWorkload(Properties props, Thread warningthread, Workload workload, Tracer tracer) {
//        try {
//            try (final TraceScope span = tracer.newScope(CLIENT_WORKLOAD_INIT_SPAN)) {
//                workload.init(props);
//                warningthread.interrupt();
//            }
//        } catch (WorkloadException e) {
//            XxlJobHelper.log(e);
//            throw new YcsbException(e);
//        }
//    }
//
//    private static HTraceConfiguration getHTraceConfiguration(Properties props) {
//        final Map<String, String> filteredProperties = new HashMap<>();
//        for (String key : props.stringPropertyNames()) {
//            if (key.startsWith(HTRACE_KEY_PREFIX)) {
//                filteredProperties.put(key.substring(HTRACE_KEY_PREFIX.length()), props.getProperty(key));
//            }
//        }
//        return HTraceConfiguration.fromMap(filteredProperties);
//    }
//
//    private static Thread setupWarningThread() {
//        //show a warning message that creating the workload is taking a while
//        //but only do so if it is taking longer than 2 seconds
//        //(showing the message right away if the setup wasn't taking very long was confusing people)
//        return new Thread(() -> {
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                return;
//            }
//            XxlJobHelper.log(" (might take a few minutes for large data sets)");
//        });
//    }
//
//    private static Workload getWorkload(Properties props) {
//        ClassLoader classLoader = Client.class.getClassLoader();
//        XxlJobHelper.log("YCSB Client is 0.17.0");
//        XxlJobHelper.log("Loading workload...");
//
//        try {
//            Class workloadClass = classLoader.loadClass(props.getProperty(WORKLOAD_PROPERTY));
//            return (Workload) workloadClass.newInstance();
//        } catch (Exception e) {
//            XxlJobHelper.log(e);
//            throw new YcsbException(e);
//        }
//    }
//
//    private static Properties parseArguments(String[] args) {
//        Properties props = new Properties();
//        XxlJobHelper.log("Command line:");
//        for (String arg : args) {
//            XxlJobHelper.log(" " + arg);
//        }
//
//        Properties fileprops = new Properties();
//        int argindex = 0;
//
//        if (args.length == 0) {
//            usageMessage();
//            throw new YcsbException("At least one argument specifying a workload is required.");
//        }
//
//        while (args[argindex].startsWith("-")) {
//            if (args[argindex].compareTo("-threads") == 0) {
//                argindex++;
//                if (argindex >= args.length) {
//                    usageMessage();
//                    throw new YcsbException("Missing argument value for -threads.");
//                }
//                int tcount = Integer.parseInt(args[argindex]);
//                props.setProperty(THREAD_COUNT_PROPERTY, String.valueOf(tcount));
//                argindex++;
//            } else if (args[argindex].compareTo("-target") == 0) {
//                argindex++;
//                if (argindex >= args.length) {
//                    usageMessage();
//                    throw new YcsbException("Missing argument value for -target.");
//                }
//                int ttarget = Integer.parseInt(args[argindex]);
//                props.setProperty(TARGET_PROPERTY, String.valueOf(ttarget));
//                argindex++;
//            } else if (args[argindex].compareTo("-load") == 0) {
//                props.setProperty(DO_TRANSACTIONS_PROPERTY, String.valueOf(false));
//                argindex++;
//            } else if (args[argindex].compareTo("-t") == 0) {
//                props.setProperty(DO_TRANSACTIONS_PROPERTY, String.valueOf(true));
//                argindex++;
//            } else if (args[argindex].compareTo("-s") == 0) {
//                props.setProperty(STATUS_PROPERTY, String.valueOf(true));
//                argindex++;
//            } else if (args[argindex].compareTo("-db") == 0) {
//                argindex++;
//                if (argindex >= args.length) {
//                    usageMessage();
//                    throw new YcsbException("Missing argument value for -db.");
//                }
//                props.setProperty(DB_PROPERTY, args[argindex]);
//                argindex++;
//            } else if (args[argindex].compareTo("-l") == 0) {
//                argindex++;
//                if (argindex >= args.length) {
//                    usageMessage();
//                    throw new YcsbException("Missing argument value for -l.");
//                }
//                props.setProperty(LABEL_PROPERTY, args[argindex]);
//                argindex++;
//            } else if (args[argindex].compareTo("-P") == 0) {
//                argindex++;
//                if (argindex >= args.length) {
//                    usageMessage();
//                    throw new YcsbException("Missing argument value for -P.");
//                }
//                String workloadFile = args[argindex];
//                argindex++;
//
//                Properties workloadProps = new Properties();
//                try {
//                    workloadProps.load(new FileReader(workloadFile));
//                } catch (IOException e) {
//                    XxlJobHelper.log("load workload file " + workloadFile + " failed!");
//                    try {
//                        workloadFile = System.getProperty("user.dir") + "/conf/workloads/" + workloadFile;
//                        workloadProps.load(new FileReader(workloadFile));
//                    } catch (IOException ex) {
//                        XxlJobHelper.log(ex.getMessage());
//                        throw new YcsbException("Unable to load the properties file " + workloadFile);
//                    }
//                }
//                XxlJobHelper.log("load workload file " + workloadFile + " successfully!");
//
//                //Issue #5 - remove call to stringPropertyNames to make compilable under Java 1.5
//                for (Enumeration e = workloadProps.propertyNames(); e.hasMoreElements(); ) {
//                    String prop = (String) e.nextElement();
//                    fileprops.setProperty(prop, workloadProps.getProperty(prop));
//                }
//
//            } else if (args[argindex].compareTo("-p") == 0) {
//                argindex++;
//                if (argindex >= args.length) {
//                    usageMessage();
//                    throw new YcsbException("Missing argument value for -p");
//                }
//                int eq = args[argindex].indexOf('=');
//                if (eq < 0) {
//                    usageMessage();
//                    throw new YcsbException("Argument '-p' expected to be in key=value format (e.g., -p operationcount=99999)");
//                }
//
//                String name = args[argindex].substring(0, eq);
//                String value = args[argindex].substring(eq + 1);
//                props.put(name, value);
//                argindex++;
//            } else {
//                usageMessage();
//                throw new YcsbException("Unknown option " + args[argindex]);
//            }
//
//            if (argindex >= args.length) {
//                break;
//            }
//        }
//
//        if (argindex != args.length) {
//            usageMessage();
//            if (argindex < args.length) {
//                throw new YcsbException("An argument value without corresponding argument specifier (e.g., -p, -s) was found. "
//                        + "We expected an argument specifier and instead found " + args[argindex]);
//            } else {
//                throw new YcsbException("An argument specifier without corresponding value was found at the end of the supplied " +
//                        "command line arguments.");
//            }
//        }
//
//        //overwrite file properties with properties from the command line
//
//        //Issue #5 - remove call to stringPropertyNames to make compilable under Java 1.5
//        for (Enumeration e = props.propertyNames(); e.hasMoreElements(); ) {
//            String prop = (String) e.nextElement();
//
//            fileprops.setProperty(prop, props.getProperty(prop));
//        }
//
//        props = fileprops;
//
//        if (!checkRequiredProperties(props)) {
//            throw new YcsbException("Failed check required properties.");
//        }
//
//        return props;
//    }
//
//    public static void toStop() {
//        toStop = true;
//    }
//
//
//}
