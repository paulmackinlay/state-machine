/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service;

public class TestAppContext extends AbstractAppContext<TestAppContext> {

  protected TestAppContext(String appName, String[] initArgs) {
    super(appName, initArgs);
  }
}
