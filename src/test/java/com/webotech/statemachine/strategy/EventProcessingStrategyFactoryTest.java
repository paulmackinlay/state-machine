/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import com.webotech.statemachine.UnexpectedFlowListener;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import com.webotech.statemachine.strategy.EventProcessingStrategyFactory.Config;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;

class EventProcessingStrategyFactoryTest {

  @Test
  void shouldBuildWithExecutorService() {
    ExecutorService executor = mock(ExecutorService.class);
    Config<Void, Void> config = new Config<Void, Void>().withExecutor(executor);
    assertSame(executor, config.getExecutor());
  }

  @Test
  void shouldBuildWithUnmappedEventHandler() {
    BiConsumer<StateEvent<Void>, StateMachine<Void, Void>> unmappedEventHander = mock(
        BiConsumer.class);
    Config<Void, Void> config = new Config<Void, Void>().withUnmappedEventHandler(
        unmappedEventHander);
    assertSame(unmappedEventHander, config.getUnmappedEventHandler());
  }

  @Test
  void shouldBuildWithUnexpectedFlowListener() {
    UnexpectedFlowListener<Void, Void> unexpectedFlowListener = mock(UnexpectedFlowListener.class);
    Config<Void, Void> config = new Config<Void, Void>().withUnexpectedFlowListener(
        unexpectedFlowListener);
    assertSame(unexpectedFlowListener, config.getUnexpectedFlowListener());
  }

  @Test
  void shouldBuildWithQueueSize() {
    int queueSize = 123;
    Config<Void, Void> config = new Config<Void, Void>().withMaxQueueSize(queueSize);
    assertEquals(queueSize, config.getMaxQueueSize());
  }

  @Test
  void shouldBuildWithThreadName() {
    String threadName = "a-name";
    Config<Void, Void> config = new Config<Void, Void>().withThreadName(threadName);
    assertSame(threadName, config.getThreadName());
  }

  @Test
  void shouldBuildWithQueue() {
    Queue<EventMachinePair<Void, Void>> queue = mock(Queue.class);
    Config<Void, Void> config = new Config<Void, Void>().withEventQueue(queue);
    assertSame(queue, config.getEventQueue());
  }

  @Test
  void shouldCreateIndependentStrategies() {
    EventProcessingStrategy<Void, Void> strategy1 = EventProcessingStrategyFactory.createDefaultStrategy();
    EventProcessingStrategy<Void, Void> strategy2 = EventProcessingStrategyFactory.createDefaultStrategy();

    assertEquals(0, strategy1.getEventQueueSize());
    assertNotNull(strategy1.getUnexpectedFlowListener());
    assertInstanceOf(DefaultEventStrategy.class, strategy1);
    assertNotNull(((DefaultEventStrategy) strategy1).getEventQueue());
    assertNotSame(strategy1, strategy2);
  }

  @Test
  void shouldCreateDropDuplicateEventIndependentStrategies() {
    EventProcessingStrategy<Void, Void> strategy1 = EventProcessingStrategyFactory.createDropDuplicateStrategy();
    EventProcessingStrategy<Void, Void> strategy2 = EventProcessingStrategyFactory.createDropDuplicateStrategy();

    assertEquals(0, strategy1.getEventQueueSize());
    assertNotNull(strategy1.getUnexpectedFlowListener());
    assertInstanceOf(DropDuplicateEventStrategy.class, strategy1);
    assertNotSame(strategy1, strategy2);
  }

  @Test
  void shouldCreateConfiguredDefaultStrategy() {
    ExecutorService executor = mock(ExecutorService.class);
    String threadName = "my-test-tread";
    Queue<EventMachinePair<Void, Void>> eventQueue = mock(Queue.class);
    BiConsumer<StateEvent<Void>, StateMachine<Void, Void>> unmappedEventHandler = mock(
        BiConsumer.class);
    UnexpectedFlowListener<Void, Void> unexpectedFlowListener = mock(UnexpectedFlowListener.class);
    Config<Void, Void> config = new Config<Void, Void>().withExecutor(executor)
        .withThreadName(threadName).withEventQueue(eventQueue).withMaxQueueSize(10)
        .withUnmappedEventHandler(unmappedEventHandler)
        .withUnexpectedFlowListener(unexpectedFlowListener);
    EventProcessingStrategy<Void, Void> strategy = EventProcessingStrategyFactory.createDefaultStrategy(
        config);
    DefaultEventStrategy<Void, Void> defaultEventStrategy = (DefaultEventStrategy) strategy;

    assertSame(eventQueue, defaultEventStrategy.getEventQueue());
    assertEquals(0, defaultEventStrategy.getEventQueueSize());
    assertSame(unexpectedFlowListener, defaultEventStrategy.getUnexpectedFlowListener());
  }

  @Test
  void shouldCreateConfiguredDropDuplicateEventStrategy() {
    ExecutorService executor = mock(ExecutorService.class);
    String threadName = "my-test-tread";
    Queue<EventMachinePair<Void, Void>> eventQueue = mock(Queue.class);
    BiConsumer<StateEvent<Void>, StateMachine<Void, Void>> unmappedEventHandler = mock(
        BiConsumer.class);
    UnexpectedFlowListener<Void, Void> unexpectedFlowListener = mock(UnexpectedFlowListener.class);
    Config<Void, Void> config = new Config<Void, Void>().withExecutor(executor)
        .withThreadName(threadName).withEventQueue(eventQueue).withMaxQueueSize(10)
        .withUnmappedEventHandler(unmappedEventHandler)
        .withUnexpectedFlowListener(unexpectedFlowListener);
    EventProcessingStrategy<Void, Void> strategy = EventProcessingStrategyFactory.createDropDuplicateStrategy(
        config);
    DropDuplicateEventStrategy<Void, Void> dropDuplicateEventStrategy = (DropDuplicateEventStrategy) strategy;

    assertEquals(0, dropDuplicateEventStrategy.getEventQueueSize());
    assertSame(unexpectedFlowListener, dropDuplicateEventStrategy.getUnexpectedFlowListener());
  }
}
