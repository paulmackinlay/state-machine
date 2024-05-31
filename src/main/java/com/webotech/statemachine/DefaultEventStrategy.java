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

public class DefaultEventStrategy<T, S> implements EventProcessingStrategy<T, S> {

  private final ConcurrentLinkedQueue<Entry<StateEvent<S>, GenericStateMachine<T, S>>> eventQueue;
  private final ExecutorService executor;
  private final TransitionTask<T, S> transitionTask;
  private final UnexpectedFlowListener<T, S> unexpectedFlowListener;

  /**
   * The default {@link EventProcessingStrategy}, it transitions state atomically. All
   * {@link StateEvent}s are processed. By default {@link StateEvent}s are processed in sequence, in
   * the order they were received.
   */
  public DefaultEventStrategy(BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler,
      ExecutorService executor, UnexpectedFlowListener<T, S> unexpectedFlowListener) {
    this.executor = executor;
    this.eventQueue = new ConcurrentLinkedQueue<>();
    this.transitionTask = new TransitionTask<>(unmappedEventHandler);
    this.unexpectedFlowListener = unexpectedFlowListener;
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
        Entry<StateEvent<S>, GenericStateMachine<T, S>> eventPair = eventQueue.poll();
        StateEvent<S> event = eventPair.getKey();
        GenericStateMachine<T, S> machine = eventPair.getValue();
        try {
          transitionTask.execute(event, machine);
        } catch (Exception e) {
          unexpectedFlowListener.onExceptionDuringEventProcessing(event, machine,
              Thread.currentThread(), e);
        }
      }
    });
  }

  @Override
  public void setStates(Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states) {
    this.transitionTask.setStates(states);
  }

  ConcurrentLinkedQueue<Entry<StateEvent<S>, GenericStateMachine<T, S>>> getEventQueue() {
    return eventQueue;
  }
}
