/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;

public class DefaultEventStrategy<T, S> implements EventProcessingStrategy<T, S> {

  private final Queue<EventMachinePair<T, S>> eventQueue;
  private final ExecutorService executor;
  private final TransitionTask<T, S> transitionTask;
  private final UnexpectedFlowListener<T, S> unexpectedFlowListener;
  private final EventMachinePairPool<T, S> eventMachinePairPool;
  private final int maxQueueSize;

  /**
   * The default {@link EventProcessingStrategy}, it transitions state atomically. All
   * {@link StateEvent}s are processed. By default {@link StateEvent}s are processed in sequence,
   * in the order they were received.
   * <p>
   * This {@link EventProcessingStrategy} is backed by a lock-free and thread-safe queue. The
   * maxQueueSize parameter is used control how many events can queue up for processing.
   * <p>
   * If the parameter is negative, the queue is unbound and in the case where the sustained rate of
   * {@link StateEvent}s received is higher than the rate they are being processed (slow
   * consumption), it will ultimately lead to memory starvation and a possible our of memory error.
   * <p>
   * If the maxQueueSize is positive it will bound the queue's size and in the case where the
   * sustained rate of {@link StateEvent}s received is higher than the rate they are being processed
   * (slow consumption), it will ultimately lead to {@link IllegalStateException}s being called back
   * on {@link UnexpectedFlowListener#onExceptionDuringEventProcessing(StateEvent, StateMachine,
   * Thread, Exception)} and the {@link StateEvent} will not be processed.
   */
  public DefaultEventStrategy(BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler,
      ExecutorService executor, UnexpectedFlowListener<T, S> unexpectedFlowListener,
      EventMachinePairPool<T, S> eventMachinePairPool, int maxQueueSize) {
    this.executor = executor;
    this.unexpectedFlowListener = unexpectedFlowListener;
    this.eventMachinePairPool = eventMachinePairPool;
    this.eventQueue = new ConcurrentLinkedQueue<>();
    this.transitionTask = new TransitionTask<>(unmappedEventHandler);
    this.maxQueueSize = maxQueueSize;
  }

  @Override
  public int getEventQueueSize() {
    return eventQueue.size();
  }

  @Override
  public void processEvent(StateEvent<S> stateEvent, GenericStateMachine<T, S> stateMachine) {
    int queueSize = getEventQueueSize();
    if (maxQueueSize > 0 && queueSize >= maxQueueSize) {
      unexpectedFlowListener.onExceptionDuringEventProcessing(stateEvent, stateMachine,
          Thread.currentThread(), new IllegalStateException(
              String.format("Queue size is maxed out at %s - dropping event", queueSize)));
      return;
    }
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

  protected Queue<EventMachinePair<T, S>> getEventQueue() {
    return eventQueue;
  }

  protected UnexpectedFlowListener<T, S> getUnexpectedFlowListener() {
    return unexpectedFlowListener;
  }
}
