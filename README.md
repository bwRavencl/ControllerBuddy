# ControllerBuddy

<!--suppress HtmlDeprecatedAttribute -->
<img src="icon.svg" alt="ControllerBuddy Logo" align="right" width="128px"/>

[![ControllerBuddy Release Status](https://github.com/bwRavencl/ControllerBuddy/actions/workflows/release.yml/badge.svg)](https://github.com/bwRavencl/ControllerBuddy/actions/workflows/release.yml)

Visit the [ControllerBuddy Homepage](https://controllerbuddy.org) to get started.  
For more support, join the [ControllerBuddy Discord](https://discord.gg/2Sg9ggZrAp).

## Description

ControllerBuddy is an advanced gamepad mapping software that supports the creation of input profiles for complex target applications such as flight simulators.

In addition to the simple mapping of buttons and axes of a physical game controller to keyboard and mouse input, ControllerBuddy also supports feeding input commands to a virtual joystick device (vJoy / uinput).

The goal of ControllerBuddy is to allow the user to control target applications exclusively with a gamepad without ever having to reach for a keyboard or mouse.

## Download and Installation

**Tip:** Check out the [ControllerBuddy-Install-Script](https://github.com/bwRavencl/ControllerBuddy-Install-Script),
it automates all the steps below and much more!

### Windows x86-64

1. First download and install [vJoy](https://github.com/jshafer817/vJoy/releases/latest) on your system.
2. [Click here](https://github.com/bwRavencl/ControllerBuddy/releases/latest) and download the latest build of ControllerBuddy for Windows as a ZIP archive.
3. Extract the `ControllerBuddy` directory from the archive to any desired location on your hard-drive.
4. Run `ControllerBuddy.exe` inside the extracted `ControllerBuddy` directory.

### Linux x86-64

1. First, make sure you have libsdl2 installed on your system:
    - Debian / Ubuntu: `sudo apt-get install libsdl2-2.0`
    - Red-Hat-based: `sudo yum install SDL2`
    - Arch Linux: `sudo pacman -S sdl2`
    - SUSE Linux: `sudo zypper install SDL2`
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

- Map gamepad axes and buttons to highly customizable actions:
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
    - Support for light and dark UI themes
- In-game overlay:
    - Displays current mode
    - Can show current position of virtual axes
    - On-Screen-Keyboard that can be controlled by gamepad
    - VR support (OpenVR)
    - Customizable position and colors
- Two modes of operation:
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
    - macOS (server only - no binaries provided!)
- Language support for:
    - English
    - German

## Profiles

Profiles are used to configure your gamepad for a specific target application.  
The general structure of a profile is as follows:

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

When switching from one Mode to another, all the axes and buttons that are not used by the other mode retain their function from the previous mode. This works across multiple levels of modes.

Mode switching can be configured to work in two different ways:

- Default: works like the SHIFT key on your keyboard
- Toggle: works like the Caps Lock key

The [ControllerBuddy-Profiles](https://github.com/bwRavencl/ControllerBuddy-Profiles) repository contains a number of well-thought-out profiles for many different flight simulators.

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

## Screenshots

![Modes Tab](screenshot_1.png)

![Assignments Tab](screenshot_2.png)

![Component Editor - Button](screenshot_3.png)

![Component Editor - Axis](screenshot_4.png)

![Dark Mode](screenshot_5.png)

![Visualization Tab](screenshot_6.png)

![Overlay](screenshot_7.png)

![VR Overlay](screenshot_8.png)

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

If one instance of ControllerBuddy is already running, starting a second instance with the parameters marked as "available for scripting" will trigger the corresponding action in the first instance and shut down the second instance immediately.

This can be used to integrate ControllerBuddy into third party applications.  
For more information see [ControllerBuddy-DCS-Integration](https://github.com/bwRavencl/ControllerBuddy-DCS-Integration), an exemplary integration of ControllerBuddy into [DCS World](https://www.digitalcombatsimulator.com).

## Attribution

ControllerBuddy makes use of these awesome software technologies and libraries:

- [OpenJDK](https://openjdk.org)
- [Apache Batik](https://xmlgraphics.apache.org/batik)
- [Apache Commons CLI](https://commons.apache.org/proper/commons-cli)
- [ClassGraph](https://github.com/classgraph/classgraph)
- [dbus-java](https://hypfvieh.github.io/dbus-java/)
- [Error Prone](https://errorprone.info/)
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

If you want to build ControllerBuddy from source, this section might be helpful to get you started.  
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

## License

[GNU General Public License v3.0](LICENSE)
