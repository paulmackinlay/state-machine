/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;

/**
 * Encapsulates callbacks for unexpected event flow.
 * <p>Generic types are
 * <li>T - the context for the {@link StateMachine}</li>
 * <li>S - the payload of the {@link StateEvent}</li>
 * </p>
 */
public interface UnexpectedFlowListener<T, S> {

  /**
   * Called when an uncaught {@link Exception} results from processing of a {@link StateEvent} on
   * thread
   */
  void onExceptionDuringEventProcessing(StateEvent<S> stateEvent, StateMachine<T, S> stateMachine,
      Thread thread, Exception e);

  /**
   * Called when a {@link StateEvent} is received after the
   * {@link com.webotech.statemachine.api.StateMachine} has ended
   */
  void onEventAfterMachineEnd(StateEvent<S> stateEvent, StateMachine<T, S> stateMachine);

  /**
   * Called when a {@link StateEvent} is received before the
   * {@link com.webotech.statemachine.api.StateMachine} has started
   */
  void onEventBeforeMachineStart(StateEvent<S> stateEvent, StateMachine<T, S> stateMachine);
}
