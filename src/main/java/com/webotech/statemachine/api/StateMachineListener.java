/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.api;

/**
 * <p></p>Encapsulates the API for the listening to state transitions for a
 * {@link StateMachine}.</p>
 * <p>Generic types are
 * <li>T - the context for the {@link StateMachine}</li>
 * <li>S - the payload of the {@link StateEvent}</li>
 * </p>
 */
public interface StateMachineListener<T, S> {

  /**
   * This is called at the beginning of a {@link State} transition, before all {@link StateAction}s
   * are executed.
   */
  void onStateChangeBegin(State<T, S> fromState, StateEvent<S> event, State<T, S> toState);

  /**
   * This is called at the beginning of a {@link State} transition, after all {@link StateAction}s
   * are executed.
   */
  void onStateChangeEnd(State<T, S> fromState, StateEvent<S> event, State<T, S> toState);

}
