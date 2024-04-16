package com.webotech.statemachine.servicetemp;

import static com.webotech.statemachine.LifecycleStateMachineFactory.completeEvt;
import static com.webotech.statemachine.LifecycleStateMachineFactory.errorEvt;
import static com.webotech.statemachine.LifecycleStateMachineFactory.startEvt;
import static com.webotech.statemachine.LifecycleStateMachineFactory.stopEvt;

import com.webotech.statemachine.GeneralPurposeStateMachine;
import com.webotech.statemachine.HandleExceptionAction;
import com.webotech.statemachine.LifecycleStateMachineFactory;
import com.webotech.statemachine.ServiceOperator;
import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateMachine;
import com.webotech.statemachine.api.StateMachineListener;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
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
  private final StateMachine<C> appStateMachine;
  private final CountDownLatch appLatch;
  private final ServiceOperator<C> appOperator;
  private final C appContext;
  private State<C> stopped;

  protected AbstractAppService(C appContext) {
    this.logger = LogManager.getLogger(
        AbstractAppService.class); // Do this here so that logging can be re-initialised statically by concrete class
    this.appLatch = new CountDownLatch(1);
    this.appStateMachine = (new GeneralPurposeStateMachine.Builder<C>().setContext(
        appContext)).build();
    this.appOperator = new ServiceOperator<>(this.appStateMachine, appContext.getAppName());
    this.appContext = appContext;
    configureAppStateMachine();
  }

  private void configureAppStateMachine() {
    setStateMachineListener(LifecycleStateMachineFactory.stateMachineLogger());
    BiConsumer<StateMachine<C>, Exception> exceptionHandler = (stateMachine, e) -> {
      //TODO
      this.logger.error(ERROR_WHILE_APP_IS_IN + stateMachine.getCurrentState().getName() + STATE,
          e);
      AbstractAppService.this.appOperator.fireAsync(errorEvt);
    };

    State<C> uninitialised = LifecycleStateMachineFactory.newUnitialisedState();
    State<C> starting = LifecycleStateMachineFactory.newStartingState(
        new HandleExceptionAction<>(stateMachine -> {
          this.logger.info(STARTING_APP_WITH_ARGS,
              Arrays.toString(stateMachine.getContext().getInitArgs()));
          for (Component<C> component : this.appContext.getComponents()) {
            component.start(this.appContext);
          }
          AbstractAppService.this.appOperator.fireAsync(completeEvt);
        }, exceptionHandler));
    State<C> started = LifecycleStateMachineFactory
        .newStartedState(new HandleExceptionAction<>(stateMachine -> this.logger.info(APP_STARTED),
            exceptionHandler));
    State<C> stopping = LifecycleStateMachineFactory.newStoppingState(
        new HandleExceptionAction<>(stateMachine -> {
          this.logger.info(STOPPING_APP);
          List<Component<C>> components = this.appContext.getComponents();
          ListIterator<Component<C>> listIterator = components.listIterator(components.size());
          while (listIterator.hasPrevious()) {
            listIterator.previous().stop(this.appContext);
          }
          AbstractAppService.this.appOperator.fireAsync(completeEvt);
        }, exceptionHandler));
    this.stopped = LifecycleStateMachineFactory.newStoppedState(stateMachine -> {
      this.logger.info(APP_STOPPED);
      this.appLatch.countDown();
    });

    LifecycleStateMachineFactory.configureAppStateMachine(this.appStateMachine, uninitialised,
        starting, started, stopping,
        this.stopped);
  }

  public final void start() {
    this.appOperator.fireAsync(startEvt);
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
    this.appOperator.fireAsync(stopEvt);
  }

  public final State<C> getLifecycleState() {
    return this.appStateMachine.getCurrentState();
  }

  public final void setStateMachineListener(StateMachineListener<C> stateMachineListener) {
    this.appStateMachine.setStateMachineListener(stateMachineListener);
  }
}
