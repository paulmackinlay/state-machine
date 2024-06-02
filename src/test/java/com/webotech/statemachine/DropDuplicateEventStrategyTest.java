/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.webotech.statemachine.api.StateEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DropDuplicateEventStrategyTest {

  private static final StateEvent<Void> event1 = new NamedStateEvent<>("event1");
  private GenericStateMachine<Void, Void> stateMachine;
  private DropDuplicateEventStrategy<Void, Void> strategy;
  private DefaultEventStrategy<Void, Void> defaultStrategy;

  @BeforeEach
  void setup() {
    stateMachine = mock(GenericStateMachine.class, Mockito.RETURNS_DEEP_STUBS);
    defaultStrategy = mock(DefaultEventStrategy.class);
    strategy = new DropDuplicateEventStrategy<>(defaultStrategy);
  }

  @Test
  void shouldDropDuplicateEvent() throws IOException {
    ConcurrentLinkedQueue<EventMachinePair<Void, Void>> eventQueue = new ConcurrentLinkedQueue<>();
    when(defaultStrategy.getEventQueue()).thenReturn(eventQueue);
    EventMachinePair<Void, Void> eventMachinePair = new EventMachinePair<>();
    eventMachinePair.setEventMachinePair(event1, stateMachine);
    eventQueue.offer(eventMachinePair);
    try (OutputStream logStream = TestingUtil.initLogCaptureStream()) {
      this.strategy.processEvent(event1, stateMachine);
      assertEquals("Event [NamedStateEvent[event1]] already in queue, will drop it\n",
          logStream.toString());
    }
  }
}
