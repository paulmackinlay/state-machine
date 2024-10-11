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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractAppServiceTest {

  private static final int TIMEOUT = 2000;
  private TestService testService;

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
    testService.start();
    boolean success = TestingUtil.awaitCondition(TIMEOUT, TimeUnit.MILLISECONDS,
        () -> testService.getLifecycleState().getName()
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

  private static void assertTransition(String from, String event, String to,
      TestStateMachineListener listener, int idx) {
    assertEquals(from, listener.changeBegins.get(idx).get(0));
    assertEquals(from, listener.changeEnds.get(idx).get(0));
    assertEquals(event, listener.changeBegins.get(idx).get(1));
    assertEquals(event, listener.changeEnds.get(idx).get(1));
    assertEquals(to, listener.changeBegins.get(idx).get(2));
    assertEquals(to, listener.changeEnds.get(idx).get(2));
  }

  private class TestService extends AbstractAppService<TestContext> {

    TestService() {
      super(new TestContext());
    }
  }

  private class TestContext extends AbstractAppContext<TestContext> {

    TestContext() {
      super("TestService", new String[0]);
    }
  }

  private class TestStateMachineListener implements StateMachineListener<TestContext, Void> {

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