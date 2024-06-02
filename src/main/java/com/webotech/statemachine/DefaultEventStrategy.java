/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;

public class DefaultEventStrategy<T, S> implements EventProcessingStrategy<T, S> {

  private final ConcurrentLinkedQueue<EventMachinePair<T, S>> eventQueue;
  private final ExecutorService executor;
  private final TransitionTask<T, S> transitionTask;
  private final UnexpectedFlowListener<T, S> unexpectedFlowListener;
  private final EventMachinePairPool<T, S> eventMachinePairPool;

  /**
   * The default {@link EventProcessingStrategy}, it transitions state atomically. All
   * {@link StateEvent}s are processed. By default {@link StateEvent}s are processed in sequence, in
   * the order they were received.
   */
  public DefaultEventStrategy(BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler,
      ExecutorService executor, UnexpectedFlowListener<T, S> unexpectedFlowListener,
      EventMachinePairPool<T, S> eventMachinePairPool) {
    this.executor = executor;
    this.unexpectedFlowListener = unexpectedFlowListener;
    this.eventMachinePairPool = eventMachinePairPool;
    this.eventQueue = new ConcurrentLinkedQueue<>();
    this.transitionTask = new TransitionTask<>(unmappedEventHandler);
  }

  @Override
  public int getEventQueueSize() {
    return eventQueue.size();
  }

  @Override
  public void processEvent(StateEvent<S> stateEvent, GenericStateMachine<T, S> stateMachine) {
    EventMachinePair<T, S> inboundPair = this.eventMachinePairPool.take();
    if (stateEvent.getPayload() != null) {
      /* Use a safe copy of the StateEvent in case the client is
        setting different payloads on the same event instance */
      inboundPair.setEventMachinePair(new NamedStateEvent<>(stateEvent), stateMachine);
    } else {
      inboundPair.setEventMachinePair(stateEvent, stateMachine);
    }
    eventQueue.offer(inboundPair);
    executor.execute(() -> {
      while (!eventQueue.isEmpty()) {
        EventMachinePair<T, S> consumedPair = eventQueue.poll();
        StateEvent<S> event = consumedPair.getStateEvent();
        GenericStateMachine<T, S> machine = consumedPair.getStateMachine();
        try {
          transitionTask.execute(event, machine);
        } catch (Exception e) {
          unexpectedFlowListener.onExceptionDuringEventProcessing(event, machine,
              Thread.currentThread(), e);
        } finally {
          eventMachinePairPool.give(consumedPair);
        }
      }
    });
  }

  @Override
  public void setStates(Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states) {
    this.transitionTask.setStates(states);
  }

  ConcurrentLinkedQueue<EventMachinePair<T, S>> getEventQueue() {
    return eventQueue;
  }
}
