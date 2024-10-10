/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateAction;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import org.junit.jupiter.api.Test;

class LifecycleStateMachineUtilTest {

  @Test
  void shouldUseSpecifiedNamesInStates() {
    assertEquals(LifecycleStateMachineUtil.STATE_UNINITIALISED,
        LifecycleStateMachineUtil.newUnitialisedState().getName());
    assertEquals(LifecycleStateMachineUtil.STATE_STARTING,
        LifecycleStateMachineUtil.newStartingState().getName());
    assertEquals(LifecycleStateMachineUtil.STATE_STARTED,
        LifecycleStateMachineUtil.newStartedState().getName());
    assertEquals(LifecycleStateMachineUtil.STATE_STOPPING,
        LifecycleStateMachineUtil.newStoppingState().getName());
    assertEquals(LifecycleStateMachineUtil.STATE_STOPPED,
        LifecycleStateMachineUtil.newStoppedState().getName());
  }

  @Test
  void shouldAppendActionsToStates() {
    StateAction<Void, Void> stateAction = mock(StateAction.class);
    assertActionExecuted(stateAction, LifecycleStateMachineUtil.newUnitialisedState(stateAction));
    assertActionExecuted(stateAction, LifecycleStateMachineUtil.newStartingState(stateAction));
    assertActionExecuted(stateAction, LifecycleStateMachineUtil.newStartedState(stateAction));
    assertActionExecuted(stateAction, LifecycleStateMachineUtil.newStoppingState(stateAction));
    assertActionExecuted(stateAction, LifecycleStateMachineUtil.newStoppedState(stateAction));
  }

  static void assertActionExecuted(StateAction<Void, Void> stateAction, State<Void, Void> state) {
    StateMachine<Void, Void> stateMachine = mock(StateMachine.class);
    StateEvent<Void> stateEvent = mock(StateEvent.class);
    state.onEntry(stateEvent, stateMachine);
    verify(stateAction, times(1)).execute(stateEvent, stateMachine);
  }
}