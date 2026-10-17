/*
 * Copyright (C) 2026 Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.bwravencl.controllerbuddy;

import de.bwravencl.controllerbuddy.constants.Constants;
import de.bwravencl.controllerbuddy.ffi.Wayland;
import de.bwravencl.controllerbuddy.gui.GuiUtils;
import de.bwravencl.controllerbuddy.gui.MainFrame;
import de.bwravencl.controllerbuddy.util.MainLoop;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;
import org.apache.commons.cli.help.TextHelpAppendable;
import org.jspecify.annotations.Nullable;

/// The main entry point for the application.
///
/// Parses command-line arguments, enforces single-instance constraints,
/// initializes SDL, and launches the GUI on the event dispatch thread.
@SuppressWarnings("exports")
public final class Main {

	/// Application identifier derived from the package of the [Main] class.
	public static final String APPLICATION_ID = Main.class.getPackageName();

	/// Classpath resource paths for the application window icon at various sizes.
	public static final List<String> ICON_RESOURCE_PATHS = List.of("/icon_16.png", "/icon_32.png", "/icon_64.png",
			"/icon_128.png");

	/// CLI option name for the autostart mode selection.
	public static final String OPTION_AUTOSTART = "autostart";

	/// CLI autostart value that selects client mode.
	public static final String OPTION_AUTOSTART_VALUE_CLIENT = "client";

	/// CLI autostart value that selects local mode.
	public static final String OPTION_AUTOSTART_VALUE_LOCAL = "local";

	/// CLI autostart value that selects server mode.
	public static final String OPTION_AUTOSTART_VALUE_SERVER = "server";

	/// CLI option name for exporting a profile to HTML.
	public static final String OPTION_EXPORT = "export";

	/// CLI option name for specifying a custom game controller database path.
	public static final String OPTION_GAME_CONTROLLER_DB = "gamecontrollerdb";

	/// CLI option name for specifying the remote host address.
	public static final String OPTION_HOST = "host";

	/// CLI option name for specifying the connection password.
	public static final String OPTION_PASSWORD = "password";

	/// CLI option name for specifying the network port.
	public static final String OPTION_PORT = "port";

	/// CLI option name for specifying the profile file to load.
	public static final String OPTION_PROFILE = "profile";

	/// CLI option name for quitting a running instance.
	public static final String OPTION_QUIT = "quit";

	/// CLI option name for saving the current profile on start.
	public static final String OPTION_SAVE = "save";

	/// CLI option name for specifying the connection timeout.
	public static final String OPTION_TIMEOUT = "timeout";

	/// CLI option name for minimizing the application to the system tray on start.
	public static final String OPTION_TRAY = "tray";

	/// The OS architecture string, as reported by the `os.arch` system property.
	public static final String OS_ARCH = System.getProperty("os.arch");

	/// The OS name string, as reported by the `os.name` system property.
	public static final String OS_NAME = System.getProperty("os.name");

	/// Whether the current operating system is Linux.
	public static final boolean IS_LINUX = OS_NAME.startsWith("Linux");

	/// Whether the current operating system is macOS.
	public static final boolean IS_MAC = OS_NAME.startsWith("Mac") || OS_NAME.startsWith("Darwin");

	/// Whether the current operating system is Windows.
	public static final boolean IS_WINDOWS = OS_NAME.startsWith("Windows");

	/// Preferences key for the auto-restart output setting.
	public static final String PREFERENCES_AUTO_RESTART_OUTPUT = "auto_restart_output";

	/// Preferences key for the haptic feedback setting.
	public static final String PREFERENCES_HAPTIC_FEEDBACK = "haptic_feedback";

	/// Preferences key for the stored remote host address.
	public static final String PREFERENCES_HOST = "host";

	/// Preferences key for the hot-swapping button assignment.
	public static final String PREFERENCES_HOT_SWAPPING_BUTTON = "hot_swapping_button";

	/// Preferences key for the last used controller ID.
	public static final String PREFERENCES_LAST_CONTROLLER = "last_controller";

	/// Preferences key for the last loaded profile path.
	public static final String PREFERENCES_LAST_PROFILE = "last_profile";

	/// Preferences key for the controller LED color.
	public static final String PREFERENCES_LED_COLOR = "led_color";

	/// Preferences key for the circular-to-square axis mapping setting.
	public static final String PREFERENCES_MAP_CIRCULAR_AXES_TO_SQUARE = "map_circular_axes_to_square";

	/// Preferences key for the maximum polling rate.
	public static final String PREFERENCES_MAX_POLLING_RATE = "max_polling_rate";

	/// Preferences key for the minimum polling rate.
	public static final String PREFERENCES_MIN_POLLING_RATE = "min_polling_rate";

	/// Preferences key for the overlay scaling factor.
	public static final String PREFERENCES_OVERLAY_SCALING = "overlay_scaling";

	/// Preferences key for the stored connection password.
	public static final String PREFERENCES_PASSWORD = "password";

	/// Preferences key for the stored network port.
	public static final String PREFERENCES_PORT = "port";

	/// Preferences key for the prevent-power-save-mode setting.
	public static final String PREFERENCES_PREVENT_POWER_SAVE_MODE = "prevent_power_save_mode";

	/// Preferences key for the show-donate-button visibility setting.
	public static final String PREFERENCES_SHOW_DONATE_BUTTON = "show_donate_button";

	/// Preferences key for the skip-controller-dialogs setting.
	public static final String PREFERENCES_SKIP_CONTROLLER_DIALOGS = "skip_controller_dialogs";

	/// Preferences key for the skip-tray-icon-hint setting.
	public static final String PREFERENCES_SKIP_TRAY_ICON_HINT = "skip_tray_icon_hint";

	/// Preferences key for the swap-left-and-right-sticks setting.
	public static final String PREFERENCES_SWAP_LEFT_AND_RIGHT_STICKS = "swap_left_and_right_sticks";

	/// Preferences key for the UI theme selection.
	public static final String PREFERENCES_THEME = "theme";

	/// Preferences key for the stored connection timeout value.
	public static final String PREFERENCES_TIMEOUT = "timeout";

	/// Preferences key for the touchpad cursor sensitivity value.
	public static final String PREFERENCES_TOUCHPAD_CURSOR_SENSITIVITY = "touchpad_cursor_sensitivity";

	/// Preferences key for the touchpad-enabled setting.
	public static final String PREFERENCES_TOUCHPAD_ENABLED = "touchpad_enabled";

	/// Preferences key for the touchpad scroll sensitivity value.
	public static final String PREFERENCES_TOUCHPAD_SCROLL_SENSITIVITY = "touchpad_scroll_sensitivity";

	/// Preferences key for the vJoy device index.
	public static final String PREFERENCES_VJOY_DEVICE = "vjoy_device";

	/// Preferences key for the vJoy installation directory path.
	public static final String PREFERENCES_VJOY_DIRECTORY = "vjoy_directory";

	/// Preferences key for the Vulkan hardware acceleration setting.
	public static final String PREFERENCES_VULKAN_HARDWARE_ACCELERATION = "vulkan_hardware_acceleration";

	/// Preferences key for the Vulkan renderer setting.
	public static final String PREFERENCES_VULKAN_RENDERER = "vulkan_renderer";

	/// Preferences key for the Wayland support setting.
	public static final String PREFERENCES_WAYLAND_SUPPORT = "wayland_support";

	/// Preferences key for the XInput setting.
	public static final String PREFERENCES_XINPUT = "xinput";

	/// Acknowledgement token exchanged in the single-instance IPC protocol.
	public static final String SINGLE_INSTANCE_ACK = "ACK";

	/// End-of-file sentinel token in the single-instance IPC protocol.
	public static final String SINGLE_INSTANCE_EOF = "EOF";

	/// Initialization token sent when a second instance contacts the first.
	public static final String SINGLE_INSTANCE_INIT = "INIT";

	/// Lock file used to detect and communicate with an already-running instance.
	public static final File SINGLE_INSTANCE_LOCK_FILE = new File(
			System.getProperty("java.io.tmpdir") + File.separator + Constants.APPLICATION_NAME + ".lock");

	/// Apache Commons CLI options descriptor for the application's command line.
	public static final Options options = new Options();

	/// Persistent user preferences store for this application.
	public static final Preferences preferences;

	/// Resource bundle containing localized UI strings.
	public static final ResourceBundle strings = ResourceBundle.getBundle("strings");

	/// Number of retries when attempting to read the single-instance lock file
	private static final int NUM_LOCK_FILE_READ_RETRIES = 3;

	/// CLI option name for printing the help text.
	private static final String OPTION_HELP = "help";

	/// CLI option name for suppressing message dialogs.
	private static final String OPTION_SKIP_MESSAGE_DIALOGS = "skipMessageDialogs";

	/// CLI option name for printing the application version.
	private static final String OPTION_VERSION = "version";

	private static final Logger logger = Logger.getLogger(Main.class.getName());

	/// The singleton [MainFrame] instance.
	private static @Nullable MainFrame mainFrame;

	/// The [MainLoop] used for SDL thread dispatch.
	private static @Nullable MainLoop mainLoop;

	/// Whether the application has received a termination signal.
	private static volatile boolean terminated;

	static {
		options.addOption(OPTION_AUTOSTART, true,
				MessageFormat.format(strings.getString("AUTOSTART_OPTION_DESCRIPTION"),
						IS_WINDOWS || IS_LINUX ? strings.getString("LOCAL_FEEDER_OR_CLIENT_OR_SERVER")
								: strings.getString("SERVER")));
		options.addOption(OPTION_PROFILE, true, strings.getString("PROFILE_OPTION_DESCRIPTION"));
		options.addOption(OPTION_HOST, true, strings.getString("HOST_OPTION_DESCRIPTION"));
		options.addOption(OPTION_PORT, true, strings.getString("PORT_OPTION_DESCRIPTION"));
		options.addOption(OPTION_TIMEOUT, true, strings.getString("TIMEOUT_OPTION_DESCRIPTION"));
		options.addOption(OPTION_PASSWORD, true, strings.getString("PASSWORD_OPTION_DESCRIPTION"));
		options.addOption(OPTION_GAME_CONTROLLER_DB, true, strings.getString("GAME_CONTROLLER_DB_OPTION_DESCRIPTION"));
		options.addOption(OPTION_TRAY, false, strings.getString("TRAY_OPTION_DESCRIPTION"));
		options.addOption(OPTION_SAVE, true, strings.getString("SAVE_OPTION_DESCRIPTION"));
		options.addOption(OPTION_EXPORT, true, strings.getString("EXPORT_OPTION_DESCRIPTION"));
		options.addOption(OPTION_SKIP_MESSAGE_DIALOGS, false,
				strings.getString("SKIP_MESSAGE_DIALOGS_OPTION_DESCRIPTION"));
		options.addOption(OPTION_QUIT, false, strings.getString("QUIT_OPTION_DESCRIPTION"));
		options.addOption(OPTION_VERSION, false, strings.getString("VERSION_OPTION_DESCRIPTION"));
		options.addOption(OPTION_HELP, false, strings.getString("HELP_OPTION_DESCRIPTION"));

		preferences = Preferences.userRoot().node("/" + APPLICATION_ID.replace('.', '/'));

		if (IS_LINUX && isWaylandSupport()
				&& (System.getenv("WAYLAND_DISPLAY") != null || System.getenv("WAYLAND_SOCKET") != null)) {
			try {
				if (new WaylandLayerShellProbe().probe()) {
					System.setProperty("awt.toolkit.name", "WLToolkit");

					if (isVulkanRenderer()) {
						System.setProperty("sun.java2d.vulkan", "true");

						if (isVulkanHardwareAcceleration()) {
							System.setProperty("sun.java2d.vulkan.accelsd", "true");
						}
					}
				}
			} catch (final Throwable t) {
				logger.log(Level.WARNING, t.getMessage(), t);
			}
		}
	}

	/// Prevents instantiation.
	private Main() {
	}

	/// Deletes the single-instance lock file and logs a warning if deletion fails.
	private static void deleteSingleInstanceLockFile() {
		if (!SINGLE_INSTANCE_LOCK_FILE.delete()) {
			logger.warning("Could not delete single instance lock file " + SINGLE_INSTANCE_LOCK_FILE.getAbsolutePath());
		}
	}

	/// Returns the URL of a classpath resource by its path.
	///
	/// @param resourcePath the path of the resource relative to the classpath
	/// @return the [URL] of the resource
	/// @throws RuntimeException if no resource is found at the given path
	public static URL getResourceLocation(final String resourcePath) {
		final var resourceLocation = MainFrame.class.getResource(resourcePath);
		if (resourceLocation == null) {
			throw new RuntimeException("Resource not found: " + resourcePath);
		}

		return resourceLocation;
	}

	/// Returns whether the Vulkan renderer should use hardware acceleration.
	///
	/// @return `true` if the Vulkan hardware acceleration should be used
	public static boolean isVulkanHardwareAcceleration() {
		return preferences.getBoolean(PREFERENCES_VULKAN_HARDWARE_ACCELERATION, false);
	}

	/// Returns whether the Vulkan renderer should be used.
	///
	/// @return `true` if the Vulkan renderer should be used
	public static boolean isVulkanRenderer() {
		return preferences.getBoolean(PREFERENCES_VULKAN_RENDERER, false);
	}

	/// Returns whether native Wayland support should be used for rendering if
	/// available.
	///
	/// @return `true` if native Wayland support should be used
	public static boolean isWaylandSupport() {
		return preferences.getBoolean(PREFERENCES_WAYLAND_SUPPORT, false);
	}

	/// Application entry point. Parses command-line arguments, enforces
	/// single-instance constraints, initializes SDL, and launches the GUI on the
	/// event dispatch thread.
	///
	/// @param args command-line arguments
	static void main(final String[] args) {
		logger.info("Launching " + Constants.APPLICATION_NAME + " " + Constants.VERSION);
		logger.info("Operating System: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " "
				+ OS_ARCH);
		logger.info("AWT Toolkit: " + Toolkit.getDefaultToolkit().getClass().getName());

		Thread.setDefaultUncaughtExceptionHandler((_, e) -> {
			logger.log(Level.SEVERE, e.getMessage(), e);

			Thread.setDefaultUncaughtExceptionHandler(null);

			if (GraphicsEnvironment.isHeadless() || mainFrame == null) {
				terminate(1);
				return;
			}

			GuiUtils.invokeOnEventDispatchThreadIfRequired(() -> {
				final var stringWriter = new StringWriter();
				e.printStackTrace(new PrintWriter(stringWriter));

				final var panel = new JPanel();
				panel.setLayout(new BorderLayout(5, 5));
				panel.add(new JLabel(MessageFormat.format(strings.getString("UNCAUGHT_EXCEPTION_DIALOG_TEXT"),
						Constants.APPLICATION_NAME)), BorderLayout.NORTH);
				final var textArea = new JTextArea(stringWriter.toString());
				textArea.setEditable(false);
				panel.add(GuiUtils.wrapComponentInScrollPane(textArea, new Dimension(600, 400)), BorderLayout.CENTER);
				GuiUtils.showMessageDialog(mainFrame, mainFrame, panel, strings.getString("ERROR_DIALOG_TITLE"),
						JOptionPane.ERROR_MESSAGE, true);

				if (mainFrame != null && mainFrame.isUnsavedChanges()) {
					try {
						final var filename = mainFrame.getCurrentFile().map(File::getName).orElse("unnamed_profile");
						final var file = new File(System.getProperty("java.io.tmpdir") + File.separator + filename);

						if (!file.exists()) {
							mainFrame.saveProfile(file, false);
						}
					} catch (final Exception e1) {
						logger.log(Level.SEVERE, e1.getMessage(), e1);
					}
				}

				terminate(1);
			});
		});

		try {
			final var commandLine = new DefaultParser().parse(options, args);
			if (commandLine.hasOption(OPTION_VERSION)) {
				printCommandLineMessage(Constants.APPLICATION_NAME + " " + Constants.VERSION);

				return;
			}
			if (!commandLine.hasOption(OPTION_HELP)) {
				var continueLaunch = true;

				if (SINGLE_INSTANCE_LOCK_FILE.exists()) {
					String portString = null;
					String randomNumberString = null;

					for (var i = 0; i < NUM_LOCK_FILE_READ_RETRIES; i++) {
						try (final var lockFileReader = new BufferedReader(
								new FileReader(SINGLE_INSTANCE_LOCK_FILE, StandardCharsets.UTF_8))) {
							portString = lockFileReader.readLine();
							if (portString != null) {
								randomNumberString = lockFileReader.readLine();
							}
						} catch (final IOException e) {
							portString = null;
							randomNumberString = null;
						}

						if (portString != null && randomNumberString != null) {
							break;
						}

						if (i < NUM_LOCK_FILE_READ_RETRIES - 1) {
							try {
								Thread.sleep(100);
							} catch (final InterruptedException e) {
								Thread.currentThread().interrupt();
								break;
							}
						}
					}

					try {
						if (portString == null || randomNumberString == null) {
							throw new IOException("Could not read single instance lock data after "
									+ NUM_LOCK_FILE_READ_RETRIES + " retries");
						}

						final var port = Integer.parseInt(portString);

						try (final var socket = new Socket(InetAddress.getLoopbackAddress(), port);
								final var printStream = new PrintStream(socket.getOutputStream(), false,
										StandardCharsets.UTF_8);
								final var socketBufferedReader = new BufferedReader(
										new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
							socket.setSoTimeout(5000);
							printStream.println(randomNumberString);
							printStream.println(SINGLE_INSTANCE_INIT);

							for (final var arg : args) {
								printStream.println(arg);
							}

							printStream.println(SINGLE_INSTANCE_EOF);
							printStream.flush();

							String responseLine = null;
							try {
								responseLine = socketBufferedReader.readLine();
							} catch (final IOException e) {
								logger.log(Level.WARNING, "Other " + Constants.APPLICATION_NAME
										+ " instance did not acknowledge invocation in time", e);
							}

							if (!SINGLE_INSTANCE_ACK.equals(responseLine)) {
								if (responseLine != null) {
									logger.warning("Other " + Constants.APPLICATION_NAME
											+ " instance sent unexpected response: " + responseLine);
								}
								terminate(1);
								return;
							}

							continueLaunch = false;
						}
					} catch (final IOException | NumberFormatException e) {
						logger.log(Level.WARNING, e.getMessage(), e);
						deleteSingleInstanceLockFile();
					}
				}

				if (!continueLaunch) {
					logger.info("Another " + Constants.APPLICATION_NAME + " instance is already running");
					terminate(0);

					return;
				}

				if (GraphicsEnvironment.isHeadless()) {
					logger.severe("Headless environment detected - aborting launch");
					System.err.println(
							"Error: A graphical environment is required to run " + Constants.APPLICATION_NAME + ".");
					terminate(1);

					return;
				}

				GuiUtils.skipMessageDialogs = commandLine.hasOption(OPTION_SKIP_MESSAGE_DIALOGS);

				final var mainLoop = new MainLoop();
				Main.mainLoop = new MainLoop();

				EventQueue.invokeLater(() -> {
					Runtime.getRuntime().addShutdownHook(new Thread(() -> {
						if (!terminated) {
							Runtime.getRuntime().halt(2);
						}
					}, "Shutdown Hook"));

					mainFrame = new MainFrame(mainLoop, commandLine);
					mainLoop.startSdlEventPolling();
				});

				mainLoop.enterLoop();

				return;
			}
		} catch (ParseException _) {
			// handled below
		}

		final var stringWriter = new StringWriter();
		try (final var printWriter = new PrintWriter(stringWriter)) {
			final var helpFormatter = HelpFormatter.builder().setHelpAppendable(new TextHelpAppendable(printWriter))
					.setShowSince(false).get();
			helpFormatter.printHelp(Constants.APPLICATION_NAME, null, options, null, true);
			printWriter.flush();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		printCommandLineMessage(stringWriter.toString());
	}

	/// Prints a message to standard output and, when running in a graphical
	/// environment, also shows it in a dialog.
	///
	/// @param message the message text to display
	private static void printCommandLineMessage(final String message) {
		IO.println(message);

		if (!GraphicsEnvironment.isHeadless()) {
			EventQueue.invokeLater(() -> {
				final var textArea = new JTextArea(message);
				textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
				textArea.setEditable(false);

				final var imageIcon = new ImageIcon(getResourceLocation(ICON_RESOURCE_PATHS.get(2)));
				GuiUtils.showMessageDialog(null, null, textArea, Constants.APPLICATION_NAME,
						JOptionPane.INFORMATION_MESSAGE, imageIcon, true);
			});
		}
	}

	/// Shuts down the application cleanly and exits the JVM with the given status
	/// code.
	///
	/// Shuts down the main loop if present, flushes the preferences, logs the
	/// termination, and calls `System.exit`.
	///
	/// @param status the exit status code
	public static void terminate(final int status) {
		if (mainLoop != null) {
			mainLoop.shutdown();
		}

		try {
			preferences.flush();
		} catch (final BackingStoreException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
		}

		logger.info("Terminated (" + status + ")");

		terminated = true;

		System.exit(status);
	}

	/// Checks whether the compositor offers `zwlr_layer_shell_v1`, without loading
	/// any AWT classes.
	///
	/// It opens its own short-lived Wayland connection, so it can run before the
	/// AWT toolkit is chosen.
	@SuppressWarnings("restricted")
	private static final class WaylandLayerShellProbe {

		/// Whether the registry announced the layer shell global.
		private boolean found;

		/// Handles `wl_registry.global`.
		///
		/// @param data unused user data
		/// @param registry the registry
		/// @param name the numeric name of the global
		/// @param iface the interface name as C string
		/// @param version the interface version
		@SuppressWarnings("unused")
		private void onGlobal(final MemorySegment data, final MemorySegment registry, final int name,
				final MemorySegment iface, final int version) {
			try {
				if (Wayland.ZWLR_LAYER_SHELL_V1_NAME.equals(iface.reinterpret(Long.MAX_VALUE).getString(0))) {
					found = true;
				}
			} catch (final Throwable t) {
				logger.log(Level.SEVERE, t.getMessage(), t);
			}
		}

		/// Probes the compositor to see if `zwlr_layer_shell_v1` is available.
		///
		/// A connection to the Wayland display is established, the globals are listed,
		/// then the connection is closed again.
		///
		/// @return `true` if the `zwlr_layer_shell_v1` global exists
		private boolean probe() {
			final var display = Wayland.wl_display_connect(MemorySegment.NULL);
			if (Wayland.isNull(display)) {
				return false;
			}

			try (final var arena = Arena.ofConfined()) {
				final var listener = Wayland.listener(arena,
						Wayland.stub(MethodHandles.lookup(), this, "onGlobal", Wayland.V_PPIPI, arena),
						Wayland.noop(Wayland.V_PPI, arena));
				final var registry = Wayland.create(display, Wayland.WL_DISPLAY_GET_REGISTRY,
						Wayland.WL_REGISTRY_INTERFACE, Wayland.M_P, MemorySegment.NULL);

				Wayland.wl_proxy_add_listener(registry, listener, MemorySegment.NULL);
				final var roundtripSucceeded = Wayland.wl_display_roundtrip(display) >= 0;
				Wayland.wl_proxy_destroy(registry);

				return roundtripSucceeded && found;
			} finally {
				Wayland.wl_display_disconnect(display);
			}
		}
	}
}
