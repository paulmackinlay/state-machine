/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateAction;
import com.webotech.statemachine.api.StateMachine;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Stream;

/**
 * A {@link State}, each instance with the same name is are {@link #equals(Object)}
 */
public final class NamedState<T, S> implements State<T, S> {

  private final String name;
  private final List<StateAction<T, S>> entryActions;
  private final List<StateAction<T, S>> exitActions;

  public NamedState(String name) {
    this.name = name;
    this.entryActions = new ArrayList<>();
    this.exitActions = new ArrayList<>();
  }

  @Override
  public void onEntry(StateMachine<T, S> stateMachine) {
    this.entryActions.forEach(a -> a.execute(stateMachine));
  }

  @Override
  public void onExit(StateMachine<T, S> stateMachine) {
    this.exitActions.forEach(a -> a.execute(stateMachine));
  }

  @SuppressWarnings("unchecked")
  @Override
  public void appendEntryActions(StateAction<T, S>... actions) {
    Stream.of(actions).filter(Objects::nonNull).forEach(this.entryActions::add);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void appendExitActions(StateAction<T, S>... actions) {
    Stream.of(actions).filter(Objects::nonNull).forEach(this.exitActions::add);
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NamedState<?, ?> that = (NamedState<?, ?>) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", NamedState.class.getSimpleName() + "[", "]")
        .add(name).toString();
  }
}
