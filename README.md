![ControllerBuddy Logo](https://github.com/bwRavencl/ControllerBuddy/raw/master/res/icon_128.png
"ControllerBuddy")
##ControllerBuddy

#####License Information:
GNU General Public License v2.0

#####Description:
ControllerBuddy allows the feeding of input from a physical game-controller connected to a host computer to a virtual joystick, provided by the vJoy device driver. ControllerBuddy can either feed to a local vJoy device on the host or to one on a second computer.

Consequently ControllerBuddy can operate in three different roles:
- Local (Single machine with a physical and a vJoy device)
- Server (Source machine with the physical device)
- Client (Target machine with the vJoy device)

When using ControllerBuddy in Client-Server mode, both instances communicate over a lightweight UDP-based protocol.

In order to support complex input profiles, ControllerBuddy provides very flexible programming of the physical controller. Each axis or button present on the device can be mapped to one or multiple actions invoked on the target host.

Currently the following actions are supported:
- Axis movements
- Relative axis movements
- Button presses
- Cursor movement
- Keyboard inputs
- Mouse button presses
- Mouse scrolling
- Cycles of actions
- Mode switching
- Relative axis resetting

ControllerBuddy supports the creation of multiple input modes, which are best be described as shift-states.
In each mode, each physical axis or button can be mapped to a different action.
Mode switching can be done by either holding down a button on the physical controller or by tapping it once.

The whole programming of the physical controller can be performed via the graphical user interface of ControllerBuddy.
The resulting can be exported to a simple JSON-based file format.

For maximum platform-independence ControllerBuddy was implemented as a Java application, supporting all three major operating systems Windows, Mac OS X and Linux when running as a server. When running as a client currently only Windows is supported.

#####Architecture:
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

#####Command Line Parameters:
usage: ControllerBuddy [-autostart <arg>] [-tray]
 -autostart <arg>   automatically start the:
                    local feeder [-autostart local] or
                    client [-autostart client] or
                    server [-autostart server]
 -tray              launch in tray

#####Requirements:
- Java SE Runtime Environment 8 (http://www.oracle.com/technetwork/java/javase/overview/index.html)
- vJoy 2.1.6 (http://vjoystick.sourceforge.net)
