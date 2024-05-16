/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import java.util.Map;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransitionTaskTest {

  private static final StateEvent<Void> event1 = new NamedStateEvent<>("event1");
  private static final StateEvent<Void> event2 = new NamedStateEvent<>("event2");
  private static final State<Void, Void> state1 = new NamedState("STATE-1");
  private static final State<Void, Void> state2 = new NamedState("STATE-2");
  private static final State<Void, Void> noopState = new NamedState<>(
      GenericStateMachine.RESERVED_STATE_NAME_NOOP);
  private static final State<Void, Void> endState = new NamedState<>(
      GenericStateMachine.RESERVED_STATE_NAME_END);
  private static final StateEvent<Void> immediateEvent = new NamedStateEvent<>(
      GenericStateMachine.RESERVED_STATE_EVENT_NAME_IMMEDIATE);
  private BiConsumer<StateEvent<Void>, StateMachine<Void, Void>> unmappedEventHandler;
  private GenericStateMachine<Void, Void> stateMachine;
  private TransitionTask<Void, Void> transitionTask;

  @BeforeEach
  void setup() {
    stateMachine = mock(GenericStateMachine.class);
    unmappedEventHandler = mock(BiConsumer.class);
    Map<State<Void, Void>, Map<StateEvent<Void>, State<Void, Void>>> states = Map.of(state1,
        Map.of(event1, state2), state2, Map.of(event1, noopState));
    transitionTask = new TransitionTask<>(states, unmappedEventHandler);

    when(stateMachine.getNoopState()).thenReturn(noopState);
    when(stateMachine.getCurrentState()).thenReturn(state1);
    when(stateMachine.getEndState()).thenReturn(endState);
    when(stateMachine.getImmediateEvent()).thenReturn(immediateEvent);
  }

  @Test
  void shouldHandleUnmappedEvent() {
    transitionTask.execute(event2, stateMachine);

    verify(unmappedEventHandler, times(1)).accept(event2, stateMachine);
  }

  @Test
  void shouldHandleNoopState() {
    when(stateMachine.getCurrentState()).thenReturn(state2);

    transitionTask.execute(event1, stateMachine);

    // notify to NOOP state before/after
    verify(stateMachine, times(1)).notifyStateMachineListener(false, state2,
        event1, noopState);
    verify(stateMachine, times(1)).notifyStateMachineListener(true, state2,
        event1, noopState);

    // ensure no state change
    verify(stateMachine, times(0)).setCurrentState(any());
  }

  @Test
  void shouldTransition() {
    State<Void, Void> mockState = mock(State.class);
    when(stateMachine.getCurrentState()).thenReturn(state1, state1, mockState);

    transitionTask.execute(event1, stateMachine);

    // notification before/after state transition
    verify(stateMachine, times(1)).notifyStateMachineListener(false, state1, event1, state2);
    verify(stateMachine, times(1)).notifyStateMachineListener(false, state1, event1, state2);

    // transition
    verify(stateMachine, times(1)).setCurrentState(state2);

    // entry/exit actions
    verify(mockState, times(1)).onExit(event1, stateMachine);
    verify(mockState, times(1)).onEntry(event1, stateMachine);
  }
}