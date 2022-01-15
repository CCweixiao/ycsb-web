package com.leo.ycsb;

import com.leo.ycsb.job.core.context.XxlJobHelper;

import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

/**
 * @author leojie 2022/1/14 11:39 下午
 */
public abstract class AbstractYcsbDbClientThread extends Thread {
    /**
     * The default record count
     */
    public static final String DEFAULT_RECORD_COUNT = "0";

    /**
     * The target number of operations to perform.
     */
    public static final String OPERATION_COUNT_PROPERTY = "operationcount";

    /**
     * The number of records to load into the database initially.
     */
    public static final String RECORD_COUNT_PROPERTY = "recordcount";

    /**
     * The workload class to be loaded.
     */
    public static final String WORKLOAD_PROPERTY = "workload";

    /**
     * The database class to be used.
     */
    public static final String DB_PROPERTY = "db";

    /**
     * The exporter class to be used. The default is
     * com.leo.ycsb.measurements.exporter.XxlJobMeasurementsExporter.
     */
    public static final String EXPORTER_PROPERTY = "exporter";

    /**
     * If set to the path of a file, YCSB will write all output to this file
     * instead of STDOUT.
     */
    public static final String EXPORT_FILE_PROPERTY = "exportfile";

    /**
     * The number of YCSB client threads to run.
     */
    public static final String THREAD_COUNT_PROPERTY = "threadcount";

    /**
     * Indicates how many inserts to do if less than recordcount.
     * Useful for partitioning the load among multiple servers if the client is the bottleneck.
     * Additionally workloads should support the "insertstart" property which tells them which record to start at.
     */
    public static final String INSERT_COUNT_PROPERTY = "insertcount";

    /**
     * Target number of operations per second.
     */
    public static final String TARGET_PROPERTY = "target";

    /**
     * The maximum amount of time (in seconds) for which the benchmark will be run.
     */
    public static final String MAX_EXECUTION_TIME = "maxexecutiontime";

    /**
     * Whether or not this is the transaction phase (run) or not (load).
     */
    public static final String DO_TRANSACTIONS_PROPERTY = "dotransactions";

    /**
     * Whether or not to show status during run.
     */
    public static final String STATUS_PROPERTY = "status";

    /**
     * Use label for status (e.g. to label one experiment out of a whole batch).
     */
    public static final String LABEL_PROPERTY = "label";

    /**
     * All keys for configuring the tracing system start with this prefix.
     */
    protected static final String HTRACE_KEY_PREFIX = "htrace.";
    protected static final String CLIENT_WORKLOAD_INIT_SPAN = "Client#workload_init";
    protected static final String CLIENT_INIT_SPAN = "Client#init";
    protected static final String CLIENT_WORKLOAD_SPAN = "Client#workload";
    protected static final String CLIENT_CLEANUP_SPAN = "Client#cleanup";
    protected static final String CLIENT_EXPORT_MEASUREMENTS_SPAN = "Client#export_measurements";

    public static void usageMessage() {
        XxlJobHelper.log("Options:");
        XxlJobHelper.log("  -threads n: execute using n threads (default: 1) - can also be specified as the \n" +
                "        \"threadcount\" property using -p");
        XxlJobHelper.log("  -target n: attempt to do n operations per second (default: unlimited) - can also\n" +
                "       be specified as the \"target\" property using -p");
        XxlJobHelper.log("  -load:  run the loading phase of the workload");
        XxlJobHelper.log("  -t:  run the transactions phase of the workload (default)");
        XxlJobHelper.log("  -db dbname: specify the name of the DB to use (default: com.leo.ycsb.BasicDB) - \n" +
                "        can also be specified as the \"db\" property using -p");
        XxlJobHelper.log("  -P propertyfile: load properties from the given file. Multiple files can");
        XxlJobHelper.log("           be specified, and will be processed in the order specified");
        XxlJobHelper.log("  -p name=value:  specify a property to be passed to the DB and workloads;");
        XxlJobHelper.log("          multiple properties can be specified, and override any");
        XxlJobHelper.log("          values in the propertyfile");
        XxlJobHelper.log("  -s:  show status during run (default: no status)");
        XxlJobHelper.log("  -l label:  use label for status (e.g. to label one experiment out of a whole batch)");
        XxlJobHelper.log("");
        XxlJobHelper.log("Required properties:");
        XxlJobHelper.log("  " + WORKLOAD_PROPERTY + ": the name of the workload class to use (e.g. " +
                "com.leo.ycsb.workloads.CoreWorkload)");
        XxlJobHelper.log("");
        XxlJobHelper.log("To run the transaction phase from multiple servers, start a separate client on each.");
        XxlJobHelper.log("To run the load phase from multiple servers, start a separate client on each; additionally,");
        XxlJobHelper.log("use the \"insertcount\" and \"insertstart\" properties to divide up the records " +
                "to be inserted");
    }

    public static boolean checkRequiredProperties(Properties props) {
        if (props.getProperty(WORKLOAD_PROPERTY) == null) {
            throw new YcsbException("Missing property: " + WORKLOAD_PROPERTY);
        }
        return true;
    }

