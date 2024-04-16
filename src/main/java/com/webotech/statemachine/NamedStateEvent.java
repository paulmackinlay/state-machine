package com.webotech.statemachine;

import com.webotech.statemachine.api.StateEvent;
import java.util.Objects;

/**
 * A {@link StateEvent}, each instance with the same name is are {@link #equals(Object)}
 */
public final class NamedStateEvent implements StateEvent {

  private final String name;

  public NamedStateEvent(String name) {
    this.name = name;
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
    NamedStateEvent that = (NamedStateEvent) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name);
  }
}
