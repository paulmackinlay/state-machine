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

//TODO
public class EventProcessingStrategyFactory<T, S> {

  private static final Logger logger = LogManager.getLogger(EventProcessingStrategyFactory.class);

  /*
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

  public EventProcessingStrategy<T, S> createStrategy(Config<T, S> config) {
   return new DefaultEventStrategy<T, S>(config.getUnmappedEventHandler(), config.getExecutor(),
        config.getUnexpectedFlowListener(), config.getMaxQueueSize());
  }

  public class Config<T, S> {

    private static final String LOG_EVENT_NOT_MAPPED = "StateEvent [{}] not mapped for state [{}], ignoring";
    private static final String LOG_UNHANDLED_EXCEPTION = "Unhandled exception in thread {}";
    private ExecutorService executor;
    private UnexpectedFlowListener<T, S> unexpectedFlowListener;
    private Queue eventQueue;
    private int maxQueueSize = -1;
    private BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler;
    private String threadName = "state-machine"

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

    public Config<T, S> withEventQueue(Queue eventQueue) {
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

    public ExecutorService getExecutor() {
      if (executor == null) {
        executor = Executors.newSingleThreadExecutor(
            Threads.newNamedDaemonThreadFactory(threadName == null ? "state-machine" : threadName,
                (t, e) -> logger.error(LOG_UNHANDLED_EXCEPTION, t.getName(), e)));
      }
      return executor;
    }

    public UnexpectedFlowListener<T, S> getUnexpectedFlowListener() {
      if (unexpectedFlowListener == null) {
        unexpectedFlowListener = new DefaultUnexpectedFlowListener<>();
      }
      return unexpectedFlowListener;
    }

    public int getMaxQueueSize() {
      return maxQueueSize;
    }
  }
}
