/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service;

import static com.webotech.statemachine.service.LifecycleStateMachineFactory.evtComplete;
import static com.webotech.statemachine.service.LifecycleStateMachineFactory.evtError;
import static com.webotech.statemachine.service.LifecycleStateMachineFactory.evtStart;
import static com.webotech.statemachine.service.LifecycleStateMachineFactory.evtStop;

import com.webotech.statemachine.GenericStateMachine;
import com.webotech.statemachine.HandleExceptionAction;
import com.webotech.statemachine.HandleExceptionAction.ExceptionHandler;
import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateMachine;
import com.webotech.statemachine.api.StateMachineListener;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CountDownLatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractAppService<C extends AbstractAppContext<C>> {

  //TODO review these
  private static final String MAIN_LATCH_WAS_INTERRUPTED = "Main latch was interrupted";
  private static final String APP_STOPPED = "App stopped";
  private static final String STOPPING_APP = "Stopping app";
  private static final String APP_STARTED = "App started";
  private static final String STARTING_APP_WITH_ARGS = "Starting app with args ";
  private static final String STATE = "] state: ";
  private static final String ERROR_WHILE_APP_IS_IN = "Error while app is in [";
  //TODO review the logging
  private final Logger logger;
  private final StateMachine<C, Void> appStateMachine;
  //TODO is this needed?
  private final CountDownLatch appLatch;
  private final C appContext;
  private State<C, Void> stopped;

  protected AbstractAppService(C appContext) {
    // Construct logger here so that logging can be re-initialised statically by concrete class
    logger = LogManager.getLogger(AbstractAppService.class);
    appLatch = new CountDownLatch(1);
    //TODO add statemachine logger here?
    appStateMachine = new GenericStateMachine.Builder<C, Void>().setContext(appContext).build();
    this.appContext = appContext;
    configureAppStateMachine();
  }

  private void configureAppStateMachine() {
    setStateMachineListener(LifecycleStateMachineFactory.stateMachineLogger());
    ExceptionHandler<C, Void> exceptionHandler = (se, sa, e) -> {
      //TODO
      logger.error(ERROR_WHILE_APP_IS_IN + sa.getClass().getSimpleName() + STATE, e);
      appStateMachine.fire(evtError);
    };

    State<C, Void> uninitialised = LifecycleStateMachineFactory.newUnitialisedState();
    State<C, Void> starting = LifecycleStateMachineFactory.newStartingState(
        new HandleExceptionAction<>((ev, sm) -> {
          logger.info(STARTING_APP_WITH_ARGS,
              Arrays.toString(sm.getContext().getInitArgs()));
          for (Subsystem<C> subsystem : appContext.getSubsystems()) {
            subsystem.start(appContext);
          }
          appStateMachine.fire(evtComplete);
        }, exceptionHandler));
    State<C, Void> started = LifecycleStateMachineFactory
        .newStartedState(new HandleExceptionAction<>((ev, sm) -> logger.info(APP_STARTED),
            exceptionHandler));
    State<C, Void> stopping = LifecycleStateMachineFactory.newStoppingState(
        new HandleExceptionAction<>((ev, sm) -> {
          logger.info(STOPPING_APP);
          List<Subsystem<C>> subsystems = appContext.getSubsystems();
          ListIterator<Subsystem<C>> listIterator = subsystems.listIterator(subsystems.size());
          while (listIterator.hasPrevious()) {
            listIterator.previous().stop(appContext);
          }
          appStateMachine.fire(evtComplete);
        }, exceptionHandler));
    stopped = LifecycleStateMachineFactory.newStoppedState((stopEvt, stateMachine) -> {
      logger.info(APP_STOPPED);
      appLatch.countDown();
    });

    LifecycleStateMachineFactory.configureAppStateMachine(appStateMachine, uninitialised, starting,
        started, stopping, stopped);
  }

  public final void start() {
    appStateMachine.start();
    appStateMachine.fire(evtStart);
    try {
      appLatch.await();
    } catch (InterruptedException e) {
      //TODO
      logger.error(MAIN_LATCH_WAS_INTERRUPTED, e);
      Thread.currentThread().interrupt();
    } finally {
      if (!getLifecycleState().equals(stopped)) {
        stop();
      }
    }
  }

  public final void stop() {
    appStateMachine.fire(evtStop);
  }

  public final State<C, Void> getLifecycleState() {
    return appStateMachine.getCurrentState();
  }

  public final void setStateMachineListener(StateMachineListener<C, Void> stateMachineListener) {
    appStateMachine.setStateMachineListener(stateMachineListener);
  }
}
