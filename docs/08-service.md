## Service API & app starter implementation

This project also contains a
[service API](../src/main/java/com/webotech/statemachine/service/api/AppService.java) which defines
the basics of an application (or service). Abstract classes located in the
[com.webotech.statemachine.service](../src/main/java/com/webotech/statemachine/service) package,
form a starter implementation that will run as a stand-alone app. The app is backed by a state
machine.

The API and implementation are the blueprint for a generic microservice.

## App design

The implementation is intentionally minimal and is for an app that is started and stopped in a
predictable and graceful manner. Here is the state diagram:

![](media/State_diagram_4.png)

Note that in the **STOPPED** state, it can exit immediately (denoted in green) or can process
**start** and **stop** events (denoted in blue). These two modes of operation are controlled at
construction with an _isExitOnStop_ flag.

Once an _AppService_ is constructed, it will transition from an **UNINITIALISED** state to a
**STARTING** state and then to a **STARTED** state. During the **STARTING** state, constructed
subsystems are started in a sequential manner. Once all have started the app transitions to a
**STARTED** state. It will run indefinitely, servicing any operations that are needed and will also
process a **stop** event which will cause it to gracefully wind down.

During wind down, the app transitions to a **STOPPING** state and all subsystems are stopped in
reverse order to how they were started. When subsystems are stopped they return to their constructed
state and when all are stopped, the app transitions to a **STOPPED** state.

As mentioned earler an app can be constructed with _isExitOnStop_ set to true or false, this
dictates it's mode of operation. In the 'exit on stop' mode, the app will exit immediately once it
is in the **STOPPED** state. Otherwise the app will continue to run as a warm standby that can be
revived when it will transition to the **STARTED** state again.

## API and core classes

TODO - AppService, AppContext and Subsystem - AbstractAppService, AbstractAppContext

### Steps to create an app

TODO
