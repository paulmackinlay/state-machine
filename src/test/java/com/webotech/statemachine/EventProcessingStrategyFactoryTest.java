/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import com.webotech.statemachine.EventProcessingStrategyFactory.Config;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class EventProcessingStrategyFactoryTest {

  private EventProcessingStrategyFactory strategyFactory;

  @BeforeEach
  void setup() {
    strategyFactory = new EventProcessingStrategyFactory();
  }

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
  @Disabled
  void shouldDo() {
    /**
     * TODO test building other types of strategy and work out best way of passing in Queue
     */
    EventProcessingStrategy<Void, Void> strategy = strategyFactory.createDefaultStrategy();
    fail("TODO");
  }
}
