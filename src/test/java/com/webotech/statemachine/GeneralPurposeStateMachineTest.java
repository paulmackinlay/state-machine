package com.webotech.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.webotech.statemachine.GeneralPurposeStateMachine.Builder;
import com.webotech.statemachine.api.StateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeneralPurposeStateMachineTest {

  private GeneralPurposeStateMachine<Void> stateMachine;

  @BeforeEach
  void setup() {
    Builder<Void> builder = new GeneralPurposeStateMachine.Builder<>();
    stateMachine = builder.build();
  }

  @Test
  void shouldBuildWithNoContext() {
    StateMachine<Void> noContextStateMachine = (new GeneralPurposeStateMachine.Builder<Void>()).build();
    assertNull(noContextStateMachine.getContext());
  }

  @Test
  void shouldBuildWithImmutableContext() {
    StateMachine<String> stringContextStateMachine = (new GeneralPurposeStateMachine.Builder<String>().setContext(
        "my-context")).build();
    assertEquals("my-context", stringContextStateMachine.getContext());
  }

  @Test
  void shouldBuildWithMutableContext() {
    Object obj = new Object();
    StateMachine<Object> objContextStateMachine = (new GeneralPurposeStateMachine.Builder<>().setContext(
        obj)).build();
    assertSame(obj, objContextStateMachine.getContext());
  }
}