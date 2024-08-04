/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TestApp extends AbstractAppService<TestAppContext> {

  protected TestApp(String[] args) {
    super(new TestAppContext(TestApp.class.getSimpleName(), args));
  }

  public static void main(String[] args) {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    try {
      TestApp testApp = new TestApp(args);
      scheduledExecutorService.schedule(testApp::stop, 3, TimeUnit.SECONDS);
      testApp.start();
    } finally {
      scheduledExecutorService.shutdownNow();
    }
  }
}
