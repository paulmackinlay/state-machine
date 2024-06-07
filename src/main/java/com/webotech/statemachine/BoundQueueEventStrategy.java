/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import java.util.Map;

public class BoundQueueEventStrategy<T, S> implements EventProcessingStrategy<T, S> {

  private final DefaultEventStrategy<T, S> defaultEventStrategy;
  private final UnexpectedFlowListener<T, S> unexpectedFlowListener;
  private final int maxQueueSize;

  /**
   * A memory safe {@link EventProcessingStrategy}, it transitions state atomically. All
   * {@link StateEvent}s are processed. By default {@link StateEvent}s are processed in sequence,
   * in the order they were received.
   * <p>
   * This {@link EventProcessingStrategy} is backed by a bounded (of size maxQueueSize), lock-free
   * and thread-safe queue. In the case where the sustained rate of {@link StateEvent}s received is
   * higher than the rate they are being processed (slow consumption), it will ultimately lead to
   * {@link IllegalStateException}s being called back on
   * {@link UnexpectedFlowListener#onExceptionDuringEventProcessing(StateEvent, StateMachine,
   * Thread, Exception)} and the {@link StateEvent} will not be processed.
   */
  public BoundQueueEventStrategy(DefaultEventStrategy<T, S> defaultEventStrategy,
      int maxQueueSize) {
    this.defaultEventStrategy = defaultEventStrategy;
    this.unexpectedFlowListener = defaultEventStrategy.getUnexpectedFlowListener();
    this.maxQueueSize = maxQueueSize;
  }

  @Override
  public int getEventQueueSize() {
    return defaultEventStrategy.getEventQueueSize();
  }

  @Override
  public void processEvent(StateEvent<S> stateEvent, GenericStateMachine<T, S> stateMachine) {
    int queueSize = getEventQueueSize();
    if (queueSize >= this.maxQueueSize) {
      unexpectedFlowListener.onExceptionDuringEventProcessing(stateEvent, stateMachine,
          Thread.currentThread(), new IllegalStateException(
              String.format("Queue size is maxed out at %s - dropping event", queueSize)));
      return;
    }
    defaultEventStrategy.processEvent(stateEvent, stateMachine);
  }

  @Override
  public void setStates(Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states) {
    defaultEventStrategy.setStates(states);
  }
}
