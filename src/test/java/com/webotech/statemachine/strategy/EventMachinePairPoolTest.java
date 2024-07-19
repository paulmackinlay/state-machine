/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.strategy;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventMachinePairPoolTest {

  private EventMachinePairPool<Void, Void> eventMachinePairPool;

  @BeforeEach
  void setup() {
    eventMachinePairPool = new EventMachinePairPool<>();
  }

  @Test
  void shouldTakeNewObjects() {
    EventMachinePair<Void, Void> pair1 = eventMachinePairPool.take();
    EventMachinePair<Void, Void> pair2 = eventMachinePairPool.take();
    assertNotNull(pair1);
    assertNotNull(pair2);
    assertNotSame(pair1, pair2);
  }

  @Test
  void shouldReuseObjects() {
    EventMachinePair<Void, Void> pair1 = eventMachinePairPool.take();
    assertNotNull(pair1);
    eventMachinePairPool.give(pair1);
    EventMachinePair<Void, Void> pair2 = eventMachinePairPool.take();
    assertNotNull(pair2);
    assertSame(pair1, pair2);
  }

  @Test
  void shouldHandleNulls() {
    try {
      eventMachinePairPool.give(null);
    } catch (Exception e) {
      fail("Expect no exception");
    }
  }
}
