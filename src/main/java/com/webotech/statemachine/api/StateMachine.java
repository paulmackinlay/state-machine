package com.webotech.statemachine.api;

public interface StateMachine<T> {

    StateMachine<T> initialSate(State<T> initState);

    StateMachine<T> when(State<T> state);

    StateMachine<T> receives(StateEvent stateEvent);

    StateMachine<T> itEnds();

    StateMachine<T> itTransitionsTo(State<T> state);

    void start();

    void fire(StateEvent stateEvent);

    State<T> getCurrentState();
    
    T getContext();

    void setStateMachineListener(StateMachineListener<T> stateMachineListener);
}
