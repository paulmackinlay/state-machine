package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateAction;
import com.webotech.statemachine.api.StateMachine;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A {@link State}, each instance with the same name is are {@link #equals(Object)}
 */
public class NamedState<T> implements State<T> {

  private final String name;
  private final List<StateAction<T>> entryActions;
  private final List<StateAction<T>> exitActions;

  public NamedState(String name) {
    this.name = name;
    this.entryActions = new ArrayList<>();
    this.exitActions = new ArrayList<>();
  }

  @Override
  public void onEntry(StateMachine<T> stateMachine) {
    this.entryActions.forEach(a -> a.execute(stateMachine));
  }

  @Override
  public void onExit(StateMachine<T> stateMachine) {
    this.exitActions.forEach(a -> a.execute(stateMachine));
  }

  @SuppressWarnings("unchecked")
  @Override
  public void appendEntryActions(StateAction<T>... actions) {
    Stream.of(actions).filter(Objects::nonNull).forEach(this.entryActions::add);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void appendExitActions(StateAction<T>... actions) {
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
    NamedState<?> that = (NamedState<?>) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name);
  }
}
