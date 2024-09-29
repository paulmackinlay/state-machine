## Service API & app starter implementation

This project also contains a
[service API](../src/main/java/com/webotech/statemachine/service/api/AppService.java) which defines
the basics of an application (or service). Abstract classes (in the
[com.webotech.statemachine.service](../src/main/java/com/webotech/statemachine/service) package)
that form a starter implementation can be extended to run as a stand-alone app, the app is backed by
a state machine.

The API and implementation are the blueprint for a generic microservice.

## App design

TODO - talk about the states, the AppService, AppContext and Subsystems

The app implementation is intentionally minimal and is for an app that is started and stopped in a
predictable and graceful manner. It will transition from a STOPPED state to a STARTING state and
then to a STARTED state.

During the STARTING state, constructed subsystems are started in a sequential manner. Once all have
started the app transitions to a STARTED state. The app in the STARTED state will run indefinitely,
servicing any operations that are needed and will also process a STOP event which will cause it to
gracefully wind down.

During wind down, the app transitions to a STOPPING state and all subsystems are stopped in reverse
order to how they were started. When subsystems are stopped, they return to their states right after
construction. When all are stopped, the app transitions to a STOPPED state. By default, once in the
STOPPED state, the app will exit however by overriding the Xstart methodX you may choose another
behaviour. For example, you may wish to leave the app running as a warm standby that can be revived
and transition to the STARTED state again).

Here is the app's state diagram:
TODO