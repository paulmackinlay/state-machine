/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import com.webotech.statemachine.util.Threads;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DropDuplicateEventStrategy<T, S> implements EventProcessingStrategy<T, S> {

  private static final Logger logger = LogManager.getLogger(DropDuplicateEventStrategy.class);
  private static final String LOG_EVENT_NOT_MAPPED = "StateEvent [{}] not mapped for state [{}], ignoring";
  public static final String EVENT_ALREADY_IN_QUEUE_WILL_DROP_IT = "Event [{}] already in queue, will drop it";
  private final DefaultEventStrategy<T, S> defaultStrategy;

  /**
   * An {@link EventProcessingStrategy} that transitions state atomically. Any {@link StateEvent}
   * that is received when the internal queue already contains the same {@link StateEvent} is
   * dropped. {@link StateEvent}s in the queue are processed in sequence, in
   * the order they were received.
   */
  private DropDuplicateEventStrategy(Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states,
      BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler,
      ExecutorService executor, DefaultEventStrategy<T, S> defaultEventStrategy) {
    this.defaultStrategy = defaultEventStrategy;
  }

  @Override
  public int getEventQueueSize() {
    return defaultStrategy.getEventQueueSize();
  }

  @Override
  public void processEvent(StateEvent<S> stateEvent, GenericStateMachine<T, S> stateMachine) {
    ConcurrentLinkedQueue<Entry<StateEvent<S>, GenericStateMachine<T, S>>> eventQueue = defaultStrategy.getEventQueue();
    if (eventQueue.stream().anyMatch(en -> en.getKey().equals(stateEvent))) {
      logger.info(EVENT_ALREADY_IN_QUEUE_WILL_DROP_IT, stateEvent);
      return;
    }
    defaultStrategy.processEvent(stateEvent, stateMachine);
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

    /**
     * The {@link ExecutorService} passed in here will be responsible for processing events, a
     * single thread executor is needed to guarantee sequential processing.
     */
    public Builder<T, S> setExecutor(ExecutorService executor) {
      this.executor = executor;
      return this;
    }

    public DropDuplicateEventStrategy<T, S> build() {
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
      DefaultEventStrategy<T, S> defaultEventStrategy = new DefaultEventStrategy.Builder<>(
          stateMachineName, states).setExecutor(executor)
          .setUnmappedEventHandler(unmappedEventHandler).build();
      return new DropDuplicateEventStrategy<>(states, unmappedEventHandler, executor,
          defaultEventStrategy);
    }
  }
}
