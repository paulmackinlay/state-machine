package com.webotech.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.webotech.statemachine.api.StateEvent;
import org.junit.jupiter.api.Test;

class NamedStateEventTest {

  @Test
  void shouldHaveNameBasedEquality() {
    StateEvent event1 = new NamedStateEvent("event");
    StateEvent event2 = new NamedStateEvent("event");
    StateEvent event3 = new NamedStateEvent("other event");
    assertEquals(event1, event2);
    assertNotEquals(event1, event3);
  }
}