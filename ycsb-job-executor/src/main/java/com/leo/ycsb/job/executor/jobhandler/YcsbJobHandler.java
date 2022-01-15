package com.leo.ycsb.job.executor.jobhandler;

import com.leo.ycsb.Client;
import com.leo.ycsb.YcsbDbClientThreadThread;
import com.leo.ycsb.job.core.context.XxlJobHelper;
import com.leo.ycsb.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * @author leojie
 */
@Component
public class YcsbJobHandler {
    private static final Logger logger = LoggerFactory.getLogger(YcsbJobHandler.class);
    private YcsbDbClientThreadThread dbClientThread = null;

    @XxlJob(value = "HBaseJobHandler", init = "init", destroy = "destroy")
    public void hbaseJobHandler() throws Exception {
        // param parse
        String param = XxlJobHelper.getJobParam();
        if (param == null || param.trim().length() == 0) {
            Client.usageMessage();
            XxlJobHelper.handleFail();
            return;
        }
       String[] args = Arrays.stream(param.split("\\s+"))
                .filter(x -> x != null && x.trim().length()>0).toArray(String[]::new);
       //  Client.mainWork(args);
        dbClientThread = new YcsbDbClientThreadThread(args);
        dbClientThread.start();
        try {
            dbClientThread.join();
        }catch (InterruptedException ex) {
            logger.info("Db client thread has stopped.");
        }
    }

    public void init() {
        logger.info("YcsbJobHandler init successfully.");
    }

    public void destroy() {
        logger.info("YcsbJobHandler destroy ...");
        dbClientThread.toStop();
        logger.info("YcsbJobHandler destroy successfully.");
    }
}
