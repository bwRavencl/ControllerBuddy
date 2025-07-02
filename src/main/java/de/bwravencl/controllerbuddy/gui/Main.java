/* Copyright (C) 2014  Matteo Hausner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.bwravencl.controllerbuddy.gui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import de.bwravencl.controllerbuddy.constants.Constants;
import de.bwravencl.controllerbuddy.ffi.VjoyInterface;
import de.bwravencl.controllerbuddy.gui.GuiUtils.FrameDragListener;
import de.bwravencl.controllerbuddy.input.Input;
import de.bwravencl.controllerbuddy.input.Input.VirtualAxis;
import de.bwravencl.controllerbuddy.input.LockKey;
import de.bwravencl.controllerbuddy.input.Mode;
import de.bwravencl.controllerbuddy.input.OverlayAxis;
import de.bwravencl.controllerbuddy.input.Profile;
import de.bwravencl.controllerbuddy.input.ScanCode;
import de.bwravencl.controllerbuddy.input.action.AxisToRelativeAxisAction;
import de.bwravencl.controllerbuddy.input.action.ButtonToModeAction;
import de.bwravencl.controllerbuddy.input.action.IAction;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction;
import de.bwravencl.controllerbuddy.input.action.IActivatableAction.Activation;
import de.bwravencl.controllerbuddy.input.action.ILongPressAction;
import de.bwravencl.controllerbuddy.json.ActionTypeAdapter;
import de.bwravencl.controllerbuddy.json.ColorTypeAdapter;
import de.bwravencl.controllerbuddy.json.LockKeyAdapter;
import de.bwravencl.controllerbuddy.json.ProfileTypeAdapterFactory;
import de.bwravencl.controllerbuddy.json.ScanCodeAdapter;
import de.bwravencl.controllerbuddy.runmode.ClientRunMode;
import de.bwravencl.controllerbuddy.runmode.LocalRunMode;
import de.bwravencl.controllerbuddy.runmode.OutputRunMode;
import de.bwravencl.controllerbuddy.runmode.RunMode;
import de.bwravencl.controllerbuddy.runmode.ServerRunMode;
import de.bwravencl.controllerbuddy.runmode.UinputDevice;
import de.bwravencl.controllerbuddy.util.RunnableWithDefaultExceptionHandler;
import de.bwravencl.controllerbuddy.util.VersionUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serial;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.PlainDocument;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.anim.dom.SVGStylableElement;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.constants.XMLConstants;
import org.apache.batik.dom.util.DOMUtilities;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.util.CSSConstants;
import org.apache.batik.util.SVGConstants;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLGUID;
import org.lwjgl.sdl.SDLGamepad;
import org.lwjgl.sdl.SDLHints;
import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLMisc;
import org.lwjgl.sdl.SDLPixels;
import org.lwjgl.sdl.SDLSurface;
import org.lwjgl.sdl.SDLTray;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_GUID;
import org.lwjgl.sdl.SDL_Surface;
import org.lwjgl.system.MemoryStack;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.svg.SVGDocument;

public final class Main {

	public static final int DEFAULT_HGAP = 10;

	public static final int DEFAULT_VGAP = 10;

	public static final String OS_ARCH = System.getProperty("os.arch");

	public static final String OS_NAME = System.getProperty("os.name");

	public static final boolean IS_LINUX = OS_NAME.startsWith("Linux");

	public static final boolean IS_MAC = OS_NAME.startsWith("Mac") || OS_NAME.startsWith("Darwin");

	public static final boolean IS_WINDOWS = OS_NAME.startsWith("Windows");

	public static final int PASSWORD_MAX_LENGTH = 24;

	public static final int PASSWORD_MIN_LENGTH = 6;

	public static final ResourceBundle STRINGS = ResourceBundle.getBundle("strings");

	public static final String SWAPPED_SYMBOL = "⇆";

	@SuppressWarnings("exports")
	public static final Color TRANSPARENT = new Color(255, 255, 255, 0);

	static final int BUTTON_DIMENSION_HEIGHT = 25;

	@SuppressWarnings("exports")
	public static final Dimension BUTTON_DIMENSION = new Dimension(115, BUTTON_DIMENSION_HEIGHT);

	@SuppressWarnings({ "exports", "SuspiciousNameCombination" })
	public static final Dimension SQUARE_BUTTON_DIMENSION = new Dimension(BUTTON_DIMENSION_HEIGHT,
			BUTTON_DIMENSION_HEIGHT);

	static final int DEFAULT_OVERLAY_SCALING = 1;

	private static final String CONTROLLER_SVG_FILENAME = "controller.svg";

	private static final FlowLayout DEFAULT_FLOW_LAYOUT = new FlowLayout(FlowLayout.LEFT, DEFAULT_HGAP, DEFAULT_VGAP);

	private static final int DIALOG_BOUNDS_HEIGHT = 710;

	private static final int DIALOG_BOUNDS_WIDTH = 1015;

	private static final int DIALOG_BOUNDS_X = 100;

	private static final int DIALOG_BOUNDS_Y = 100;

	private static final String GAME_CONTROLLER_DATABASE_FILENAME = "gamecontrollerdb.txt";

	private static final Insets GRID_BAG_ITEM_INSETS = new Insets(8, DEFAULT_HGAP, 8, DEFAULT_HGAP);

	private static final String[] ICON_RESOURCE_PATHS = { "/icon_16.png", "/icon_32.png", "/icon_64.png",
			"/icon_128.png" };

	private static final Border LIST_ITEM_BORDER = BorderFactory.createEtchedBorder();

	private static final Insets LIST_ITEM_INNER_INSETS = new Insets(4, 4, 4, 4);

	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

	private static final FlowLayout LOWER_BUTTONS_FLOW_LAYOUT = new FlowLayout(FlowLayout.RIGHT, DEFAULT_HGAP + 2, 5);

	private static final Options OPTIONS = new Options();

	private static final String OPTION_AUTOSTART = "autostart";

	private static final String OPTION_AUTOSTART_VALUE_CLIENT = "client";

	private static final String OPTION_AUTOSTART_VALUE_LOCAL = "local";

	private static final String OPTION_AUTOSTART_VALUE_SERVER = "server";

	private static final String OPTION_EXPORT = "export";

	private static final String OPTION_GAME_CONTROLLER_DB = "gamecontrollerdb";

	private static final String OPTION_HELP = "help";

	private static final String OPTION_HOST = "host";

	private static final String OPTION_PASSWORD = "password";

	private static final String OPTION_PORT = "port";

	private static final String OPTION_PROFILE = "profile";

	private static final String OPTION_QUIT = "quit";

	private static final String OPTION_SAVE = "save";

	private static final String OPTION_SKIP_MESSAGE_DIALOGS = "skipMessageDialogs";

	private static final String OPTION_TIMEOUT = "timeout";

	private static final String OPTION_TRAY = "tray";

	private static final String OPTION_VERSION = "version";

	private static final int OVERLAY_INDICATOR_PROGRESS_BAR_HEIGHT = 150;

	private static final int OVERLAY_INDICATOR_PROGRESS_BAR_WIDTH = 20;

	private static final int OVERLAY_MODE_LABEL_MAX_WIDTH = 200;

	private static final long OVERLAY_POSITION_UPDATE_DELAY = 1L;

	private static final long OVERLAY_POSITION_UPDATE_INTERVAL = 10L;

	private static final String PREFERENCES_AUTO_RESTART_OUTPUT = "auto_restart_output";

	private static final String PREFERENCES_HAPTIC_FEEDBACK = "haptic_feedback";

	private static final String PREFERENCES_HOST = "host";

	private static final String PREFERENCES_HOT_SWAPPING_BUTTON = "hot_swapping_button";

	private static final String PREFERENCES_LAST_CONTROLLER = "last_controller";

	private static final String PREFERENCES_LAST_PROFILE = "last_profile";

	private static final String PREFERENCES_LED_COLOR = "led_color";

	private static final String PREFERENCES_MAP_CIRCULAR_AXES_TO_SQUARE = "map_circular_axes_to_square";

	private static final String PREFERENCES_OVERLAY_SCALING = "overlay_scaling";

	private static final String PREFERENCES_PASSWORD = "password";

	private static final String PREFERENCES_POLL_INTERVAL = "poll_interval";

	private static final String PREFERENCES_PORT = "port";

	private static final String PREFERENCES_PREVENT_POWER_SAVE_MODE = "prevent_power_save_mode";

	private static final String PREFERENCES_SKIP_CONTROLLER_DIALOGS = "skip_controller_dialogs";

	private static final String PREFERENCES_SKIP_TRAY_ICON_HINT = "skip_tray_icon_hint";

	private static final String PREFERENCES_SWAP_LEFT_AND_RIGHT_STICKS = "swap_left_and_right_sticks";

	private static final String PREFERENCES_THEME = "theme";

	private static final String PREFERENCES_TIMEOUT = "timeout";

	private static final String PREFERENCES_TOUCHPAD_CURSOR_SENSITIVITY = "touchpad_cursor_sensitivity";

	private static final String PREFERENCES_TOUCHPAD_ENABLED = "touchpad_enabled";

	private static final String PREFERENCES_TOUCHPAD_SCROLL_SENSITIVITY = "touchpad_scroll_sensitivity";

	private static final String PREFERENCES_VJOY_DEVICE = "vjoy_device";

	private static final String PREFERENCES_VJOY_DIRECTORY = "vjoy_directory";

	private static final String PROFILE_FILE_EXTENSION = "json";

	private static final String PROFILE_FILE_SUFFIX = "." + PROFILE_FILE_EXTENSION;

	private static final int SETTINGS_LABEL_DIMENSION_HEIGHT = 15;

	private static final Dimension SETTINGS_LABEL_DIMENSION = new Dimension(160, SETTINGS_LABEL_DIMENSION_HEIGHT);

	private static final Dimension OVERLAY_SETTINGS_LABEL_DIMENSION = new Dimension(100,
			SETTINGS_LABEL_DIMENSION_HEIGHT);

	private static final Dimension CONNECTION_SETTINGS_LABEL_DIMENSION = new Dimension(80,
			SETTINGS_LABEL_DIMENSION_HEIGHT);

	private static final String SINGLE_INSTANCE_ACK = "ACK";

	private static final String SINGLE_INSTANCE_EOF = "EOF";

	private static final String SINGLE_INSTANCE_INIT = "INIT";

	private static final File SINGLE_INSTANCE_LOCK_FILE;

	private static final String SVG_DARK_THEME_PATH_COLOR = "#AAA";

	private static final String SVG_DARK_THEME_TEXT_COLOR = "#FFFFFF";

	private static final String SVG_ID_A = "a";

	private static final String SVG_ID_B = "b";

	private static final String SVG_ID_BACK = "back";

	private static final String SVG_ID_DPAD_DOWN = "dpdown";

	private static final String SVG_ID_DPAD_LEFT = "dpleft";

	private static final String SVG_ID_DPAD_RIGHT = "dpright";

	private static final String SVG_ID_DPAD_UP = "dpup";

	private static final String SVG_ID_GUIDE = "guide";

	private static final String SVG_ID_LEFT_SHOULDER = "leftshoulder";

	private static final String SVG_ID_LEFT_STICK = "leftstick";

	private static final String SVG_ID_LEFT_TRIGGER = "lefttrigger";

	private static final String SVG_ID_LEFT_X = "leftx";

	private static final String SVG_ID_LEFT_Y = "lefty";

	private static final String SVG_ID_RIGHT_SHOULDER = "rightshoulder";

	private static final String SVG_ID_RIGHT_STICK = "rightstick";

	private static final String SVG_ID_RIGHT_TRIGGER = "righttrigger";

	private static final String SVG_ID_RIGHT_X = "rightx";

	private static final String SVG_ID_RIGHT_Y = "righty";

	private static final String SVG_ID_START = "start";

	private static final String SVG_ID_X = "x";

	private static final String SVG_ID_Y = "y";

	private static final int SVG_VIEWBOX_MARGIN = 20;

	private static final String TRAY_ICON_HINT_IMAGE_RESOURCE_PATH = "/tray_icon_hint.png";

	private static final String VJOY_DEVICE_VID_PID = "0x1234/0xBEAD";

	private static final String WEBSITE_URL = "https://controllerbuddy.org";

	static volatile Main main;

	static boolean skipMessageDialogs;

	private static volatile boolean terminated;

	static {
		OPTIONS.addOption(OPTION_AUTOSTART, true,
				MessageFormat.format(STRINGS.getString("AUTOSTART_OPTION_DESCRIPTION"),
						IS_WINDOWS || IS_LINUX ? STRINGS.getString("LOCAL_FEEDER_OR_CLIENT_OR_SERVER")
								: STRINGS.getString("SERVER")));
		OPTIONS.addOption(OPTION_PROFILE, true, STRINGS.getString("PROFILE_OPTION_DESCRIPTION"));
		OPTIONS.addOption(OPTION_HOST, true, STRINGS.getString("HOST_OPTION_DESCRIPTION"));
		OPTIONS.addOption(OPTION_PORT, true, STRINGS.getString("PORT_OPTION_DESCRIPTION"));
		OPTIONS.addOption(OPTION_TIMEOUT, true, STRINGS.getString("TIMEOUT_OPTION_DESCRIPTION"));
		OPTIONS.addOption(OPTION_PASSWORD, true, STRINGS.getString("PASSWORD_OPTION_DESCRIPTION"));
		OPTIONS.addOption(OPTION_GAME_CONTROLLER_DB, true, STRINGS.getString("GAME_CONTROLLER_DB_OPTION_DESCRIPTION"));
		OPTIONS.addOption(OPTION_TRAY, false, STRINGS.getString("TRAY_OPTION_DESCRIPTION"));
		OPTIONS.addOption(OPTION_SAVE, true, STRINGS.getString("SAVE_OPTION_DESCRIPTION"));
		OPTIONS.addOption(OPTION_EXPORT, true, STRINGS.getString("EXPORT_OPTION_DESCRIPTION"));
		OPTIONS.addOption(OPTION_SKIP_MESSAGE_DIALOGS, false,
				STRINGS.getString("SKIP_MESSAGE_DIALOGS_OPTION_DESCRIPTION"));
		OPTIONS.addOption(OPTION_QUIT, false, STRINGS.getString("QUIT_OPTION_DESCRIPTION"));
		OPTIONS.addOption(OPTION_VERSION, false, STRINGS.getString("VERSION_OPTION_DESCRIPTION"));
		OPTIONS.addOption(OPTION_HELP, false, STRINGS.getString("HELP_OPTION_DESCRIPTION"));

		SINGLE_INSTANCE_LOCK_FILE = new File(
				System.getProperty("java.io.tmpdir") + File.separator + Constants.APPLICATION_NAME + ".lock");

		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);

		try {
			UIManager.setLookAndFeel(new FlatLightLaf());
		} catch (final UnsupportedLookAndFeelException e) {
			throw new RuntimeException(e);
		}
	}

	private final AssignmentsComponent assignmentsComponent;

	private final Set<Controller> controllers = new HashSet<>();

	private final JMenu deviceMenu = new JMenu(STRINGS.getString("DEVICE_MENU"));

	private final JFrame frame;

	private final JPanel globalSettingsPanel;

	private final JPanel indicatorsListPanel;

	private final JScrollPane indicatorsScrollPane;

	private final JLabel ledPreviewLabel;

	private final MainLoop mainLoop;

	private final JMenuBar menuBar = new JMenuBar();

	private final JPanel modesListPanel;

	private final JScrollPane modesScrollPane;

	private final JPanel newModePanel;

	private final OnScreenKeyboard onScreenKeyboard;

	private final OpenAction openAction = new OpenAction();

	private final Preferences preferences;

	private final ProfileFileChooser profileFileChooser = new ProfileFileChooser();

	private final JPanel profileSettingsPanel;

	private final QuitAction quitAction = new QuitAction();

	private final Random random;

	private final JMenu runMenu = new JMenu(STRINGS.getString("RUN_MENU"));

	private final boolean sdlVideoInitialized;

	private final StartClientAction startClientAction = new StartClientAction();

	private final StartLocalAction startLocalAction = new StartLocalAction();

	private final StartServerAction startServerAction = new StartServerAction();

	private final JMenuItem startServerMenuItem;

	private final JLabel statusLabel = new JLabel(STRINGS.getString("STATUS_READY"));

	private final StopAction stopAction = new StopAction();

	private final JMenuItem stopMenuItem;

	private final JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);

	private final SVGDocument templateSvgDocument;

	private final Timer timer = new Timer();

	private final JPanel touchpadCursorSensitivityPanel;

	private final JPanel touchpadScrollSensitivityPanel;

	private final Map<VirtualAxis, JProgressBar> virtualAxisToProgressBarMap = new HashMap<>();

	private final JPanel visualizationPanel;

	private Float cachedTouchpadCursorSensitivity;

	private Float cachedTouchpadScrollSensitivity;

	private IAction<?> clipboardAction;

	private File currentFile;

	private JLabel currentModeLabel;

	private boolean hasSystemTray = SystemTray.isSupported();

	private JPanel indicatorPanel;

	private Input input;

	private RunModeType lastRunModeType = RunModeType.NONE;

	private String loadedProfile = null;

	private FlatLaf lookAndFeel;

	private JComboBox<Mode> modeComboBox;

	private ScheduledExecutorService overlayExecutorService;

	private volatile JFrame overlayFrame;

	private FrameDragListener overlayFrameDragListener;

	private Float prevFinger0X;

	private Float prevFinger0Y;

	private Float prevFinger1Y;

	private Rectangle prevTotalDisplayBounds;

	private volatile RunMode runMode;

	private volatile boolean scheduleOnScreenKeyboardModeSwitch;

	private volatile Controller selectedController;

	private volatile long showTrayEntry;

	private JMenuItem startClientMenuItem;

	private long startClientTrayEntry;

	private JMenuItem startLocalMenuItem;

	private long startLocalTrayEntry;

	private long startServerTrayEntry;

	private long stopTrayEntry;

	private JSVGCanvas svgCanvas;

	private volatile Rectangle totalDisplayBounds;

	private volatile long tray;

	private volatile long trayMenu;

	private volatile boolean unsavedChanges = false;

	private JLabel vJoyDirectoryLabel;

	private Main(final MainLoop mainLoop, final String cmdProfilePath, final String cmdGameControllerDbPath) {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (!terminated) {
				LOGGER.log(Level.INFO, "Forcing immediate halt");

				Runtime.getRuntime().halt(2);
			}
		}));

		this.mainLoop = mainLoop;

		try {
			random = SecureRandom.getInstanceStrong();
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		Thread.startVirtualThread(() -> {
			try (final var singleInstanceServerSocket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
				SINGLE_INSTANCE_LOCK_FILE.deleteOnExit();

				final var randomNumber = random.nextInt();

				try {
					Files.writeString(SINGLE_INSTANCE_LOCK_FILE.toPath(),
							singleInstanceServerSocket.getLocalPort() + "\n" + randomNumber, StandardCharsets.UTF_8);
				} catch (final IOException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}

				for (;;) {
					String line;
					String[] arguments = null;
					try (final var socket = singleInstanceServerSocket.accept();
							final var bufferedReader = new BufferedReader(
									new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
						line = bufferedReader.readLine();
						if (!String.valueOf(randomNumber).equals(line)) {
							LOGGER.log(Level.WARNING,
									"Received unexpected value for random number on single instance socket: " + line);
							continue;
						}

						line = bufferedReader.readLine();
						if (SINGLE_INSTANCE_INIT.equals(line)) {
							final var receivedArgs = new ArrayList<String>();

							while (true) {
								try {
									line = bufferedReader.readLine();
									if (SINGLE_INSTANCE_EOF.equals(line)) {
										break;
									}
									receivedArgs.add(line);
								} catch (final IOException e) {
									LOGGER.log(Level.SEVERE, e.getMessage(), e);
								}
							}
							arguments = receivedArgs.toArray(String[]::new);
						} else {
							LOGGER.log(Level.WARNING, "Received unexpected line on single instance socket: " + line);
						}

						if (arguments != null) {
							newActivation(arguments);

							try (final var printStream = new PrintStream(socket.getOutputStream(), false,
									StandardCharsets.UTF_8)) {
								printStream.println(SINGLE_INSTANCE_ACK);
								printStream.flush();
							}
						}
					} catch (final IOException e) {
						LOGGER.log(Level.SEVERE, e.getMessage(), e);
					}
				}
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		});

		final var mainClassPackageName = Main.class.getPackageName();
		final var applicationId = mainClassPackageName.substring(0, mainClassPackageName.lastIndexOf('.'));

		preferences = Preferences.userRoot().node("/" + applicationId.replace('.', '/'));

		frame = new JFrame();

		frame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent e) {
				super.windowClosing(e);

				if (tray == 0L) {
					quit();
					return;
				}

				EventQueue.invokeLater(() -> {
					updateShowTrayEntry();
					showTrayIconHint();
				});
			}

			@Override
			public void windowDeiconified(final WindowEvent e) {
				super.windowDeiconified(e);

				EventQueue.invokeLater(() -> updateShowTrayEntry());
			}

			@Override
			public void windowIconified(final WindowEvent e) {
				super.windowIconified(e);

				EventQueue.invokeLater(() -> updateShowTrayEntry());
			}

			@Override
			public void windowOpened(final WindowEvent e) {
				super.windowOpened(e);

				EventQueue.invokeLater(() -> {
					updateShowTrayEntry();
					updateVisualizationPanel();
				});
			}
		});

		frame.setBounds(DIALOG_BOUNDS_X, DIALOG_BOUNDS_Y, DIALOG_BOUNDS_WIDTH, DIALOG_BOUNDS_HEIGHT);

		final var icons = new ArrayList<Image>();
		for (final var path : ICON_RESOURCE_PATHS) {
			final var icon = new ImageIcon(getResourceLocation(path));
			icons.add(icon.getImage());
		}
		frame.setIconImages(icons);

		frame.setJMenuBar(menuBar);

		final JMenu fileMenu = new JMenu(STRINGS.getString("FILE_MENU"));
		fileMenu.add(new NewAction());
		fileMenu.add(openAction);
		fileMenu.add(new SaveAction());
		fileMenu.add(new SaveAsAction());
		fileMenu.insertSeparator(4);
		fileMenu.add(quitAction);

		menuBar.add(fileMenu);

		if (IS_WINDOWS || IS_LINUX) {
			startLocalMenuItem = runMenu.add(startLocalAction);
			startClientMenuItem = runMenu.add(startClientAction);
		}

		startServerMenuItem = runMenu.add(startServerAction);
		runMenu.addSeparator();
		stopMenuItem = runMenu.add(stopAction);

		menuBar.add(runMenu);

		final var helpMenu = new JMenu(STRINGS.getString("HELP_MENU"));
		menuBar.add(helpMenu);
		helpMenu.add(new ShowLicensesAction());
		helpMenu.add(new ShowWebsiteAction());
		helpMenu.add(new ShowAboutDialogAction());

		frame.getContentPane().add(tabbedPane);

		final JPanel modesPanel = new JPanel(new BorderLayout());
		final JScrollPane globalSettingsScrollPane = new JScrollPane();
		tabbedPane.addTab(STRINGS.getString("MODES_TAB"), modesPanel);

		modesListPanel = new JPanel();
		modesListPanel.setLayout(new GridBagLayout());

		modesScrollPane = new JScrollPane();
		modesPanel.add(modesScrollPane, BorderLayout.CENTER);

		newModePanel = new JPanel(LOWER_BUTTONS_FLOW_LAYOUT);
		final var newModeButton = new JButton(new NewModeAction());
		newModeButton.setPreferredSize(BUTTON_DIMENSION);
		newModePanel.add(newModeButton);
		modesPanel.add(newModePanel, BorderLayout.SOUTH);

		assignmentsComponent = new AssignmentsComponent(this);
		tabbedPane.addTab(STRINGS.getString("ASSIGNMENTS_TAB"), assignmentsComponent);

		final JPanel overlayPanel = new JPanel(new BorderLayout());
		tabbedPane.addTab(STRINGS.getString("OVERLAY_TAB"), overlayPanel);

		indicatorsListPanel = new JPanel();
		indicatorsListPanel.setLayout(new GridBagLayout());

		indicatorsScrollPane = new JScrollPane();
		overlayPanel.add(indicatorsScrollPane, BorderLayout.CENTER);

		final var controllerSvgInputStream = ClassLoader.getSystemResourceAsStream(CONTROLLER_SVG_FILENAME);
		if (controllerSvgInputStream == null) {
			throw new RuntimeException("Resource not found " + CONTROLLER_SVG_FILENAME);
		}

		try (final var bufferedReader = new BufferedReader(
				new InputStreamReader(controllerSvgInputStream, StandardCharsets.UTF_8))) {
			final var svgDocumentFactory = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());
			templateSvgDocument = (SVGDocument) svgDocumentFactory.createDocument(null, bufferedReader);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		visualizationPanel = new JPanel(new BorderLayout());
		tabbedPane.addTab(STRINGS.getString("VISUALIZATION_TAB"), visualizationPanel);

		tabbedPane.addChangeListener(e -> {
			if (e.getSource() != tabbedPane || tabbedPane.getSelectedComponent() != visualizationPanel) {
				return;
			}

			if (svgCanvas == null) {
				svgCanvas = new JSVGCanvas(null, false, false);
				svgCanvas.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
				visualizationPanel.add(svgCanvas, BorderLayout.CENTER);

				updateVisualizationPanel();
			}
		});

		final var exportPanel = new JPanel(LOWER_BUTTONS_FLOW_LAYOUT);
		final var exportButton = new JButton(new ExportAction());
		exportButton.setPreferredSize(BUTTON_DIMENSION);
		exportPanel.add(exportButton);
		visualizationPanel.add(exportPanel, BorderLayout.SOUTH);

		profileSettingsPanel = new JPanel();
		profileSettingsPanel.setLayout(new GridBagLayout());

		final JScrollPane profileSettingsScrollPane = new JScrollPane(profileSettingsPanel);
		tabbedPane.addTab(STRINGS.getString("PROFILE_SETTINGS_TAB"), profileSettingsScrollPane);

		globalSettingsPanel = new JPanel();
		globalSettingsPanel.setLayout(new GridBagLayout());

		globalSettingsScrollPane.setViewportView(globalSettingsPanel);
		tabbedPane.addTab(STRINGS.getString("GLOBAL_SETTINGS_TAB"), null, globalSettingsScrollPane);

		final var constraints = new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0d, 0d,
				GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, GRID_BAG_ITEM_INSETS, 0, 5);

		final var inputSettingsPanel = new JPanel();
		inputSettingsPanel.setLayout(new BoxLayout(inputSettingsPanel, BoxLayout.Y_AXIS));
		inputSettingsPanel
				.setBorder(BorderFactory.createTitledBorder(STRINGS.getString("INPUT_OUTPUT_SETTINGS_BORDER_TITLE")));
		globalSettingsPanel.add(inputSettingsPanel, constraints);

		final var pollIntervalPanel = new JPanel(DEFAULT_FLOW_LAYOUT);
		inputSettingsPanel.add(pollIntervalPanel);

		final var pollIntervalLabel = new JLabel(STRINGS.getString("POLL_INTERVAL_LABEL"));
		pollIntervalLabel.setPreferredSize(SETTINGS_LABEL_DIMENSION);
		pollIntervalPanel.add(pollIntervalLabel);

		final var pollIntervalSpinner = new JSpinner(new SpinnerNumberModel(getPollInterval(), 1, 100, 1));
		final var pollIntervalSpinnerEditor = new NumberEditor(pollIntervalSpinner,
				"# " + STRINGS.getString("MILLISECOND_SYMBOL"));
		((DefaultFormatter) pollIntervalSpinnerEditor.getTextField().getFormatter()).setCommitsOnValidEdit(true);
		pollIntervalSpinner.setEditor(pollIntervalSpinnerEditor);
		pollIntervalSpinner.addChangeListener(event -> preferences.putInt(PREFERENCES_POLL_INTERVAL,
				(int) ((JSpinner) event.getSource()).getValue()));
		pollIntervalPanel.add(pollIntervalSpinner);

		final var physicalAxesPanel = new JPanel(DEFAULT_FLOW_LAYOUT);
		inputSettingsPanel.add(physicalAxesPanel, constraints);

		final var leftPhysicalAxesPanel = new JPanel();
		leftPhysicalAxesPanel.setLayout(new BoxLayout(leftPhysicalAxesPanel, BoxLayout.Y_AXIS));
		physicalAxesPanel.add(leftPhysicalAxesPanel);

		final var physicalAxesPanelLabel = new JLabel(STRINGS.getString("PHYSICAL_AXES_LABEL"));
		physicalAxesPanelLabel.setPreferredSize(SETTINGS_LABEL_DIMENSION);
		leftPhysicalAxesPanel.add(physicalAxesPanelLabel);

		final var rightPhysicalAxesPanel = new JPanel();
		rightPhysicalAxesPanel.setLayout(new BoxLayout(rightPhysicalAxesPanel, BoxLayout.Y_AXIS));
		physicalAxesPanel.add(rightPhysicalAxesPanel);

		final var mapCircularAxesToSquareCheckBox = new JCheckBox(
				STRINGS.getString("MAP_CIRCULAR_AXES_TO_SQUARE_CHECK_BOX"));
		mapCircularAxesToSquareCheckBox.setSelected(isMapCircularAxesToSquareAxes());
		mapCircularAxesToSquareCheckBox.addActionListener(event -> {
			final var mapCircularAxesToSquare = ((JCheckBox) event.getSource()).isSelected();
			preferences.putBoolean(PREFERENCES_MAP_CIRCULAR_AXES_TO_SQUARE, mapCircularAxesToSquare);
		});
		rightPhysicalAxesPanel.add(mapCircularAxesToSquareCheckBox);

		rightPhysicalAxesPanel.add(Box.createVerticalStrut(DEFAULT_VGAP));

		final var swapLeftAndRightSticksCheckBox = new JCheckBox(
				STRINGS.getString("SWAP_LEFT_AND_RIGHT_STICKS_CHECK_BOX"));
		swapLeftAndRightSticksCheckBox.setSelected(isSwapLeftAndRightSticks());
		swapLeftAndRightSticksCheckBox.addActionListener(event -> {
			final var swapLeftAndRightStick = ((JCheckBox) event.getSource()).isSelected();
			preferences.putBoolean(PREFERENCES_SWAP_LEFT_AND_RIGHT_STICKS, swapLeftAndRightStick);
			updateVisualizationPanel();
		});
		rightPhysicalAxesPanel.add(swapLeftAndRightSticksCheckBox);

		leftPhysicalAxesPanel.add(Box.createVerticalStrut(
				rightPhysicalAxesPanel.getPreferredSize().height - physicalAxesPanelLabel.getPreferredSize().height));

		final var hapticFeedbackPanel = new JPanel(DEFAULT_FLOW_LAYOUT);
		inputSettingsPanel.add(hapticFeedbackPanel, constraints);

		final var hapticFeedbackLabel = new JLabel(STRINGS.getString("HAPTIC_FEEDBACK_LABEL"));
		hapticFeedbackLabel.setPreferredSize(SETTINGS_LABEL_DIMENSION);
		hapticFeedbackPanel.add(hapticFeedbackLabel);

		final var hapticFeedbackCheckBox = new JCheckBox(STRINGS.getString("HAPTIC_FEEDBACK_CHECK_BOX"));
		hapticFeedbackCheckBox.setSelected(isHapticFeedback());
		hapticFeedbackCheckBox.addActionListener(event -> {
			final var hapticFeedback = ((JCheckBox) event.getSource()).isSelected();
			preferences.putBoolean(PREFERENCES_HAPTIC_FEEDBACK, hapticFeedback);
		});
		hapticFeedbackPanel.add(hapticFeedbackCheckBox);

		final var hotSwapPanel = new JPanel(DEFAULT_FLOW_LAYOUT);
		inputSettingsPanel.add(hotSwapPanel, constraints);

		final var hotSwappingLabel = new JLabel(STRINGS.getString("HOT_SWAPPING_LABEL"));
		hotSwappingLabel.setPreferredSize(SETTINGS_LABEL_DIMENSION);
		hotSwapPanel.add(hotSwappingLabel);

		final var hotSwappingButtonLabel = new JLabel(STRINGS.getString("HOT_SWAPPING_BUTTON_LABEL"));
		hotSwapPanel.add(hotSwappingButtonLabel);

		final var hotSwapButtonComboBox = new JComboBox<>(HotSwappingButton.values());
		final var selectedHotSwappingButton = HotSwappingButton.fromId(getSelectedHotSwappingButtonId());
		hotSwapButtonComboBox.setSelectedItem(selectedHotSwappingButton);
		hotSwapPanel.add(hotSwapButtonComboBox);
		hotSwapButtonComboBox.setAction(new SetHotSwapButtonAction());

		final var noControllerDialogsPanel = new JPanel(DEFAULT_FLOW_LAYOUT);
		inputSettingsPanel.add(noControllerDialogsPanel, constraints);

		final var noControllerDialogsLabel = new JLabel(STRINGS.getString("SKIP_CONTROLLER_DIALOGS_LABEL"));
		noControllerDialogsLabel.setPreferredSize(SETTINGS_LABEL_DIMENSION);
		noControllerDialogsPanel.add(noControllerDialogsLabel);

		final var noControllerDialogsCheckBox = new JCheckBox(STRINGS.getString("SKIP_CONTROLLER_DIALOGS_CHECK_BOX"));
		noControllerDialogsCheckBox.setSelected(isSkipControllerDialogs());
		noControllerDialogsCheckBox.addActionListener(event -> {
			final var noControllerDialogs = ((JCheckBox) event.getSource()).isSelected();
			preferences.putBoolean(PREFERENCES_SKIP_CONTROLLER_DIALOGS, noControllerDialogs);
		});
		noControllerDialogsPanel.add(noControllerDialogsCheckBox);

		final var autoRestartOutputPanel = new JPanel(DEFAULT_FLOW_LAYOUT);
		inputSettingsPanel.add(autoRestartOutputPanel, constraints);

		final var autoRestartOutputLabel = new JLabel(STRINGS.getString("AUTO_RESTART_OUTPUT_LABEL"));
		autoRestartOutputLabel.setPreferredSize(SETTINGS_LABEL_DIMENSION);
		autoRestartOutputPanel.add(autoRestartOutputLabel);

		final var autoRestartOutputCheckBox = new JCheckBox(STRINGS.getString("AUTO_RESTART_OUTPUT_CHECK_BOX"));
		autoRestartOutputCheckBox.setSelected(isAutoRestartOutput());
		autoRestartOutputCheckBox.addActionListener(event -> {
			final var autoRestartOutput = ((JCheckBox) event.getSource()).isSelected();
			preferences.putBoolean(PREFERENCES_AUTO_RESTART_OUTPUT, autoRestartOutput);
		});
		autoRestartOutputPanel.add(autoRestartOutputCheckBox);

		if (IS_WINDOWS) {
			final var vJoySettingsPanel = new JPanel();
			vJoySettingsPanel.setLayout(new BoxLayout(vJoySettingsPanel, BoxLayout.Y_AXIS));
			vJoySettingsPanel
					.setBorder(BorderFactory.createTitledBorder(STRINGS.getString("VJOY_SETTINGS_BORDER_TITLE")));
			globalSettingsPanel.add(vJoySettingsPanel, constraints);

			final var vJoyDirectoryPanel = new JPanel(DEFAULT_FLOW_LAYOUT);
			vJoySettingsPanel.add(vJoyDirectoryPanel);

			final var vJoyDirectoryLabel = new JLabel(STRINGS.getString("VJOY_DIRECTORY_LABEL"));
			vJoyDirectoryLabel.setPreferredSize(SETTINGS_LABEL_DIMENSION);
			vJoyDirectoryPanel.add(vJoyDirectoryLabel);

			this.vJoyDirectoryLabel = new JLabel(getVJoyDirectory());
			vJoyDirectoryPanel.add(this.vJoyDirectoryLabel);

			final var vJoyDirectoryButton = new JButton(new ChangeVJoyDirectoryAction());
			vJoyDirectoryPanel.add(vJoyDirectoryButton);

			final var vJoyDevicePanel = new JPanel(DEFAULT_FLOW_LAYOUT);
			vJoySettingsPanel.add(vJoyDevicePanel);

			final var vJoyDeviceLabel = new JLabel(STRINGS.getString("VJOY_DEVICE_LABEL"));
			vJoyDeviceLabel.setPreferredSize(SETTINGS_LABEL_DIMENSION);
			vJoyDevicePanel.add(vJoyDeviceLabel);

			final var vJoyDeviceSpinner = new JSpinner(new SpinnerNumberModel(getVJoyDevice(), 1, 16, 1));
			final var vJoyDeviceSpinnerEditor = new NumberEditor(vJoyDeviceSpinner, "#");
			((DefaultFormatter) vJoyDeviceSpinnerEditor.getTextField().getFormatter()).setCommitsOnValidEdit(true);
			vJoyDeviceSpinner.setEditor(vJoyDeviceSpinnerEditor);
			vJoyDeviceSpinner.addChangeListener(event -> preferences.putInt(PREFERENCES_VJOY_DEVICE,
					(int) ((JSpinner) event.getSource()).getValue()));
			vJoyDevicePanel.add(vJoyDeviceSpinner);
		}

		final var appearanceSettingsPanel = new JPanel();
		appearanceSettingsPanel.setLayout(new BoxLayout(appearanceSettingsPanel, BoxLayout.Y_AXIS));
		appearanceSettingsPanel
				.setBorder(BorderFactory.createTitledBorder(STRINGS.getString("APPEARANCE_SETTINGS_BORDER_TITLE")));
		constraints.gridx = 1;
		globalSettingsPanel.add(appearanceSettingsPanel, constraints);

		final var themePanel = new JPanel(DEFAULT_FLOW_LAYOUT);
		appearanceSettingsPanel.add(themePanel);

		final var themeLabel = new JLabel(STRINGS.getString("THEME_LABEL"));
		themeLabel.setPreferredSize(SETTINGS_LABEL_DIMENSION);
		themePanel.add(themeLabel);

		final var themeComboBox = new JComboBox<>(Theme.values());
		themeComboBox.setSelectedItem(getSelectedTheme());
		themePanel.add(themeComboBox);
		themeComboBox.setAction(new SetThemeAction());

		final var overlayScalingPanel = new JPanel(DEFAULT_FLOW_LAYOUT);
		appearanceSettingsPanel.add(overlayScalingPanel);

		final var overlayScalingLabel = new JLabel(STRINGS.getString("OVERLAY_SCALING_LABEL"));
		overlayScalingLabel.setPreferredSize(SETTINGS_LABEL_DIMENSION);
		overlayScalingPanel.add(overlayScalingLabel);

		final var overlayScalingSpinner = new JSpinner(new SpinnerNumberModel(getOverlayScaling(), .5, 6d, .25));
		final var overlayScalingSpinnerEditor = new NumberEditor(overlayScalingSpinner, "#.## x");
		((DefaultFormatter) overlayScalingSpinnerEditor.getTextField().getFormatter()).setCommitsOnValidEdit(true);
		overlayScalingSpinner.setEditor(overlayScalingSpinnerEditor);
		overlayScalingSpinner.addChangeListener(event -> preferences.putFloat(PREFERENCES_OVERLAY_SCALING,
				((Double) ((JSpinner) event.getSource()).getValue()).floatValue()));
		overlayScalingPanel.add(overlayScalingSpinner);

		if (!IS_MAC) {
			final var preventPowerSaveModeSettingsPanel = new JPanel(DEFAULT_FLOW_LAYOUT);
			appearanceSettingsPanel.add(preventPowerSaveModeSettingsPanel, constraints);

			final var preventPowerSaveModeLabel = new JLabel(STRINGS.getString("POWER_SAVE_MODE_LABEL"));
			preventPowerSaveModeLabel.setPreferredSize(SETTINGS_LABEL_DIMENSION);
			preventPowerSaveModeSettingsPanel.add(preventPowerSaveModeLabel);

			final var preventPowerSaveModeCheckBox = new JCheckBox(
					STRINGS.getString("PREVENT_POWER_SAVE_MODE_CHECK_BOX"));
			preventPowerSaveModeCheckBox.setSelected(isPreventPowerSaveMode());
			preventPowerSaveModeCheckBox.addActionListener(event -> {
				final var preventPowerSaveMode = ((JCheckBox) event.getSource()).isSelected();
				preferences.putBoolean(PREFERENCES_PREVENT_POWER_SAVE_MODE, preventPowerSaveMode);
			});
			preventPowerSaveModeSettingsPanel.add(preventPowerSaveModeCheckBox);
		}

		final var ledColorPanel = new JPanel(DEFAULT_FLOW_LAYOUT);
		appearanceSettingsPanel.add(ledColorPanel);

		final var ledColorLabel = new JLabel(STRINGS.getString("LED_COLOR_LABEL"));
		ledColorLabel.setPreferredSize(SETTINGS_LABEL_DIMENSION);
		ledColorPanel.add(ledColorLabel);

		final var ledColorButton = new JButton(new SelectLedColorAction());
		ledColorButton.setPreferredSize(SQUARE_BUTTON_DIMENSION);

		ledPreviewLabel = new JLabel();
		final var ledColorButtonHeight = ledColorButton.getPreferredSize().height;
		ledPreviewLabel.setPreferredSize(new Dimension(ledColorButtonHeight * 3, ledColorButtonHeight));
		ledPreviewLabel.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));
		ledPreviewLabel.setOpaque(true);
		ledPreviewLabel.setBackground(getLedColor());
		ledColorPanel.add(ledPreviewLabel);
		ledColorPanel.add(ledColorButton);

		final var touchpadSettingsPanel = new JPanel();
		touchpadSettingsPanel.setLayout(new BoxLayout(touchpadSettingsPanel, BoxLayout.Y_AXIS));
		touchpadSettingsPanel
				.setBorder(BorderFactory.createTitledBorder(STRINGS.getString("TOUCHPAD_SETTINGS_BORDER_TITLE")));
		globalSettingsPanel.add(touchpadSettingsPanel, constraints);

		final var touchpadPanel = new JPanel(DEFAULT_FLOW_LAYOUT);
		touchpadSettingsPanel.add(touchpadPanel);

		final var enableTouchpadLabel = new JLabel(STRINGS.getString("TOUCHPAD_LABEL"));
		enableTouchpadLabel.setPreferredSize(SETTINGS_LABEL_DIMENSION);
		touchpadPanel.add(enableTouchpadLabel);

		touchpadCursorSensitivityPanel = new JPanel(DEFAULT_FLOW_LAYOUT);
		touchpadSettingsPanel.add(touchpadCursorSensitivityPanel);

		final var touchpadCursorSensitivityLabel = new JLabel(STRINGS.getString("TOUCHPAD_CURSOR_SENSITIVITY"));
		touchpadCursorSensitivityLabel.setPreferredSize(SETTINGS_LABEL_DIMENSION);
		touchpadCursorSensitivityPanel.add(touchpadCursorSensitivityLabel);

		final var cursorSensitivitySpinner = new JSpinner(
				new SpinnerNumberModel(getTouchpadCursorSensitivity(), .1, 5d, .05));
		final var cursorSensitivitySpinnerEditor = new NumberEditor(cursorSensitivitySpinner);
		((DefaultFormatter) cursorSensitivitySpinnerEditor.getTextField().getFormatter()).setCommitsOnValidEdit(true);
		cursorSensitivitySpinner.setEditor(cursorSensitivitySpinnerEditor);
		cursorSensitivitySpinner.addChangeListener(event -> {
			cachedTouchpadCursorSensitivity = ((Double) ((JSpinner) event.getSource()).getValue()).floatValue();
			preferences.putFloat(PREFERENCES_TOUCHPAD_CURSOR_SENSITIVITY, cachedTouchpadCursorSensitivity);
		});
		touchpadCursorSensitivityPanel.add(cursorSensitivitySpinner);

		touchpadScrollSensitivityPanel = new JPanel(DEFAULT_FLOW_LAYOUT);
		touchpadSettingsPanel.add(touchpadScrollSensitivityPanel);

		final var touchpadScrollSensitivityLabel = new JLabel(STRINGS.getString("TOUCHPAD_SCROLL_SENSITIVITY"));
		touchpadScrollSensitivityLabel.setPreferredSize(SETTINGS_LABEL_DIMENSION);
		touchpadScrollSensitivityPanel.add(touchpadScrollSensitivityLabel);

		final var touchpadScrollSensitivitySpinner = new JSpinner(
				new SpinnerNumberModel(getTouchpadScrollSensitivity(), .1, 1d, .05));
		final var scrollSensitivitySpinnerEditor = new NumberEditor(touchpadScrollSensitivitySpinner);
		((DefaultFormatter) scrollSensitivitySpinnerEditor.getTextField().getFormatter()).setCommitsOnValidEdit(true);
		touchpadScrollSensitivitySpinner.setEditor(scrollSensitivitySpinnerEditor);
		touchpadScrollSensitivitySpinner.addChangeListener(event -> {
			cachedTouchpadScrollSensitivity = ((Double) ((JSpinner) event.getSource()).getValue()).floatValue();
			preferences.putFloat(PREFERENCES_TOUCHPAD_SCROLL_SENSITIVITY, cachedTouchpadScrollSensitivity);
		});
		touchpadScrollSensitivityPanel.add(touchpadScrollSensitivitySpinner);

		final var touchpadEnabledCheckBox = new JCheckBox(STRINGS.getString("TOUCHPAD_ENABLED_CHECK_BOX"));
		touchpadEnabledCheckBox.setSelected(isTouchpadEnabled());
		touchpadEnabledCheckBox.addActionListener(event -> {
			final var enableTouchpad = ((JCheckBox) event.getSource()).isSelected();
			preferences.putBoolean(PREFERENCES_TOUCHPAD_ENABLED, enableTouchpad);

			updateTouchpadSettings();
		});
		touchpadPanel.add(touchpadEnabledCheckBox);

		addGlueToSettingsPanel(globalSettingsPanel);

		updateTouchpadSettings();
		updateTitle();

		final var outsideBorder = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		final var insideBorder = BorderFactory.createEmptyBorder(0, 5, 0, 5);
		statusLabel.setBorder(BorderFactory.createCompoundBorder(outsideBorder, insideBorder));
		frame.add(statusLabel, BorderLayout.SOUTH);

		onScreenKeyboard = new OnScreenKeyboard(this);

		if (IS_LINUX) {
			final var toolkit = Toolkit.getDefaultToolkit();
			if (isXToolkit(toolkit)) {
				try {
					@SuppressWarnings({ "Java9ReflectionClassVisibility", "RedundantSuppression" })
					final var unixToolkitClass = Class.forName("sun.awt.UNIXToolkit");
					final var getDesktopMethod = unixToolkitClass.getDeclaredMethod("getDesktop");
					final var desktop = getDesktopMethod.invoke(toolkit);

					if ("gnome".equals(desktop)) {
						hasSystemTray = false;
						try (final var dBusConnection = DBusConnectionBuilder.forSessionBus().build()) {
							final var extensions = dBusConnection.getRemoteObject("org.gnome.Shell.Extensions",
									"/org/gnome/Shell/Extensions", Extensions.class);

							final var gnomeShellVersion = extensions.getShellVersion();
							if (gnomeShellVersion != null) {
								final var matcher = Pattern.compile("(\\d+)\\.(\\d+).*").matcher(gnomeShellVersion);
								if (matcher.matches()) {
									final var majorVersion = Integer.parseInt(matcher.group(1));
									final var minorVersion = Integer.parseInt(matcher.group(2));

									if ((majorVersion == 3 && minorVersion <= 25)) {
										hasSystemTray = true;
									} else if (majorVersion >= 45) {
										final var gnomeSystemTrayExtensions = Set.of(
												"appindicatorsupport@rgcjonas.gmail.com",
												"status-icons@gnome-shell-extensions.gcampax.github.com",
												"ubuntu-appindicators@ubuntu.com");

										hasSystemTray = extensions.ListExtensions().entrySet().stream()
												.anyMatch(e -> gnomeSystemTrayExtensions.contains(e.getKey())
														&& Boolean.TRUE.equals(e.getValue().get("enabled").getValue()));
									}
								}
							}
						}
					}
				} catch (final Throwable t) {
					LOGGER.log(Level.SEVERE, t.getMessage(), t);
				}
			}
		}

		newProfile(false);

		final var sdlInitialized = mainLoop.runSync(() -> {
			SDLInit.SDL_SetAppMetadata(Constants.APPLICATION_NAME, Constants.VERSION, applicationId);

			SDLHints.SDL_SetHint(SDLHints.SDL_HINT_JOYSTICK_ALLOW_BACKGROUND_EVENTS, "1");
			SDLHints.SDL_SetHint(SDLHints.SDL_HINT_GAMECONTROLLER_IGNORE_DEVICES, VJOY_DEVICE_VID_PID);

			SDLHints.SDL_SetHint(SDLHints.SDL_HINT_VIDEO_ALLOW_SCREENSAVER, "1");
			if (IS_LINUX) {
				SDLHints.SDL_SetHint(SDLHints.SDL_HINT_VIDEO_DRIVER, "x11");
			}

			var flags = SDLInit.SDL_INIT_GAMEPAD;
			if (!IS_MAC) {
				flags |= SDLInit.SDL_INIT_VIDEO;
			}
			return SDLInit.SDL_Init(flags);
		}).orElse(false);

		sdlVideoInitialized = SDLInit.SDL_WasInit(SDLInit.SDL_INIT_VIDEO) != 0;
		hasSystemTray &= sdlVideoInitialized;

		updateTheme();

		if (!sdlInitialized) {
			var errorDetails = SDLError.SDL_GetError();

			final var hasErrorDetails = errorDetails != null && !errorDetails.isBlank();
			LOGGER.log(Level.SEVERE, "Could not initialize SDL" + (hasErrorDetails ? ": " + errorDetails : ""));

			if (!hasErrorDetails) {
				errorDetails = STRINGS.getString("NO_ERROR_DETAILS");
			}

			if (IS_WINDOWS || IS_LINUX) {
				GuiUtils.showMessageDialog(this, frame,
						MessageFormat.format(STRINGS.getString("COULD_NOT_INITIALIZE_SDL_DIALOG_TEXT"),
								Constants.APPLICATION_NAME, errorDetails),
						STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
			} else {
				GuiUtils.showMessageDialog(this, frame, MessageFormat
						.format(STRINGS.getString("COULD_NOT_INITIALIZE_SDL_DIALOG_TEXT_MAC"), errorDetails),
						STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				quit();
			}

			onControllersChanged(true);
			initProfile(cmdProfilePath, true);

			return;
		}

		String errorDetails;
		try {
			final var inputStream = ClassLoader.getSystemResourceAsStream(GAME_CONTROLLER_DATABASE_FILENAME);
			if (inputStream == null) {
				throw new IllegalStateException("Could not open resource " + GAME_CONTROLLER_DATABASE_FILENAME);
			}
			final var tempFilePath = Files.createTempFile(GAME_CONTROLLER_DATABASE_FILENAME, null);
			try {
				Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
				errorDetails = updateGameControllerMappings(tempFilePath.toString(),
						"internal file " + GAME_CONTROLLER_DATABASE_FILENAME);
			} finally {
				Files.delete(tempFilePath);
			}
		} catch (final Throwable t) {
			LOGGER.log(Level.SEVERE, t.getMessage(), t);
			errorDetails = STRINGS.getString("NO_ERROR_DETAILS");
		}

		if (errorDetails != null && !errorDetails.isBlank()) {
			GuiUtils.showMessageDialog(this, frame,
					MessageFormat.format(
							STRINGS.getString("ERROR_UPDATING_GAME_CONTROLLER_DB_FROM_INTERNAL_FILE_DIALOG_TEXT"),
							Constants.APPLICATION_NAME, errorDetails),
					STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
		}

		if (cmdGameControllerDbPath != null) {
			updateGameControllerMappingsFromFile(cmdGameControllerDbPath);
		}

		controllers.clear();
		mainLoop.runSync(() -> {
			final var instanceIdBuffer = SDLGamepad.SDL_GetGamepads();
			if (instanceIdBuffer == null) {
				return;
			}

			while (instanceIdBuffer.hasRemaining()) {
				controllers.add(new Controller(instanceIdBuffer.get()));
			}
		});

		if (!controllers.isEmpty()) {
			LOGGER.log(Level.INFO,
					"Present controllers:" + controllers.stream()
							.map(controller -> assembleControllerLoggingMessage("\n\t", controller))
							.collect(Collectors.joining()));

			final var lastControllerGuid = preferences.get(PREFERENCES_LAST_CONTROLLER, null);
			if (lastControllerGuid != null) {
				controllers.stream().filter(controller -> lastControllerGuid.equals(controller.guid)).findFirst()
						.ifPresentOrElse(controller -> {
							LOGGER.log(Level.INFO,
									assembleControllerLoggingMessage("Found previously used controller ", controller));
							setSelectedController(controller);
						}, () -> {
							LOGGER.log(Level.INFO, "Previously used controller is not present");
							setSelectedController(controllers.stream().findFirst().orElse(null));
						});
			}
		}

		onControllersChanged(true);

		final var noControllerConnected = controllers.isEmpty();

		if (noControllerConnected && !isSkipControllerDialogs()) {
			if (IS_WINDOWS || IS_LINUX) {
				GuiUtils.showMessageDialog(this, frame,
						MessageFormat.format(STRINGS.getString("NO_CONTROLLER_CONNECTED_DIALOG_TEXT"),
								Constants.APPLICATION_NAME),
						STRINGS.getString("INFORMATION_DIALOG_TITLE"), JOptionPane.INFORMATION_MESSAGE);
			} else {
				GuiUtils.showMessageDialog(this, frame, STRINGS.getString("NO_CONTROLLER_CONNECTED_DIALOG_TEXT_MAC"),
						STRINGS.getString("INFORMATION_DIALOG_TITLE"), JOptionPane.INFORMATION_MESSAGE);
			}
		}

		initProfile(cmdProfilePath, noControllerConnected);
	}

	private static void addGlueToSettingsPanel(final JPanel settingsPanel) {
		final var constraints = new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1d, 1d,
				GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);

		settingsPanel.add(Box.createGlue(), constraints);

		constraints.gridx = 1;
		settingsPanel.add(Box.createGlue(), constraints);
	}

	public static String assembleControllerLoggingMessage(final String prefix, final Controller controller) {
		final var sb = new StringBuilder();
		sb.append(prefix);

		final var appendGamepadName = controller.name != null;

		if (appendGamepadName) {
			sb.append(controller.name).append(" (");
		}

		sb.append(controller.instanceId);

		if (appendGamepadName) {
			sb.append(")");
		}

		if (controller.guid != null) {
			sb.append(" [").append(controller.guid).append("]");
		}

		return sb.toString();
	}

	private static void checkMainThread() {
		if (!SDLInit.SDL_IsMainThread()) {
			throw new RuntimeException();
		}
	}

	private static LineBorder createOverlayBorder() {
		return new LineBorder(UIManager.getColor("Component.borderColor"), 1);
	}

	private static void deleteSingleInstanceLockFile() {
		if (!SINGLE_INSTANCE_LOCK_FILE.delete()) {
			LOGGER.log(Level.WARNING,
					"Could not delete single instance lock file " + SINGLE_INSTANCE_LOCK_FILE.getAbsolutePath());
		}
	}

	private static String getDefaultVJoyPath() {
		return System.getenv("ProgramFiles") + File.separator + "vJoy";
	}

	private static int getExtendedKeyCodeForMenu(final AbstractButton button,
			final Set<Integer> alreadyAssignedKeyCodes) {
		var keyCode = KeyEvent.VK_UNDEFINED;

		final var text = button.getText();
		if (text != null && !text.isEmpty()) {
			var index = 0;
			do {
				keyCode = KeyEvent.getExtendedKeyCodeForChar(text.charAt(index));
				index++;
			} while (keyCode != KeyEvent.VK_UNDEFINED && !alreadyAssignedKeyCodes.add(keyCode));
		}

		return keyCode;
	}

	private static int getExtendedKeyCodeForMenuItem(final AbstractButton button) {
		final var action = button.getAction();

		if (action == null) {
			return KeyEvent.VK_UNDEFINED;
		}

		return switch (action) {
		case final NewAction _ -> KeyEvent.VK_N;
		case final OpenAction _ -> KeyEvent.VK_O;
		case final SaveAction _ -> KeyEvent.VK_S;
		case final StartLocalAction _ -> KeyEvent.VK_L;
		case final StartClientAction _ -> KeyEvent.VK_C;
		case final StartServerAction _ -> KeyEvent.VK_E;
		case final StopAction _ -> KeyEvent.VK_T;
		default -> KeyEvent.VK_UNDEFINED;
		};
	}

	private static URL getResourceLocation(final String resourcePath) {
		final var resourceLocation = Main.class.getResource(resourcePath);
		if (resourceLocation == null) {
			throw new RuntimeException("Resource not found " + resourcePath);
		}

		return resourceLocation;
	}

	private static long insertTrayEntryFromAction(final long menu, final Action action) {
		checkMainThread();

		final var label = (String) action.getValue(Action.NAME);
		final var trayEntry = SDLTray.SDL_InsertTrayEntryAt(menu, -1, label, SDLTray.SDL_TRAYENTRY_BUTTON);

		SDLTray.SDL_SetTrayEntryCallback(trayEntry,
				(_, _) -> EventQueue.invokeLater(
						() -> action.actionPerformed(new ActionEvent(trayEntry, ActionEvent.ACTION_PERFORMED, label))),
				0L);

		return trayEntry;
	}

	private static boolean isModalDialogShowing() {
		final var windows = Window.getWindows();
		if (windows != null) {
			for (final Window w : windows) {
				if (w.isShowing() && w instanceof final Dialog dialog && dialog.isModal()) {
					return true;
				}
			}
		}

		return false;
	}

	private static boolean isValidHost(final String host) {
		return host != null && !host.isBlank();
	}

	private static boolean isValidPassword(final String password) {
		if (password == null) {
			return false;
		}

		final var length = password.length();
		return !password.isBlank() && length >= 6 && length <= 24;
	}

	private static boolean isXToolkit(final Toolkit toolkit) {
		return "sun.awt.X11.XToolkit".equals(toolkit.getClass().getName());
	}

	public static String logSdlError(final String message) {
		final var errorDetails = SDLError.SDL_GetError();
		LOGGER.log(Level.WARNING,
				message + (errorDetails != null && !errorDetails.isBlank() ? ": " + errorDetails : ""));

		return errorDetails;
	}

	public static void main(final String[] args) {
		LOGGER.log(Level.INFO, "Launching " + Constants.APPLICATION_NAME + " " + Constants.VERSION);
		LOGGER.log(Level.INFO, "Operating System: " + System.getProperty("os.name") + " "
				+ System.getProperty("os.version") + " " + OS_ARCH);

		Thread.setDefaultUncaughtExceptionHandler((_, e) -> {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);

			if (!GraphicsEnvironment.isHeadless() && main != null && main.frame != null) {
				GuiUtils.invokeOnEventDispatchThreadIfRequired(() -> {
					final var sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));

					final var panel = new JPanel();
					panel.setLayout(new BorderLayout(5, 5));
					panel.add(new JLabel(MessageFormat.format(STRINGS.getString("UNCAUGHT_EXCEPTION_DIALOG_TEXT"),
							Constants.APPLICATION_NAME)), BorderLayout.NORTH);
					final var textArea = new JTextArea(sw.toString());
					textArea.setEditable(false);
					final var scrollPane = new JScrollPane(textArea);
					scrollPane.setPreferredSize(new Dimension(600, 400));
					panel.add(scrollPane, BorderLayout.CENTER);
					GuiUtils.showMessageDialog(main, main.frame, panel, STRINGS.getString("ERROR_DIALOG_TITLE"),
							JOptionPane.ERROR_MESSAGE);

					if (main.unsavedChanges) {
						try {
							final var filename = main.currentFile != null ? main.currentFile.getName()
									: "unnamed_profile";
							final var file = new File(System.getProperty("java.io.tmpdir") + File.separator + filename);

							if (!file.exists()) {
								main.saveProfile(file, false);
							}
						} catch (final Throwable t) {
							LOGGER.log(Level.SEVERE, t.getMessage(), t);
						}
					}

					terminate(1, main);
				});
			} else {
				terminate(1, main);
			}
		});

		try {
			final var commandLine = new DefaultParser().parse(OPTIONS, args);
			if (commandLine.hasOption(OPTION_VERSION)) {
				printCommandLineMessage(Constants.APPLICATION_NAME + " " + Constants.VERSION);

				return;
			}
			if (!commandLine.hasOption(OPTION_HELP)) {
				var continueLaunch = true;

				if (SINGLE_INSTANCE_LOCK_FILE.exists()) {
					try (final var fileBufferedReader = new BufferedReader(
							new FileReader(SINGLE_INSTANCE_LOCK_FILE, StandardCharsets.UTF_8))) {
						final var portString = fileBufferedReader.readLine();
						if (portString == null) {
							throw new IOException("Could not read port");
						}
						final var port = Integer.parseInt(portString);

						final var randomNumberString = fileBufferedReader.readLine();
						if (randomNumberString == null) {
							throw new IOException("Could not read random number");
						}

						try (final var socket = new Socket(InetAddress.getLoopbackAddress(), port);
								final var printStream = new PrintStream(socket.getOutputStream(), false,
										StandardCharsets.UTF_8);
								final var socketBufferedReader = new BufferedReader(
										new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
							socket.setSoTimeout(5000);
							printStream.println(randomNumberString);
							printStream.println(SINGLE_INSTANCE_INIT);

							for (final String arg : args) {
								printStream.println(arg);
							}

							printStream.println(SINGLE_INSTANCE_EOF);
							printStream.flush();

							for (var i = 0; i < 5; i++) {
								final var str = socketBufferedReader.readLine();

								if (str == null) {
									break;
								}

								if (SINGLE_INSTANCE_ACK.equals(str)) {
									continueLaunch = false;
									break;
								}
							}

							if (continueLaunch) {
								LOGGER.log(Level.WARNING, "Other " + Constants.APPLICATION_NAME
										+ " instance did not acknowledge invocation");
							}
						}
					} catch (final IOException | NumberFormatException e) {
						LOGGER.log(Level.WARNING, e.getMessage(), e);
						deleteSingleInstanceLockFile();
					}
				}

				if (continueLaunch) {
					final var taskRunner = new MainLoop();

					EventQueue.invokeLater(() -> {
						skipMessageDialogs = commandLine.hasOption(OPTION_SKIP_MESSAGE_DIALOGS);

						final var cmdProfilePath = commandLine.getOptionValue(OPTION_PROFILE);
						final var cmdGameControllerDbPath = commandLine.getOptionValue(OPTION_GAME_CONTROLLER_DB);

						main = new Main(taskRunner, cmdProfilePath, cmdGameControllerDbPath);

						EventQueue.invokeLater(() -> main.handleRemainingCommandLine(commandLine));

						taskRunner.startSdlEventPolling();
					});

					taskRunner.enterLoop();
				} else {
					LOGGER.log(Level.INFO, "Another " + Constants.APPLICATION_NAME + " instance is already running");
					terminate(0, null);
				}

				return;
			}
		} catch (final ParseException _) {
			// handled below
		}

		final var stringWriter = new StringWriter();
		try (final var printWriter = new PrintWriter(stringWriter)) {
			final var helpFormatter = new HelpFormatter();
			helpFormatter.printHelp(printWriter, helpFormatter.getWidth(), Constants.APPLICATION_NAME, null, OPTIONS,
					helpFormatter.getLeftPadding(), helpFormatter.getDescPadding(), null, true);
			printWriter.flush();
		}
		printCommandLineMessage(stringWriter.toString());
	}

	private static void openBrowser(final Component parentComponent, final URI uri) {
		if (Desktop.isDesktopSupported()) {
			final var desktop = Desktop.getDesktop();

			if (desktop.isSupported(Desktop.Action.BROWSE)) {
				try {
					desktop.browse(uri);
					return;
				} catch (final Throwable t) {
					LOGGER.log(Level.WARNING, t.getMessage(), t);
				}
			}
		}

		if (SDLMisc.SDL_OpenURL(uri.toString())) {
			return;
		}

		showPleaseVisitDialog(parentComponent, uri);
	}

	private static void printCommandLineMessage(final String message) {
		System.out.println(message);

		if (!GraphicsEnvironment.isHeadless()) {
			EventQueue.invokeLater(() -> {
				final var textArea = new JTextArea(message);
				textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
				textArea.setEditable(false);

				final var imageIcon = new ImageIcon(getResourceLocation(ICON_RESOURCE_PATHS[2]));
				GuiUtils.showMessageDialog(null, null, textArea, Constants.APPLICATION_NAME,
						JOptionPane.INFORMATION_MESSAGE, imageIcon);
			});
		}
	}

	private static void showPleaseVisitDialog(final Component parentComponent, final URI uri) {
		JOptionPane.showMessageDialog(parentComponent,
				MessageFormat.format(STRINGS.getString("PLEASE_VISIT_DIALOG_TEXT"), uri),
				STRINGS.getString("INFORMATION_DIALOG_TITLE"), JOptionPane.INFORMATION_MESSAGE);
	}

	private static void terminate(final int status, final Main main) {
		if (main != null && main.mainLoop != null) {
			if (main.trayMenu != 0L && main.mainLoop.isAvailable()) {
				try {
					main.mainLoop.runSync(() -> SDLTray.SDL_DestroyTray(main.tray));
				} catch (final IllegalStateException _) {
					// ignore main loop is no longer able to process tasks
				}
			}

			main.mainLoop.shutdown();
		}

		LOGGER.log(Level.INFO, "Terminated (" + status + ")");

		terminated = true;

		System.exit(status);
	}

	private void addTSpanElement(final List<? extends IAction<?>> actions, final Node parentNode) {
		addTSpanElement(actions.stream().map(action -> {
			var description = action.getDescription(input);

			if (action instanceof final ButtonToModeAction buttonToModeAction) {
				description = (buttonToModeAction.isToggle() ? "⇪" : "⇧") + " " + description;
			}

			return description;
		}).distinct().collect(Collectors.joining(", ")), false, parentNode);
	}

	private void addTSpanElement(final String textContent, final boolean bold, final Node parentNode) {
		final var prefixTSpanElement = parentNode.getOwnerDocument().createElementNS(SVGConstants.SVG_NAMESPACE_URI,
				SVGConstants.SVG_TSPAN_TAG);

		if (bold) {
			prefixTSpanElement.setAttribute("style", "font-weight: bold;");
		}

		prefixTSpanElement.setTextContent(textContent);

		parentNode.appendChild(prefixTSpanElement);
	}

	private void deInitOverlay() {
		if (overlayFrame != null) {
			for (var i = 0; i < 10; i++) {
				overlayFrame.dispose();

				if (!overlayFrame.isDisplayable()) {
					break;
				}

				try {
					Thread.sleep(100L);
				} catch (final InterruptedException _) {
					Thread.currentThread().interrupt();
				}
			}

			overlayFrame = null;
		}

		currentModeLabel = null;
		virtualAxisToProgressBarMap.clear();
	}

	private void deInitOverlayAndHideOnScreenKeyboard() {
		deInitOverlay();
		onScreenKeyboard.setVisible(false);
	}

	public <T> T executeWhileVisible(final Callable<T> callable) {
		final var wasInvisible = show();

		try {
			final var result = callable.call();

			if (wasInvisible) {
				frame.setVisible(false);
				updateShowTrayEntry();
			}

			return result;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void executeWhileVisible(final Runnable runnable) {
		executeWhileVisible(() -> {
			runnable.run();

			return null;
		});
	}

	public void exportVisualization(final File file) {
		if (templateSvgDocument == null) {
			return;
		}

		try {
			final var domImplementation = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder()
					.getDOMImplementation();
			final var htmlDocumentType = domImplementation.createDocumentType("html", "-//W3C//DTD XHTML 1.1//EN",
					"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd");
			final var htmlDocument = domImplementation.createDocument(XMLConstants.XLINK_NAMESPACE_URI, "html",
					htmlDocumentType);

			final var headElement = htmlDocument.createElementNS(XMLConstants.XLINK_NAMESPACE_URI, "head");
			htmlDocument.getDocumentElement().appendChild(headElement);

			final var colorSchemeMetaElement = htmlDocument.createElementNS(XMLConstants.XLINK_NAMESPACE_URI, "meta");
			colorSchemeMetaElement.setAttribute("name", "color-scheme");
			colorSchemeMetaElement.setAttribute("content", "light dark");
			headElement.appendChild(colorSchemeMetaElement);

			final var darkColorSchemeStyleElement = htmlDocument.createElementNS(XMLConstants.XLINK_NAMESPACE_URI,
					"style");
			darkColorSchemeStyleElement
					.setTextContent("@media(prefers-color-scheme:dark){body{background-color:#969696}}");
			headElement.appendChild(darkColorSchemeStyleElement);

			final var svgDivStyleElement = htmlDocument.createElementNS(XMLConstants.XLINK_NAMESPACE_URI, "style");
			svgDivStyleElement.setTextContent(".svg-div{aspect-ratio:2.5;margin-top:50px}");
			headElement.appendChild(svgDivStyleElement);

			final var titleElement = htmlDocument.createElementNS(XMLConstants.XLINK_NAMESPACE_URI, "title");
			final var title = currentFile != null ? currentFile.getName() : STRINGS.getString("UNTITLED");
			titleElement.setTextContent(title);
			headElement.appendChild(titleElement);

			final var bodyElement = htmlDocument.createElementNS(XMLConstants.XLINK_NAMESPACE_URI, "body");
			bodyElement.setAttribute("style", "text-align:center");
			htmlDocument.getDocumentElement().appendChild(bodyElement);

			final var profileHeaderElement = htmlDocument.createElementNS(XMLConstants.XLINK_NAMESPACE_URI, "h1");
			profileHeaderElement.setTextContent(title);
			bodyElement.appendChild(profileHeaderElement);

			final var labelElement = htmlDocument.createElementNS(XMLConstants.XLINK_NAMESPACE_URI, "label");
			labelElement.setTextContent("Mode: ");
			labelElement.setAttribute("style", "font-size:1.17em;font-weight:bold");
			bodyElement.appendChild(labelElement);

			final var selectElement = htmlDocument.createElementNS(XMLConstants.XLINK_NAMESPACE_URI, "select");
			selectElement.setAttribute("onchange",
					"Array.from(document.getElementsByClassName('svg-div')).forEach(e=>e.style.display=(e.id===this.value?'block':'none'))");
			selectElement.setAttribute("style", "vertical-align:text-bottom");
			labelElement.appendChild(selectElement);

			input.getProfile().getModes().forEach(mode -> {
				final var svgDivElement = htmlDocument.createElementNS(XMLConstants.XLINK_NAMESPACE_URI, "div");
				final var svgDivElementId = mode.getUuid().toString();

				svgDivElement.setAttribute("id", svgDivElementId);
				svgDivElement.setAttribute("class", "svg-div");
				svgDivElement.setAttribute("style",
						"display:" + (Profile.DEFAULT_MODE.equals(mode) ? "block" : "none"));
				bodyElement.appendChild(svgDivElement);

				final var svgDocument = generateSvgDocument(mode, false);
				final var importedSvgNode = htmlDocument.importNode(svgDocument.getRootElement(), true);
				svgDivElement.appendChild(importedSvgNode);

				final var optionElement = htmlDocument.createElementNS(XMLConstants.XLINK_NAMESPACE_URI, "option");
				optionElement.setAttribute("value", svgDivElementId);
				optionElement.setTextContent(mode.getDescription());
				selectElement.appendChild(optionElement);
			});

			final var transformerFactory = TransformerFactory.newInstance();
			final var transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, htmlDocumentType.getPublicId());
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, htmlDocumentType.getSystemId());

			try (final var fileOutputStream = new FileOutputStream(file)) {
				transformer.transform(new DOMSource(htmlDocument), new StreamResult(fileOutputStream));
				LOGGER.log(Level.INFO, "Exported visualization of profile " + title + " to: " + file.getAbsolutePath());
			}
		} catch (final DOMException | ParserConfigurationException | TransformerException | IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			GuiUtils.showMessageDialog(main, frame, STRINGS.getString("COULD_NOT_EXPORT_VISUALIZATION_DIALOG_TEXT"),
					STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
		}
	}

	private SVGDocument generateSvgDocument(final Mode mode, final boolean darkTheme) {
		Objects.requireNonNull(templateSvgDocument, "Field templateSvgDocument must not be null");

		final var workingCopySvgDocument = (SVGDocument) DOMUtilities.deepCloneDocument(templateSvgDocument,
				templateSvgDocument.getImplementation());

		final var userAgentAdapter = new UserAgentAdapter();
		final var documentLoader = new DocumentLoader(userAgentAdapter);
		final var bridgeContext = new BridgeContext(userAgentAdapter, documentLoader);
		bridgeContext.setDynamicState(BridgeContext.DYNAMIC);
		new GVTBuilder().build(bridgeContext, workingCopySvgDocument);

		final var swapLeftAndRightSticks = isSwapLeftAndRightSticks();

		for (var axis = 0; axis < SDLGamepad.SDL_GAMEPAD_AXIS_COUNT; axis++) {
			var swapped = false;

			final var idPrefix = switch (axis) {
			case SDLGamepad.SDL_GAMEPAD_AXIS_LEFT_TRIGGER -> SVG_ID_LEFT_TRIGGER;
			case SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX -> {
				swapped = swapLeftAndRightSticks;
				yield swapLeftAndRightSticks ? SVG_ID_RIGHT_X : SVG_ID_LEFT_X;
			}
			case SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY -> {
				swapped = swapLeftAndRightSticks;
				yield swapLeftAndRightSticks ? SVG_ID_RIGHT_Y : SVG_ID_LEFT_Y;
			}
			case SDLGamepad.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER -> SVG_ID_RIGHT_TRIGGER;
			case SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX -> {
				swapped = swapLeftAndRightSticks;
				yield swapLeftAndRightSticks ? SVG_ID_LEFT_X : SVG_ID_RIGHT_X;
			}
			case SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY -> {
				swapped = swapLeftAndRightSticks;
				yield swapLeftAndRightSticks ? SVG_ID_LEFT_Y : SVG_ID_RIGHT_Y;
			}
			default -> null;
			};

			final var actions = mode.getAxisToActionsMap().get(axis);
			updateSvgElements(workingCopySvgDocument, idPrefix, actions, darkTheme, swapped);
		}

		for (var button = 0; button <= SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT; button++) {
			var swapped = false;

			final var idPrefix = switch (button) {
			case SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH -> SVG_ID_A;
			case SDLGamepad.SDL_GAMEPAD_BUTTON_EAST -> SVG_ID_B;
			case SDLGamepad.SDL_GAMEPAD_BUTTON_BACK -> SVG_ID_BACK;
			case SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_DOWN -> SVG_ID_DPAD_DOWN;
			case SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_LEFT -> SVG_ID_DPAD_LEFT;
			case SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT -> SVG_ID_DPAD_RIGHT;
			case SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_UP -> SVG_ID_DPAD_UP;
			case SDLGamepad.SDL_GAMEPAD_BUTTON_GUIDE -> SVG_ID_GUIDE;
			case SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER -> SVG_ID_LEFT_SHOULDER;
			case SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK -> {
				swapped = swapLeftAndRightSticks;
				yield swapLeftAndRightSticks ? SVG_ID_RIGHT_STICK : SVG_ID_LEFT_STICK;
			}
			case SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER -> SVG_ID_RIGHT_SHOULDER;
			case SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK -> {
				swapped = swapLeftAndRightSticks;
				yield swapLeftAndRightSticks ? SVG_ID_LEFT_STICK : SVG_ID_RIGHT_STICK;
			}
			case SDLGamepad.SDL_GAMEPAD_BUTTON_START -> SVG_ID_START;
			case SDLGamepad.SDL_GAMEPAD_BUTTON_WEST -> SVG_ID_X;
			case SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH -> SVG_ID_Y;
			default -> null;
			};

			final var combinedActions = new ArrayList<IAction<Boolean>>();

			final var normalActions = mode.getButtonToActionsMap().get(button);
			if (normalActions != null) {
				combinedActions.addAll(normalActions);
			}

			if (Profile.DEFAULT_MODE.equals(mode)) {
				final var modeActions = input.getProfile().getButtonToModeActionsMap().get(button);
				if (modeActions != null) {
					combinedActions.addAll(modeActions);
				}
			}

			updateSvgElements(workingCopySvgDocument, idPrefix, combinedActions, darkTheme, swapped);
		}

		return workingCopySvgDocument;
	}

	IAction<?> getClipboardAction() {
		return clipboardAction;
	}

	public Set<Controller> getControllers() {
		return controllers;
	}

	@SuppressWarnings("exports")
	public JFrame getFrame() {
		return frame;
	}

	public String getHost() {
		return preferences.get(PREFERENCES_HOST, "");
	}

	Input getInput() {
		return input;
	}

	public Color getLedColor() {
		return new Color(preferences.getInt(PREFERENCES_LED_COLOR, 0x448ADE));
	}

	public MainLoop getMainLoop() {
		return mainLoop;
	}

	public OnScreenKeyboard getOnScreenKeyboard() {
		return onScreenKeyboard;
	}

	float getOverlayScaling() {
		return preferences.getFloat(PREFERENCES_OVERLAY_SCALING, DEFAULT_OVERLAY_SCALING);
	}

	public String getPassword() {
		return preferences.get(PREFERENCES_PASSWORD, "");
	}

	public int getPollInterval() {
		return preferences.getInt(PREFERENCES_POLL_INTERVAL, RunMode.DEFAULT_POLL_INTERVAL);
	}

	public int getPort() {
		return preferences.getInt(PREFERENCES_PORT, ServerRunMode.DEFAULT_PORT);
	}

	@SuppressWarnings("exports")
	public Preferences getPreferences() {
		return preferences;
	}

	public Random getRandom() {
		return random;
	}

	public int getSelectedHotSwappingButtonId() {
		return Math.min(Math.max(preferences.getInt(PREFERENCES_HOT_SWAPPING_BUTTON, HotSwappingButton.NONE.id),
				HotSwappingButton.NONE.id), SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT);
	}

	private Theme getSelectedTheme() {
		return Theme.fromId(preferences.getInt(PREFERENCES_THEME, Theme.SYSTEM.id));
	}

	public int getTimeout() {
		return preferences.getInt(PREFERENCES_TIMEOUT, ServerRunMode.DEFAULT_TIMEOUT);
	}

	public float getTouchpadCursorSensitivity() {
		if (cachedTouchpadCursorSensitivity == null) {
			cachedTouchpadCursorSensitivity = preferences.getFloat(PREFERENCES_TOUCHPAD_CURSOR_SENSITIVITY, 1f);
		}

		return cachedTouchpadCursorSensitivity;
	}

	public float getTouchpadScrollSensitivity() {
		if (cachedTouchpadScrollSensitivity == null) {
			cachedTouchpadScrollSensitivity = preferences.getFloat(PREFERENCES_TOUCHPAD_SCROLL_SENSITIVITY, 1f);
		}

		return cachedTouchpadScrollSensitivity;
	}

	public int getVJoyDevice() {
		return preferences.getInt(PREFERENCES_VJOY_DEVICE, OutputRunMode.VJOY_DEFAULT_DEVICE);
	}

	public String getVJoyDirectory() {
		return preferences.get(PREFERENCES_VJOY_DIRECTORY, getDefaultVJoyPath());
	}

	private boolean handleNetworkCommandLineOptions(final CommandLine commandLine) {
		var valid = true;

		var host = commandLine.getOptionValue(OPTION_HOST);
		if (host != null) {
			host = host.strip();
			if (!isValidHost(host)) {
				valid = false;
				GuiUtils.showMessageDialog(this, frame,
						MessageFormat.format(
								STRINGS.getString("INVALID_VALUE_FOR_COMMAND_LINE_OPTION_HOST_DIALOG_TEXT"),
								OPTION_HOST, host),
						STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
			}
			preferences.put(PREFERENCES_HOST, host);
		}

		final var port = commandLine.getOptionValue(OPTION_PORT);
		if (port != null) {
			try {
				preferences.putInt(PREFERENCES_PORT, Integer.parseInt(port));
			} catch (final NumberFormatException _) {
				valid = false;
				GuiUtils.showMessageDialog(this, frame,
						MessageFormat.format(
								STRINGS.getString("INVALID_VALUE_FOR_INTEGER_COMMAND_LINE_OPTION_DIALOG_TEXT"),
								OPTION_PORT, port),
						STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
			}
		}

		final var timeout = commandLine.getOptionValue(OPTION_TIMEOUT);
		if (timeout != null) {
			try {
				preferences.putInt(PREFERENCES_TIMEOUT, Integer.parseInt(timeout));
			} catch (final NumberFormatException _) {
				valid = false;
				GuiUtils.showMessageDialog(this, frame,
						MessageFormat.format(
								STRINGS.getString("INVALID_VALUE_FOR_INTEGER_COMMAND_LINE_OPTION_DIALOG_TEXT"),
								OPTION_TIMEOUT, timeout),
						STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
			}
		}

		final var password = commandLine.getOptionValue(OPTION_PASSWORD);
		if (password != null) {
			if (!isValidPassword(password)) {
				valid = false;
				GuiUtils.showMessageDialog(this, frame,
						MessageFormat.format(
								STRINGS.getString("INVALID_VALUE_FOR_COMMAND_LINE_OPTION_PASSWORD_DIALOG_TEXT"),
								OPTION_PASSWORD, PASSWORD_MIN_LENGTH, PASSWORD_MAX_LENGTH),
						STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
			}
			preferences.put(PREFERENCES_PASSWORD, password);
		}

		return valid;
	}

	public void handleOnScreenKeyboardModeChange() {
		if (scheduleOnScreenKeyboardModeSwitch) {
			for (final var buttonToModeActions : input.getProfile().getButtonToModeActionsMap().values()) {
				for (final var buttonToModeAction : buttonToModeActions) {
					if (OnScreenKeyboard.ON_SCREEN_KEYBOARD_MODE.equals(buttonToModeAction.getMode(input))) {
						buttonToModeAction.doAction(input, -1, true);
						break;
					}
				}
			}

			scheduleOnScreenKeyboardModeSwitch = false;
		}
	}

	private void handleRemainingCommandLine(final CommandLine commandLine) {
		if (frame != null) {
			final var hasTrayOption = commandLine.hasOption(OPTION_TRAY);

			var visible = !hasTrayOption || isModalDialogShowing();
			if (tray == 0L && hasTrayOption) {
				LOGGER.log(Level.WARNING,
						"System Tray is not supported - ignoring '-" + OPTION_TRAY + "' command-line option");
				visible = true;
			}

			frame.setVisible(visible);
			updateShowTrayEntry();

			if (!visible) {
				showTrayIconHint();
			}
		}

		if (handleNetworkCommandLineOptions(commandLine)) {
			final var autostartOptionValue = commandLine.getOptionValue(OPTION_AUTOSTART);
			if (autostartOptionValue != null) {
				if ((IS_WINDOWS || IS_LINUX) && OPTION_AUTOSTART_VALUE_LOCAL.equals(autostartOptionValue)) {
					startLocal();
				} else if ((IS_WINDOWS || IS_LINUX) && OPTION_AUTOSTART_VALUE_CLIENT.equals(autostartOptionValue)) {
					startClient();
				} else if (OPTION_AUTOSTART_VALUE_SERVER.equals(autostartOptionValue)) {
					startServer();
				} else {
					GuiUtils.showMessageDialog(this, frame,
							MessageFormat.format(
									STRINGS.getString("INVALID_VALUE_FOR_COMMAND_LINE_OPTION_AUTOSTART_DIALOG_TEXT"),
									OPTION_AUTOSTART, autostartOptionValue,
									IS_WINDOWS || IS_LINUX ? STRINGS.getString("LOCAL_FEEDER_OR_CLIENT_OR_SERVER")
											: STRINGS.getString("SERVER")),
							STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				}
			}
		}

		final var saveOptionValue = commandLine.getOptionValue(OPTION_SAVE);
		if (saveOptionValue != null) {
			saveProfile(new File(saveOptionValue), false);
		}

		final var exportOptionValue = commandLine.getOptionValue(OPTION_EXPORT);
		if (exportOptionValue != null) {
			exportVisualization(new File(exportOptionValue));
		}

		if (commandLine.hasOption(OPTION_QUIT) && handleUnsavedChanges()) {
			quit();
		}
	}

	private boolean handleUnsavedChanges() {
		if (!unsavedChanges) {
			return true;
		}

		final var path = currentFile != null ? currentFile.getAbsolutePath() : STRINGS.getString("UNTITLED");

		final var selectedOption = JOptionPane.showConfirmDialog(frame,
				MessageFormat.format(STRINGS.getString("SAVE_CHANGES_DIALOG_TEXT"), path),
				STRINGS.getString("WARNING_DIALOG_TITLE"), JOptionPane.YES_NO_CANCEL_OPTION);

		return switch (selectedOption) {
		case JOptionPane.YES_OPTION -> {
			saveProfile();
			yield !unsavedChanges;
		}
		case JOptionPane.NO_OPTION -> true;
		default -> false;
		};
	}

	private void initOverlay() {
		if (!input.getProfile().isShowOverlay()) {
			return;
		}

		final var modes = input.getProfile().getModes();
		final var multipleModes = modes.size() > 1;
		final var virtualAxisToOverlayAxisMap = input.getProfile().getVirtualAxisToOverlayAxisMap();
		if (!multipleModes && virtualAxisToOverlayAxisMap.isEmpty()) {
			return;
		}

		overlayFrame = new JFrame("Overlay");
		final var overlayFrameRootPane = overlayFrame.getRootPane();
		overlayFrameRootPane.setWindowDecorationStyle(JRootPane.NONE);
		overlayFrameRootPane.setBackground(TRANSPARENT);
		overlayFrame.setUndecorated(true);
		overlayFrame.setType(Window.Type.POPUP);
		overlayFrame.setLayout(new BorderLayout());
		overlayFrame.setFocusableWindowState(false);
		overlayFrame.setBackground(TRANSPARENT);
		overlayFrame.setAlwaysOnTop(true);
		overlayFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		final var overlayScaling = getOverlayScaling();

		if (multipleModes) {
			currentModeLabel = new JLabel(input.getProfile().getActiveMode().getDescription());
			currentModeLabel.setOpaque(true);
			final var outerBorder = createOverlayBorder();
			final var innerBorder = BorderFactory.createEmptyBorder(0, 1, 0, 1);
			final var border = BorderFactory.createCompoundBorder(outerBorder, innerBorder);
			currentModeLabel.setBorder(border);
			final var defaultFont = currentModeLabel.getFont();
			final var newFont = currentModeLabel.getFont().deriveFont(Font.BOLD,
					defaultFont.getSize2D() * overlayScaling);
			currentModeLabel.setFont(newFont);
			final var fontMetrics = currentModeLabel.getFontMetrics(newFont);
			final var longestDescription = modes.stream().map(Mode::getDescription)
					.max(Comparator.comparingInt(String::length)).orElse("");
			final var modeLabelWidth = Math.min(
					fontMetrics.stringWidth(longestDescription) + outerBorder.getThickness() * 2
							+ outerBorder.getThickness() * Math.round(2 * overlayScaling),
					Math.round(OVERLAY_MODE_LABEL_MAX_WIDTH * overlayScaling));
			currentModeLabel.setPreferredSize(
					new Dimension(modeLabelWidth, fontMetrics.getHeight() + Math.round(1 * overlayScaling)));

			currentModeLabel.setHorizontalAlignment(SwingConstants.CENTER);
			overlayFrame.add(currentModeLabel, BorderLayout.SOUTH);
		}

		final var indicatorPanelFlowLayout = new FlowLayout(FlowLayout.CENTER, 10, 5);
		indicatorPanel = new JPanel(indicatorPanelFlowLayout);
		indicatorPanel.setBackground(TRANSPARENT);

		EnumSet.allOf(VirtualAxis.class).forEach(virtualAxis -> {
			final var overlayAxis = virtualAxisToOverlayAxisMap.get(virtualAxis);
			if (overlayAxis != null) {
				final var detentValues = new HashSet<Float>();

				input.getProfile().getModes().forEach(
						mode -> mode.getAxisToActionsMap().values().forEach(actions -> actions.forEach(action -> {
							if (action instanceof final AxisToRelativeAxisAction axisToRelativeAxisAction
									&& axisToRelativeAxisAction.getVirtualAxis() == virtualAxis) {
								final var detentValue = axisToRelativeAxisAction.getDetentValue();

								if (detentValue != null) {
									detentValues.add(detentValue);
								}
							}
						})));

				final var indicatorProgressBar = new IndicatorProgressBar(detentValues, overlayAxis);
				indicatorProgressBar.setPreferredSize(
						new Dimension(Math.round(OVERLAY_INDICATOR_PROGRESS_BAR_WIDTH * overlayScaling),
								Math.round(OVERLAY_INDICATOR_PROGRESS_BAR_HEIGHT * overlayScaling)));
				indicatorProgressBar.setForeground(overlayAxis.color);

				indicatorPanel.add(indicatorProgressBar);
				virtualAxisToProgressBarMap.put(virtualAxis, indicatorProgressBar);
			}
		});

		overlayFrame.add(indicatorPanel, BorderLayout.CENTER);

		overlayFrameDragListener = new FrameDragListener(this, overlayFrame) {

			@Override
			public void mouseDragged(final MouseEvent e) {
				super.mouseDragged(e);

				if (!IS_MAC) {
					totalDisplayBounds = GuiUtils.getTotalDisplayBounds();
					updateOverlayAlignment(totalDisplayBounds);
				}
			}

			@Override
			public void mouseReleased(final MouseEvent e) {
				super.mouseReleased(e);

				if (IS_MAC) {
					deInitOverlay();
					initOverlay();
				}
			}
		};
		overlayFrame.addMouseListener(overlayFrameDragListener);
		overlayFrame.addMouseMotionListener(overlayFrameDragListener);
		overlayFrame.pack();

		final var maximumWindowBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		final var defaultLocation = new Point();
		defaultLocation.x = 0;
		defaultLocation.y = maximumWindowBounds.height - overlayFrame.getHeight();

		prevTotalDisplayBounds = GuiUtils.getTotalDisplayBounds();

		GuiUtils.loadFrameLocation(preferences, overlayFrame, defaultLocation, prevTotalDisplayBounds);

		updateOverlayAlignment(prevTotalDisplayBounds);

		overlayFrame.setVisible(true);
	}

	private void initProfile(final String cmdProfilePath, final boolean noControllerConnected) {
		final var profilePath = cmdProfilePath != null ? cmdProfilePath
				: preferences.get(PREFERENCES_LAST_PROFILE, null);
		if (profilePath != null) {
			loadProfile(new File(profilePath), noControllerConnected, false);
			if (loadedProfile == null && cmdProfilePath == null) {
				LOGGER.log(Level.INFO, "Removing " + PREFERENCES_LAST_PROFILE + " from preferences");
				preferences.remove(PREFERENCES_LAST_PROFILE);
			}
		}
	}

	public boolean isAutoRestartOutput() {
		return preferences.getBoolean(PREFERENCES_AUTO_RESTART_OUTPUT, false);
	}

	private boolean isClientRunning() {
		return mainLoop.isTaskOfTypeRunning(ClientRunMode.class);
	}

	public boolean isHapticFeedback() {
		return preferences.getBoolean(PREFERENCES_HAPTIC_FEEDBACK, true);
	}

	public boolean isLocalRunning() {
		return mainLoop.isTaskOfTypeRunning(LocalRunMode.class);
	}

	public boolean isMapCircularAxesToSquareAxes() {
		return preferences.getBoolean(PREFERENCES_MAP_CIRCULAR_AXES_TO_SQUARE, true);
	}

	boolean isOverlayInLowerHalf(final Rectangle totalDisplayBounds) {
		return overlayFrame.getY() + overlayFrame.getHeight() / 2 < totalDisplayBounds.height / 2;
	}

	public boolean isPreventPowerSaveMode() {
		return preferences.getBoolean(PREFERENCES_PREVENT_POWER_SAVE_MODE, true);
	}

	private boolean isRunning() {
		return isLocalRunning() || isClientRunning() || isServerRunning();
	}

	private boolean isSelectedController(final int instanceId) {
		return selectedController != null && selectedController.instanceId == instanceId;
	}

	public boolean isServerRunning() {
		return mainLoop.isTaskOfTypeRunning(ServerRunMode.class);
	}

	public boolean isSkipControllerDialogs() {
		return preferences.getBoolean(PREFERENCES_SKIP_CONTROLLER_DIALOGS, false);
	}

	public boolean isSwapLeftAndRightSticks() {
		return preferences.getBoolean(PREFERENCES_SWAP_LEFT_AND_RIGHT_STICKS, false);
	}

	public boolean isTouchpadEnabled() {
		return preferences.getBoolean(PREFERENCES_TOUCHPAD_ENABLED, true);
	}

	private void loadProfile(final File file, final boolean skipMessageDialogs,
			final boolean performGarbageCollection) {
		final var wasRunning = isRunning();
		stopAll(true, false, performGarbageCollection);

		EventQueue.invokeLater(() -> {
			LOGGER.log(Level.INFO, "Loading profile: " + file.getAbsolutePath());

			var profileLoaded = false;

			try {
				final var jsonString = Files.readString(file.toPath(), StandardCharsets.UTF_8);
				final var jsonContext = JsonContext.create();

				try {
					final var profile = jsonContext.gson.fromJson(jsonString, Profile.class);
					final var versionsComparisonResult = VersionUtils.compareVersions(profile.getVersion());
					if (versionsComparisonResult.isEmpty()) {
						LOGGER.log(Level.WARNING, "Trying to load a profile without version information");

						if (!skipMessageDialogs) {
							GuiUtils.showMessageDialog(main, frame,
									MessageFormat.format(STRINGS.getString("PROFILE_VERSION_MISMATCH_DIALOG_TEXT"),
											file.getName(), STRINGS.getString("AN_UNKNOWN"),
											Constants.APPLICATION_NAME),
									STRINGS.getString("WARNING_DIALOG_TITLE"), JOptionPane.WARNING_MESSAGE);
						}
					} else {
						final int v = versionsComparisonResult.get();
						if (v < 0) {
							LOGGER.log(Level.WARNING, "Trying to load a profile for an older release");

							if (!skipMessageDialogs) {
								GuiUtils.showMessageDialog(main, frame,
										MessageFormat.format(STRINGS.getString("PROFILE_VERSION_MISMATCH_DIALOG_TEXT"),
												file.getName(), STRINGS.getString("AN_OLDER"),
												Constants.APPLICATION_NAME),
										STRINGS.getString("WARNING_DIALOG_TITLE"), JOptionPane.WARNING_MESSAGE);
							}
						} else if (v > 0) {
							LOGGER.log(Level.WARNING, "Trying to load a profile for a newer release");

							if (!skipMessageDialogs) {
								GuiUtils.showMessageDialog(main, frame,
										MessageFormat.format(STRINGS.getString("PROFILE_VERSION_MISMATCH_DIALOG_TEXT"),
												file.getName(), STRINGS.getString("A_NEWER"),
												Constants.APPLICATION_NAME),
										STRINGS.getString("WARNING_DIALOG_TITLE"), JOptionPane.WARNING_MESSAGE);
							}
						}
					}

					final var unknownActionClasses = jsonContext.actionTypeAdapter.getUnknownActionClasses();
					if (!unknownActionClasses.isEmpty()) {
						LOGGER.log(Level.WARNING, "Encountered the unknown actions while loading profile:"
								+ String.join(", ", unknownActionClasses));

						if (!skipMessageDialogs) {
							GuiUtils.showMessageDialog(main, frame,
									MessageFormat.format(STRINGS.getString("UNKNOWN_ACTION_TYPES_DIALOG_TEXT"),
											String.join("\n", unknownActionClasses)),
									STRINGS.getString("WARNING_DIALOG_TITLE"), JOptionPane.WARNING_MESSAGE);
						}
					}

					profileLoaded = input.setProfile(profile);
					if (profileLoaded) {
						saveLastProfile(file);
						updateModesPanel(false);
						updateVisualizationPanel();
						updateOverlayPanel();
						updateProfileSettingsPanel();
						updatePanelAccess();
						loadedProfile = file.getName();
						setUnsavedChanges(false);
						setStatusBarText(MessageFormat.format(STRINGS.getString("STATUS_PROFILE_LOADED"),
								file.getAbsolutePath()));
						scheduleStatusBarText(STRINGS.getString("STATUS_READY"));
						profileFileChooser.setSelectedFile(file);

						if (wasRunning) {
							restartLast();
						}
					}
				} catch (final JsonParseException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
			} catch (final NoSuchFileException | InvalidPathException e) {
				LOGGER.log(Level.FINE, e.getMessage(), e);
			} catch (final IOException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}

			if (!profileLoaded) {
				LOGGER.log(Level.SEVERE, "Could not load profile");

				if (!skipMessageDialogs) {
					GuiUtils.showMessageDialog(main, frame,
							MessageFormat.format(STRINGS.getString("COULD_NOT_LOAD_PROFILE_DIALOG_TEXT"),
									Constants.APPLICATION_NAME),
							STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				}
			}
		});
	}

	public void newActivation(final String[] args) {
		LOGGER.log(Level.INFO, "New activation with arguments: " + String.join(" ", args));

		if (args.length > 0) {
			try {
				final var commandLine = new DefaultParser().parse(OPTIONS, args);
				final var cmdProfilePath = commandLine.getOptionValue(OPTION_PROFILE);
				final var gameControllerDbPath = commandLine.getOptionValue(OPTION_GAME_CONTROLLER_DB);

				EventQueue.invokeLater(() -> {
					if (cmdProfilePath != null && handleUnsavedChanges()) {
						loadProfile(new File(cmdProfilePath), false, true);
					}

					if (gameControllerDbPath != null) {
						updateGameControllerMappingsFromFile(gameControllerDbPath);
					}

					EventQueue.invokeLater(() -> handleRemainingCommandLine(commandLine));
				});
			} catch (final ParseException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		} else {
			EventQueue.invokeLater(() -> GuiUtils.showMessageDialog(main, frame,
					MessageFormat.format(STRINGS.getString("ALREADY_RUNNING_DIALOG_TEXT"), Constants.APPLICATION_NAME),
					STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE));
		}
	}

	private void newProfile(final boolean performGarbageCollection) {
		stopAll(true, false, performGarbageCollection);

		profileFileChooser.resetSelectedFile();
		currentFile = null;

		if (input != null) {
			input.deInit();
		}

		input = new Input(this, selectedController, null);

		loadedProfile = null;
		unsavedChanges = false;
		updateTitleAndTooltip();
		updateModesPanel(false);
		updateVisualizationPanel();
		updateOverlayPanel();
		updateProfileSettingsPanel();
		setStatusBarText(STRINGS.getString("STATUS_READY"));
	}

	private void onControllersChanged(final boolean selectFirstTab) {
		final var controllerConnected = !controllers.isEmpty();

		final var previousSelectedTabIndex = tabbedPane.getSelectedIndex();
		deviceMenu.removeAll();
		menuBar.remove(deviceMenu);

		final var runMenuVisible = startClientMenuItem != null || controllerConnected;

		runMenu.setVisible(runMenuVisible);
		if (startLocalMenuItem != null) {
			startLocalMenuItem.setVisible(controllerConnected);
		}
		if (startServerMenuItem != null) {
			startServerMenuItem.setVisible(controllerConnected);
		}

		if (hasSystemTray) {
			final var trayCreated = mainLoop.runSync(() -> {
				if (tray == 0L) {
					final var iconImage = frame.getIconImages().stream()
							.max(Comparator.comparingInt(o -> o.getWidth(null)))
							.orElseThrow(() -> new RuntimeException("No icon set for frame"));
					final var width = iconImage.getWidth(null);
					final var height = iconImage.getHeight(null);

					final var bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

					final var graphics2D = bufferedImage.createGraphics();
					graphics2D.drawImage(iconImage, 0, 0, null);
					graphics2D.dispose();

					final var pixels = new int[width * height];
					bufferedImage.getRGB(0, 0, width, height, pixels, 0, width);

					try (final var surface = SDLSurface.SDL_CreateSurface(width, height,
							SDLPixels.SDL_PIXELFORMAT_BGRA8888)) {
						if (surface == null) {
							logSdlError("Failed to create surface");
							return false;
						}

						final var pixelsByteBuffer = surface.pixels();
						if (pixelsByteBuffer == null) {
							return false;
						}

						for (var y = 0; y < height; y++) {
							for (var x = 0; x < width; x++) {
								final var pixel = pixels[y * width + x];
								pixelsByteBuffer.put((byte) ((pixel >> 24) & 0xFF));
								pixelsByteBuffer.put((byte) ((pixel >> 16) & 0xFF));
								pixelsByteBuffer.put((byte) ((pixel >> 8) & 0xFF));
								pixelsByteBuffer.put((byte) (pixel & 0xFF));
							}
						}

						final var surfaceBuffer = SDL_Surface.createSafe(surface.address(), surface.sizeof());
						if (surfaceBuffer == null) {
							return false;
						}
						tray = SDLTray.SDL_CreateTray(surfaceBuffer, Constants.APPLICATION_NAME);
					}

					if (tray == 0L) {
						logSdlError("Failed to create tray");
						return false;
					}
				}

				if (trayMenu == 0L) {
					trayMenu = SDLTray.SDL_CreateTrayMenu(tray);
				} else {
					for (;;) {
						final var trayEntriesPointerBuffer = SDLTray.SDL_GetTrayEntries(trayMenu);
						if (trayEntriesPointerBuffer == null || !trayEntriesPointerBuffer.hasRemaining()) {
							break;
						}
						final var trayEntry = trayEntriesPointerBuffer.get();
						SDLTray.SDL_RemoveTrayEntry(trayEntry);
					}

					showTrayEntry = 0L;
					startLocalTrayEntry = 0L;
					startClientTrayEntry = 0L;
					startServerTrayEntry = 0L;
					stopTrayEntry = 0L;
				}

				showTrayEntry = SDLTray.SDL_InsertTrayEntryAt(trayMenu, -1, STRINGS.getString("SHOW_TRAY_ENTRY_LABEL"),
						SDLTray.SDL_TRAYENTRY_BUTTON);
				SDLTray.SDL_SetTrayEntryEnabled(showTrayEntry, !frame.isVisible());
				SDLTray.SDL_SetTrayEntryCallback(showTrayEntry, (_, _) -> EventQueue.invokeLater(() -> {
					final var openEvent = new WindowEvent(frame, WindowEvent.WINDOW_OPENED);
					Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(openEvent);
					frame.setVisible(true);
					frame.setExtendedState(Frame.NORMAL);
				}), 0x0);

				SDLTray.SDL_InsertTrayEntryAt(trayMenu, -1, (ByteBuffer) null, 0x0);

				insertTrayEntryFromAction(trayMenu, openAction);

				if (runMenuVisible) {
					final var runMenuTrayEntry = SDLTray.SDL_InsertTrayEntryAt(trayMenu, -1,
							STRINGS.getString("RUN_MENU"), SDLTray.SDL_TRAYENTRY_SUBMENU);

					final var runSubMenu = SDLTray.SDL_CreateTraySubmenu(runMenuTrayEntry);

					if (IS_WINDOWS || IS_LINUX) {
						if (controllerConnected) {
							startLocalTrayEntry = insertTrayEntryFromAction(runSubMenu, startLocalAction);
						}

						startClientTrayEntry = insertTrayEntryFromAction(runSubMenu, startClientAction);
					}

					if (controllerConnected) {
						startServerTrayEntry = insertTrayEntryFromAction(runSubMenu, startServerAction);
					}

					SDLTray.SDL_InsertTrayEntryAt(runSubMenu, -1, (ByteBuffer) null, 0x0);

					stopTrayEntry = insertTrayEntryFromAction(runSubMenu, stopAction);
				}

				SDLTray.SDL_InsertTrayEntryAt(trayMenu, -1, (ByteBuffer) null, 0x0);

				insertTrayEntryFromAction(trayMenu, quitAction);

				return true;
			}).orElse(false);

			if (trayCreated) {
				updateTrayEntriesEnabled();
				updateTitleAndTooltip();
				frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
			}
		}

		var restartOutput = false;
		if (!controllerConnected) {
			selectedController = null;
			setSelectedControllerAndUpdateInput(null, null);
		} else {
			final Controller controller;
			if (selectedController != null && controllers.contains(selectedController)) {
				controller = selectedController;
			} else {
				controller = controllers.stream().findFirst().orElse(null);
			}

			if (selectedController == null
					|| (input != null && !Objects.equals(input.getSelectedController(), controller))) {
				setSelectedControllerAndUpdateInput(controller, null);

				if (isAutoRestartOutput()) {
					restartOutput = switch (lastRunModeType) {
					case NONE, CLIENT -> false;
					case LOCAL, SERVER -> true;
					};
				}
			}
		}

		if (controllerConnected) {
			final var devicesButtonGroup = new ButtonGroup();
			controllers.forEach(controller -> {
				final var deviceRadioButtonMenuItem = new JRadioButtonMenuItem(new SelectControllerAction(controller));
				devicesButtonGroup.add(deviceRadioButtonMenuItem);
				deviceMenu.add(deviceRadioButtonMenuItem);
			});
			menuBar.add(deviceMenu, 1);
		} else {
			LOGGER.log(Level.INFO, "No controllers connected");
		}

		updateDeviceMenuSelection();

		if (selectFirstTab) {
			tabbedPane.setSelectedIndex(0);
		} else if (previousSelectedTabIndex < tabbedPane.getTabCount()) {
			tabbedPane.setSelectedIndex(previousSelectedTabIndex);
		}

		updateMenuShortcuts();
		updateModesPanel(false);
		updateVisualizationPanel();
		updateOverlayPanel();
		updateProfileSettingsPanel();
		updatePanelAccess();

		menuBar.revalidate();

		if (restartOutput) {
			restartLast();
		}
	}

	private void onRunModeChanged(final boolean running) {
		if (startLocalMenuItem != null) {
			startLocalMenuItem.setEnabled(!running);
		}
		if (startClientMenuItem != null) {
			startClientMenuItem.setEnabled(!running);
		}
		if (startServerMenuItem != null) {
			startServerMenuItem.setEnabled(!running);
		}
		if (stopMenuItem != null) {
			stopMenuItem.setEnabled(running);
		}

		mainLoop.runSync(this::updateTrayEntriesEnabled);

		updateMenuShortcuts();
		updatePanelAccess(running);
	}

	public void pollSdlEvents() {
		try (final var stack = MemoryStack.stackPush()) {
			final var sdlEvent = SDL_Event.malloc(stack);

			while (SDLEvents.SDL_PollEvent(sdlEvent)) {
				switch (sdlEvent.type()) {
				case SDLEvents.SDL_EVENT_GAMEPAD_ADDED -> {
					final var instanceId = sdlEvent.gdevice().which();

					final var controller = new Controller(instanceId);
					if (controllers.add(controller)) {
						LOGGER.log(Level.INFO, assembleControllerLoggingMessage("Connected controller ", controller));

						if (input.isInitialized()) {
							input.openController(controller);
						}

						EventQueue.invokeLater(() -> onControllersChanged(false));
					}
				}
				case SDLEvents.SDL_EVENT_GAMEPAD_REMOVED -> {
					final var instanceId = sdlEvent.gdevice().which();

					controllers.stream().filter(controller -> controller.instanceId == instanceId).findFirst()
							.ifPresent(controller -> {
								controllers.remove(controller);
								LOGGER.log(Level.INFO,
										assembleControllerLoggingMessage("Disconnected controller ", controller));

								if (selectedController != null && selectedController.instanceId == instanceId) {
									selectedController = null;
									input.deInit();
								}

								EventQueue.invokeLater(() -> onControllersChanged(false));
							});
				}
				case SDLEvents.SDL_EVENT_GAMEPAD_TOUCHPAD_MOTION -> {
					if (!isTouchpadEnabled()) {
						break;
					}

					final var gamepadTouchpadEvent = sdlEvent.gtouchpad();
					if (input == null || !isSelectedController(gamepadTouchpadEvent.which())) {
						break;
					}

					switch (gamepadTouchpadEvent.finger()) {
					case 0 -> {
						final var x = gamepadTouchpadEvent.x();
						final var y = gamepadTouchpadEvent.y();

						if (prevFinger0X != null && prevFinger0Y != null) {
							final var deltaX = x - prevFinger0X;
							input.setCursorDeltaX((int) (deltaX * 1000f * getTouchpadCursorSensitivity()));

							final var deltaY = y - prevFinger0Y;
							input.setCursorDeltaY((int) (deltaY * 1000f * getTouchpadCursorSensitivity()));
						}

						prevFinger0X = x;
						prevFinger0Y = y;
					}
					case 1 -> {
						final var y = gamepadTouchpadEvent.y();

						if (prevFinger1Y != null) {
							final var deltaY = y - prevFinger1Y;
							input.setScrollClicks((int) (-deltaY * 100f * getTouchpadScrollSensitivity()));
						}

						prevFinger1Y = y;
					}
					default -> {
					}
					}
				}
				case SDLEvents.SDL_EVENT_GAMEPAD_TOUCHPAD_UP -> {
					if (!isTouchpadEnabled()) {
						break;
					}

					final var gamepadTouchpadEvent = sdlEvent.gtouchpad();
					if (input == null || !isSelectedController(gamepadTouchpadEvent.which())) {
						break;
					}

					switch (gamepadTouchpadEvent.finger()) {
					case 0 -> {
						prevFinger0X = null;
						prevFinger0Y = null;
					}
					case 1 -> prevFinger1Y = null;
					default -> {
					}
					}
				}
				case SDLEvents.SDL_EVENT_GAMEPAD_BUTTON_DOWN, SDLEvents.SDL_EVENT_GAMEPAD_BUTTON_UP -> {
					if (!isTouchpadEnabled()) {
						break;
					}

					final var gamepadButtonEvent = sdlEvent.gbutton();
					if (input == null || !isSelectedController(gamepadButtonEvent.which())
							|| gamepadButtonEvent.button() != SDLGamepad.SDL_GAMEPAD_BUTTON_TOUCHPAD) {
						break;
					}

					final var downMouseButtons = input.getDownMouseButtons();
					if (gamepadButtonEvent.down()) {
						downMouseButtons.add(prevFinger1Y != null ? 2 : 1);
					} else {
						downMouseButtons.clear();
					}
				}
				case SDLEvents.SDL_EVENT_JOYSTICK_BATTERY_UPDATED -> {
					final var joyBatteryEvent = sdlEvent.jbattery();
					if (!isSelectedController(joyBatteryEvent.which())) {
						break;
					}

					updateTrayIconToolTip(joyBatteryEvent.percent());
				}
				case SDLEvents.SDL_EVENT_SYSTEM_THEME_CHANGED -> {
					if (getSelectedTheme() == Theme.SYSTEM) {
						EventQueue.invokeLater(this::updateTheme);
					}
				}
				default -> {
				}
				}
			}
		}
	}

	private void quit() {
		if (input != null) {
			input.deInit();
		}

		if (IS_LINUX) {
			UinputDevice.shutdown();
		}

		stopAll(true, false, false);

		terminate(0, this);
	}

	private void repaintOnScreenKeyboardAndOverlay() {
		if (onScreenKeyboard.isVisible()) {
			onScreenKeyboard.validate();
			onScreenKeyboard.repaint();
		}

		if (IS_WINDOWS && overlayFrame != null) {
			overlayFrame.validate();
			overlayFrame.repaint();
		}
	}

	public void restartLast() {
		switch (lastRunModeType) {
		case LOCAL -> startLocal();
		case CLIENT -> startClient();
		case SERVER -> startServer();
		case NONE -> {
		}
		}
	}

	private void saveLastProfile(final File file) {
		currentFile = file;
		preferences.put(PREFERENCES_LAST_PROFILE, file.getAbsolutePath());
	}

	private void saveProfile() {
		if (currentFile != null) {
			saveProfile(currentFile, true);
		} else {
			saveProfileAs();
		}
	}

	private void saveProfile(File file, final boolean saveAsLastProfile) {
		if (!file.getName().toLowerCase(Locale.ROOT).endsWith(PROFILE_FILE_SUFFIX)) {
			file = new File(file.getAbsoluteFile() + PROFILE_FILE_SUFFIX);
			profileFileChooser.setSelectedFile(file);
		}

		LOGGER.log(Level.INFO, "Saving profile: " + file.getAbsolutePath());

		final var profile = input.getProfile();
		profile.setVersion(VersionUtils.getMajorAndMinorVersion());

		final var jsonString = JsonContext.create().gson.toJson(profile);
		try {
			Files.writeString(file.toPath(), jsonString, StandardCharsets.UTF_8);

			if (saveAsLastProfile) {
				saveLastProfile(file);
			}

			loadedProfile = file.getName();
			setUnsavedChanges(false);
			setStatusBarText(MessageFormat.format(STRINGS.getString("STATUS_PROFILE_SAVED"), file.getAbsolutePath()));
			scheduleStatusBarText(STRINGS.getString("STATUS_READY"));
		} catch (final IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			GuiUtils.showMessageDialog(main, frame, STRINGS.getString("COULD_NOT_SAVE_PROFILE_DIALOG_TEXT"),
					STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
		}
	}

	private void saveProfileAs() {
		profileFileChooser.setSelectedFile(
				currentFile != null ? currentFile : new File(STRINGS.getString("UNTITLED") + PROFILE_FILE_SUFFIX));

		if (profileFileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
			saveProfile(profileFileChooser.getSelectedFile(), true);
		}
	}

	public void scheduleStatusBarText(final String text) {
		class StatusBarTextTimerTask extends TimerTask {

			private final String newText;

			private final String originalText;

			private StatusBarTextTimerTask(final String newText) {
				this.newText = newText;
				originalText = statusLabel.getText();
			}

			@Override
			public void run() {
				EventQueue.invokeLater(() -> {
					if (statusLabel.getText().equals(originalText)) {
						setStatusBarText(newText);
					}
				});
			}
		}

		timer.schedule(new StatusBarTextTimerTask(text), 5000L);
	}

	void setClipboardAction(final IAction<?> action) {
		clipboardAction = action;
	}

	public void setOnScreenKeyboardVisible(final boolean visible) {
		if (isLocalRunning() || isServerRunning()) {
			EventQueue.invokeLater(() -> {
				onScreenKeyboard.setVisible(visible);

				repaintOnScreenKeyboardAndOverlay();
			});
		}
	}

	public void setOverlayText(final String text) {
		GuiUtils.invokeOnEventDispatchThreadIfRequired(() -> {
			if (currentModeLabel != null) {
				currentModeLabel.setText(text);
			}
		});
	}

	private void setSelectedController(final Controller controller) {
		if (Objects.equals(selectedController, controller)) {
			return;
		}

		selectedController = controller;

		if (controller != null) {
			LOGGER.log(Level.INFO, assembleControllerLoggingMessage("Selected controller ", controller));

			if (controller.guid != null) {
				preferences.put(PREFERENCES_LAST_CONTROLLER, controller.guid);
			}
		}
	}

	public void setSelectedControllerAndUpdateInput(final Controller controller,
			@SuppressWarnings("exports") final EnumMap<VirtualAxis, Integer> axes) {
		stopAll(true, false, true);

		setSelectedController(controller);

		Profile previousProfile = null;
		if (input != null) {
			input.deInit();
			previousProfile = input.getProfile();
		}

		input = new Input(this, selectedController, axes);

		if (previousProfile != null) {
			input.setProfile(previousProfile);
		}
	}

	public void setStatusBarText(final String text) {
		statusLabel.setText(text);
	}

	void setTotalDisplayBounds(final Rectangle totalDisplayBounds) {
		this.totalDisplayBounds = totalDisplayBounds;
	}

	void setUnsavedChanges(final boolean unsavedChanges) {
		this.unsavedChanges = unsavedChanges;
		updateTitleAndTooltip();
	}

	boolean show() {
		if (frame == null || frame.isVisible()) {
			return false;
		}

		frame.setVisible(true);
		updateShowTrayEntry();

		return true;
	}

	private void showTrayIconHint() {
		if (preferences.getBoolean(PREFERENCES_SKIP_TRAY_ICON_HINT, false)) {
			return;
		}

		final var imageLabel = new JLabel(new ImageIcon(getResourceLocation(TRAY_ICON_HINT_IMAGE_RESOURCE_PATH)));
		imageLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 0, 25, 10),
				BorderFactory.createLoweredBevelBorder()));

		final var doNotShowMessageAgainCheckbox = new JCheckBox(STRINGS.getString("DO_NOT_SHOW_MESSAGE_AGAIN"));

		GuiUtils.showMessageDialog(null, frame,
				new Object[] { MessageFormat.format(STRINGS.getString("TRAY_ICON_HINT_DIALOG_TEXT"),
						Constants.APPLICATION_NAME), imageLabel, doNotShowMessageAgainCheckbox },
				STRINGS.getString("INFORMATION_DIALOG_TITLE"), JOptionPane.INFORMATION_MESSAGE);

		if (doNotShowMessageAgainCheckbox.isSelected()) {
			preferences.putBoolean(PREFERENCES_SKIP_TRAY_ICON_HINT, true);
		}
	}

	private void startClient() {
		lastRunModeType = RunModeType.CLIENT;

		if (isRunning()) {
			return;
		}

		final var clientRunMode = new ClientRunMode(this, input);
		runMode = clientRunMode;
		mainLoop.runAsync(clientRunMode);

		onRunModeChanged(true);
	}

	private void startLocal() {
		lastRunModeType = RunModeType.LOCAL;

		if (selectedController == null || input.getSelectedController() == null || isRunning()) {
			return;
		}

		final var localRunMode = new LocalRunMode(this, input);
		runMode = localRunMode;
		mainLoop.runAsync(localRunMode);

		onRunModeChanged(true);

		initOverlay();
		startOverlayTimerTask();
	}

	@SuppressWarnings("FutureReturnValueIgnored")
	private void startOverlayTimerTask() {
		stopOverlayTimerTask();

		overlayExecutorService = Executors.newSingleThreadScheduledExecutor();
		overlayExecutorService.scheduleAtFixedRate(new RunnableWithDefaultExceptionHandler(this::updateOverlayPosition),
				OVERLAY_POSITION_UPDATE_DELAY, OVERLAY_POSITION_UPDATE_INTERVAL, TimeUnit.SECONDS);
	}

	private void startServer() {
		lastRunModeType = RunModeType.SERVER;

		if (selectedController == null || input.getSelectedController() == null || isRunning()) {
			return;
		}

		final var serverThread = new ServerRunMode(this, input);
		runMode = serverThread;
		mainLoop.runAsync(serverThread);

		onRunModeChanged(true);

		initOverlay();
		startOverlayTimerTask();
	}

	public void stopAll(final boolean initiateStop, final boolean resetLastRunModeType,
			final boolean performGarbageCollection) {
		if (IS_WINDOWS || IS_LINUX) {
			stopLocal(initiateStop, resetLastRunModeType);
			stopClient(initiateStop, resetLastRunModeType);
		}
		stopServer(initiateStop, resetLastRunModeType);

		if (performGarbageCollection) {
			System.gc();
		}
	}

	private void stopClient(final boolean initiateStop, final boolean resetLastRunModeType) {
		final var running = isClientRunning();

		if (initiateStop && running) {
			((ClientRunMode) runMode).close();
		}

		if (running) {
			mainLoop.waitForTask();
		}

		if (resetLastRunModeType) {
			lastRunModeType = RunModeType.NONE;
		}

		GuiUtils.invokeOnEventDispatchThreadIfRequired(() -> onRunModeChanged(isRunning()));
	}

	private void stopLocal(final boolean initiateStop, final boolean resetLastRunModeType) {
		final var running = isLocalRunning();

		if (initiateStop && running) {
			mainLoop.stopTask();
		}

		if (running) {
			mainLoop.waitForTask();
		}

		if (resetLastRunModeType) {
			lastRunModeType = RunModeType.NONE;
		}

		GuiUtils.invokeOnEventDispatchThreadIfRequired(() -> {
			final var isRunningAgain = isRunning();

			if (!isRunningAgain) {
				stopOverlayTimerTask();
				deInitOverlayAndHideOnScreenKeyboard();
			}

			onRunModeChanged(isRunningAgain);
		});
	}

	private void stopOverlayTimerTask() {
		if (overlayExecutorService != null) {
			overlayExecutorService.shutdown();

			try {
				// noinspection ResultOfMethodCallIgnored
				overlayExecutorService.awaitTermination(2L, TimeUnit.SECONDS);
			} catch (final InterruptedException _) {
				Thread.currentThread().interrupt();
			} finally {
				overlayExecutorService = null;
			}
		}
	}

	private void stopServer(final boolean initiateStop, final boolean resetLastRunModeType) {
		final var running = runMode instanceof ServerRunMode;

		if (initiateStop && running) {
			((ServerRunMode) runMode).close();
		}

		if (running) {
			mainLoop.waitForTask();
		}

		if (resetLastRunModeType) {
			lastRunModeType = RunModeType.NONE;
		}

		GuiUtils.invokeOnEventDispatchThreadIfRequired(() -> {
			final var isRunningAgain = isRunning();

			if (!isRunningAgain) {
				stopOverlayTimerTask();
				deInitOverlayAndHideOnScreenKeyboard();
			}

			onRunModeChanged(isRunningAgain);
		});
	}

	public void updateDeviceMenuSelection() {
		if (selectedController == null) {
			return;
		}

		for (var i = 0; i < deviceMenu.getItemCount(); i++) {
			final var menuItem = deviceMenu.getItem(i);
			final var action = (SelectControllerAction) menuItem.getAction();
			if (selectedController.instanceId == action.controller.instanceId) {
				menuItem.setSelected(true);
				break;
			}
		}
	}

	private String updateGameControllerMappings(final String path, final String sourceName) {
		return mainLoop.runSync(() -> {
			final var numMappingsAdded = SDLGamepad.SDL_AddGamepadMappingsFromFile(path);

			if (numMappingsAdded == -1) {
				return logSdlError("Failed to update game controller mappings from " + sourceName);
			}

			LOGGER.log(Level.INFO, "Added " + numMappingsAdded + " game controller mappings from " + sourceName);

			return null;
		}).orElse(null);
	}

	private void updateGameControllerMappingsFromFile(final String path) {
		if (isLocalRunning()) {
			stopLocal(true, false);
		} else if (isServerRunning()) {
			stopServer(true, false);
		}

		String errorDetails = null;
		try {
			errorDetails = updateGameControllerMappings(path, "external file: " + path);
		} catch (final Throwable t) {
			LOGGER.log(Level.WARNING, "Could not read external game controller mappings file: " + path);

			GuiUtils.showMessageDialog(main, frame, MessageFormat
					.format(STRINGS.getString("COULD_NOT_READ_GAME_CONTROLLER_MAPPINGS_FILE_DIALOG_TEXT"), path),
					STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
		}

		if (errorDetails != null && !errorDetails.isBlank()) {
			GuiUtils.showMessageDialog(main, frame,
					MessageFormat.format(
							STRINGS.getString("ERROR_UPDATING_GAME_CONTROLLER_DB_FROM_EXTERNAL_FILE_DIALOG_TEXT"), path,
							errorDetails),
					STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
		}
	}

	private void updateMenuShortcuts() {
		final var menuCount = menuBar.getMenuCount();
		final var alreadyAssignedMenuKeyCodes = new HashSet<Integer>(menuCount);

		for (var i = 0; i < menuCount; i++) {
			final var menu = menuBar.getMenu(i);
			menu.setMnemonic(getExtendedKeyCodeForMenu(menu, alreadyAssignedMenuKeyCodes));

			final var menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
			for (var j = 0; j < menu.getItemCount(); j++) {
				final var menuItem = menu.getItem(j);
				if (menuItem != null) {
					KeyStroke keyStroke = null;

					if (menuItem.isEnabled()) {
						final var keyCode = getExtendedKeyCodeForMenuItem(menuItem);

						if (keyCode != KeyEvent.VK_UNDEFINED) {
							keyStroke = KeyStroke.getKeyStroke(keyCode, menuShortcutKeyMask);
						}
					}

					menuItem.setAccelerator(keyStroke);
				}
			}
		}
	}

	void updateModesPanel(final boolean newModeAdded) {
		if (modesListPanel == null) {
			return;
		}

		modesListPanel.removeAll();

		if (input == null) {
			return;
		}

		final var buttonGridBagConstraints = new GridBagConstraints(4, GridBagConstraints.RELATIVE, 1, 1, 0d, 0d,
				GridBagConstraints.CENTER, GridBagConstraints.NONE, LIST_ITEM_INNER_INSETS, 0, 0);

		final var modes = input.getProfile().getModes();
		final var numModes = modes.size();
		for (var i = 0; i < numModes; i++) {
			final var mode = modes.get(i);

			final var modePanel = new JPanel(new GridBagLayout());
			modePanel.setBorder(LIST_ITEM_BORDER);
			modesListPanel.add(modePanel, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0d, 0d,
					GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL, GRID_BAG_ITEM_INSETS, 0, 0));

			final var modeNoLabel = new JLabel(MessageFormat.format(STRINGS.getString("MODE_LABEL_NO"), i + 1));
			modeNoLabel.setPreferredSize(OVERLAY_SETTINGS_LABEL_DIMENSION);
			modePanel.add(modeNoLabel, new GridBagConstraints(0, 0, 1, 1, 0d, 0d, GridBagConstraints.CENTER,
					GridBagConstraints.NONE, LIST_ITEM_INNER_INSETS, 0, 0));

			modePanel.add(Box.createGlue(), new GridBagConstraints(1, GridBagConstraints.RELATIVE, 1, 1, 1d, 1d,
					GridBagConstraints.CENTER, GridBagConstraints.NONE, LIST_ITEM_INNER_INSETS, 0, 0));

			final var descriptionTextField = GuiUtils.createTextFieldWithMenu(mode.getDescription(), 35);
			modePanel.add(descriptionTextField, new GridBagConstraints(2, 0, 1, 1, 1d, 1d, GridBagConstraints.CENTER,
					GridBagConstraints.NONE, LIST_ITEM_INNER_INSETS, 0, 0));
			if (newModeAdded && i == numModes - 1) {
				descriptionTextField.grabFocus();
				descriptionTextField.selectAll();
			} else {
				descriptionTextField.setCaretPosition(0);
			}

			final var setModeDescriptionAction = new SetModeDescriptionAction(mode, descriptionTextField);
			descriptionTextField.addActionListener(setModeDescriptionAction);
			descriptionTextField.addFocusListener(setModeDescriptionAction);
			descriptionTextField.getDocument().addDocumentListener(setModeDescriptionAction);

			modePanel.add(Box.createGlue(), new GridBagConstraints(3, GridBagConstraints.RELATIVE, 1, 1, 1d, 1d,
					GridBagConstraints.CENTER, GridBagConstraints.NONE, LIST_ITEM_INNER_INSETS, 0, 0));

			if (Profile.DEFAULT_MODE.equals(mode) || OnScreenKeyboard.ON_SCREEN_KEYBOARD_MODE.equals(mode)) {
				descriptionTextField.setEditable(false);
				descriptionTextField.setFocusable(false);
				modePanel.add(Box.createHorizontalStrut(BUTTON_DIMENSION.width), buttonGridBagConstraints);
			} else {
				final var deleteButton = new JButton(new RemoveModeAction(mode));
				deleteButton.setPreferredSize(BUTTON_DIMENSION);
				modePanel.add(deleteButton, buttonGridBagConstraints);
			}
		}

		modesListPanel.add(Box.createGlue(), new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1d, 1d,
				GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

		modesScrollPane.setViewportView(modesListPanel);

		if (newModeAdded) {
			final var verticalScrollBar = modesScrollPane.getVerticalScrollBar();
			verticalScrollBar.setValue(verticalScrollBar.getMaximum());
		}
	}

	private void updateOverlayAlignment(final Rectangle totalDisplayBounds) {
		if (currentModeLabel != null) {
			overlayFrame.remove(currentModeLabel);
			overlayFrame.add(currentModeLabel,
					isOverlayInLowerHalf(totalDisplayBounds) ? BorderLayout.NORTH : BorderLayout.SOUTH);
		}

		overlayFrame.pack();

		if (IS_MAC) {
			final var overlayFrameContentPane = overlayFrame.getContentPane();
			overlayFrameContentPane.invalidate();
			overlayFrameContentPane.repaint();
		}
	}

	public void updateOverlayAxisIndicators(final boolean forceRepaint) {
		if (runMode == null || (!isLocalRunning() && !isServerRunning())) {
			return;
		}

		EnumSet.allOf(VirtualAxis.class).stream().filter(virtualAxisToProgressBarMap::containsKey)
				.forEach(virtualAxis -> {
					final var progressBar = virtualAxisToProgressBarMap.get(virtualAxis);
					var repaint = forceRepaint;

					final var minAxisValue = runMode.getMinAxisValue();
					final var maxAxisValue = runMode.getMaxAxisValue();
					final var negativeMinAxisValue = minAxisValue < 0;

					final var minimum = negativeMinAxisValue ? minAxisValue : -maxAxisValue;
					final var maximum = negativeMinAxisValue ? maxAxisValue : minAxisValue;

					final var newMaximum = maximum - minimum;
					if (progressBar.getMaximum() != newMaximum) {
						progressBar.setMaximum(newMaximum);
						repaint = true;
					}

					final var newValue = -input.getAxes().get(virtualAxis) - minimum - (negativeMinAxisValue ? 1 : 0);
					if (progressBar.getValue() != newValue) {
						progressBar.setValue(newValue);
						repaint = true;
					}

					if (repaint) {
						progressBar.repaint();
					}
				});
	}

	private void updateOverlayPanel() {
		if (indicatorsListPanel == null) {
			return;
		}

		indicatorsListPanel.removeAll();

		if (input == null) {
			return;
		}

		final var borderColor = UIManager.getColor("Component.borderColor");

		EnumSet.allOf(VirtualAxis.class).forEach(virtualAxis -> {
			final var indicatorPanel = new JPanel(new GridBagLayout());
			indicatorPanel.setBorder(LIST_ITEM_BORDER);
			indicatorsListPanel.add(indicatorPanel, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0d, 0d,
					GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL, GRID_BAG_ITEM_INSETS, 0, 0));

			final var virtualAxisLabel = new JLabel(
					MessageFormat.format(STRINGS.getString("AXIS_LABEL"), virtualAxis.toString()));
			virtualAxisLabel.setPreferredSize(OVERLAY_SETTINGS_LABEL_DIMENSION);
			indicatorPanel.add(virtualAxisLabel, new GridBagConstraints(0, 0, 1, 1, 0d, 0d, GridBagConstraints.CENTER,
					GridBagConstraints.NONE, LIST_ITEM_INNER_INSETS, 0, 0));

			final var virtualAxisToOverlayAxisMap = input.getProfile().getVirtualAxisToOverlayAxisMap();
			final var overlayAxis = virtualAxisToOverlayAxisMap.get(virtualAxis);
			final var enabled = overlayAxis != null;

			final var colorLabel = new JLabel();
			if (enabled) {
				colorLabel.setOpaque(true);
				colorLabel.setBackground(overlayAxis.color);
			} else {
				colorLabel.setText(STRINGS.getString("INDICATOR_DISABLED_LABEL"));
			}
			colorLabel.setHorizontalAlignment(SwingConstants.CENTER);

			colorLabel.setPreferredSize(OVERLAY_SETTINGS_LABEL_DIMENSION);
			colorLabel.setBorder(BorderFactory.createLineBorder(borderColor));
			indicatorPanel.add(colorLabel, new GridBagConstraints(1, 0, 1, 1, 0.2d, 0d, GridBagConstraints.CENTER,
					GridBagConstraints.NONE, LIST_ITEM_INNER_INSETS, 0, 0));

			final var colorButton = new JButton(new SelectIndicatorColorAction(virtualAxis));
			colorButton.setPreferredSize(SQUARE_BUTTON_DIMENSION);
			colorButton.setEnabled(enabled);
			indicatorPanel.add(colorButton, new GridBagConstraints(2, 0, 1, 1, 0d, 0d, GridBagConstraints.CENTER,
					GridBagConstraints.NONE, LIST_ITEM_INNER_INSETS, 0, 0));

			final var invertedCheckBox = new JCheckBox(new InvertIndicatorAction(virtualAxis));
			invertedCheckBox.setSelected(enabled && overlayAxis.inverted);
			invertedCheckBox.setEnabled(enabled);
			indicatorPanel.add(invertedCheckBox, new GridBagConstraints(3, 0, 1, 1, 1d, 0d, GridBagConstraints.CENTER,
					GridBagConstraints.NONE, LIST_ITEM_INNER_INSETS, 0, 0));

			final var displayCheckBox = new JCheckBox(new DisplayIndicatorAction(virtualAxis));
			displayCheckBox.setSelected(enabled);
			indicatorPanel.add(displayCheckBox, new GridBagConstraints(4, GridBagConstraints.RELATIVE, 1, 1, 0d, 0d,
					GridBagConstraints.CENTER, GridBagConstraints.NONE, LIST_ITEM_INNER_INSETS, 0, 0));
		});

		indicatorsListPanel.add(Box.createGlue(), new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1d, 1d,
				GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

		indicatorsScrollPane.setViewportView(indicatorsListPanel);
	}

	private void updateOverlayPosition() {
		EventQueue.invokeLater(() -> {
			if (!isModalDialogShowing()) {
				if (overlayFrame != null) {
					GuiUtils.makeWindowTopmost(overlayFrame);
				}

				if (onScreenKeyboard.isVisible()) {
					GuiUtils.makeWindowTopmost(onScreenKeyboard);
				}
			}

			totalDisplayBounds = GuiUtils.getTotalDisplayBounds();
			if (!totalDisplayBounds.equals(prevTotalDisplayBounds)) {
				prevTotalDisplayBounds = totalDisplayBounds;

				if (overlayFrame != null && overlayFrameDragListener != null
						&& !overlayFrameDragListener.isDragging()) {
					deInitOverlay();
					initOverlay();
				}

				onScreenKeyboard.updateLocation();
			}

			if (IS_WINDOWS) {
				repaintOnScreenKeyboardAndOverlay();
			} else if (IS_LINUX && currentModeLabel != null) {
				currentModeLabel.validate();
				currentModeLabel.repaint();
				updateOverlayAxisIndicators(true);
			}
		});
	}

	private void updatePanelAccess() {
		updatePanelAccess(isRunning());
	}

	private void updatePanelAccess(final boolean running) {
		GuiUtils.setEnabledRecursive(modesListPanel, !running);
		GuiUtils.setEnabledRecursive(newModePanel, !running);

		if (assignmentsComponent != null) {
			assignmentsComponent.setEnabled(!running);
		}

		if (running || (input != null && !input.getProfile().isShowOverlay())) {
			GuiUtils.setEnabledRecursive(indicatorsListPanel, false);
		} else {
			updateOverlayPanel();
		}

		GuiUtils.setEnabledRecursive(profileSettingsPanel, !running);
		if (!running) {
			updateProfileSettingsPanel();
		}
		GuiUtils.setEnabledRecursive(globalSettingsPanel, !running);
	}

	private void updateProfileSettingsPanel() {
		if (profileSettingsPanel == null) {
			return;
		}

		profileSettingsPanel.removeAll();

		if (input == null) {
			return;
		}

		final var constraints = new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0d, 0d,
				GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH, GRID_BAG_ITEM_INSETS, 0, 5);

		final var inputSettingsPanel = new JPanel();
		inputSettingsPanel.setLayout(new BoxLayout(inputSettingsPanel, BoxLayout.Y_AXIS));
		inputSettingsPanel
				.setBorder(BorderFactory.createTitledBorder(STRINGS.getString("INPUT_OUTPUT_SETTINGS_BORDER_TITLE")));
		profileSettingsPanel.add(inputSettingsPanel, constraints);

		final var keyRepeatIntervalPanel = new JPanel(DEFAULT_FLOW_LAYOUT);
		inputSettingsPanel.add(keyRepeatIntervalPanel, constraints);

		final var keyRepeatIntervalLabel = new JLabel(STRINGS.getString("KEY_REPEAT_INTERVAL_LABEL"));
		keyRepeatIntervalLabel.setPreferredSize(SETTINGS_LABEL_DIMENSION);
		keyRepeatIntervalPanel.add(keyRepeatIntervalLabel);

		final var profile = input.getProfile();

		final var keyRepeatIntervalSpinner = new JSpinner(
				new SpinnerNumberModel((int) profile.getKeyRepeatInterval(), 0, 1000, 1));
		final var keyRepeatIntervalEditor = new NumberEditor(keyRepeatIntervalSpinner,
				"# " + STRINGS.getString("MILLISECOND_SYMBOL"));
		((DefaultFormatter) keyRepeatIntervalEditor.getTextField().getFormatter()).setCommitsOnValidEdit(true);
		keyRepeatIntervalSpinner.setEditor(keyRepeatIntervalEditor);
		keyRepeatIntervalSpinner.addChangeListener(event -> {
			final var keyRepeatInterval = (int) ((JSpinner) event.getSource()).getValue();
			input.getProfile().setKeyRepeatInterval(keyRepeatInterval);
			setUnsavedChanges(true);
		});
		keyRepeatIntervalPanel.add(keyRepeatIntervalSpinner);

		final var appearanceSettingsPanel = new JPanel();
		appearanceSettingsPanel.setLayout(new BoxLayout(appearanceSettingsPanel, BoxLayout.Y_AXIS));
		appearanceSettingsPanel
				.setBorder(BorderFactory.createTitledBorder(STRINGS.getString("APPEARANCE_SETTINGS_BORDER_TITLE")));
		constraints.gridx = 1;
		profileSettingsPanel.add(appearanceSettingsPanel, constraints);

		final var overlaySettingsPanel = new JPanel(DEFAULT_FLOW_LAYOUT);
		appearanceSettingsPanel.add(overlaySettingsPanel, constraints);

		final var overlayLabel = new JLabel(STRINGS.getString("OVERLAY_LABEL"));
		overlayLabel.setPreferredSize(SETTINGS_LABEL_DIMENSION);
		overlaySettingsPanel.add(overlayLabel);

		final var showOverlayCheckBox = new JCheckBox(STRINGS.getString("SHOW_OVERLAY_CHECK_BOX"));
		showOverlayCheckBox.setSelected(profile.isShowOverlay());
		showOverlayCheckBox.addActionListener(event -> {
			profile.setShowOverlay(((JCheckBox) event.getSource()).isSelected());

			updatePanelAccess();
			setUnsavedChanges(true);
		});
		overlaySettingsPanel.add(showOverlayCheckBox);

		addGlueToSettingsPanel(profileSettingsPanel);
	}

	private void updateShowTrayEntry() {
		if (showTrayEntry == 0L) {
			return;
		}

		final var enabled = !frame.isVisible();
		mainLoop.runSync(() -> SDLTray.SDL_SetTrayEntryEnabled(showTrayEntry, enabled));
	}

	private void updateSvgElements(final SVGDocument svgDocument, final String idPrefix,
			final List<? extends IAction<?>> actions, final boolean darkTheme, final boolean swapped) {
		final var groupElement = (SVGStylableElement) svgDocument.getElementById(idPrefix + "Group");
		if (groupElement == null) {
			throw new IllegalArgumentException("Parameter idPrefix has invalid value: " + idPrefix);
		}

		final var hide = actions == null || actions.isEmpty();
		groupElement.getStyle().setProperty(CSSConstants.CSS_DISPLAY_PROPERTY,
				hide ? CSSConstants.CSS_NONE_VALUE : CSSConstants.CSS_INLINE_VALUE, "");

		if (hide) {
			return;
		}

		final var delayedActions = new ArrayList<ILongPressAction<?>>();
		final var onReleaseActions = new ArrayList<IActivatableAction<?>>();
		final var otherActions = new ArrayList<IAction<?>>();

		for (final var action : actions) {
			var addToOtherActions = true;

			if (action instanceof final ILongPressAction<?> longPressAction && longPressAction.isLongPress()) {
				delayedActions.add(longPressAction);
				addToOtherActions = false;
			}
			if (action instanceof final IActivatableAction<?> activatableAction
					&& activatableAction.getActivation() == Activation.SINGLE_ON_RELEASE) {
				onReleaseActions.add(activatableAction);
				addToOtherActions = false;
			}

			if (addToOtherActions) {
				otherActions.add(action);
			}
		}

		final List<? extends IAction<?>> actionGroupA;
		final List<? extends IAction<?>> actionGroupB;
		final String groupAPrefix;
		final String groupBPrefix;

		// noinspection SuspiciousMethodCalls
		if (delayedActions.isEmpty() || delayedActions.containsAll(actions)) {
			actionGroupA = Stream.concat(otherActions.stream(), delayedActions.stream()).toList();
			actionGroupB = onReleaseActions;
			groupAPrefix = "VISUALIZATION_ON_PRESS_PREFIX";
			groupBPrefix = "VISUALIZATION_ON_RELEASE_PREFIX";
		} else {
			actionGroupA = Stream.concat(otherActions.stream(), onReleaseActions.stream()).toList();
			actionGroupB = delayedActions;
			groupAPrefix = "VISUALIZATION_SHORT_PREFIX";
			groupBPrefix = "VISUALIZATION_LONG_PREFIX";
		}

		final var groupBPresent = !actionGroupB.isEmpty();
		final var bothGroupsPresent = !actionGroupA.isEmpty() && groupBPresent;

		final var textElement = (SVGStylableElement) svgDocument.getElementById(idPrefix + "Text");
		final var tSpanNode = textElement.getFirstChild();
		tSpanNode.setTextContent(null);

		if (bothGroupsPresent) {
			addTSpanElement("• " + STRINGS.getString(groupAPrefix) + ": ", true, tSpanNode);
		}

		addTSpanElement(actionGroupA, tSpanNode);

		if (bothGroupsPresent) {
			addTSpanElement(" • " + STRINGS.getString(groupBPrefix) + ": ", true, tSpanNode);
		}

		addTSpanElement(actionGroupB, tSpanNode);

		if (swapped) {
			addTSpanElement(" " + SWAPPED_SYMBOL, true, tSpanNode);
		}

		if (darkTheme) {
			textElement.getStyle().setProperty(CSSConstants.CSS_FILL_PROPERTY, SVG_DARK_THEME_TEXT_COLOR, "");

			final var pathElement = (SVGStylableElement) svgDocument.getElementById(idPrefix + "Path");
			pathElement.getStyle().setProperty(CSSConstants.CSS_STROKE_PROPERTY, SVG_DARK_THEME_PATH_COLOR, "");
		}

		final var rootElement = svgDocument.getRootElement();
		final var bBox = rootElement.getBBox();

		final var halfMargin = SVG_VIEWBOX_MARGIN / 2;

		final var viewBoxX = bBox.getX() - halfMargin;
		final var viewBoxY = bBox.getY() - halfMargin;
		final var viewBoxWidth = bBox.getWidth() + SVG_VIEWBOX_MARGIN;
		final var viewBoxHeight = bBox.getHeight() + SVG_VIEWBOX_MARGIN;

		rootElement.setAttributeNS(null, "viewBox",
				viewBoxX + " " + viewBoxY + " " + viewBoxWidth + " " + viewBoxHeight);
	}

	private void updateTheme() {
		lookAndFeel = switch (getSelectedTheme()) {
		case SYSTEM -> {
			if (sdlVideoInitialized) {
				final var optionalSystemTheme = mainLoop.runSync(SDLVideo::SDL_GetSystemTheme);
				if (optionalSystemTheme.isPresent() && optionalSystemTheme.get() == SDLVideo.SDL_SYSTEM_THEME_DARK) {
					yield new FlatDarkLaf();
				}
			}

			yield new FlatLightLaf();
		}
		case LIGHT -> new FlatLightLaf();
		case DARK -> new FlatDarkLaf();
		};

		try {
			UIManager.setLookAndFeel(lookAndFeel);
		} catch (final UnsupportedLookAndFeelException e) {
			throw new RuntimeException(e);
		}

		FlatLaf.updateUI();

		profileFileChooser.updateUI();

		updateVisualizationPanel();
	}

	public void updateTitle() {
		final String title;
		final var profileTitle = (unsavedChanges ? "*" : "")
				+ (loadedProfile != null ? loadedProfile : STRINGS.getString("UNTITLED"));
		title = MessageFormat.format(STRINGS.getString("MAIN_FRAME_TITLE"), profileTitle, Constants.APPLICATION_NAME);

		frame.setTitle(title);
		if (IS_LINUX) {
			final var toolkit = Toolkit.getDefaultToolkit();
			if (isXToolkit(toolkit)) {
				final var toolkitClass = toolkit.getClass();
				try {
					final var awtAppClassName = toolkitClass.getDeclaredField("awtAppClassName");
					awtAppClassName.setAccessible(true);
					awtAppClassName.set(null, title);
				} catch (final NoSuchFieldException | SecurityException | IllegalArgumentException
						| IllegalAccessException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
	}

	public void updateTitleAndTooltip() {
		updateTitle();

		if (tray != 0L && mainLoop.isAvailable()) {
			try {
				mainLoop.runSync(() -> updateTrayIconToolTip(-1));
			} catch (final IllegalStateException _) {
				// ignore main loop is no longer able to process tasks
			}
		}
	}

	private void updateTouchpadSettings() {
		final var enableTouchpad = isTouchpadEnabled();

		GuiUtils.setEnabledRecursive(touchpadCursorSensitivityPanel, enableTouchpad);
		GuiUtils.setEnabledRecursive(touchpadScrollSensitivityPanel, enableTouchpad);
	}

	private void updateTrayEntriesEnabled() {
		final var running = isRunning();

		if (startLocalTrayEntry != 0L) {
			SDLTray.SDL_SetTrayEntryEnabled(startLocalTrayEntry, !running);
		}
		if (startClientTrayEntry != 0L) {
			SDLTray.SDL_SetTrayEntryEnabled(startClientTrayEntry, !running);
		}
		if (startServerTrayEntry != 0L) {
			SDLTray.SDL_SetTrayEntryEnabled(startServerTrayEntry, !running);
		}
		if (stopTrayEntry != 0L) {
			SDLTray.SDL_SetTrayEntryEnabled(stopTrayEntry, running);
		}
	}

	public void updateTrayIconToolTip(final int batteryPercent) {
		checkMainThread();

		if (tray == 0L) {
			return;
		}

		var tooltip = frame.getTitle();
		if (batteryPercent >= 0 && batteryPercent <= 100) {
			tooltip = MessageFormat.format(STRINGS.getString("BATTERY_TOOLTIP_PERCENT"), tooltip, batteryPercent);
		}

		SDLTray.SDL_SetTrayTooltip(tray, tooltip);
	}

	void updateVisualizationPanel() {
		if (visualizationPanel == null || svgCanvas == null || input == null) {
			return;
		}

		final var modes = input.getProfile().getModes();
		final var model = new DefaultComboBoxModel<>(modes.toArray(Mode[]::new));

		if (modeComboBox == null) {
			modeComboBox = GuiUtils.addModePanel(visualizationPanel, modes, new AbstractAction() {

				@Serial
				private static final long serialVersionUID = -9107064465015662054L;

				@SuppressWarnings("unchecked")
				@Override
				public void actionPerformed(final ActionEvent e) {
					final var selectedMode = (Mode) ((JComboBox<Mode>) e.getSource()).getSelectedItem();

					final var workingCopySvgDocument = generateSvgDocument(selectedMode, lookAndFeel.isDark());

					svgCanvas.setSVGDocument(workingCopySvgDocument);
				}
			});
		}

		modeComboBox.setModel(model);
		modeComboBox.setSelectedIndex(model.getSize() > 0 ? 0 : -1);

		svgCanvas.setBackground(UIManager.getColor("Panel.background"));
	}

	public enum HotSwappingButton {

		NONE(-1, "NONE"), A(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH, "A_BUTTON"),
		B(SDLGamepad.SDL_GAMEPAD_BUTTON_EAST, "B_BUTTON"), X(SDLGamepad.SDL_GAMEPAD_BUTTON_WEST, "X_BUTTON"),
		Y(SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH, "Y_BUTTON"),
		LEFT_SHOULDER(SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER, "LEFT_SHOULDER"),
		RIGHT_SHOULDER(SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER, "RIGHT_SHOULDER"),
		BACK(SDLGamepad.SDL_GAMEPAD_BUTTON_BACK, "BACK_BUTTON"),
		START(SDLGamepad.SDL_GAMEPAD_BUTTON_START, "START_BUTTON"),
		GUIDE(SDLGamepad.SDL_GAMEPAD_BUTTON_GUIDE, "GUIDE_BUTTON"),
		LEFT_STICK(SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK, "LEFT_STICK"),
		RIGHT_STICK(SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK, "RIGHT_STICK"),
		DPAD_UP(SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_UP, "DPAD_UP"),
		DPAD_RIGHT(SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT, "DPAD_RIGHT"),
		DPAD_DOWN(SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_DOWN, "DPAD_DOWN"),
		DPAD_LEFT(SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_LEFT, "DPAD_LEFT");

		public final int id;

		private final String label;

		HotSwappingButton(final int id, final String labelKey) {
			this.id = id;
			label = STRINGS.getString(labelKey);
		}

		private static HotSwappingButton fromId(final int id) {
			return EnumSet.allOf(HotSwappingButton.class).stream()
					.filter(hotSwappingButton -> hotSwappingButton.id == id).findFirst().orElse(NONE);
		}

		@Override
		public String toString() {
			return label;
		}
	}

	private enum RunModeType {
		NONE, LOCAL, CLIENT, SERVER
	}

	private enum Theme {

		SYSTEM(0, "THEME_SYSTEM"), LIGHT(1, "THEME_LIGHT"), DARK(2, "THEME_DARK");

		private final int id;

		private final String label;

		Theme(final int id, final String labelKey) {
			this.id = id;
			label = STRINGS.getString(labelKey);
		}

		private static Theme fromId(final int id) {
			return EnumSet.allOf(Theme.class).stream().filter(theme -> theme.id == id).findFirst().orElse(SYSTEM);
		}

		@Override
		public String toString() {
			return label;
		}
	}

	@DBusInterfaceName("org.gnome.Shell.Extensions")
	private interface Extensions extends DBusInterface {

		Map<String, Map<String, Variant<?>>> ListExtensions();

		@DBusBoundProperty(access = Access.READ, name = "ShellVersion")
		String getShellVersion();
	}

	private abstract static class AbstractProfileFileChooser extends JFileChooser {

		@Serial
		private static final long serialVersionUID = -4669170626378955605L;

		private AbstractProfileFileChooser(final FileFilter fileFilter) {
			setFileFilter(fileFilter);
		}

		@SuppressWarnings("fallthrough")
		@Override
		public void approveSelection() {
			final var file = getSelectedFile();
			if (file.exists() && getDialogType() == SAVE_DIALOG) {
				final var selectedOption = JOptionPane.showConfirmDialog(this,
						MessageFormat.format(file.getName(), STRINGS.getString("FILE_EXISTS_DIALOG_TEXT")),
						STRINGS.getString("FILE_EXISTS_DIALOG_TITLE"), JOptionPane.YES_NO_CANCEL_OPTION);
				switch (selectedOption) {
				case JOptionPane.CANCEL_OPTION:
					cancelSelection();
				case JOptionPane.NO_OPTION, JOptionPane.CLOSED_OPTION:
					return;
				default:
					break;
				}
			}
			super.approveSelection();
		}
	}

	public record Controller(int instanceId, String name, String guid) {

		private Controller(final int instanceId) {
			this(instanceId, SDLGamepad.SDL_GetGamepadNameForID(instanceId), prepareGuid(instanceId));
		}

		private static String prepareGuid(final int instanceId) {
			String guid;
			try (final var stack = MemoryStack.stackPush()) {
				final var sdlGuid = SDL_GUID.malloc(stack);
				SDLGamepad.SDL_GetGamepadGUIDForID(instanceId, sdlGuid);
				final var guidByteBuffer = stack.calloc(33);
				SDLGUID.SDL_GUIDToString(sdlGuid, guidByteBuffer);
				guid = StandardCharsets.UTF_8.decode(guidByteBuffer).toString();
				final var nullPos = guid.indexOf('\u0000');
				if (nullPos != -1) {
					guid = guid.substring(0, nullPos);
				}
			}

			return guid;
		}
	}

	private static final class HtmlFileChooser extends AbstractProfileFileChooser {

		@Serial
		private static final long serialVersionUID = -1707951153902772391L;

		private HtmlFileChooser(final File profileFile) {
			super(new FileNameExtensionFilter(STRINGS.getString("HTML_FILE_DESCRIPTION"), "htm", "html"));

			String filename;
			if (profileFile != null) {
				filename = profileFile.getName();
				filename = filename.substring(0, filename.lastIndexOf('.'));
			} else {
				filename = STRINGS.getString("UNTITLED");
			}

			setSelectedFile(new File(filename + ".html"));
		}
	}

	private static final class IndicatorProgressBar extends JProgressBar {

		@Serial
		private static final long serialVersionUID = 8167193907929992395L;

		@SuppressWarnings({ "serial", "RedundantSuppression" })
		private final Set<Float> detentValues;

		@SuppressWarnings({ "serial", "RedundantSuppression" })
		private final OverlayAxis overlayAxis;

		private final int subdivisionHeight;

		private IndicatorProgressBar(final Set<Float> detentValues, final OverlayAxis overlayAxis) {
			super(VERTICAL);

			setBorder(createOverlayBorder());
			this.detentValues = detentValues;
			this.overlayAxis = overlayAxis;
			subdivisionHeight = Math.round(main.getOverlayScaling());
		}

		@Override
		protected void paintComponent(final Graphics g) {
			super.paintComponent(g);

			final var width = getWidth();
			final var height = getHeight();

			final var subdivisions = 3;
			for (var i = 1; i <= subdivisions; i++) {
				g.setColor(Color.WHITE);
				final var y = i * (height / (subdivisions + 1));
				g.fillRect(0, y - subdivisionHeight / 2, width, subdivisionHeight);
			}

			detentValues.forEach(detentValue -> {
				g.setColor(Color.RED);
				final var y = (int) Input.normalize(detentValue, -1f, 1f, 0, height);
				g.fillRect(0, y - subdivisionHeight / 2, width, subdivisionHeight);
			});
		}

		@Serial
		private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(IndicatorProgressBar.class.getName());
		}

		@Override
		public void setMaximum(final int n) {
			if (overlayAxis.inverted) {
				super.setMinimum(-n);
			} else {
				super.setMaximum(n);
			}
		}

		@Override
		public void setMinimum(final int n) {
			if (overlayAxis.inverted) {
				super.setMaximum(-n);
			} else {
				super.setMinimum(n);
			}
		}

		@Override
		public void setValue(final int n) {
			super.setValue(overlayAxis.inverted ? -n : n);
		}

		@Serial
		private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(IndicatorProgressBar.class.getName());
		}
	}

	private record JsonContext(@SuppressWarnings("unused") Gson gson,
			@SuppressWarnings("unused") ActionTypeAdapter actionTypeAdapter) {

		private static JsonContext create() {
			final var actionAdapter = new ActionTypeAdapter();
			final var gson = new GsonBuilder().registerTypeAdapterFactory(new ProfileTypeAdapterFactory())
					.registerTypeAdapter(Color.class, new ColorTypeAdapter())
					.registerTypeAdapter(IAction.class, actionAdapter)
					.registerTypeAdapter(LockKey.class, new LockKeyAdapter())
					.registerTypeAdapter(ScanCode.class, new ScanCodeAdapter()).setPrettyPrinting().create();

			return new JsonContext(gson, actionAdapter);
		}
	}

	private static final class LimitedLengthPlainDocument extends PlainDocument {

		@Serial
		private static final long serialVersionUID = 6672096787814740118L;

		private final int limit;

		private LimitedLengthPlainDocument(final int limit) {
			this.limit = limit;
		}

		@Override
		public void insertString(final int offs, final String str, final AttributeSet a) throws BadLocationException {
			if (str == null) {
				return;
			}

			if ((getLength() + str.length()) <= limit) {
				super.insertString(offs, str, a);
			}
		}
	}

	public static final class MainLoop {

		private final BlockingQueue<TaskQueueEntry> taskQueue = new LinkedBlockingDeque<>();

		private final Thread thread = Thread.currentThread();

		private volatile TaskQueueEntry currentTaskQueueEntry;

		private volatile boolean pollSdlEvents;

		private volatile boolean shuttingDown;

		private MainLoop() {
		}

		private void addTaskQueueEntry(final TaskQueueEntry taskQueueEntry) {
			if (!thread.isAlive()) {
				throw new IllegalStateException("Main loop thread is not alive");
			}

			if (shuttingDown) {
				throw new IllegalStateException("Main loop is shutting down");
			}

			synchronized (this) {
				taskQueue.add(taskQueueEntry);
				notifyAll();
			}
		}

		private void enterLoop() {
			LOGGER.log(Level.INFO, "Entering main loop");
			try {
				while (!Thread.interrupted()) {
					currentTaskQueueEntry = taskQueue.poll();
					if (currentTaskQueueEntry != null) {
						try {
							executeTaskQueueEntry(currentTaskQueueEntry);
						} finally {
							currentTaskQueueEntry = null;
						}
					} else if (pollSdlEvents) {
						main.pollSdlEvents();

						try {
							// noinspection BusyWait
							Thread.sleep(10L);
						} catch (final InterruptedException _) {
							return;
						}
					} else {
						try {
							synchronized (this) {
								if ( taskQueue.isEmpty() && !pollSdlEvents) {
									wait();
								}
							}
						} catch (final InterruptedException _) {
							return;
						}
					}
				}
			} catch (final Throwable t) {
				synchronized (this) {
					while (!taskQueue.isEmpty()) {
						final var taskQueueEntry = taskQueue.poll();
						if (taskQueueEntry.futureResult != null) {
							taskQueueEntry.futureResult.completeExceptionally(
									new Exception("Task aborted due to uncaught exception in previous task", t));
						}
					}
				}

				throw t;
			} finally {
				shuttingDown = true;

				try {
					SDLInit.SDL_Quit();
				} catch (final Throwable t) {
					LOGGER.log(Level.SEVERE, t.getMessage(), t);
				}

				LOGGER.log(Level.INFO, "Exiting main loop");
			}
		}

		private void executeTaskQueueEntry(final TaskQueueEntry taskQueueEntry) {
			try {
				if (taskQueueEntry.task instanceof final Callable<?> callable) {
					final var result = callable.call();
					taskQueueEntry.futureResult.complete(result);
				} else if (taskQueueEntry.task instanceof final Runnable runnable) {
					runnable.run();
					if (taskQueueEntry.futureResult != null) {
						taskQueueEntry.futureResult.complete(null);
					}
				}
			} catch (final Throwable t) {
				if (taskQueueEntry.futureResult != null) {
					taskQueueEntry.futureResult.completeExceptionally(t);
				} else {
					shuttingDown = true;
					throw new RuntimeException(t);
				}
			}
		}

		private boolean isAvailable() {
			return thread.isAlive() && !shuttingDown;
		}

		private boolean isTaskOfTypeRunning(final Class<?> clazz) {
			if (currentTaskQueueEntry == null) {
				return false;
			}

			return clazz.isAssignableFrom(currentTaskQueueEntry.task.getClass());
		}

		private void runAsync(final Runnable runnable) {
			addTaskQueueEntry(new TaskQueueEntry(runnable, null));
		}

		private void runSync(final Runnable runnable) {
			final var futureResult = new CompletableFuture<>();
			addTaskQueueEntry(new TaskQueueEntry(runnable, futureResult));

			try {
				futureResult.get();
			} catch (final ExecutionException e) {
				throw new RuntimeException(e);
			} catch (final InterruptedException _) {
				Thread.currentThread().interrupt();
			}
		}

		@SuppressWarnings("unchecked")
		private <V> Optional<V> runSync(final Callable<V> callable) {
			final var futureResult = new CompletableFuture<>();
			addTaskQueueEntry(new TaskQueueEntry(callable, futureResult));

			try {
				return Optional.ofNullable((V) futureResult.get());
			} catch (final ExecutionException e) {
				throw new RuntimeException(e);
			} catch (final InterruptedException _) {
				Thread.currentThread().interrupt();
			}

			return Optional.empty();
		}

		private void shutdown() {
			pollSdlEvents = false;

			thread.interrupt();

			while (thread.isAlive()) {
				try {
					// noinspection BusyWait
					Thread.sleep(10L);
				} catch (final InterruptedException _) {
					Thread.currentThread().interrupt();
				}
			}
		}

		private void startSdlEventPolling() {
			synchronized (this) {
				pollSdlEvents = true;
				notifyAll();
			}
		}

		private void stopTask() {
			thread.interrupt();

			waitForTask();
		}

		private void waitForTask() {
			while (currentTaskQueueEntry != null || !taskQueue.isEmpty()) {
				try {
					// noinspection BusyWait
					Thread.sleep(10L);
				} catch (final InterruptedException _) {
					Thread.currentThread().interrupt();
				}
			}
		}

		@SuppressWarnings("NamedLikeContextualKeyword")
		public void yield() {
			if (Thread.currentThread() != thread) {
				throw new RuntimeException("yield() can only be called on " + thread.getName());
			}

			for (;;) {
				final var taskQueueEntry = taskQueue.poll();
				if (taskQueueEntry == null) {
					break;
				}
				executeTaskQueueEntry(taskQueueEntry);
			}
		}

		private record TaskQueueEntry(Object task, CompletableFuture<Object> futureResult) {
		}
	}

	private static final class ShowWebsiteAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -9029607010261185834L;

		private ShowWebsiteAction() {
			putValue(NAME, STRINGS.getString("SHOW_WEBSITE_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, MessageFormat.format(STRINGS.getString("SHOW_WEBSITE_ACTION_DESCRIPTION"),
					Constants.APPLICATION_NAME));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			try {
				openBrowser(main.frame, new URI(WEBSITE_URL));
			} catch (final URISyntaxException e1) {
				throw new RuntimeException(e1);
			}
		}
	}

	private final class ChangeVJoyDirectoryAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -7672382299595684105L;

		private ChangeVJoyDirectoryAction() {
			putValue(NAME, "...");
			putValue(SHORT_DESCRIPTION, STRINGS.getString("CHANGE_VJOY_DIRECTORY_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final var vJoyDirectoryFileChooser = new JFileChooser(getVJoyDirectory());
			vJoyDirectoryFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			if (vJoyDirectoryFileChooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
				return;
			}

			final var vjoyDirectory = vJoyDirectoryFileChooser.getSelectedFile();
			final var dllFile = new File(vjoyDirectory,
					VjoyInterface.GetVJoyArchFolderName() + File.separator + VjoyInterface.VJOY_LIBRARY_FILENAME);
			if (!dllFile.exists()) {
				GuiUtils.showMessageDialog(main, frame,
						MessageFormat.format(STRINGS.getString("INVALID_VJOY_DIRECTORY_DIALOG_TEXT"),
								getDefaultVJoyPath()),
						STRINGS.getString("ERROR_DIALOG_TITLE"), JOptionPane.ERROR_MESSAGE);
				return;
			}

			final var oldVjoyPath = getVJoyDirectory();
			final var newVjoyPath = vjoyDirectory.getAbsolutePath();

			if (Objects.equals(oldVjoyPath, newVjoyPath)) {
				return;
			}

			preferences.put(PREFERENCES_VJOY_DIRECTORY, newVjoyPath);
			vJoyDirectoryLabel.setText(newVjoyPath);

			if (VjoyInterface.isInitialized() && JOptionPane.showConfirmDialog(frame,
					MessageFormat.format(STRINGS.getString("RESTART_REQUIRED_DIALOG_TEXT"), Constants.APPLICATION_NAME),
					STRINGS.getString("INFORMATION_DIALOG_TITLE"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION
					&& handleUnsavedChanges()) {
				quit();
			}
		}
	}

	private abstract class ConnectAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = 3506732842101613495L;

		private final boolean withHost;

		private ConnectAction(final boolean withHost) {
			this.withHost = withHost;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			showConnectDialog();
		}

		abstract void proceed();

		private void showConnectDialog() {
			final var connectionSettingsPanel = new ConnectionSettingsPanel(withHost);

			executeWhileVisible(() -> {
				if (JOptionPane.showConfirmDialog(frame, connectionSettingsPanel,
						STRINGS.getString("CONNECT_DIALOG_TITLE"), JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
					final var errorMessage = connectionSettingsPanel.saveSettings();

					if (errorMessage == null) {
						proceed();
					} else {
						GuiUtils.showMessageDialog(main, frame, errorMessage, STRINGS.getString("ERROR_DIALOG_TITLE"),
								JOptionPane.ERROR_MESSAGE);
						showConnectDialog();
					}
				}
			});
		}
	}

	private class ConnectionSettingsPanel extends JPanel {

		private static final int TEXT_FIELD_COLUMNS = 15;

		@Serial
		private static final long serialVersionUID = 2959405425250777631L;

		private final JPasswordField passwordPasswordField;

		private final JSpinner portSpinner;

		private final JSpinner timeoutSpinner;

		private JTextField hostTextField;

		private ConnectionSettingsPanel(final boolean withHost) {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			if (withHost) {
				final var hostPanel = new JPanel(DEFAULT_FLOW_LAYOUT);
				add(hostPanel);

				final var hostLabel = new JLabel(STRINGS.getString("HOST_LABEL"));
				hostLabel.setPreferredSize(CONNECTION_SETTINGS_LABEL_DIMENSION);
				hostPanel.add(hostLabel);

				final var host = getHost();
				hostTextField = GuiUtils.createTextFieldWithMenu(new LimitedLengthPlainDocument(64), host,
						TEXT_FIELD_COLUMNS);
				hostTextField.setCaretPosition(0);
				hostPanel.add(hostTextField);

				if (host == null || host.isBlank()) {
					hostTextField.grabFocus();
				}
			}

			final var portPanel = new JPanel(DEFAULT_FLOW_LAYOUT);
			add(portPanel);

			final var portLabel = new JLabel(STRINGS.getString("PORT_LABEL"));
			portLabel.setPreferredSize(CONNECTION_SETTINGS_LABEL_DIMENSION);
			portPanel.add(portLabel);

			portSpinner = new JSpinner(new SpinnerNumberModel(getPort(), 1024, 65_535, 1));
			final var portSpinnerEditor = new NumberEditor(portSpinner, "#");
			((DefaultFormatter) portSpinnerEditor.getTextField().getFormatter()).setCommitsOnValidEdit(true);
			portSpinner.setEditor(portSpinnerEditor);
			portPanel.add(portSpinner);

			final var timeoutPanel = new JPanel(DEFAULT_FLOW_LAYOUT);
			add(timeoutPanel);

			final var timeoutLabel = new JLabel(STRINGS.getString("TIMEOUT_LABEL"));
			timeoutLabel.setPreferredSize(CONNECTION_SETTINGS_LABEL_DIMENSION);
			timeoutPanel.add(timeoutLabel);

			timeoutSpinner = new JSpinner(new SpinnerNumberModel(getTimeout(), 10, 60_000, 1));
			final var timeoutSpinnerEditor = new NumberEditor(timeoutSpinner,
					"# " + STRINGS.getString("MILLISECOND_SYMBOL"));
			((DefaultFormatter) timeoutSpinnerEditor.getTextField().getFormatter()).setCommitsOnValidEdit(true);
			timeoutSpinner.setEditor(timeoutSpinnerEditor);
			timeoutPanel.add(timeoutSpinner);

			final var passwordPanel = new JPanel(DEFAULT_FLOW_LAYOUT);
			add(passwordPanel);

			final var passwordLabel = new JLabel(STRINGS.getString("PASSWORD_LABEL"));
			passwordLabel.setPreferredSize(CONNECTION_SETTINGS_LABEL_DIMENSION);
			passwordPanel.add(passwordLabel);

			final var password = getPassword();
			passwordPasswordField = new JPasswordField(new LimitedLengthPlainDocument(PASSWORD_MAX_LENGTH), password,
					TEXT_FIELD_COLUMNS);
			passwordPasswordField.setCaretPosition(0);
			passwordPanel.add(passwordPasswordField);
		}

		private String saveSettings() {
			if (hostTextField != null) {
				final var host = hostTextField.getText().strip();
				if (!isValidHost(host)) {
					return STRINGS.getString("NO_HOST_ADDRESS_ERROR_DIALOG_TEXT");
				}
				preferences.put(PREFERENCES_HOST, host);
			}

			preferences.putInt(PREFERENCES_PORT, (int) portSpinner.getValue());
			preferences.putInt(PREFERENCES_TIMEOUT, (int) timeoutSpinner.getValue());

			final var password = new String(passwordPasswordField.getPassword());
			if (!isValidPassword(password)) {
				return MessageFormat.format(STRINGS.getString("INVALID_PASSWORD_ERROR_DIALOG_TEXT"),
						PASSWORD_MIN_LENGTH, PASSWORD_MAX_LENGTH);
			}
			preferences.put(PREFERENCES_PASSWORD, password);

			return null;
		}
	}

	private final class DisplayIndicatorAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = 3316770144012465987L;

		private final VirtualAxis virtualAxis;

		private DisplayIndicatorAction(final VirtualAxis virtualAxis) {
			this.virtualAxis = virtualAxis;

			putValue(NAME, STRINGS.getString("DISPLAY_INDICATOR_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, STRINGS.getString("DISPLAY_INDICATOR_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (((JCheckBox) e.getSource()).isSelected()) {
				input.getProfile().getVirtualAxisToOverlayAxisMap().put(virtualAxis, new OverlayAxis());
			} else {
				input.getProfile().getVirtualAxisToOverlayAxisMap().remove(virtualAxis);
			}

			setUnsavedChanges(true);
			updateOverlayPanel();
		}
	}

	private final class ExportAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -6582801831348704984L;

		private ExportAction() {
			putValue(NAME, STRINGS.getString("EXPORT_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, STRINGS.getString("EXPORT_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final var htmlFileChooser = new HtmlFileChooser(currentFile);

			if (htmlFileChooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) {
				return;
			}

			exportVisualization(htmlFileChooser.getSelectedFile());
		}
	}

	private final class InvertIndicatorAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = 3316770144012465987L;

		private final VirtualAxis virtualAxis;

		private InvertIndicatorAction(final VirtualAxis virtualAxis) {
			this.virtualAxis = virtualAxis;

			putValue(NAME, STRINGS.getString("INVERT_INDICATOR_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, STRINGS.getString("INVERT_INDICATOR_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			input.getProfile().getVirtualAxisToOverlayAxisMap().get(virtualAxis).inverted = ((JCheckBox) e.getSource())
					.isSelected();

			setUnsavedChanges(true);
			updateOverlayPanel();
		}
	}

	private final class NewAction extends UnsavedChangesAwareAction {

		@Serial
		private static final long serialVersionUID = 5703987691203427504L;

		private NewAction() {
			putValue(NAME, STRINGS.getString("NEW_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, STRINGS.getString("NEW_ACTION_DESCRIPTION"));
		}

		@Override
		protected void doAction() {
			newProfile(true);
		}
	}

	private final class NewModeAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -4881923833724315489L;

		private NewModeAction() {
			putValue(NAME, STRINGS.getString("NEW_MODE_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, STRINGS.getString("NEW_MODE_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final var mode = new Mode();
			input.getProfile().getModes().add(mode);

			setUnsavedChanges(true);
			updateModesPanel(true);
		}
	}

	private final class OpenAction extends UnsavedChangesAwareAction {

		@Serial
		private static final long serialVersionUID = -8932510785275935297L;

		private OpenAction() {
			putValue(NAME, STRINGS.getString("OPEN_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, STRINGS.getString("OPEN_ACTION_DESCRIPTION"));
		}

		@Override
		protected void doAction() {
			executeWhileVisible(() -> {
				if (profileFileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
					loadProfile(profileFileChooser.getSelectedFile(), false, true);
				}
			});
		}
	}

	private final class ProfileFileChooser extends AbstractProfileFileChooser {

		@Serial
		private static final long serialVersionUID = -4669170626378955605L;

		private ProfileFileChooser() {
			super(new FileNameExtensionFilter(
					MessageFormat.format(STRINGS.getString("PROFILE_FILE_DESCRIPTION"), Constants.APPLICATION_NAME),
					PROFILE_FILE_EXTENSION));

			resetSelectedFile();
		}

		private void resetSelectedFile() {
			String profileDirectoryPath = null;

			if (currentFile != null && currentFile.getParentFile().isDirectory()) {
				profileDirectoryPath = currentFile.getAbsolutePath();
			} else {
				final var controllerBuddyProfilesDir = System.getenv("CONTROLLER_BUDDY_PROFILES_DIR");

				if (controllerBuddyProfilesDir != null && !controllerBuddyProfilesDir.isBlank()) {
					final var file = new File(controllerBuddyProfilesDir);
					if (file.isDirectory()) {
						profileDirectoryPath = file.getAbsolutePath();
					}
				}
			}

			if (profileDirectoryPath != null && !profileDirectoryPath.endsWith(File.separator)) {
				profileDirectoryPath += File.separator;
			}

			setSelectedFile(new File(profileDirectoryPath + "*" + PROFILE_FILE_SUFFIX));
		}
	}

	private final class QuitAction extends UnsavedChangesAwareAction {

		@Serial
		private static final long serialVersionUID = 8952460723177800923L;

		private QuitAction() {
			putValue(NAME, STRINGS.getString("QUIT_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION,
					MessageFormat.format(STRINGS.getString("QUIT_ACTION_DESCRIPTION"), Constants.APPLICATION_NAME));
		}

		@Override
		protected void doAction() {
			quit();
		}
	}

	private final class RemoveModeAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -1056071724769862582L;

		@SuppressWarnings({ "serial", "RedundantSuppression" })
		private final Mode mode;

		private RemoveModeAction(final Mode mode) {
			this.mode = mode;

			putValue(NAME, STRINGS.getString("REMOVE_MODE_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION,
					MessageFormat.format(STRINGS.getString("REMOVE_MODE_ACTION_DESCRIPTION"), mode.getDescription()));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			input.getProfile().removeMode(input, mode);
			setUnsavedChanges(true);
			updateModesPanel(false);
			updateVisualizationPanel();
		}

		@Serial
		private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(RemoveModeAction.class.getName());
		}

		@Serial
		private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(RemoveModeAction.class.getName());
		}
	}

	private final class SaveAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -8469921697479550983L;

		private SaveAction() {
			putValue(NAME, STRINGS.getString("SAVE_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, STRINGS.getString("SAVE_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			saveProfile();
		}
	}

	private final class SaveAsAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -8469921697479550983L;

		private SaveAsAction() {
			putValue(NAME, STRINGS.getString("SAVE_AS_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, STRINGS.getString("SAVE_AS_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			saveProfileAs();
		}
	}

	private final class SelectControllerAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -2043467156713598592L;

		@SuppressWarnings({ "serial", "RedundantSuppression" })
		private final Controller controller;

		private SelectControllerAction(final Controller controller) {
			this.controller = controller;

			putValue(NAME, controller.name);
			putValue(SHORT_DESCRIPTION,
					MessageFormat.format(STRINGS.getString("SELECT_CONTROLLER_ACTION_DESCRIPTION"), controller.name));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (selectedController != null && selectedController.instanceId == controller.instanceId) {
				return;
			}

			final var wasRunning = isRunning();
			setSelectedControllerAndUpdateInput(controller, input.isInitialized() ? input.getAxes() : null);

			if (wasRunning) {
				restartLast();
			}
		}

		@Serial
		private void readObject(final ObjectInputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(SelectControllerAction.class.getName());
		}

		@Serial
		private void writeObject(final ObjectOutputStream ignoredStream) throws NotSerializableException {
			throw new NotSerializableException(SelectControllerAction.class.getName());
		}
	}

	private final class SelectIndicatorColorAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = 3316770144012465987L;

		private final VirtualAxis virtualAxis;

		private SelectIndicatorColorAction(final VirtualAxis virtualAxis) {
			this.virtualAxis = virtualAxis;

			putValue(NAME, "🎨");
			putValue(SHORT_DESCRIPTION, STRINGS.getString("CHANGE_INDICATOR_COLOR_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final var overlayAxis = input.getProfile().getVirtualAxisToOverlayAxisMap().get(virtualAxis);

			final var color = JColorChooser.showDialog(frame, STRINGS.getString("INDICATOR_COLOR_CHOOSER_TITLE"),
					overlayAxis.color);
			if (color != null) {
				overlayAxis.color = color;
			}

			setUnsavedChanges(true);
			updateOverlayPanel();
		}
	}

	private final class SelectLedColorAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = 3316770144012465987L;

		private SelectLedColorAction() {
			putValue(NAME, "🎨");
			putValue(SHORT_DESCRIPTION, STRINGS.getString("CHANGE_LED_COLOR_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final var color = JColorChooser.showDialog(frame, STRINGS.getString("LED_COLOR_CHOOSER_TITLE"),
					getLedColor());
			if (color == null) {
				return;
			}

			preferences.putInt(PREFERENCES_LED_COLOR, color.getRGB());
			ledPreviewLabel.setBackground(color);
		}
	}

	private final class SetHotSwapButtonAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = 6854936097922617928L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			final var hotSwappingButton = (HotSwappingButton) ((JComboBox<?>) e.getSource()).getSelectedItem();
			if (hotSwappingButton != null) {
				preferences.putInt(PREFERENCES_HOT_SWAPPING_BUTTON, hotSwappingButton.id);
			}
		}
	}

	private final class SetModeDescriptionAction extends AbstractAction implements DocumentListener, FocusListener {

		@Serial
		private static final long serialVersionUID = -6706537047137827688L;

		@SuppressWarnings({ "serial", "RedundantSuppression" })
		private final Mode mode;

		private final JTextField modeDescriptionTextField;

		private SetModeDescriptionAction(final Mode mode, final JTextField modeDescriptionTextField) {
			this.mode = mode;
			this.modeDescriptionTextField = modeDescriptionTextField;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			setModeDescription(true);
		}

		@Override
		public void changedUpdate(final DocumentEvent e) {
			setModeDescription(false);
		}

		@Override
		public void focusGained(final FocusEvent e) {
		}

		@Override
		public void focusLost(final FocusEvent e) {
			setModeDescription(true);
		}

		@Override
		public void insertUpdate(final DocumentEvent e) {
			setModeDescription(false);
		}

		@Override
		public void removeUpdate(final DocumentEvent e) {
			setModeDescription(false);
		}

		private void setModeDescription(final boolean updateTextField) {
			var description = modeDescriptionTextField.getText();

			if (!Objects.equals(mode.getDescription(), description)) {
				if (description != null && !description.isEmpty()) {
					final var strippedDescription = description.strip();

					if (updateTextField && !strippedDescription.equals(description)) {
						EventQueue.invokeLater(() -> modeDescriptionTextField.setText(strippedDescription));
					}

					description = strippedDescription;
				}

				mode.setDescription(description);

				setUnsavedChanges(true);
				updateVisualizationPanel();
			}
		}
	}

	private final class SetThemeAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -8033657996255756808L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			final var theme = (Theme) ((JComboBox<?>) e.getSource()).getSelectedItem();
			if (theme != null) {
				preferences.putInt(PREFERENCES_THEME, theme.id);
				updateTheme();
			}
		}
	}

	private final class ShowAboutDialogAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -2578971543384483382L;

		private ShowAboutDialogAction() {
			putValue(NAME, STRINGS.getString("SHOW_ABOUT_DIALOG_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, STRINGS.getString("SHOW_ABOUT_DIALOG_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final var imageIcon = new ImageIcon(getResourceLocation(ICON_RESOURCE_PATHS[2]));

			GuiUtils.showMessageDialog(
					main, frame, MessageFormat.format(STRINGS.getString("ABOUT_DIALOG_TEXT"),
							Constants.APPLICATION_NAME, Constants.VERSION),
					(String) getValue(NAME), JOptionPane.INFORMATION_MESSAGE, imageIcon);
		}
	}

	private final class ShowLicensesAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = 2471952794110895043L;

		private ShowLicensesAction() {
			putValue(NAME, STRINGS.getString("SHOW_LICENSES_DIALOG_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, STRINGS.getString("SHOW_LICENSES_DIALOG_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent actionEvent) {
			final var editorPane = new JEditorPane();
			editorPane.setContentType("text/html");
			editorPane.setEditable(false);
			editorPane.setCaretColor(editorPane.getBackground());

			final var scrollPane = new JScrollPane(editorPane);
			scrollPane.setPreferredSize(new Dimension(1015, 400));

			editorPane.addHyperlinkListener(hyperlinkEvent -> {
				if (hyperlinkEvent.getEventType() != EventType.ACTIVATED) {
					return;
				}

				final var description = hyperlinkEvent.getDescription();
				if (description != null && description.startsWith("#")) {
					editorPane.scrollToReference(description.substring(1));
					return;
				}

				try {
					openBrowser(scrollPane, hyperlinkEvent.getURL().toURI());
				} catch (final URISyntaxException e) {
					throw new RuntimeException(e);
				}
			});

			editorPane.setText(Constants.LICENSES_HTML);
			editorPane.setCaretPosition(0);

			GuiUtils.showMessageDialog(main, frame, scrollPane, (String) getValue(NAME), JOptionPane.DEFAULT_OPTION);
		}
	}

	private final class StartClientAction extends ConnectAction {

		@Serial
		private static final long serialVersionUID = 3975574941559749481L;

		private StartClientAction() {
			super(true);

			putValue(NAME, STRINGS.getString("START_CLIENT_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, STRINGS.getString("START_CLIENT_ACTION_DESCRIPTION"));
		}

		@Override
		void proceed() {
			startClient();
		}
	}

	private final class StartLocalAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -2003502124995392039L;

		private StartLocalAction() {
			putValue(NAME, STRINGS.getString("START_LOCAL_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, STRINGS.getString("START_LOCAL_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			startLocal();
		}
	}

	private final class StartServerAction extends ConnectAction {

		@Serial
		private static final long serialVersionUID = 1758447420975631146L;

		private StartServerAction() {
			super(false);

			putValue(NAME, STRINGS.getString("START_SERVER_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, STRINGS.getString("START_SERVER_ACTION_DESCRIPTION"));
		}

		@Override
		void proceed() {
			startServer();
		}
	}

	private final class StopAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = -2863419586328503426L;

		private StopAction() {
			putValue(NAME, STRINGS.getString("STOP_ACTION_NAME"));
			putValue(SHORT_DESCRIPTION, STRINGS.getString("STOP_ACTION_DESCRIPTION"));
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			stopAll(true, true, false);
		}
	}

	private abstract class UnsavedChangesAwareAction extends AbstractAction {

		@Serial
		private static final long serialVersionUID = 1387266903295357716L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (handleUnsavedChanges()) {
				doAction();
			}
		}

		protected abstract void doAction();
	}
}
