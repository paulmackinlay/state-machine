/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import com.webotech.statemachine.util.Threads;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Responsible for firing {@link StateEvent}s into a {@link StateMachine}. The {@link StateEvent}s
 * can be fired asynchronously using an {@link ExecutorService} or bound to the current thread.
 */
public class EventManager<T, S> {

  private static final Logger logger = LogManager.getLogger(EventManager.class);
  private final StateMachine<T, S> stateMachine;
  private final ExecutorService executorService;

  /**
   * Uses an internally generated single thread executor with a daemon thread.
   */
  public EventManager(StateMachine<T, S> stateMachine, String serviceThreadName) {
    this(stateMachine,
        Executors.newSingleThreadExecutor(Threads.newNamedDaemonThreadFactory(serviceThreadName,
            (Thread thread, Throwable t) -> logger.error("Unhandled exception in thread {}",
                thread.getName(), t))));
  }

  public EventManager(StateMachine<T, S> stateMachine, ExecutorService executorService) {
    this.executorService = executorService;
    this.stateMachine = stateMachine;
  }

  /**
   * Fires a {@link StateEvent} asynchronously using an {@link ExecutorService}.
   */
  public void fireAsync(StateEvent<S> stateEvent) {
    this.executorService.execute(() -> this.stateMachine.fire(stateEvent));
  }

  /**
   * Fires a {@link StateEvent} bound to the current thread.
   */
  public void fireBound(StateEvent<S> stateEvent) {
    this.stateMachine.fire(stateEvent);
  }
}
