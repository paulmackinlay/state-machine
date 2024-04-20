package com.webotech.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.webotech.statemachine.GenericStateMachine.Builder;
import com.webotech.statemachine.api.State;
import com.webotech.statemachine.api.StateEvent;
import com.webotech.statemachine.api.StateMachine;
import com.webotech.statemachine.api.StateMachineListener;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GenericStateMachineTest {

  public static final State<Void> state1 = new NamedState<>("STATE-1");
  public static final State<Void> state2 = new NamedState<>("STATE-2");
  public static final State<Void> stateReserved1 = new NamedState<>("_UNINITIALISED_");
  public static final State<Void> stateReserved2 = new NamedState<>("_END_");
  public static final StateEvent event1 = new NamedStateEvent("event-1");
  public static final StateEvent event2 = new NamedStateEvent("event-2");
  public static final StateEvent eventReserved = new NamedStateEvent("_immediate_");
  private GenericStateMachine<Void> stateMachine;
  private GenericStateMachine<List<String>> txtStateMachine;

  @BeforeEach
  void setup() {
    Builder<Void> builder = new GenericStateMachine.Builder<>();
    stateMachine = builder.build();
    txtStateMachine = new GenericStateMachine.Builder<List<String>>().setContext(
        new ArrayList<>()).build();
  }

  @Test
  void shouldHandleUnsupportedEvent() {
    stateMachine.initialSate(state1).receives(event1).itEnds();
    stateMachine.start();
    OutputStream logStream = TestingUtil.initLogCaptureStream();
    stateMachine.fire(event2);
    assertEquals("StateEvent [event-2] not mapped for state [STATE-1], ignoring\n",
        logStream.toString());
  }

  @Test
  void shouldNotifyListener() {
    List<Object> stateData = new ArrayList<>();
    stateMachine.initialSate(state1).receives(event1).itEnds();
    stateMachine.setStateMachineListener(new StateMachineListener<Void>() {
      @Override
      public void onStateChangeBegin(State<Void> fromState, StateEvent event, State<Void> toState) {
        stateData.add(fromState);
        stateData.add(event);
        stateData.add(toState);
      }

      @Override
      public void onStateChangeEnd(State<Void> fromState, StateEvent event, State<Void> toState) {
        stateData.add(fromState);
        stateData.add(event);
        stateData.add(toState);
      }
    });
    stateMachine.start();
    stateMachine.fire(event1);
    assertEquals(List.of(state1, event1, new NamedState<Void>("_END_"),
        state1, event1, new NamedState<Void>("_END_")), stateData);
    assertTrue(stateMachine.isEnded());
  }

  @Test
  void shouldMaintainOrderOrEntryAndExitActions() {
    State<List<String>> one = new NamedState<>("one");
    one.appendEntryActions(sm -> sm.getContext().add("1.1 entered"),
        sm -> sm.getContext().add("1.2 entered"));
    one.appendExitActions(sm -> sm.getContext().add("1.1 exited"),
        sm -> sm.getContext().add("1.2 exited"));
    txtStateMachine.initialSate(one).receives(event1).itEnds();
    txtStateMachine.start();
    txtStateMachine.fire(event1);
    assertEquals(List.of("1.1 entered", "1.2 entered", "1.1 exited", "1.2 exited"),
        txtStateMachine.getContext());
    assertTrue(txtStateMachine.isEnded());
  }

  @Test
  void shouldEnsureContextIsConsistent() {
    State<List<String>> one = new NamedState<>("one");
    State<List<String>> two = new NamedState<>("two");
    one.appendEntryActions(sm -> sm.getContext().add("1 entered"));
    two.appendEntryActions(sm -> sm.getContext().add("2 entered"));
    two.appendExitActions(sm -> sm.getContext().add("ending"));
    txtStateMachine.initialSate(one).receives(event1).itTransitionsTo(two).
        when(two).receives(event1).itEnds();
    txtStateMachine.start();
    txtStateMachine.fire(event1);
    txtStateMachine.fire(event1);
    assertEquals(List.of("1 entered", "2 entered", "ending"), txtStateMachine.getContext());
    assertTrue(txtStateMachine.isEnded());
  }

  @Test
  void shouldTransitionThroughStates() {
    stateMachine.initialSate(state1).receives(event1).itTransitionsTo(state2)
        .when(state2).receives(event1).itTransitionsTo(state1)
        .when(state1).receives(event2).itEnds();

    assertFalse(stateMachine.isStarted());
    assertFalse(stateMachine.isEnded());
    stateMachine.start();
    assertTrue(stateMachine.isStarted());
    assertFalse(stateMachine.isEnded());
    assertEquals(state1, stateMachine.getCurrentState());
    stateMachine.fire(event1);
    assertEquals(state2, stateMachine.getCurrentState());
    stateMachine.fire(event1);
    assertEquals(state1, stateMachine.getCurrentState());
    stateMachine.fire(event2);
    assertTrue(stateMachine.isEnded());
  }

  @Test
  void shouldNotEndAnUnconfiguredStateMachine() {
    assertThrows(IllegalStateException.class, () -> stateMachine.itEnds());
  }

  @Test
  void shouldForceSensibleConfiguration() {
    assertThrows(IllegalStateException.class, () -> stateMachine.when(state1));
    assertThrows(IllegalStateException.class, () -> stateMachine.initialSate(stateReserved1));
    assertThrows(IllegalStateException.class, () -> stateMachine.initialSate(stateReserved2));
    stateMachine.initialSate(state1);
    assertThrows(IllegalStateException.class, () -> stateMachine.initialSate(state2));
    assertThrows(IllegalStateException.class, () -> stateMachine.when(stateReserved1));
    assertThrows(IllegalStateException.class, () -> stateMachine.when(stateReserved2));
    assertThrows(IllegalStateException.class, () -> stateMachine.when(state2));
    assertThrows(IllegalStateException.class, () -> stateMachine.receives(eventReserved));
    stateMachine.receives(event1);
    assertThrows(IllegalStateException.class, () -> stateMachine.when(state2));
    assertThrows(IllegalStateException.class, () -> stateMachine.start());
    stateMachine.itTransitionsTo(state2);
    stateMachine.start();
  }

  @Test
  void shouldBuildWithNoContext() {
    StateMachine<Void> noContextStateMachine = (new GenericStateMachine.Builder<Void>()).build();
    assertNull(noContextStateMachine.getContext());
  }

  @Test
  void shouldBuildWithImmutableContext() {
    StateMachine<String> stringContextStateMachine = (new GenericStateMachine.Builder<String>().setContext(
        "my-context")).build();
    assertEquals("my-context", stringContextStateMachine.getContext());
  }

  @Test
  void shouldBuildWithUnmappedEventHandler() {
    BiConsumer<StateEvent, StateMachine<Void>> unmappedEventHandler = (ev, sm) -> {
    };
    Builder<Void> builder = new GenericStateMachine.Builder<Void>().setUnmappedEventHandler(
        unmappedEventHandler);
    assertSame(unmappedEventHandler, builder.getUnmappedEventHandler());
  }

  @Test
  void shouldBuildWithPool() {
    Supplier<AtomicBoolean> poolSupplier = AtomicBoolean::new;
    Consumer<AtomicBoolean> poolConsumer = a -> {
    };
    Builder<Void> builder = new GenericStateMachine.Builder<Void>().withAtomicBooleanPool(
        poolSupplier, poolConsumer);
    assertSame(poolSupplier, builder.getAtomicBooleanSupplier());
    assertSame(poolConsumer, builder.getAtomicBooleanConsumer());
  }

  @Test
  void shouldBuildWithMutableContext() {
    Object obj = new Object();
    StateMachine<Object> objContextStateMachine = (new GenericStateMachine.Builder<>().setContext(
        obj)).build();
    assertSame(obj, objContextStateMachine.getContext());
  }
}