/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;

/**
 * Encapsulates strategies for processing state machine transitions.
 */
public interface EventProcessingStrategy<T, S> {

  /**
   * Called when the next state (toState) has been determined by a transition due to receiving a
   * stateEvent. It will not be called if a stateEvent resulted in no transition.
   */
  void processEvent(StateEvent<S> stateEvent, GenericStateMachine<T, S> stateMachine,
      State<T, S> toState);
}
