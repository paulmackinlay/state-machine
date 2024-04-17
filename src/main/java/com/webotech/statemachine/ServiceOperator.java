package com.webotech.statemachine;

import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import com.webotech.statemachine.util.Threads;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//TODO review this
public class ServiceOperator<T> {

  private static final Logger logger = LogManager.getLogger(ServiceOperator.class);
  private final StateMachine<T> stateMachine;
  private final Executor serviceExecutor;

  /**
   * Uses an internally generated single thread executor with a daemon thread.
   */
  public ServiceOperator(StateMachine<T> stateMachine, String serviceThreadName) {
    this(stateMachine,
        Executors.newSingleThreadExecutor(Threads.newNamedDaemonThreadFactory(serviceThreadName,
            (Thread thread, Throwable t) -> logger.error("Unhandled exception in thread {}",
                thread.getName(), t))));
  }

  public ServiceOperator(StateMachine<T> stateMachine, Executor serviceExecutor) {
    this.serviceExecutor = serviceExecutor;
    this.stateMachine = stateMachine;
  }

  /**
   * Fires an event using a service executor.
   */
  public void fireAsync(StateEvent stateEvent) {
    this.serviceExecutor.execute(() -> this.stateMachine.fire(stateEvent));
  }
}
