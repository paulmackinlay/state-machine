/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TestApp extends AbstractAppService<TestAppContext> {

  public TestApp(String[] args) {
    super(new TestAppContext(TestApp.class.getSimpleName(), args));
  }

  public static void main(String[] args) {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    try {
      String[] args1 = Arrays.copyOf(args, args.length + 1);
      args1[args.length] = "test-arg";
      TestApp testApp = new TestApp(args1);
      scheduledExecutorService.schedule(testApp::stop, 3, TimeUnit.SECONDS);
      testApp.start();
    } finally {
      scheduledExecutorService.shutdownNow();
    }
  }
}
