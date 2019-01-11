/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.oracle.si;

import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.security.PrivilegedAction;
import java.security.AccessController;
import java.security.SecureRandom;

/*
 * Singleton
 *
 * This is a simple implementation of the functionality previously contained in
 * Java Web Start or JavaFXPackager as 'SingleInstanceService', now available
 * to any Java application.
 *
 * The class has 4 public methods: start(), stop(), invoke() and running(),
 * and a public interface Singleton.SingletonApp.
 *
 * To use, an app would at a minimum use invoke() and start() in it's main()
 * method as follows:
 *
 * public class SiApp implements Singleton.SingletonApp {
 *     public static void main (String[] args) {
 *         String id = "com.me.SiApp";
 *         if (Singleton.invoke(id, args)) {
 *             System.out.println("invoked existing instance of SiApp");
 *         } else {
 *             System.out.println("starting first instance of SiApp");
 *             SiApp siapp = new SiApp(args);
 *             Singleton.start(siapp, id);
 *             // anything here you want the app to normally do
 *         }
 *     }
 *     public void newActivation(String[] args) {
 *         // anything here you want the app to do when
 *         // invoked from subsequent invocations.
 *     }
 * }
 *
 */


public class Singleton {

    private static final String SI_FILEDIR = getJavaTmpDir()
           + File.separator + "singleton" + File.separator;
    private static final String SI_MAGICWORD = "si.init";
    private static final String SI_ACK = "si.ack";
    private static final String SI_STOP = "si.stop";
    private static final String SI_EOF = "si.EOF";
    private static final int ENCODING_PLATFORM = 1;
    private static final int ENCODING_UNICODE = 2;

    private static final String ENCODING_PLATFORM_NAME = "UTF-8";
    private static final String ENCODING_UNICODE_NAME = "UTF-16LE";
    private static final boolean DEBUG = false;

    private static final String APP_ID_PREFIX = "singleton.";

    private static int currPort;
    private static String stringId = null;
    private static String randomNumberString = null;

    private static SingletonServer siServer;
    private static SingletonApp siApp = null;

    private static final SecureRandom random = new SecureRandom();
    private static volatile boolean serverStarted = false;
    private static int randomNumber;

    private static final Object lock = new Object();

    public interface SingletonApp {
        public void newActivation(String... args);
    }

    public static void start(SingletonApp sia, String id) {

        if (sia == null || id == null) {
            trace("Singleton.start called with null "
                    + ((sia == null) ? "SingletonApp" : "id string"));
            return;
        }

        synchronized (lock) {
            if (!serverStarted) {
                try {
                    siServer = new SingletonServer(id);
                    siServer.start();
                } catch (Exception e) {
                    return;
                }
                siApp = sia;
                serverStarted = true;
            }
        }
    }

