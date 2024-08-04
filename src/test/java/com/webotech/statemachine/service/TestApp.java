/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestApp extends AbstractAppService<TestAppContext> {

  protected TestApp(String[] args) {
    super(new TestAppContext(TestApp.class.getSimpleName(), args));
  }

  public static void main(String[] args) {
    TestApp testApp = new TestApp(args);
    Executors.newSingleThreadScheduledExecutor().schedule(testApp::stop, 3, TimeUnit.SECONDS);
    testApp.start();
  }
}
