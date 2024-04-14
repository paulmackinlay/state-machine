package com.webotech.statemachine;

import com.webotech.statemachine.api.StateEvent;

/**
 * Each instance with the same name is a replica, so they are equal()
 */
//TODO - call this NamedStateEvent?
public final class ReplicaStateEvent implements StateEvent {

  private final String name;

  public ReplicaStateEvent(String name) {
    this.name = name;
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
    ReplicaStateEvent other = (ReplicaStateEvent) obj;
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
