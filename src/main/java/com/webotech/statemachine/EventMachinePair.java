/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.StateEvent;

/**
 * Encapsulates the {@link com.webotech.statemachine.api.StateEvent} and
 * {@link GenericStateMachine} objects that are exchanged between publisher
 * and consumer threads. This is intentionally mutable as it used for object pooling.
 */
public class EventMachinePair<T, S> {

  private StateEvent<S> stateEvent;
  private GenericStateMachine<T, S> stateMachine;

  EventMachinePair() {
    super();
  }

  void setEventMachinePair(StateEvent<S> stateEvent, GenericStateMachine<T, S> stateMachine) {
    this.stateEvent = stateEvent;
    this.stateMachine = stateMachine;
  }

  StateEvent<S> getStateEvent() {
    return stateEvent;
  }

  GenericStateMachine<T, S> getStateMachine() {
    return stateMachine;
  }
}
