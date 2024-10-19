/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine.strategy;

import com.webotech.statemachine.GenericStateMachine;
import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A task that transitions a {@link StateMachine}. For use in an {@link EventProcessingStrategy}.
 */
public class TransitionTask<T, S> {

  private final BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler;
  private Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states;

  public TransitionTask(BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler) {
    this.unmappedEventHandler = unmappedEventHandler;
  }

  void execute(StateEvent<S> event, GenericStateMachine<T, S> machine) {
    //TODO work out how stateEventStateMap can be null
    // Map<StateEvent<S>, State<T, S>> stateEventStateMap = states.get(machine.getCurrentState());
    State<T, S> toState = states.get(machine.getCurrentState()).get(event);
    if (toState == null) {
      unmappedEventHandler.accept(event, machine);
      return;
    }
    if (machine.getNoopState().equals(toState)) {
      // No transition but notify the listener so it can tell a StateEvent was received
      machine.notifyStateMachineListener(false, machine.getCurrentState(), event, toState);
      machine.notifyStateMachineListener(true, machine.getCurrentState(), event, toState);
      return;
    }
    State<T, S> fromState = machine.getCurrentState();
    machine.notifyStateMachineListener(false, fromState, event, toState);
    machine.getCurrentState().onExit(event, machine);
    machine.setCurrentState(toState);
    machine.getCurrentState().onEntry(event, machine);
    machine.notifyStateMachineListener(true, fromState, event, toState);
    if (states.get(toState) == null || machine.getEndState()
        .equals(states.get(toState).get(machine.getImmediateEvent()))) {
      machine.stop();
    }
  }

  public void setStates(Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states) {
    this.states = states;
  }
}
