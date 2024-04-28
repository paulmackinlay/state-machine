/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateAction;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NamedStateTest {

  private State<Void, Void> state;
  private StateMachine<Void, Void> stateMachine;
  private StateEvent<Void> event;

  @BeforeEach
  void setup() {
    stateMachine = mock(StateMachine.class);
    state = new NamedState<>("state");
    event = mock(StateEvent.class);
  }

  @SuppressWarnings("unchecked")
  @Test
  void shouldHaveNameBasedEquality() {
    State<Void, Void> state1 = new NamedState<>("state");
    state1.appendEntryActions((ev, sm) -> sm.notify());
    State<Void, Void> state2 = new NamedState<>("state");
    State<Void, Void> state3 = new NamedState<>("other state");
    assertEquals(state1, state2);
    assertNotEquals(state1, state3);
  }

  @SuppressWarnings("unchecked")
  @Test
  void shouldFireNonNullActions() {
    AtomicInteger entryInteger = new AtomicInteger();
    AtomicInteger exitInteger = new AtomicInteger();
    StateAction<Void, Void> entryAction = (ev, sm) -> entryInteger.incrementAndGet();
    StateAction<Void, Void> exitAction = (ev, sm) -> exitInteger.incrementAndGet();
    state.appendEntryActions(entryAction, null, entryAction);
    state.appendExitActions(exitAction, null, exitAction, exitAction);
    assertEquals(0, entryInteger.get());
    assertEquals(0, exitInteger.get());
    state.onEntry(event, stateMachine);
    assertEquals(2, entryInteger.get());
    assertEquals(0, exitInteger.get());
    state.onExit(event, stateMachine);
    assertEquals(2, entryInteger.get());
    assertEquals(3, exitInteger.get());
  }
}
