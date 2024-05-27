/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import java.util.Map;

/**
 * Encapsulates strategies for processing state machine transitions.
 * <p>Generic types are
 * <li>T - the context for the {@link com.webotech.statemachine.api.StateMachine}</li>
 * <li>S - the payload of the {@link StateEvent}</li>
 * </p>
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
   * Sets the {@link Map} of states/event that are configured in the
   * {@link com.webotech.statemachine.api.StateMachine}
   */
  void setStates(Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states);
}
