##RemoteStick Server
==================

#####License Information:
GNU General Public License v2.0

#####Description:
RemoteStick allows the feeding of input from a physical game-controller connected to a host computer to a virtual joystick (provided by the vJoy device driver) of a second computer.
Consequently RemoteStick consists of two separate applications: RemoteStick Server and RemoteStick Client (http://github.com/bwRavencl/RemoteStickClient),  both of which communicate via a lightweight UDP-based protocol.  
In order to support complex input profiles, RemoteStick Server provides very flexible programming of the physical controller. Each axis or button present on the device can be mapped to one or multiple actions invoked on the target host.

Currently the following actions are supported:
- Axis movements
- Relative axis movements
- Button presses
- Cursor movement
- Keyboard/Mouse inputs
- Mouse scrolling
- Cycles of actions

RemoteStick supports the creation of multiple input modes, which are best be described as shift-states.
In each mode, each physical axis or button can be mapped to a different action.
Mode switching can be done by either holding down a chosen button on the physical controller or by tapping it once.

The whole programming of the physical controller is done using the graphical user interface of RemoteStick Server.
Profiles created using RemoteStick Server can be exported in a simple JSON-based file format.

For maximum platform-independence RemoteStick Server was implemented as a Java application, supporting all three major operating systems Windows, Mac OS X and Linux.

#####Architecture:
<pre>
Host                        Client
----                        ------

RemoteStick Server -------> RemoteStick Client

^                           |
|                           |
|                           v

Physical Controller         vJoy Device Driver

                            |
                            |
                            v

                            Target Application
</pre>

#####Requirements:
- Java SE Runtime Environment 8
