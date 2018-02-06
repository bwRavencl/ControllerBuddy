![ControllerBuddy Logo](https://github.com/bwRavencl/ControllerBuddy/raw/master/src/main/resources/icon_128.png
"ControllerBuddy")
## ControllerBuddy

#### License Information:
GNU General Public License v2.0

#### Description:
ControllerBuddy allows the feeding of input from a physical game-controller connected to a host computer to a virtual joystick, provided by the vJoy device driver. ControllerBuddy can either feed to a local vJoy device on the host or to one on a second computer.

Consequently ControllerBuddy can operate in three different roles:
- Local (Single machine with a physical and a vJoy device)
- Server (Source machine with the physical device)
- Client (Target machine with the vJoy device)

When using ControllerBuddy in Client-Server mode, both instances communicate over a lightweight UDP-based protocol.

In order to support complex input profiles, ControllerBuddy provides very flexible programming of the physical controller. Each axis or button present on the device can be mapped to one or multiple actions invoked on the target host.

Currently among the following actions are supported:
- Axis movements
- Relative axis movements
- Button presses
- Cursor movement
- Keyboard inputs
- Mouse button presses
- Mouse scrolling
- Lock key toggling
- Cycles of actions
- Mode switching
- Relative axis resetting

ControllerBuddy supports the creation of multiple input modes, which are best be described as shift-states.
In each mode, each physical axis or button can be mapped to a different action.
Mode switching can be done by either holding down a button on the physical controller or by tapping it once.

The whole programming of the physical controller can be performed via the graphical user interface of ControllerBuddy.
The resulting profile can be exported to a simple JSON-based file format.

ControllerBuddy offers an overlay, that displays the currently active input mode and the position of the vJoy axes. The axes displayed by the overlay can be customized on a per-profile basis. The overlay was designed to be used with applications, that are running in (borderless fullscreen) windowed mode.

In addition a built-in On-Screen Keyboard is provided, that can either be controlled via special actions bound to controller buttons or via the mouse cursor.

For maximum platform-independence ControllerBuddy was implemented as a Java application, supporting all three major operating systems Windows, Mac OS X and Linux when running as a server. When running as a client currently only Windows is supported.

#### Architecture:
<pre>
       Host:                             Client:

Physical Controller
         |
         |
         v
  ControllerBuddy  --------------->  ControllerBuddy
         |                                  |
         |                                  |
         v                                  v
 vJoy Device Driver                 vJoy Device Driver
         |                                  |
         |                                  |
         v                                  v
 Target Application                 Target Application
</pre>

#### Example Screenshots:
![Assignments Tab](https://github.com/bwRavencl/ControllerBuddy/raw/master/example_screenshot_1.png) ![Overlay Tab](https://github.com/bwRavencl/ControllerBuddy/raw/master/example_screenshot_2.png)
![Component Editor](https://github.com/bwRavencl/ControllerBuddy/raw/master/example_screenshot_3.png)


#### Command Line Parameters:
<pre>
usage: ControllerBuddy [-autostart <arg>] [-tray] [-version]
 -autostart <arg>   automatically start the:
                    local feeder [-autostart local] or
                    client [-autostart client] or
                    server [-autostart server]
 -tray              launch in system tray
 -version           print version and quit
</pre>

#### Requirements:
- General requirements:
  - Java SE Runtime Environment 9 (http://www.oracle.com/technetwork/java/javase/overview/index.html)
  - vJoy 2.1.8 (http://vjoystick.sourceforge.net)

#### Building:
ControllerBuddy uses the Gradle build system. 
The following tasks are supported:

| Task                             | Command              |
| -------------------------------- | -------------------- |
| Run ControllerBuddy              | gradlew run          |
| Create a capsule executable JAR  | gradlew capsule      |
| Generate Eclipse files           | gradlew eclipse      |
| Clean Eclipse files              | gradlew cleanEclipse |
| Delete build and gen directories | gradlew clean        |

Please note that building requires Java JDK 9.

[![Build Status](https://travis-ci.org/bwRavencl/ControllerBuddy.svg?branch=master)](https://travis-ci.org/bwRavencl/ControllerBuddy)
