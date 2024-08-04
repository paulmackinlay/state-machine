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
    this.logger = LogManager.getLogger(
        AbstractAppService.class); // Do this here so that logging can be re-initialised statically by concrete class
    this.appLatch = new CountDownLatch(1);
    this.appStateMachine = new GenericStateMachine.Builder<C, Void>().setContext(appContext)
        .build();
    this.appContext = appContext;
    configureAndStartAppStateMachine();
  }

  private void configureAndStartAppStateMachine() {
    setStateMachineListener(LifecycleStateMachineFactory.stateMachineLogger());
    ExceptionHandler<C, Void> exceptionHandler = (se, sa, e) -> {
      //TODO
      this.logger.error(ERROR_WHILE_APP_IS_IN + sa.getClass().getSimpleName() + STATE,
          e);
      AbstractAppService.this.appStateMachine.fire(evtError);
    };

    State<C, Void> uninitialised = LifecycleStateMachineFactory.newUnitialisedState();
    State<C, Void> starting = LifecycleStateMachineFactory.newStartingState(
        new HandleExceptionAction<>((ev, sm) -> {
          this.logger.info(STARTING_APP_WITH_ARGS,
              Arrays.toString(sm.getContext().getInitArgs()));
          for (Component<C> component : this.appContext.getComponents()) {
            component.start(this.appContext);
          }
          AbstractAppService.this.appStateMachine.fire(evtComplete);
        }, exceptionHandler));
    State<C, Void> started = LifecycleStateMachineFactory
        .newStartedState(new HandleExceptionAction<>((ev, sm) -> this.logger.info(APP_STARTED),
            exceptionHandler));
    State<C, Void> stopping = LifecycleStateMachineFactory.newStoppingState(
        new HandleExceptionAction<>((ev, sm) -> {
          this.logger.info(STOPPING_APP);
          List<Component<C>> components = this.appContext.getComponents();
          ListIterator<Component<C>> listIterator = components.listIterator(components.size());
          while (listIterator.hasPrevious()) {
            listIterator.previous().stop(this.appContext);
          }
          AbstractAppService.this.appStateMachine.fire(evtComplete);
        }, exceptionHandler));
    this.stopped = LifecycleStateMachineFactory.newStoppedState((stopEvt, stateMachine) -> {
      this.logger.info(APP_STOPPED);
      this.appLatch.countDown();
    });

    LifecycleStateMachineFactory.configureAndStartAppStateMachine(this.appStateMachine,
        uninitialised,
        starting, started, stopping, this.stopped);
  }

  public final void start() {
    //TODO start the state machine here
    this.appStateMachine.fire(evtStart);
    try {
      this.appLatch.await();
    } catch (InterruptedException e) {
      //TODO
      this.logger.error(MAIN_LATCH_WAS_INTERRUPTED, e);
      Thread.currentThread().interrupt();
    } finally {
      if (!getLifecycleState().equals(this.stopped)) {
        stop();
      }
    }
  }

  public final void stop() {
    this.appStateMachine.fire(evtStop);
  }

  public final State<C, Void> getLifecycleState() {
    return this.appStateMachine.getCurrentState();
  }

  public final void setStateMachineListener(StateMachineListener<C, Void> stateMachineListener) {
    this.appStateMachine.setStateMachineListener(stateMachineListener);
  }
}
