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
      BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler,
      ExecutorService executor) {
    this.eventQueue = new ConcurrentLinkedQueue<>();
    this.states = states;
    this.unmappedEventHandler = unmappedEventHandler;
    this.executor = executor;
  }

  @Override
  public int getEventQueueSize() {
    return eventQueue.size();
  }

  @Override
  public void processEvent(StateEvent<S> stateEvent, GenericStateMachine<T, S> stateMachine) {
    eventQueue.offer(new AbstractMap.SimpleEntry<>(stateEvent, stateMachine));

    executor.execute(() -> {
      while (!eventQueue.isEmpty()) {
        Entry<StateEvent<S>, GenericStateMachine<T, S>> eventPair = eventQueue.peek();
        StateEvent<S> event = eventPair.getKey();
        GenericStateMachine<T, S> machine = eventPair.getValue();
        try {
          State<T, S> toState = this.states.get(machine.getCurrentState()).get(event);
          if (toState == null) {
            unmappedEventHandler.accept(event, machine);
            return;
          }
          if (machine.getNoopState().equals(toState)) {
            // No transition but notify the listener so can tell a StateEvent was processed
            machine.notifyStateMachineListener(false, machine.getCurrentState(), event, toState);
            machine.notifyStateMachineListener(true, machine.getCurrentState(), event, toState);
            return;
          }
          State<T, S> fromState = machine.getCurrentState();
          machine.notifyStateMachineListener(false, fromState, event, toState);
          machine.getCurrentState().onExit(event, machine);
          machine.setCurrentState(toState);
          machine.getCurrentState().onEntry(event, machine);
          machine.notifyStateMachineListener(true, fromState, event, toState);
        } finally {
          eventQueue.poll();
        }
      }
    });
  }

  static class Builder<T, S> {

    private final Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states;
    private final String stateMachineName;
    private BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler;
    private ExecutorService executor;

    Builder(String stateMachineName, Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states) {
      this.states = states;
      this.stateMachineName = stateMachineName;
    }

    public Builder<T, S> setUnmappedEventHandler(
        BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler) {
      this.unmappedEventHandler = unmappedEventHandler;
      return this;
    }

    BiConsumer<StateEvent<S>, StateMachine<T, S>> getUnmappedEventHandler() {
      return unmappedEventHandler;
    }

    ExecutorService getExecutor() {
      return executor;
    }

    public Builder<T, S> setExecutor(ExecutorService executor) {
      this.executor = executor;
      return this;
    }

    public DefaultEventStrategy<T, S> build() {
      if (unmappedEventHandler == null) {
        unmappedEventHandler = (ev, sm) -> logger.info(LOG_EVENT_NOT_MAPPED, ev.getName(),
            sm.getCurrentState().getName());
      }
      if (executor == null) {
        executor = Executors.newSingleThreadExecutor(
            Threads.newNamedDaemonThreadFactory(stateMachineName, (t, e) -> {
              logger.error("Unhandled exception in thread {}", t.getName(), e);
            }));
      }
      return new DefaultEventStrategy<>(states, unmappedEventHandler, executor);
    }
  }
}
