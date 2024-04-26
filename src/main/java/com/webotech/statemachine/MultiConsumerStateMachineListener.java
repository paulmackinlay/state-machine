/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachineListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link StateMachineListener} that lets you add/remove multiple other
 * {@link StateMachineListener}s in
 * a thread safe way. Each will be called back independently of each other.
 */
public class MultiConsumerStateMachineListener<T, S> implements StateMachineListener<T, S> {

  private final Map<StateMachineListener<T, S>, StateMachineListener<T, S>> consumers;

  public MultiConsumerStateMachineListener() {
    this.consumers = new ConcurrentHashMap<>();
  }

  /**
   * Adds a {@link StateMachineListener} and returns true if successful. A return value of false
   * means the listener is already part of the collection of consumers.
   */
  public boolean add(StateMachineListener<T, S> stateMachineListener) {
    return this.consumers.putIfAbsent(stateMachineListener, stateMachineListener) == null;
  }

  /**
   * Removes a {@link StateMachineListener} from the collection of consumers. A return value of
   * true means it was removed successfully, false means it wasn't part of the consumers.
   */
  public boolean remove(StateMachineListener<T, S> stateMachineListener) {
    return this.consumers.remove(stateMachineListener) != null;
  }

  @Override
  public void onStateChangeBegin(State<T, S> fromState, StateEvent<S> event, State<T, S> toState) {
    for (StateMachineListener<T, S> stateMachineListener : consumers.values()) {
      stateMachineListener.onStateChangeBegin(fromState, event, toState);
    }
  }

  @Override
  public void onStateChangeEnd(State<T, S> fromState, StateEvent<S> event, State<T, S> toState) {
    for (StateMachineListener<T, S> stateMachineListener : consumers.values()) {
      stateMachineListener.onStateChangeEnd(fromState, event, toState);
    }
  }
}
