/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import java.util.Map;
import java.util.Queue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DropDuplicateEventStrategy<T, S> implements EventProcessingStrategy<T, S> {

  private static final Logger logger = LogManager.getLogger(DropDuplicateEventStrategy.class);
  public static final String EVENT_ALREADY_IN_QUEUE_WILL_DROP_IT = "Event [{}] already in queue, will drop it";
  private final DefaultEventStrategy<T, S> defaultStrategy;

  /**
   * An {@link EventProcessingStrategy} that transitions state atomically. Any {@link StateEvent}
   * that is received when the internal queue already contains the same {@link StateEvent} is
   * dropped. {@link StateEvent}s in the queue are processed in sequence, in
   * the order they were received.
   * <p>
   * This {@link EventProcessingStrategy} is backed by an unbounded, lock-free and thread-safe
   * queue. In the case where the sustained rate of {@link StateEvent}s received is higher than the
   * rate they are being processed (slow consumption), it will ultimately lead to memory
   * starvation and a possible our of memory error.
   */
  public DropDuplicateEventStrategy(DefaultEventStrategy<T, S> defaultEventStrategy) {
    this.defaultStrategy = defaultEventStrategy;
  }

  @Override
  public int getEventQueueSize() {
    return defaultStrategy.getEventQueueSize();
  }

  @Override
  public void processEvent(StateEvent<S> stateEvent, GenericStateMachine<T, S> stateMachine) {
    Queue<EventMachinePair<T, S>> eventQueue = defaultStrategy.getEventQueue();
    if (eventQueue.stream().anyMatch(en -> en.getStateEvent().equals(stateEvent))) {
      logger.info(EVENT_ALREADY_IN_QUEUE_WILL_DROP_IT, stateEvent);
      return;
    }
    defaultStrategy.processEvent(stateEvent, stateMachine);
  }

  @Override
  public void setStates(Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states) {
    defaultStrategy.setStates(states);
  }

}
