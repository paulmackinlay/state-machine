/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A task that transitions a {@link StateMachine}. For use in an {@link EventProcessingStrategy}.
 */
public class TransitionTask<T, S> {

  private final Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states;
  private final BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler;

  public TransitionTask(Map<State<T, S>, Map<StateEvent<S>, State<T, S>>> states,
      BiConsumer<StateEvent<S>, StateMachine<T, S>> unmappedEventHandler) {
    this.states = states;
    this.unmappedEventHandler = unmappedEventHandler;
  }

  void execute(StateEvent<S> event, GenericStateMachine<T, S> machine) {
    State<T, S> toState = this.states.get(machine.getCurrentState()).get(event);
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
  }
}