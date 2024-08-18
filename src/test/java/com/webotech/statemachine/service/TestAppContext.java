/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service;

import com.webotech.statemachine.service.api.AppContext;

public class TestAppContext extends AbstractAppContext<TestAppContext> implements
    AppContext<TestAppContext> {

  protected TestAppContext(String appName, String[] initArgs) {
    super(appName, initArgs);
  }
}
