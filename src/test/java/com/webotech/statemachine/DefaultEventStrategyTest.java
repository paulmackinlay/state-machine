/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import com.webotech.statemachine.DefaultEventStrategy.Builder;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultEventStrategyTest {

  private DefaultEventStrategy<Void, Void> strategy;

  @BeforeEach
  void setup() {
    strategy = new DefaultEventStrategy.Builder<Void, Void>("state-machine",
        new HashMap<>()).build();
  }

  @Test
  void shouldBuildWithUnmappedEventHandler() {
    BiConsumer<StateEvent<Void>, StateMachine<Void, Void>> unmappedEventHander = (se, sm) -> {
    };
    Builder<Void, Void> builder = new DefaultEventStrategy.Builder<Void, Void>("state-machine",
        new HashMap<>()).setUnmappedEventHandler(unmappedEventHander);
    assertSame(unmappedEventHander, builder.getUnmappedEventHandler());
  }

  @Test
  void shouldBuildWithExecutor() {
    ExecutorService executor = mock(ExecutorService.class);
    Builder<Void, Void> builder = new DefaultEventStrategy.Builder<Void, Void>("state-machine",
        new HashMap<>()).setExecutor(executor);
    assertSame(executor, builder.getExecutor());
  }
}