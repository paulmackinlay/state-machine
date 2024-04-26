/*
 * Copyright (c) 2024 Paul Mackinlay <paul.mackinlay@gmail.com>
 */

package com.webotech.statemachine;

import com.webotech.statemachine.api.StateEvent;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * A {@link StateEvent}, each instance with the same name is are {@link #equals(Object)}
 */
public final class NamedStateEvent<S> implements StateEvent<S> {

  private final String name;
  private S payload;

  public NamedStateEvent(String name) {
    this.name = name;
    this.payload = null;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public S getPayload() {
    return payload;
  }

  @Override
  public void setPayload(S payload) {
    this.payload = payload;
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

  @Override
  public String toString() {
    return new StringJoiner(", ", NamedStateEvent.class.getSimpleName() + "[", "]")
        .add(name).toString();
  }
}
