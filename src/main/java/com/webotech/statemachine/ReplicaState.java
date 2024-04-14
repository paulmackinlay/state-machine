package com.webotech.statemachine;

import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateAction;
import com.webotech.statemachine.api.StateMachine;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Each instance with the same name is a replica, so they are equal()
 */
//TODO Rename this to NamedState
public class ReplicaState<T> implements State<T> {

  private final String name;
  private final List<StateAction<T>> entryActions;
  private final List<StateAction<T>> exitActions;

  public ReplicaState(String name) {
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
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ReplicaState<?> other = (ReplicaState<?>) obj;
    if (this.name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!this.name.equals(other.name)) {
      return false;
    }
    return true;
  }

}
