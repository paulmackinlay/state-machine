/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service;

import com.webotech.statemachine.GenericStateMachine;
import com.webotech.statemachine.HandleExceptionAction;
import com.webotech.statemachine.HandleExceptionAction.ExceptionHandler;
import com.webotech.statemachine.LoggingStateMachineListener;
import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateMachine;
import com.webotech.statemachine.api.StateMachineListener;
import com.webotech.statemachine.service.api.AppContext;
import com.webotech.statemachine.service.api.AppService;
import com.webotech.statemachine.service.api.Subsystem;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CountDownLatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractAppService<C extends AppContext<C>> implements AppService<C> {

  private final Logger logger;
  private final StateMachine<C, Void> appStateMachine;
  private final CountDownLatch appLatch;
  private final C appContext;
  private State<C, Void> stopped;

  protected AbstractAppService(C appContext) {
    // Construct logger here so that logging can be re-initialised statically by concrete class
    logger = LogManager.getLogger(AbstractAppService.class);
    appLatch = new CountDownLatch(1);
    appStateMachine = new GenericStateMachine.Builder<C, Void>().setContext(appContext).build();
    this.appContext = appContext;
    configureAppStateMachine();
  }

  private void configureAppStateMachine() {
    String appName = appContext.getAppName();
    StateMachineListener<C, Void> stateMachineLogger = new LoggingStateMachineListener<>(appName);
    setStateMachineListener(stateMachineLogger);
    ExceptionHandler<C, Void> exceptionHandler = (evt, sm, e) -> {
      logger.error("Error while {} is in [{}] state", appName, sm.getCurrentState(), e);
      appStateMachine.fire(LifecycleStateMachineUtil.evtError);
    };
    State<C, Void> uninitialised = LifecycleStateMachineUtil.newUnitialisedState();
    State<C, Void> starting = LifecycleStateMachineUtil.newStartingState(
        new HandleExceptionAction<>((ev, sm) -> {
          logger.info("Starting {} with args {}", appName, appContext.getInitArgs());
          for (Subsystem<C> subsystem : appContext.getSubsystems()) {
            subsystem.start(appContext);
          }
          appStateMachine.fire(LifecycleStateMachineUtil.evtComplete);
        }, exceptionHandler));
    State<C, Void> started = LifecycleStateMachineUtil.newStartedState(
        new HandleExceptionAction<>((ev, sm) -> {
        }, exceptionHandler));
    State<C, Void> stopping = LifecycleStateMachineUtil.newStoppingState(
        new HandleExceptionAction<>((ev, sm) -> {
          logger.info("Stopping {}", appName);
          List<Subsystem<C>> subsystems = appContext.getSubsystems();
          ListIterator<Subsystem<C>> listIterator = subsystems.listIterator(subsystems.size());
          while (listIterator.hasPrevious()) {
            listIterator.previous().stop(appContext);
          }
          appStateMachine.fire(LifecycleStateMachineUtil.evtComplete);
        }, exceptionHandler));
    stopped = LifecycleStateMachineUtil.newStoppedState((stopEvt, stateMachine) -> {
      logger.info("Stopped {}", appName);
      appLatch.countDown();
    });

    LifecycleStateMachineUtil.configureAppStateMachine(appStateMachine, uninitialised, starting,
        started, stopping, stopped);
  }

  public C getAppContext() {
    return appContext;
  }

  public final void start() {
    appStateMachine.start();
    appStateMachine.fire(LifecycleStateMachineUtil.evtStart);
    try {
      appLatch.await();
    } catch (InterruptedException e) {
      logger.error("Main latch was interrupted", e);
      Thread.currentThread().interrupt();
    } finally {
      if (!appStateMachine.isEnded()) {
        stop();
      }
    }
  }

  public final void stop() {
    appStateMachine.fire(LifecycleStateMachineUtil.evtStop);
  }

  public final State<C, Void> getLifecycleState() {
    return appStateMachine.getCurrentState();
  }

  public final void setStateMachineListener(StateMachineListener<C, Void> stateMachineListener) {
    appStateMachine.setStateMachineListener(stateMachineListener);
  }
}
