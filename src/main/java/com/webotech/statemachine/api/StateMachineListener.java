package com.webotech.statemachine.api;

public interface StateMachineListener<T> {

    void onStateChangeBegin(State<T> fromState, StateEvent event, State<T> toState);

    void onStateChangeEnd(State<T> fromState, StateEvent event, State<T> toState);

}
