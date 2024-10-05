/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service;

import static org.mockito.Mockito.mock;

import com.webotech.statemachine.service.api.Subsystem;
import java.util.List;

public class RestartableTestApp extends AbstractAppService<TestAppContext> {

  public RestartableTestApp(String[] args) {
    super(new TestAppContext(TestApp.class.getSimpleName(), args).withSubsystems(
        List.of(mock(Subsystem.class), mock(Subsystem.class))), false);
  }

  public static void main(String[] args) {
    RestartableTestApp app = new RestartableTestApp(args);
    try {
      app.start();
    } catch (Exception e) {
      app.error(e);
    }
  }
}
