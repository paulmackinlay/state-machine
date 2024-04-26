/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.webotech.statemachine.api.StateEvent;
import org.junit.jupiter.api.Test;

class NamedStateEventTest {

  @Test
  void shouldStoreNonFinalPayload() {
    StateEvent<String> event = new NamedStateEvent<>("event");
    event.setPayload("payload 1");
    assertEquals("payload 1", event.getPayload());
    event.setPayload("payload 2");
    assertEquals("payload 2", event.getPayload());
  }

  @Test
  void shouldHaveNameBasedEquality() {
    StateEvent<Void> event1 = new NamedStateEvent<>("event");
    StateEvent<Void> event2 = new NamedStateEvent<>("event");
    StateEvent<Void> event3 = new NamedStateEvent<>("other event");
    assertEquals(event1, event2);
    assertNotEquals(event1, event3);
  }
}