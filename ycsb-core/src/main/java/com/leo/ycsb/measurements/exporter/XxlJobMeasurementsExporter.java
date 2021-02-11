package com.leo.ycsb.measurements.exporter;

import com.leo.ycsb.job.core.context.XxlJobHelper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * @author leojie 2021/2/11 2:32 下午
 */
public class XxlJobMeasurementsExporter implements MeasurementsExporter {
    private final BufferedWriter bw;

    public XxlJobMeasurementsExporter(OutputStream os){
        this.bw = new BufferedWriter(new OutputStreamWriter(os));
    }

    @Override
    public void write(String metric, String measurement, int i) throws IOException {
        XxlJobHelper.log("[" + metric + "], " + measurement + ", " + i);
        bw.write("[" + metric + "], " + measurement + ", " + i);
        bw.newLine();
    }

    @Override
    public void write(String metric, String measurement, long i) throws IOException {
        XxlJobHelper.log("[" + metric + "], " + measurement + ", " + i);
        bw.write("[" + metric + "], " + measurement + ", " + i);
        bw.newLine();
    }

    @Override
    public void write(String metric, String measurement, double d) throws IOException {
        XxlJobHelper.log("[" + metric + "], " + measurement + ", " + d);
        bw.write("[" + metric + "], " + measurement + ", " + d);
        bw.newLine();
    }

    @Override
    public void close() throws IOException {
        XxlJobHelper.log("XxlJobMeasurementsExporter is closed.");
        this.bw.close();
    }
}
