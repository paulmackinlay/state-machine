/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import java.util.Map;

/**
 * Encapsulates strategies for processing state machine transitions.
 */
public interface EventProcessingStrategy<T, S> {

  /**
   * @return the number of {@link StateEvent}s that haven't completed processing.
   */
  int getEventQueueSize();

  /**
   * Called when the next state (toState) has been determined by a transition due to receiving a
   * stateEvent. It will not be called if a stateEvent resulted in no transition.
   */
  void processEvent(StateEvent<S> stateEvent, GenericStateMachine<T, S> stateMachine);

  /**
   * @return the {@link Map} on configured states/events
   * <p>
   * TODO I don't like this, it's not intuitive, some through needs to be put into how to better handled it
   */
  Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> getStates();
}
