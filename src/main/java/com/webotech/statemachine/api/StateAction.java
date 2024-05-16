/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.api;

/**
 * <p>Represents an action that will be fired when a states transition from one to another.</p>
 * <p>Generic types are
 * <li>T - the context for the {@link StateMachine}</li>
 * <li>S - the payload of the {@link StateEvent}</li>
 * </p>
 */
public interface StateAction<T, S> {

  /**
   * This is called when this {@link StateAction} os executed.
   */
  void execute(StateEvent<S> stateEvent, StateMachine<T, S> stateMachine);

}
