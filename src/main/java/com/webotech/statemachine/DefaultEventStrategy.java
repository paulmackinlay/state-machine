/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DefaultEventStrategy<T, S> implements EventProcessingStrategy<T, S> {

  private static final Logger logger = LogManager.getLogger(DefaultEventStrategy.class);
  private static final String LOG_EVENT_NOT_MAPPED = "StateEvent [{}] not mapped for state [{}], ignoring";
  private final ConcurrentLinkedQueue<Entry<StateEvent<S>, GenericStateMachine<T, S>>> eventQueue;
  private final ExecutorService executor;
  private final TransitionTask<T, S> transitionTask;

  /**
   * The default {@link EventProcessingStrategy}, it transitions state atomically. All
   * {@link StateEvent}s are processed. By default {@link StateEvent}s are processed in sequence, in
   * the order they were received.
   */
  private DefaultEventStrategy(Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states,
      BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler,
      ExecutorService executor) {
    this.executor = executor;
    this.eventQueue = new ConcurrentLinkedQueue<>();
    this.transitionTask = new TransitionTask<>(states, unmappedEventHandler);
  }

  @Override
  public int getEventQueueSize() {
    return eventQueue.size();
  }

  @Override
  public void processEvent(StateEvent<S> stateEvent, GenericStateMachine<T, S> stateMachine) {
    if (stateEvent.getPayload() != null) {
      /* Use a safe copy of the StateEvent in case the client is
        setting different payloads on the same event instance */
      eventQueue.offer(
          new AbstractMap.SimpleEntry<>(new NamedStateEvent<>(stateEvent), stateMachine));
    } else {
      eventQueue.offer(new AbstractMap.SimpleEntry<>(stateEvent, stateMachine));
    }
    executor.execute(() -> {
      while (!eventQueue.isEmpty()) {
        Entry<StateEvent<S>, GenericStateMachine<T, S>> eventPair = eventQueue.peek();
        StateEvent<S> event = eventPair.getKey();
        GenericStateMachine<T, S> machine = eventPair.getValue();
        try {
          transitionTask.execute(event, machine);
        } finally {
          eventQueue.poll();
        }
      }
    });
  }

  ConcurrentLinkedQueue<Entry<StateEvent<S>, GenericStateMachine<T, S>>> getEventQueue() {
    return eventQueue;
  }

  static class Builder<T, S> {

    private final Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states;
    private final ExecutorService executor;
    private BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler;

    /**
     * The {@link ExecutorService} passed in here will be responsible for processing events, a
     * single thread executor is needed to guarantee sequential processing.
     */
    Builder(Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states, ExecutorService executor) {
      this.states = states;
      this.executor = executor;
    }

    public Builder<T, S> setUnmappedEventHandler(
        BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler) {
      this.unmappedEventHandler = unmappedEventHandler;
      return this;
    }

    BiConsumer<StateEvent<S>, StateMachine<T, S>> getUnmappedEventHandler() {
      return unmappedEventHandler;
    }

    public DefaultEventStrategy<T, S> build() {
      if (unmappedEventHandler == null) {
        unmappedEventHandler = (ev, sm) -> logger.info(LOG_EVENT_NOT_MAPPED, ev.getName(),
            sm.getCurrentState().getName());
      }
      return new DefaultEventStrategy<>(states, unmappedEventHandler, executor);
    }
  }
}
