/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.strategy;

import com.webotech.statemachine.UnexpectedFlowListener;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import com.webotech.statemachine.util.Threads;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EventProcessingStrategyFactory {

  private static final Logger logger = LogManager.getLogger(EventProcessingStrategyFactory.class);
  @SuppressWarnings("rawtypes")
  private static final Config basicConfig = new Config<>();

  private EventProcessingStrategyFactory() {
    // Not for instanciation outside this class
  }

  @SuppressWarnings("unchecked")
  public static <T, S> EventProcessingStrategy<T, S> createDefaultStrategy() {
    return createDefaultStrategy(basicConfig);
  }

  public static <T, S> EventProcessingStrategy<T, S> createDefaultStrategy(Config<T, S> config) {
    return newDefaultStrategy(config);
  }

  @SuppressWarnings("unchecked")
  public static <T, S> EventProcessingStrategy<T, S> createDropDuplicateStrategy() {
    return createDropDuplicateStrategy(basicConfig);
  }

  public static <T, S> EventProcessingStrategy<T, S> createDropDuplicateStrategy(
      Config<T, S> config) {
    return new DropDuplicateEventStrategy<>(newDefaultStrategy(config));
  }

  private static <T, S> DefaultEventStrategy<T, S> newDefaultStrategy(Config<T, S> config) {
    return new DefaultEventStrategy<>(config.getUnmappedEventHandler(), config.getExecutor(),
        config.getUnexpectedFlowListener(), config.getMaxQueueSize(), config.getEventQueue());
  }

  /**
   * Encapsulates data than can be used for configuring an {@link EventProcessingStrategy}.
   * Knowledge of specific strategy implementations is needed in order to understand how each
   * member of the {@link Config} is used. It is possible that an implementation ignores some of the
   * configuration options.
   */
  public static class Config<T, S> {

    private static final String LOG_EVENT_NOT_MAPPED = "StateEvent [{}] not mapped for state [{}], ignoring";
    private static final String LOG_UNHANDLED_EXCEPTION = "Unhandled exception in thread {}";
    private BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler;
    private ExecutorService executor;
    private UnexpectedFlowListener<T, S> unexpectedFlowListener;
    private int maxQueueSize = -1;
    private String threadName;
    private Queue<EventMachinePair<T, S>> eventQueue;

    public Config<T, S> withExecutor(ExecutorService executor) {
      this.executor = executor;
      return this;
    }

    public Config<T, S> withUnmappedEventHandler(
        BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler) {
      this.unmappedEventHandler = unmappedEventHandler;
      return this;
    }

    public Config<T, S> withUnexpectedFlowListener(
        UnexpectedFlowListener<T, S> unexpectedFlowListener) {
      this.unexpectedFlowListener = unexpectedFlowListener;
      return this;
    }

    /**
     * <b>Note</b> the event queue used in the {@link DefaultEventStrategy} and
     * {@link DropDuplicateEventStrategy} should be a thread-safe implementation for generic use
     * since typically events are fired by threads owned by third party subsystems.
     */
    public Config<T, S> withEventQueue(Queue<EventMachinePair<T, S>> eventQueue) {
      this.eventQueue = eventQueue;
      return this;
    }

    public Config<T, S> withMaxQueueSize(int maxQueueSize) {
      this.maxQueueSize = maxQueueSize;
      return this;
    }

    public Config<T, S> withThreadName(String threadName) {
      this.threadName = threadName;
      return this;
    }

    BiConsumer<StateEvent<S>, StateMachine<T, S>> getUnmappedEventHandler() {
      if (unmappedEventHandler == null) {
        unmappedEventHandler = (ev, sm) -> logger.info(LOG_EVENT_NOT_MAPPED, ev.getName(),
            sm.getCurrentState().getName());
      }
      return unmappedEventHandler;
    }

    ExecutorService getExecutor() {
      if (executor == null) {
        executor = Executors.newSingleThreadExecutor(
            Threads.newNamedDaemonThreadFactory(threadName == null ? "state-machine" : threadName,
                (t, e) -> logger.error(LOG_UNHANDLED_EXCEPTION, t.getName(), e)));
      }
      return executor;
    }

    UnexpectedFlowListener<T, S> getUnexpectedFlowListener() {
      if (unexpectedFlowListener == null) {
        unexpectedFlowListener = new DefaultUnexpectedFlowListener<>();
      }
      return unexpectedFlowListener;
    }

    int getMaxQueueSize() {
      return maxQueueSize;
    }

    String getThreadName() {
      return threadName;
    }

    Queue<EventMachinePair<T, S>> getEventQueue() {
      if (eventQueue == null) {
        eventQueue = new ConcurrentLinkedQueue<>();
      }
      return eventQueue;
    }
  }
}
