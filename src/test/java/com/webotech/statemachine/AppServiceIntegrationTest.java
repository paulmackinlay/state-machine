/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachineListener;
import com.webotech.statemachine.service.LifecycleStateMachineUtil;
import com.webotech.statemachine.service.RestartableTestApp;
import com.webotech.statemachine.service.TestApp;
import com.webotech.statemachine.service.TestAppContext;
import com.webotech.statemachine.service.api.Subsystem;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

public class AppServiceIntegrationTest {

  public static final int TIMEOUT_MILLIS = 5000;

  @Test
  void shouldStartAppOnlyInValidState() {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    TestApp testApp = new TestApp(new String[0]);
    executor.execute(() -> testApp.start());
    if (!TestingUtil.awaitCondition(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS,
        () -> testApp.getLifecycleState() != null && testApp.getLifecycleState().getName()
            .equals(LifecycleStateMachineUtil.STATE_STARTED))) {
      fail("Test app didn't start in time");
    }
    assertThrows(IllegalStateException.class, () -> testApp.start());
  }

  @Test
  void shouldStopAppOnlyInValidState() {
    TestApp testApp = new TestApp(new String[0]);
    assertThrows(IllegalStateException.class, () -> testApp.stop());
  }

  @Test
  void shouldTestStateMachineBackedApp() throws InterruptedException, IOException {
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      ExecutorService executor = Executors.newSingleThreadExecutor();
      CountDownLatch startLatch = new CountDownLatch(1);
      CountDownLatch endLatch = new CountDownLatch(1);
      String[] args = {"an-arg"};
      TestApp testApp = new TestApp(args);
      testApp.setStateMachineListener(new StateMachineListener<TestAppContext, Void>() {
        @Override
        public void onStateChangeBegin(State<TestAppContext, Void> fromState,
            StateEvent<Void> event, State<TestAppContext, Void> toState) {
          // niente
        }

        @Override
        public void onStateChangeEnd(State<TestAppContext, Void> fromState, StateEvent<Void> event,
            State<TestAppContext, Void> toState) {
          if (toState.getName().equals(LifecycleStateMachineUtil.STATE_STARTED)) {
            startLatch.countDown();
          }
          if (toState.getName().equals(GenericStateMachine.RESERVED_STATE_NAME_END)) {
            endLatch.countDown();
          }
        }
      });

      State<TestAppContext, Void> state = testApp.getLifecycleState();
      assertNull(state);
      executor.execute(() -> testApp.start());
      boolean success = startLatch.await(2, TimeUnit.SECONDS);
      if (!success) {
        fail("Did not start in time");
      }
      state = testApp.getLifecycleState();
      assertEquals(LifecycleStateMachineUtil.STATE_STARTED, state.getName());
      String log = logStream.toString();
      assertEquals("Starting TestApp with args [an-arg]\n", log);
      TestAppContext appContext = testApp.getAppContext();
      assertEquals("TestApp", appContext.getAppName());
      assertEquals(2, appContext.getSubsystems().size());
      Subsystem<TestAppContext> subsystem1 = appContext.getSubsystems().get(0);
      Subsystem<TestAppContext> subsystem2 = appContext.getSubsystems().get(1);
      assertEquals(args, appContext.getInitArgs());

      InOrder inOrder = inOrder(subsystem1, subsystem2);
      inOrder.verify(subsystem1, times(1)).start(appContext);
      inOrder.verify(subsystem2, times(1)).start(appContext);

      testApp.stop();
      success = endLatch.await(2, TimeUnit.SECONDS);
      if (!success) {
        fail("Did not end in time");
      }
      state = testApp.getLifecycleState();
      List<String> stopedStates = List.of(GenericStateMachine.RESERVED_STATE_NAME_END,
          LifecycleStateMachineUtil.STATE_STOPPED);
      assertEquals(GenericStateMachine.RESERVED_STATE_NAME_END, state.getName());
      //Stops in reverse order from how is started
      inOrder.verify(subsystem2, times(1)).stop(appContext);
      inOrder.verify(subsystem1, times(1)).stop(appContext);

      log = logStream.toString();
      assertEquals("Starting TestApp with args [an-arg]\n"
          + "Stopping TestApp\n"
          + "Stopped TestApp\n", log);
    }
  }

  @Test
  void shouldTestRestartableApp() throws InterruptedException, IOException {
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      ExecutorService executor = Executors.newSingleThreadExecutor();
      CountDownLatch startLatch = new CountDownLatch(2);
      CountDownLatch stopLatch = new CountDownLatch(1);
      CountDownLatch endLatch = new CountDownLatch(1);

      RestartableTestApp restartableApp = new RestartableTestApp(new String[0]);
      restartableApp.setStateMachineListener(new StateMachineListener<TestAppContext, Void>() {
        @Override
        public void onStateChangeBegin(State<TestAppContext, Void> fromState,
            StateEvent<Void> event, State<TestAppContext, Void> toState) {
          // niente
        }

        @Override
        public void onStateChangeEnd(State<TestAppContext, Void> fromState, StateEvent<Void> event,
            State<TestAppContext, Void> toState) {
          if (toState.getName().equals(LifecycleStateMachineUtil.STATE_STOPPED)) {
            stopLatch.countDown();
          }
          if (toState.getName().equals(GenericStateMachine.RESERVED_STATE_NAME_END)) {
            endLatch.countDown();
          }
          if (toState.getName().equals(LifecycleStateMachineUtil.STATE_STARTED)) {
            startLatch.countDown();
          }
        }
      });

      State<TestAppContext, Void> state = restartableApp.getLifecycleState();
      assertNull(state);
      executor.execute(() -> restartableApp.start());
      long epochStart = System.currentTimeMillis();
      while (startLatch.getCount() > 1) {
        TestingUtil.sleep(50);
        if (System.currentTimeMillis() - epochStart > 2000) {
          fail("Did not start in time");
        }
      }
      state = restartableApp.getLifecycleState();
      assertEquals(LifecycleStateMachineUtil.STATE_STARTED, state.getName());
      TestAppContext appContext = restartableApp.getAppContext();
      assertEquals("TestApp", appContext.getAppName());
      assertEquals(2, appContext.getSubsystems().size());
      Subsystem<TestAppContext> subsystem1 = appContext.getSubsystems().get(0);
      Subsystem<TestAppContext> subsystem2 = appContext.getSubsystems().get(1);

      InOrder inOrder = inOrder(subsystem1, subsystem2);
      inOrder.verify(subsystem1, times(1)).start(appContext);
      inOrder.verify(subsystem2, times(1)).start(appContext);

      restartableApp.stop();
      boolean success = stopLatch.await(2, TimeUnit.SECONDS);
      if (!success) {
        fail("Did not stop in time");
      }
      state = restartableApp.getLifecycleState();
      assertEquals(LifecycleStateMachineUtil.STATE_STOPPED, state.getName());
      assertEquals(1, endLatch.getCount());
      restartableApp.start();
      success = startLatch.await(2, TimeUnit.SECONDS);
      if (!success) {
        fail("Did not restart in time");
      }
      state = restartableApp.getLifecycleState();
      assertEquals(LifecycleStateMachineUtil.STATE_STARTED, state.getName());
      restartableApp.stop();
      success = TestingUtil.awaitCondition(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS,
          () -> restartableApp.getLifecycleState().getName()
              .equals(LifecycleStateMachineUtil.STATE_STOPPED));
      if (!success) {
        fail("Did not restop in time");
      }
      restartableApp.stop();
      success = endLatch.await(2, TimeUnit.SECONDS);
      if (!success) {
        fail("Did not end in time");
      }
      state = restartableApp.getLifecycleState();
      assertEquals(GenericStateMachine.RESERVED_STATE_NAME_END, state.getName());
      //Stops in reverse order from how is started
      inOrder.verify(subsystem2, times(1)).stop(appContext);
      inOrder.verify(subsystem1, times(1)).stop(appContext);

      String log = logStream.toString();
      assertEquals("Starting TestApp with args []\n"
          + "Stopping TestApp\n"
          + "Stopped TestApp\n"
          + "Starting TestApp with args []\n"
          + "Stopping TestApp\n"
          + "Stopped TestApp\n", log);
    }
  }

}
