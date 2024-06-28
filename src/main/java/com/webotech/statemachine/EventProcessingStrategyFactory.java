/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import com.webotech.statemachine.util.Threads;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EventProcessingStrategyFactory {

  private static final Logger logger = LogManager.getLogger(EventProcessingStrategyFactory.class);
  @SuppressWarnings("rawtypes")
  private static final Config basicConfig = new Config<>();

  /*
  TODO
  needs some kind of builder and factory pattern

  - queue size
  - with unmappedEvent

      this.executor = executor;
      this.unexpectedFlowListener = unexpectedFlowListener;
      this.eventMachinePairPool = eventMachinePairPool;
      this.eventQueue = new ConcurrentLinkedQueue<>();
      this.maxQueueSize = maxQueueSize;


  factory.getDefaultBuilder.withX(),withY().build();
  factory.getStrategy(Config)

   */

  @SuppressWarnings("unchecked")
  public <T, S> EventProcessingStrategy<T, S> createDefaultStrategy() {
    return createDefaultStrategy(basicConfig);
  }

  public <T, S> EventProcessingStrategy<T, S> createDefaultStrategy(Config<T, S> config) {
    return newDefaultStrategy(config);
  }

  public <T, S> EventProcessingStrategy<T, S> createDropDuplicateStrategy(Config<T, S> config) {
    return new DropDuplicateEventStrategy<>(newDefaultStrategy(config));
  }

  private <T, S> DefaultEventStrategy<T, S> newDefaultStrategy(Config<T, S> config) {
    return new DefaultEventStrategy<>(config.getUnmappedEventHandler(), config.getExecutor(),
        config.getUnexpectedFlowListener(), config.getMaxQueueSize());
  }


  public static class Config<T, S> {

    private static final String LOG_EVENT_NOT_MAPPED = "StateEvent [{}] not mapped for state [{}], ignoring";
    private static final String LOG_UNHANDLED_EXCEPTION = "Unhandled exception in thread {}";
    private ExecutorService executor;
    private UnexpectedFlowListener<T, S> unexpectedFlowListener;
    private Queue<EventMachinePair<T, S>> eventQueue;
    private int maxQueueSize = -1;
    private BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler;
    private String threadName;

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
      return eventQueue;
    }
  }
}
