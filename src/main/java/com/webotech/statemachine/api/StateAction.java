/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.api;

/**
 * Represents an action that will be fired when a states transition from one to another.
 */
public interface StateAction<T, S> {

  /**
   * This is called when this {@link StateAction} os executed.
   */
  void execute(StateMachine<T, S> stateMachine);

}
