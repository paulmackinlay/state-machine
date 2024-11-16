/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.webotech.statemachine.GenericStateMachine;
import com.webotech.statemachine.TestingUtil;
import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachineListener;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractAppServiceTest {

  private static final int TIMEOUT = 2000;
  private static ExecutorService testExecutor;
  private TestService testService;

  @BeforeAll
  static void init() {
    testExecutor = Executors.newSingleThreadExecutor();
  }

  @AfterAll
  static void shutdown() {
    testExecutor.shutdownNow();
  }

  @BeforeEach
  void setup() {
    testService = new TestService();
  }

  @Test
  void shouldTransitionThroughStates() {
    TestStateMachineListener listener = new TestStateMachineListener();
    testService.setStateMachineListener(listener);
    assertTrue(listener.changeBegins.isEmpty());
    assertTrue(listener.changeEnds.isEmpty());
    testExecutor.execute(() -> testService.start());
    boolean success = TestingUtil.awaitCondition(TIMEOUT, TimeUnit.MILLISECONDS,
        () -> testService.getLifecycleState() != null && testService.getLifecycleState().getName()
            .equals(LifecycleStateMachineUtil.STATE_STARTED));
    if (!success) {
      fail("App did not start in time");
    }
    assertEquals(3, listener.changeBegins.size());
    assertEquals(3, listener.changeEnds.size());
    testService.stop();
    success = TestingUtil.awaitCondition(TIMEOUT, TimeUnit.MILLISECONDS,
        () -> testService.getLifecycleState().getName()
            .equals(GenericStateMachine.RESERVED_STATE_NAME_END));
    if (!success) {
      fail("App did not stop in time");
    }
    assertEquals(6, listener.changeBegins.size());
    assertEquals(6, listener.changeEnds.size());
    assertTransition("_UNINITIALISED_", GenericStateMachine.RESERVED_STATE_EVENT_NAME_IMMEDIATE,
        LifecycleStateMachineUtil.STATE_UNINITIALISED, listener, 0);
    assertTransition(LifecycleStateMachineUtil.STATE_UNINITIALISED,
        LifecycleStateMachineUtil.evtStart.getName(),
        LifecycleStateMachineUtil.STATE_STARTING, listener, 1);
    assertTransition(LifecycleStateMachineUtil.STATE_STARTING,
        LifecycleStateMachineUtil.evtComplete.getName(),
        LifecycleStateMachineUtil.STATE_STARTED, listener, 2);
    assertTransition(LifecycleStateMachineUtil.STATE_STARTED,
        LifecycleStateMachineUtil.evtStop.getName(),
        LifecycleStateMachineUtil.STATE_STOPPING, listener, 3);
    assertTransition(LifecycleStateMachineUtil.STATE_STOPPING,
        LifecycleStateMachineUtil.evtComplete.getName(),
        LifecycleStateMachineUtil.STATE_STOPPED, listener, 4);
    assertTransition(LifecycleStateMachineUtil.STATE_STOPPED,
        LifecycleStateMachineUtil.evtStop.getName(),
        GenericStateMachine.RESERVED_STATE_NAME_END, listener, 5);
  }

  @Test
  void shouldGetAppContext() {
    assertInstanceOf(TestContext.class, testService.getAppContext());
    assertEquals("TestService", testService.getAppContext().getAppName());
  }

  @Test
  void shouldHandleError() throws IOException {
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      testExecutor.execute(() -> testService.start());
      boolean success = TestingUtil.awaitCondition(TIMEOUT, TimeUnit.MILLISECONDS,
          () -> testService.getLifecycleState() != null && testService.getLifecycleState().getName()
              .equals(LifecycleStateMachineUtil.STATE_STARTED));
      if (!success) {
        fail("App did not start in time");
      }
      testService.error(new IllegalStateException("test induced"));
      success = TestingUtil.awaitCondition(TIMEOUT, TimeUnit.MILLISECONDS,
          () -> testService.getLifecycleState().getName()
              .equals(GenericStateMachine.RESERVED_STATE_NAME_END));
      if (!success) {
        fail("App did not end in time");
      }
      String log = TestingUtil.asNormalisedTxt(logStream);
      assertTrue(log.startsWith(
          "Begin TestService transition: _UNINITIALISED_ + _immediate_ = UNINITIALISED\n"
              + "TestService transitioned to UNINITIALISED\n"
              + "Begin TestService transition: UNINITIALISED + start = STARTING\n"
              + "Starting TestService with args []\n"
              + "TestService transitioned to STARTING\n"
              + "Begin TestService transition: STARTING + complete = STARTED\n"
              + "TestService transitioned to STARTED\n"
              + "TestService has an error\n"
              + "java.lang.IllegalStateException: test induced"));
      assertTrue(log.endsWith("Begin TestService transition: STARTED + error = STOPPED\n"
          + "Stopped TestService\n"
          + "TestService transitioned to STOPPED\n"
          + "Begin TestService transition: STOPPED + stop = _END_\n"
          + "TestService transitioned to _END_\n"));
    }
  }

  private static void assertTransition(String from, String event, String to,
      TestStateMachineListener listener, int idx) {
    assertEquals(from, listener.changeBegins.get(idx).get(0));
    assertEquals(from, listener.changeEnds.get(idx).get(0));
    assertEquals(event, listener.changeBegins.get(idx).get(1));
    assertEquals(event, listener.changeEnds.get(idx).get(1));
    assertEquals(to, listener.changeBegins.get(idx).get(2));
    assertEquals(to, listener.changeEnds.get(idx).get(2));
  }

  private static class TestService extends AbstractAppService<TestContext> {

    TestService() {
      super(new TestContext());
    }
  }

  private static class TestContext extends AbstractAppContext<TestContext> {

    TestContext() {
      super("TestService", new String[0]);
    }
  }

  private static class TestStateMachineListener implements StateMachineListener<TestContext, Void> {

    final List<List<String>> changeBegins = new ArrayList<>();
    final List<List<String>> changeEnds = new ArrayList<>();

    @Override
    public void onStateChangeBegin(State<TestContext, Void> fromState, StateEvent<Void> event,
        State<TestContext, Void> toState) {
      changeBegins.add(List.of(fromState.getName(), event.getName(), toState.getName()));
    }

    @Override
    public void onStateChangeEnd(State<TestContext, Void> fromState, StateEvent<Void> event,
        State<TestContext, Void> toState) {
      changeEnds.add(List.of(fromState.getName(), event.getName(), toState.getName()));
    }
  }
}