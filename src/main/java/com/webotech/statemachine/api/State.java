package com.webotech.statemachine.api;

public interface State<T> {

    void onEntry(StateMachine<T> stateMachine);

    void onExit(StateMachine<T> stateMachine);

    @SuppressWarnings("unchecked")
    void appendEntryActions(StateAction<T>... actions);

    @SuppressWarnings("unchecked")
    void appendExitActions(StateAction<T>... actions);

    String getName();
}
