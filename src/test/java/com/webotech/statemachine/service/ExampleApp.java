/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service;

import java.util.List;

public class ExampleApp extends AbstractAppService<ExampleAppContext> {

  private ExampleApp(ExampleAppContext appContext) {
    super(appContext);
  }

  public static void main(String[] args) {
    ExampleApp app = new ExampleApp(
        new ExampleAppContext().withSubsystems(List.of(new ExampleSubsystem())));
    try {
      app.start();
    } catch (Exception e) {
      app.error(e);
    }
  }
}