    public static void stop() {
        if (siApp == null) {
            trace("Singleton.stop() called when not running");
            return;
        }

        synchronized (siApp) {

            siApp = null;

            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    Socket socket = null;
                    PrintStream out = null;
                    OutputStream os = null;
                    try {
                        socket = new Socket("127.0.0.1", siServer.getPort());
                        os = socket.getOutputStream();
                        byte[] encoding = new byte[1];
                        encoding[0] = ENCODING_PLATFORM;
                        os.write(encoding);
                        String charset = Charset.defaultCharset().name();
                        out = new PrintStream(os, true, charset);
                        out.println(randomNumber);
                        out.println(Singleton.SI_STOP);
                        out.flush();
                        serverStarted = false;
                    } catch (IOException ioe) {
                        trace(ioe);
                    } finally {
                        try {
                            if (out != null) {
                                out.close();
                            }
                            if (os != null) {
                                os.close();
                            }
                            if (socket != null) {
                                socket.close();
                            }
                        } catch (IOException ioe) {
                            trace(ioe);
                        }
                    }
                    return null;
                }
            });
        }
    }

    public static boolean running(String id) {
        trace("is there another instance running for id: " + id);
        File siDir = new File(SI_FILEDIR);
        String[] fList = siDir.list();
        if (fList != null) {
            String prefix = getFilePrefix(id);
            for (String file : fList) {
                trace("running: " + file);
                trace("\t String id: " + id);
                trace("\t FilePrefix: " + prefix);
                // if file with the same prefix already exist, server is running
                if (file.startsWith(prefix)) {
                    try {
                        currPort = Integer.parseInt(
                                    file.substring(file.lastIndexOf('_') + 1));
                        trace("running: " + file + ": port: " + currPort);
                    } catch (NumberFormatException nfe) {
                        trace("running: " + file + ": port parsing failed");
                        trace(nfe);
                        return false;
                    }

                    trace("Server running at port: " + currPort);
                    File siFile = new File(SI_FILEDIR, file);

                    // get random number from single instance file
                    try (BufferedReader br = new BufferedReader(
                            new FileReader(siFile))) {
                        randomNumberString = br.readLine();
                        trace("running: " + file + ": magic: "
                                + randomNumberString);
                    } catch (IOException ioe ) {
                        trace("running: " + file + ": reading magic failed");
                        trace(ioe);
                    }
                    trace("running: " + file + ": setting id - OK");
                    stringId = id;
                    return true;
                } else {
                    trace("running: " + file + ": prefix NOK");
                }
            }
        } else {
            trace("running: empty file list");
        }
        trace("running returning: false");
        return false;
    }

    public static boolean invoke (String id, String[] args) {
        if (running(id)) {
            if (connectToServer(args)) {
                return true;
           }
        }
        return false;
    }

    private static String getFilePrefix(final String stringId) {
        String filePrefix = stringId.replace('/','_');
        filePrefix = filePrefix.replace(':','_');
        return filePrefix;
    }

    private static String getJavaTmpDir() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return System.getProperty("user.home") + "\\AppData\\Local\\Java";
        } else if (os.contains("mac")) {
            return System.getProperty("user.home")
                    + "/Library/Application Support/Java";
        } else {
            return System.getProperty("user.home") + "/.java";
        }
    }


    /**
     * Returns true if we connect successfully to the server for the stringId
     */
    static boolean connectToServer(String[] args) {
        trace("Connect to: " + stringId + " " + currPort);

        if (randomNumberString == null) {
            // should not happen
            trace("MAGIC number is null, bail out.");
            return false;
        }

        // Now we open the tcpSocket and the stream
        Socket socket = null;
        OutputStream os = null;
        PrintStream out = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            socket = new Socket("127.0.0.1", currPort);
            os = socket.getOutputStream();
            byte[] encoding = new byte[1];
            encoding[0] = ENCODING_PLATFORM;
            os.write(encoding);
            String encodingName = Charset.defaultCharset().name();

            out = new PrintStream(os, true, encodingName);
            isr = new InputStreamReader(socket.getInputStream(), encodingName);
            br = new BufferedReader(isr);

            // send random number
            out.println(randomNumberString);
            // send MAGICWORD
            out.println(SI_MAGICWORD);

            for (String arg : args) {
                out.println(arg);
            }

            // indicate end of file transmission
            out.println(SI_EOF);
            out.flush();

            // wait for ACK (OK) response
            trace("Waiting for ack");
            final int tries = 5;

            // try to listen for ACK
            for (int i=0; i < tries; i++) {
                String str = br.readLine();
                if (str != null && str.equals(SI_ACK)) {
                    trace("Got ACK");
                    return true;
                }
            }
        } catch (java.net.SocketException se) {
            // no server is running - continue launch
            trace("No server is running - continue launch.");
            trace(se);
        } catch (Exception ioe) {
            trace(ioe);
        }
        finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (isr != null) {
                    isr.close();
                }
                if (out != null) {
                    out.close();
                }
                if (os != null) {
                    os.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException ioe) {
                trace(ioe);
            }
        }
        trace("No ACK from server, bail out.");
        return false;
    }

    private static class SingletonServer {

        private final SingletonServerRunnable runnable;
        private final Thread thread;

        SingletonServer(SingletonServerRunnable runnable) throws IOException {
            thread = new Thread(null, runnable, "SIThread", 0, false);
            thread.setDaemon(true);
            this.runnable = runnable;
        }

        SingletonServer(String stringId) throws IOException {
            this(new SingletonServerRunnable(stringId));
        }

        int getPort() {
            return runnable.getPort();
        }

        void start() {
            thread.start();
        }
    }

    private static class SingletonServerRunnable implements Runnable {

        ServerSocket ss;
        int port;
        String stringId;
        String[] arguments;

        int getPort() {
            return port;
        }

        SingletonServerRunnable(String id) throws IOException {
            stringId = id;

            // open a free ServerSocket
            ss = null;

            // we should bind the server to the local InetAddress 127.0.0.1
            // port number is automatically allocated for current SI
            ss = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"));

            // get the port number
            port = ss.getLocalPort();
            trace("server port at: " + port);

            // create the single instance file with canonical home and port num
            createSingletonFile(stringId, port);
        }

        private String getSiFilename(final String id, final int port) {
            String name = SI_FILEDIR + getFilePrefix(id) + "_" + port;
            trace("getSiFilename: " + name);
            return name;
        }

        private void removeSiFile(final String id, final int port) {
            new File(getSiFilename(id, port)).delete();
            trace("removed SingletonFile: " + getSiFilename(id, port));
        }

        private void createSingletonFile(final String id, final int port) {
            String filename = getSiFilename(id, port);
            final File siFile = new File(filename);
            final File siDir = new File(SI_FILEDIR);
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    siDir.mkdirs();
                    String[] fList = siDir.list();
                    if (fList != null) {
                        String prefix = getFilePrefix(id);
                        for (String file : fList) {
                            // if file with the same prefix exist, remove it
                            if (file.startsWith(prefix)) {
                                trace("removing: " + SI_FILEDIR + file);
                                new File(SI_FILEDIR + file).delete();
                            }
                        }
                    }

                    PrintStream out = null;
                    try {
                        siFile.createNewFile();
                        siFile.deleteOnExit();
                        // write random number to single instance file
                        out = new PrintStream(new FileOutputStream(siFile));
                        randomNumber = random.nextInt();
                        out.print(randomNumber);
                    } catch (IOException ioe) {
                        trace(ioe);
                    } finally {
                        if (out != null) {
                            out.close();
                        }
                    }
                    return null;
                }
            });
        }

        @Override
        public void run() {
            // handle all the incoming request from server port 
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    List<String> recvArgs = new ArrayList<>();
                    while (true) {
                        recvArgs.clear();
                        InputStream is = null;
                        BufferedReader in = null;
                        InputStreamReader isr = null;
                        Socket s = null;
                        String line = null;
                        boolean sendAck = false;
                        int port = -1;
                        String charset = null;
                        try {
                            trace("waiting connection");
                            s = ss.accept();
                            is = s.getInputStream();
                            // read first byte for encoding type
                            int encoding = is.read();
                            if (encoding == ENCODING_PLATFORM) {
                                charset = Charset.defaultCharset().name();
                            } else if (encoding == ENCODING_UNICODE) {
                                charset = ENCODING_UNICODE_NAME;
                            } else {
                                trace("Unknown encoding: " + encoding);
                                return null;
                            }
                            isr = new InputStreamReader(is, charset);
                            in = new BufferedReader(isr);
                            // first read the random number
                            line = in.readLine();
                            if (line.equals(String.valueOf(randomNumber)) ==
                                    false) {
                                // random number does not match
                                // should not happen
                                // shutdown server socket
                                removeSiFile(stringId, port);
                                ss.close();
                                serverStarted = false;
                                trace("Unexpected Error, "
                                        + "Singleton disabled");
                                return null;
                            } else {
                                line = in.readLine();
                                // no need to continue reading if MAGICWORD
                                // did not come first
                                trace("recv: " + line);
                                if (line.equals(SI_MAGICWORD)) {
                                    trace("got magic word.");
                                    while (true) {
                                        // Get input string
                                        try {
                                            line = in.readLine();
                                            if (line != null
                                                    && line.equals(SI_EOF)) {
                                                // end of file reached
                                                break;
                                            } else {
                                                recvArgs.add(line);
                                            }
                                        } catch (IOException ioe) {
                                            trace(ioe);
                                        }
                                    }
                                    arguments = recvArgs.toArray(
                                            new String[recvArgs.size()]);
                                    sendAck = true;
                                } else if (line.equals(SI_STOP)) {
                                    // remove the Singlton file
                                    removeSiFile(stringId, port);
                                    break;
                                }
                            }
                        } catch (IOException ioe) {
                            trace(ioe);
                        } finally {
                            try {
                                if (sendAck) {
                                    // let the action listener handle the rest
                                    for (String arg : arguments) {
                                        trace("Starting new instance with "
                                                + "arguments: arg:" + arg);
                                    }

                                    siApp.newActivation(arguments);

                                    // now the event is handled, we can send
                                    // out the ACK
                                    trace("sending out ACK");
                                    if (s != null) {
                                        try (OutputStream os =
                                                s.getOutputStream();
                                            PrintStream ps = new PrintStream(os,
                                                    true, charset)) {
                                            // send OK (ACK)
                                            ps.println(SI_ACK);
                                            ps.flush();
                                        }
                                    }
                                }

                                if (in != null) {
                                    in.close();
                                }

                                if (isr != null) {
                                    isr.close();
                                }

                                if (is != null) {
                                    is.close();
                                }

                                if (s != null) {
                                    s.close();
                                }
                            } catch (IOException ioe) {
                                trace(ioe);
                            }
                        }
                    }
                    return null;
                }
            });
        }
    }

    private static void trace(String s) {
        if (DEBUG) {
            System.err.println("Singleton trace: " + s);
        }
    }

    private static void trace(Exception e) {
        System.err.println("Singleton exception: " + e.getMessage());
        if (DEBUG) {
            e.printStackTrace(System.err);
        }
    }
}
