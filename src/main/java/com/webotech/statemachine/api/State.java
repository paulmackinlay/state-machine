/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.api;

/**
 * <p></p>Representation of a state of the state machine.</p>
 * <p>Generic types are
 * <li>T - the context for the {@link StateMachine}</li>
 * <li>S - the payload of the {@link StateEvent}</li>
 * </p>
 */
public interface State<T, S> {

  /**
   * This is called when the state machine transitions to this {@link State}. Any
   * {@link StateAction}s that has been assigned to state entry will fire.
   */
  void onEntry(StateEvent<S> stateEvent, StateMachine<T, S> stateMachine);

  /**
   * This is called when the state machine transitions away from this {@link State}.  Any
   * {@link StateAction}s that has been assigned to state exit will fire.
   */
  void onExit(StateEvent<S> stateEvent, StateMachine<T, S> stateMachine);

  /**
   * This will assign the {@link StateAction} to fire when the state is entered.
   */
  @SuppressWarnings("unchecked")
  void appendEntryActions(StateAction<T, S>... actions);

  /**
   * This will assign the {@link StateAction} to fire when the state is exited.
   */
  @SuppressWarnings("unchecked")
  void appendExitActions(StateAction<T, S>... actions);

  /**
   * @return the name of this {@link State}
   */
  String getName();
}
