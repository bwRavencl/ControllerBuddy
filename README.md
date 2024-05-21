# ControllerBuddy

<!--suppress HtmlDeprecatedAttribute -->
<img src="src/main/resources/icon_128.png" alt="ControllerBuddy Logo" align="right"/>

[![ControllerBuddy Release Status](https://github.com/bwRavencl/ControllerBuddy/actions/workflows/release.yml/badge.svg)](https://github.com/bwRavencl/ControllerBuddy/actions/workflows/release.yml)

Check out the [ControllerBuddy Homepage](https://controllerbuddy.org) for getting started.  
For further support join the [ControllerBuddy Discord](https://discord.gg/2Sg9ggZrAp).

## Description

ControllerBuddy is a highly advanced gamepad mapping software, which supports the creation of input profiles for complex target applications such as flight simulators.

In addition to the simplistic mapping of buttons and axes of a physical game-controller to keyboard and mouse input, ControllerBuddy also supports the feeding of input commands to a virtual joystick device (vJoy / uinput).

ControllerBuddy's goal is to enable the user to control target applications solely via a gamepad and not having to reach for a keyboard or mouse at any point in time.

## Download and Installation

**Tip:** Check out the [ControllerBuddy-Install-Script](https://github.com/bwRavencl/ControllerBuddy-Install-Script),
it automates all the steps below and much more!

### Windows x86-64

1. First ensure you have correctly installed [vJoy](https://github.com/jshafer817/vJoy/releases/latest) on your system.
2. [Click here](https://github.com/bwRavencl/ControllerBuddy/releases/latest) and download the latest build of ControllerBuddy for Windows as a ZIP archive.
3. Extract the `ControllerBuddy` directory from the archive to any desired location on your hard-drive.
4. Run `ControllerBuddy.exe` inside the extracted `ControllerBuddy` directory.

### Linux x86-64

1. First ensure you have installed libsdl2 on your system:
    - Debian / Ubuntu: `sudo apt-get install libsdl2-2.0`
    - Red-Hat-based: `sudo yum install SDL2`
    - Arch Linux: `sudo pacman -S sdl2`
2. Configure uinput and hidraw:
    1. Create an uinput group: `sudo groupadd -f uinput`
    2. Add yourself to the group: `sudo gpasswd -a "$USER" uinput`
    3. As root, create a file `/etc/udev/rules.d/99-input.rules` with the following content:
       ```
       KERNEL=="uinput", SUBSYSTEM=="misc", MODE="0660", GROUP="uinput"
       KERNEL=="hidraw*", SUBSYSTEM=="hidraw", ATTRS{idVendor}=="054c", ATTRS{idProduct}=="05c4", MODE="0666"
       KERNEL=="hidraw*", SUBSYSTEM=="hidraw", ATTRS{idVendor}=="054c", ATTRS{idProduct}=="09cc", MODE="0666"
       KERNEL=="hidraw*", SUBSYSTEM=="hidraw", ATTRS{idVendor}=="054c", ATTRS{idProduct}=="0ba0", MODE="0666"
       KERNEL=="hidraw*", SUBSYSTEM=="hidraw", ATTRS{idVendor}=="054c", ATTRS{idProduct}=="0ce6", MODE="0666"
       ```
    4. As root, create a file `/etc/modules-load.d/uinput.conf` with the following content:  
       `uinput`
3. Reboot
4. [Click here](https://github.com/bwRavencl/ControllerBuddy/releases/latest) and download the latest build of ControllerBuddy for Linux as a TGZ archive.
5. Extract the `ControllerBuddy` directory from the archive to any desired location on your hard-drive.
6. Run `ControllerBuddy` inside the extracted `ControllerBuddy/bin` directory.

## Features

- Maps gamepad axes and buttons to highly customizable actions:
    - vJoy axis movements (absolute and relative)
    - vJoy button presses
    - Keyboard inputs
    - Mouse inputs
    - Cycles of actions
    - Mode switching
    - etc.
- Powerful user interface:
    - Allows the creation of mapping profiles
    - Configuration of settings
    - Support for a light and dark UI theme
- In-game overlay:
    - Displays currently active mode
    - Can display current position of virtual axes
    - On-Screen-Keyboard that can be controlled via gamepad
    - VR support (OpenVR)
    - Customizable position and colors
- Two scenarios of operation:
    - Local
    - Server to client (experimental! use only in trusted networks!)
- Supported gamepads:
    - Xbox 360 Controller
    - Xbox One Controller
    - Xbox Series X|S Controller
    - Dual Shock 3
    - Dual Shock 4
    - Dual Sense
    - etc. (to check if your controller is supported please refer to the [SDL_GameControllerDB](https://github.com/gabomdq/SDL_GameControllerDB) project)
- Supported operating systems:
    - Windows / Linux (local / client / server)
    - macOS (only server - no binaries provided!)
- Language support for:
    - English
    - German

## Profiles

Profiles are used to configure your gamepad for a certain target application.  
A profile has the following general structure:

```
Profile (.json file)
├── Default Mode
│   ├── X Axis
│   │   ├── some Action
│   │   └── another Action
│   ├── Y Axis
│   │   └── some Action
│   ├── A Button
│   │   ├── some Action
│   │   └── another Action
│   ├── B Button
│   │   └── Switch Mode Action (switches to 'Another Mode' and back)
│   ├── X Button
│   │   └── Switch Mode Action (switches to 'Yet another Mode' and back)
│   └── Y Button
│       └── Cycle Action (performs 'Action 1', when pressed again 'Action 2', then 'Action 3', then starts over)
│           ├── Action 1
│           ├── Action 2
│           └── Action 3
├── Another Mode
│   ├── X Axis
│   │   └── some Action
│   └── A Button
│       └── some Action
└── Yet another Mode
    └── X Axis
        └── some Action
```

When switching from one Mode to another, all the axes and buttons that are not used by the other mode retain their function from the previous mode. This works over multiple levels of Modes.

Mode switching can be configured to operate in two different ways:

- Default: works like the SHIFT key on your keyboard
- Toggle: works like the Caps Lock key

A set of well-thought-out profiles for the most popular flight simulators are available in the [ControllerBuddy-Profiles](https://github.com/bwRavencl/ControllerBuddy-Profiles) repository.

## Architecture

Local mode:

```
            Local:

     Physical Controller
              |
              |
              v
       ControllerBuddy
              |
              |
              v
    vJoy + Win32 / uinput
              |
              |
              v
      Target Application
```

Server-Client mode:

```
             Server:                                 Client:

       Physical Controller
                |
                |
                v                  UDP
         ControllerBuddy  -------------------->  ControllerBuddy
                                                        |
                                                        |
                                                        v
                                              vJoy + Win32 / uinput
                                                        |
                                                        |
                                                        v
                                                Target Application
```

## Example Screenshots

![Modes Tab](example_screenshot_1.png)

![Assignments Tab](example_screenshot_2.png)

![Component Editor - Button](example_screenshot_3.png)

![Component Editor - Axis](example_screenshot_4.png)

![Dark Mode](example_screenshot_5.png)

![Visualization Tab](example_screenshot_6.png)

![Overlay](example_screenshot_7.png)

![VR Overlay](example_screenshot_8.png)

## Command Line Parameters

| Parameter           | Arguments               | Description                                                                                                 | Available for scripting |
|---------------------|-------------------------|-------------------------------------------------------------------------------------------------------------|:-----------------------:|
| -autostart          | local / client / server | starts the specified mode of operation after launch                                                         |           yes           |
| -export             | file destination        | exports a visualization of the current profile to the specified path                                        |           yes           |
| -help               |                         | prints the help and exits                                                                                   |           no            |
| -profile            | file source             | loads the specified profile after launch                                                                    |           yes           |
| -gamecontrollerdb   | file source             | adds the SDL controller mappings from the specified [file](https://github.com/gabomdq/SDL_GameControllerDB) |           yes           |
| -quit               |                         | quits the application                                                                                       |           yes           |
| -save               | file destination        | save the current profile to the specified path                                                              |           yes           |
| -skipMessageDialogs |                         | skips all message dialogs                                                                                   |           no            |
| -tray               |                         | launches the application in the system tray                                                                 |           yes           |
| -version            |                         | prints the version information and exits                                                                    |           no            |

If an instance of ControllerBuddy is already running, launching a second instance with the parameters denoted as "available for scripting" will trigger the corresponding action in the first instance and immediately shutdown the second instance.

This can be used to integrate ControllerBuddy into third party applications.  
For more information please check out [ControllerBuddy-DCS-Integration](https://github.com/bwRavencl/ControllerBuddy-DCS-Integration), an exemplary integration of ControllerBuddy into [DCS World](https://www.digitalcombatsimulator.com).

## Attribution

ControllerBuddy uses the following awesome software technologies and libraries:

- [OpenJDK](https://openjdk.org)
- [Apache Batik](https://xmlgraphics.apache.org/batik)
- [Apache Commons CLI](https://commons.apache.org/proper/commons-cli)
- [ClassGraph](https://github.com/classgraph/classgraph)
- [dbus-java](https://hypfvieh.github.io/dbus-java/)
- [FlatLaf](https://www.formdev.com/flatlaf/)
- [Gson](https://github.com/google/gson)
- [hid4java](https://github.com/gary-rowe/hid4java)
- [Java Native Access (JNA)](https://github.com/java-native-access/jna)
- [JXInput](https://github.com/StrikerX3/JXInput)
- [linuxio4j](https://github.com/bithatch/linuxio4j)
- [LWJGL - Lightweight Java Game Library 3](https://www.lwjgl.org)
- [SDL_GameControllerDB](https://github.com/gabomdq/SDL_GameControllerDB)
- [SLF4J](https://www.slf4j.org/)

## Building

If you want to build ControllerBuddy from its source code this section might be helpful to get you started.  
ControllerBuddy uses the Gradle build system, the following Gradle tasks are supported:

| Task                                   | Command                 |
|----------------------------------------|-------------------------|
| Generate version source file           | gradlew generateVersion |
| Run SpotBugs and Spotless              | gradlew check           |
| Apply Spotless formatting              | gradlew spotlessApply   |
| Run ControllerBuddy                    | gradlew run             |
| Install a jpackage image               | gradlew installDist     |
| Create a ZIP-compressed jpackage image | gradlew distZip         |
| Create a TGZ-compressed jpackage image | gradlew distTar         |
| Delete build and gen directories       | gradlew clean           |

## License Information

GNU General Public License v3.0
