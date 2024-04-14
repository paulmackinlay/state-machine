package com.webotech.statemachine.service;

public class TestApp extends AbstractAppService<TestAppContext> {

  protected TestApp(String[] args) {
    super(new TestAppContext(TestApp.class.getSimpleName(), args));
  }
}
