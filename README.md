![ControllerBuddy Logo](https://github.com/bwRavencl/ControllerBuddy/raw/master/src/main/resources/icon_128.png
"ControllerBuddy")
## ControllerBuddy

#### License Information:
GNU General Public License v2.0

#### Download and Installation:
1. First ensure you have correctly installed [vJoy 2.1](http://vjoystick.sourceforge.net) on your system.
2. [Click here](https://github.com/bwRavencl/ControllerBuddy/releases/latest) and download the latest build of ControllerBuddy for Windows as a ZIP archive.
3. Extract the ControllerBuddy directory from the archive to any desired location on your hard-drive.
4. Run 'ControllerBuddy.exe' inside the ControllerBuddy directory.

#### Description:
ControllerBuddy is a highly advanced gamepad mapping software, which supports the creation of input profiles for complex target applications such as flight simulators.  
In addition to the simplistic mapping of buttons and axes of a physical game-controller to keyboard and mouse input, ControllerBuddy also supports the feeding of input commands to a virtual joystick, provided by the vJoy device driver.  
The set goal of this is application is to allow controlling target applications without having the user take off their hands from the gamepad to reach for the keyboard or mouse.

#### Features:
ControllerBuddy can either feed inputs to the local computer or via network to a second computer.

Consequently ControllerBuddy can operate in three different roles:
- Local (Single machine with a physical controller and a vJoy device)
- Server (Source machine with the physical controller)
- Client (Target machine with the vJoy device)

When using ControllerBuddy in client-server mode, both instances communicate over a lightweight UDP-based protocol.
Please note that this mode of operation is currently considered experimental and should only ever be used in trusted networks!

In order to support the creation of very complex input profiles, ControllerBuddy comes with a variety of so called actions.  
Each axis or button present on the device can be mapped to one or multiple actions invoked on the target host.  
An action can for example be the rather common mapping of a physical axis or button to a vJoy axis, a mouse button or a keyboard shortcut.  
More specialized types of actions are available for tasks such as resetting a relative vJoy axis to its initial value or creating cycles of actions.

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
- ...

ControllerBuddy supports the creation of multiple input modes, which are best be described as shift-states.
In each mode, each physical axis or button can be mapped to one or multiple different actions.
Mode switching can be configured to operate in two different ways:
- Default mode switching works similarily to the shift key found on keyboards, when the button used for switching to a mode is realeased the shift-state becones inactive
- Toggle mode switching works like when using the Caps Lock key of a keyboard, pressing it once switches to the assigned mode and pressing it again turns the mode off
It is possible to stack multiple modes on top of each other, thereby buttons and axes that are not assigned in a mode further up on the stack retain their function from the modes further down in the stack.

The whole programming of the physical controller can be performed via the graphical user interface of ControllerBuddy.
The resulting profile can be exported to a simple JSON-based file format.

ControllerBuddy offers a freely moveable overlay window, that displays the currently active input mode and the position of the vJoy axes.
The virtual axes displayed in the overlay can be customized on a per-profile basis.
The overlay was designed to be used with applications, that are running in (borderless fullscreen) windowed mode.

In addition a built-in On-Screen Keyboard is provided, that can either be controlled via special actions bound to controller buttons or via the mouse cursor.

Both the status overlay and the On-Screen Keyboard can be displayed as overlays inside OpenVR-based applications.

For maximum platform-independence ControllerBuddy was implemented as a Java application, supporting all three major operating systems Windows, macOS and Linux when running as a server.  
When running in client- or local-mode currently only Windows is supported.  
Currently only binary builds for Windows x64 are provided.

A great number of XInput and DirectInput gamepads are supported such as:
- Xbox 360 Controller
- Xbox One Controller
- Dual Shock 3
- Dual Shock 4 (with custom support for the touchpad / rumble and lightbar)
- ...

To see if your device is supported please checkout the [SDL_GameControllerDB](https://github.com/gabomdq/SDL_GameControllerDB) project.

#### Architecture:
```
              Host:                             Client:

       Physical Controller
                |
                |
                v
         ControllerBuddy  --------------->  ControllerBuddy
         |             |                    |             |
         |             |                    |             |
         v             v                    v             v
vJoy Device Driver   Win32 API     vJoy Device Driver   Win32 API
        |                |                 |                |
        |                |                 |                |
        v                v                 v                v
        Target Application                 Target Application
```

#### Example Screenshots:
![Modes Tab](https://github.com/bwRavencl/ControllerBuddy/raw/master/example_screenshot_1.png)

![Assignments Tab](https://github.com/bwRavencl/ControllerBuddy/raw/master/example_screenshot_2.png)

![Component Editor](https://github.com/bwRavencl/ControllerBuddy/raw/master/example_screenshot_3.png)

![Overlay](https://github.com/bwRavencl/ControllerBuddy/raw/master/example_screenshot_4.png)

![VR Overlay](https://github.com/bwRavencl/ControllerBuddy/raw/master/example_screenshot_5.png)

#### Command Line Parameters:
```
usage: ControllerBuddy [-autostart <arg>] [-profile <arg>] [-tray]
       [-version]
 -autostart <arg>   automatically start the local feeder [-autostart local] or
                    client [-autostart client] or
                    server [-autostart server]
 -profile <arg>     load the specified profile
 -tray              launch in system tray
 -version           print version and quit
```

If an instance of ControllerBuddy is already running, launching a second instance with the `-profile` parameter can be used to trigger the loading of the specified profile in the first instance. This enables profile switching from any other application that can execute shell commands.

#### Dependencies:
- [OpenJDK 12](https://jdk.java.net/12/)
- [OpenJDK with jpackage support](https://jdk.java.net/jpackage/)
- [Apache Commons CLI](https://commons.apache.org/proper/commons-cli)
- [Gson](https://github.com/google/gson)
- [Java Native Access (JNA)](https://github.com/java-native-access/jna)
- [LWJGL - Lightweight Java Game Library 3](https://www.lwjgl.org)
- [Pure Java HID-API](https://github.com/nyholku/purejavahidapi)

#### Building:
ControllerBuddy uses the Gradle build system.  
The following tasks are supported:

| Task                             | Command                 |
| -------------------------------- | ----------------------- |
| Generate version source file     | gradlew generateVersion |
| Run ControllerBuddy              | gradlew run             |
| Install a jpackage image         | gradlew installDist     |
| Create a zipped jpackage image   | gradlew distZip         |
| Generate Eclipse files           | gradlew eclipse         |
| Delete Eclipse files             | gradlew cleanEclipse    |
| Delete build and gen directories | gradlew clean           |
