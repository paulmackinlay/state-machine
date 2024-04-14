package com.webotech.statemachine.api;

public interface StateAction<T> {

    void execute(StateMachine<T> stateMachine);

}
