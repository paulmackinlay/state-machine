/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import com.webotech.statemachine.util.Threads;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//TODO test this
public class DefaultEventStrategy<T, S> implements EventProcessingStrategy<T, S> {

  private static final Logger logger = LogManager.getLogger(DefaultEventStrategy.class);
  private static final String LOG_EVENT_NOT_MAPPED = "StateEvent [{}] not mapped for state [{}], ignoring";
  private final ConcurrentLinkedQueue<Entry<StateEvent<S>, GenericStateMachine<T, S>>> eventQueue;
  private final Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states;
  private final ExecutorService executor;
  private final BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler;

  /**
   * The default {@link EventProcessingStrategy}, it transitions state atomically, duplicate
   * {@link StateEvent}s received by a single {@link State} are logged but not processed.
   */
  private DefaultEventStrategy(Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states,
      BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler) {
    this.eventQueue = new ConcurrentLinkedQueue<>();
    this.states = states;
    this.unmappedEventHandler = unmappedEventHandler;
    //TODO improve this
    this.executor = Executors.newSingleThreadExecutor(
        Threads.newNamedDaemonThreadFactory("state-machine", (t, e) -> {
          //TODO
          logger.error("Exception on thread " + t.getName(), e);
        }));
  }

  @Override
  public int getEventQueueSize() {
    return eventQueue.size();
  }

  @Override
  public void processEvent(StateEvent<S> stateEvent2, GenericStateMachine<T, S> stateMachine2) {
    eventQueue.offer(new AbstractMap.SimpleEntry<>(stateEvent2, stateMachine2));

    executor.execute(() -> {
      while (!eventQueue.isEmpty()) {
        Entry<StateEvent<S>, GenericStateMachine<T, S>> eventPair = eventQueue.peek();
        StateEvent<S> stateEvent = eventPair.getKey();
        GenericStateMachine<T, S> stateMachine = eventPair.getValue();
        try {
          State<T, S> toState = this.states.get(stateMachine.getCurrentState()).get(stateEvent);
          if (toState == null) {
            unmappedEventHandler.accept(stateEvent, stateMachine);
            return;
          }
          if (stateMachine.getNoopState().equals(toState)) {
            // No transition but notify the listener so can tell a StateEvent was processed
            stateMachine.notifyStateMachineListener(false, stateMachine.getCurrentState(),
                stateEvent, toState);
            stateMachine.notifyStateMachineListener(true, stateMachine.getCurrentState(),
                stateEvent, toState);
            return;
          }
          State<T, S> fromState = stateMachine.getCurrentState();
          stateMachine.notifyStateMachineListener(false, fromState, stateEvent, toState);
          stateMachine.getCurrentState().onExit(stateEvent, stateMachine);
          stateMachine.setCurrentState(toState);
          stateMachine.getCurrentState().onEntry(stateEvent, stateMachine);
          stateMachine.notifyStateMachineListener(true, fromState, stateEvent, toState);
        } finally {
          eventQueue.poll();
        }
      }
    });
  }

  //TODO test the builder
  static class Builder<T, S> {

    private final Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states;
    private BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler;

    Builder(Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states) {
      this.states = states;
    }

    public Builder<T, S> setUnmappedEventHandler(
        BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler) {
      this.unmappedEventHandler = unmappedEventHandler;
      return this;
    }

    public DefaultEventStrategy<T, S> build() {
      if (unmappedEventHandler == null) {
        unmappedEventHandler = (ev, sm) -> logger.info(LOG_EVENT_NOT_MAPPED, ev.getName(),
            sm.getCurrentState().getName());
      }
      return new DefaultEventStrategy<>(states, unmappedEventHandler);
    }
  }
}
