/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service;

import com.webotech.statemachine.service.api.Subsystem;

public class ExampleSubsystem implements Subsystem<ExampleAppContext> {

  private String exampleState;

  @Override
  public void start(ExampleAppContext appContext) {
    exampleState = "subsystem is go!";
  }

  @Override
  public void stop(ExampleAppContext appContext) {
    exampleState = null;
  }
}
