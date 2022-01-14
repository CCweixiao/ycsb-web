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

import java.util.Collection;

/**
 * A thread that waits to stop ycsb process and then interrupts all the client
 *
 * @author leo
 */
public class StopYcsbProcessThread extends Thread {

  private final Collection<? extends Thread> threads;
  private final Workload workload;
  private final long waitTimeOutInMS;

  public StopYcsbProcessThread(Collection<? extends Thread> threads, Workload workload) {
    this.threads = threads;
    this.workload = workload;
    waitTimeOutInMS = 2000;
  }

  @Override
  public void run() {
    System.err.println("Requesting stop for the workload.");
    XxlJobHelper.log("Requesting stop for the workload.");
    workload.requestStop();
    System.err.println("Stop requested for workload. Now Joining!");
    XxlJobHelper.log("Stop requested for workload. Now Joining!");
    for (Thread t : threads) {
      while (t.isAlive()) {
        try {
          t.join(waitTimeOutInMS);
          if (t.isAlive()) {
            System.out.println("Still waiting for thread " + t.getName() + " to complete. " +
                    "Workload status: " + workload.isStopRequested());
            XxlJobHelper.log("Still waiting for thread " + t.getName() + " to complete. " +
                    "Workload status: " + workload.isStopRequested());
          }
        } catch (InterruptedException e) {
          XxlJobHelper.log("Failed to stop for the workload.");
          XxlJobHelper.log(e.getMessage());
        }
      }
    }
  }
}