    /**
     * 解析参数为YCSB所需配置项
     * @param args 传参
     * @return 配置
     */
    protected static Properties parseArguments(String[] args) {
        Properties props = new Properties();
        XxlJobHelper.log("Command line:");
        for (String arg : args) {
            XxlJobHelper.log(" " + arg);
        }

        Properties fileProps = new Properties();
        int argindex = 0;

        if (args.length == 0) {
            usageMessage();
            throw new YcsbException("At least one argument specifying a workload is required.");
        }

        while (args[argindex].startsWith("-")) {
            if (args[argindex].compareTo("-threads") == 0) {
                argindex++;
                if (argindex >= args.length) {
                    usageMessage();
                    throw new YcsbException("Missing argument value for -threads.");
                }
                int threadCount = Integer.parseInt(args[argindex]);
                props.setProperty(THREAD_COUNT_PROPERTY, String.valueOf(threadCount));
                argindex++;
            } else if (args[argindex].compareTo("-target") == 0) {
                argindex++;
                if (argindex >= args.length) {
                    usageMessage();
                    throw new YcsbException("Missing argument value for -target.");
                }
                int tTarget = Integer.parseInt(args[argindex]);
                props.setProperty(TARGET_PROPERTY, String.valueOf(tTarget));
                argindex++;
            } else if (args[argindex].compareTo("-load") == 0) {
                props.setProperty(DO_TRANSACTIONS_PROPERTY, String.valueOf(false));
                argindex++;
            } else if (args[argindex].compareTo("-t") == 0) {
                props.setProperty(DO_TRANSACTIONS_PROPERTY, String.valueOf(true));
                argindex++;
            } else if (args[argindex].compareTo("-s") == 0) {
                props.setProperty(STATUS_PROPERTY, String.valueOf(true));
                argindex++;
            } else if (args[argindex].compareTo("-db") == 0) {
                argindex++;
                if (argindex >= args.length) {
                    usageMessage();
                    throw new YcsbException("Missing argument value for -db.");
                }
                props.setProperty(DB_PROPERTY, args[argindex]);
                argindex++;
            } else if (args[argindex].compareTo("-l") == 0) {
                argindex++;
                if (argindex >= args.length) {
                    usageMessage();
                    throw new YcsbException("Missing argument value for -l.");
                }
                props.setProperty(LABEL_PROPERTY, args[argindex]);
                argindex++;
            } else if (args[argindex].compareTo("-P") == 0) {
                argindex++;
                if (argindex >= args.length) {
                    usageMessage();
                    throw new YcsbException("Missing argument value for -P.");
                }
                String workloadFile = args[argindex];
                argindex++;

                Properties workloadProps = new Properties();
                try {
                    workloadProps.load(new FileReader(workloadFile));
                } catch (IOException e) {
                    XxlJobHelper.log("load workload file " + workloadFile + " failed!");
                    try {
                        workloadFile = System.getProperty("user.dir") + "/conf/workloads/" + workloadFile;
                        workloadProps.load(new FileReader(workloadFile));
                    } catch (IOException ex) {
                        XxlJobHelper.log(ex.getMessage());
                        throw new YcsbException("Unable to load the properties file " + workloadFile);
                    }
                }
                XxlJobHelper.log("load workload file " + workloadFile + " successfully!");

                //Issue #5 - remove call to stringPropertyNames to make compilable under Java 1.5
                for (Enumeration<?> e = workloadProps.propertyNames(); e.hasMoreElements(); ) {
                    String prop = (String) e.nextElement();
                    fileProps.setProperty(prop, workloadProps.getProperty(prop));
                }

            } else if (args[argindex].compareTo("-p") == 0) {
                argindex++;
                if (argindex >= args.length) {
                    usageMessage();
                    throw new YcsbException("Missing argument value for -p");
                }
                int eq = args[argindex].indexOf('=');
                if (eq < 0) {
                    usageMessage();
                    throw new YcsbException("Argument '-p' expected to be in key=value format (e.g., -p operationcount=99999)");
                }

                String name = args[argindex].substring(0, eq);
                String value = args[argindex].substring(eq + 1);
                props.put(name, value);
                argindex++;
            } else {
                usageMessage();
                throw new YcsbException("Unknown option " + args[argindex]);
            }

            if (argindex >= args.length) {
                break;
            }
        }

        if (argindex != args.length) {
            usageMessage();
            if (argindex < args.length) {
                throw new YcsbException("An argument value without corresponding argument specifier (e.g., -p, -s) was found. "
                        + "We expected an argument specifier and instead found " + args[argindex]);
            } else {
                throw new YcsbException("An argument specifier without corresponding value was found at the end of the supplied " +
                        "command line arguments.");
            }
        }

        //overwrite file properties with properties from the command line

        //Issue #5 - remove call to stringPropertyNames to make compilable under Java 1.5
        for (Enumeration<?> e = props.propertyNames(); e.hasMoreElements(); ) {
            String prop = (String) e.nextElement();

            fileProps.setProperty(prop, props.getProperty(prop));
        }

        props = fileProps;

        if (!checkRequiredProperties(props)) {
            throw new YcsbException("Failed check required properties.");
        }

        return props;
    }

    protected static Workload getWorkload(Properties props) {
        ClassLoader classLoader = Client.class.getClassLoader();
        XxlJobHelper.log("YCSB Client is 0.17.0");
        XxlJobHelper.log("Loading workload...");

        try {
            Class<?> workloadClass = classLoader.loadClass(props.getProperty(WORKLOAD_PROPERTY));
            return (Workload) workloadClass.newInstance();
        } catch (Exception e) {
            XxlJobHelper.log(e);
            throw new YcsbException(e);
        }
    }

}
