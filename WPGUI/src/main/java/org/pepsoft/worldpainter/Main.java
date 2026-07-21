/*
 * This file is part of WorldPainter Languages, an unofficial localization
 * fork of WorldPainter (https://github.com/saplome/WorldPainter-LANGUAGES).
 *
 * Original work Copyright © pepsoft.org, The Netherlands.
 * Modifications Copyright © 2026 saplome. This file was modified in 2026.
 *
 * This file remains licensed under the GNU General Public License,
 * version 3. See the LICENSE file for details.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.jidesoft.plaf.LookAndFeelFactory;
import com.jidesoft.utils.Lm;
import org.pepsoft.util.*;
import org.pepsoft.util.plugins.PluginManager;
import org.pepsoft.worldpainter.biomeschemes.BiomeSchemeManager;
import org.pepsoft.worldpainter.layers.renderers.VoidRenderer;
import org.pepsoft.worldpainter.operations.MouseOrTabletOperation;
import org.pepsoft.worldpainter.plugins.PlatformManager;
import org.pepsoft.worldpainter.plugins.Plugin;
import org.pepsoft.worldpainter.plugins.WPPluginManager;
import org.pepsoft.worldpainter.util.BetterAction;
import org.pepsoft.worldpainter.vo.EventVO;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static org.pepsoft.util.GUIUtils.getUIScale;
import static org.pepsoft.util.swing.MessageUtils.*;
import static org.pepsoft.worldpainter.Constants.ATTRIBUTE_KEY_PLUGINS;
import static org.pepsoft.worldpainter.Constants.ATTRIBUTE_KEY_SAFE_MODE;
import static org.pepsoft.worldpainter.plugins.WPPluginManager.DESCRIPTOR_PATH;

/**
 *
 * @author pepijn
 */
public class Main {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        // Enable font anti-aliasing unless it has been explicitly configured
        // externally. This is mainly important for CJK (e.g. Chinese) glyphs,
        // which otherwise render with merged strokes at small sizes.
        if (System.getProperty("awt.useSystemAAFontSettings") == null) {
            System.setProperty("awt.useSystemAAFontSettings", "lcd");
        }
        if (System.getProperty("swing.aatext") == null) {
            System.setProperty("swing.aatext", "true");
        }

        // When this instance was spawned by an automatic restart (e.g. after a
        // language change), wait briefly so the previous instance can finish
        // shutting down: save its configuration and release the single-instance
        // lock file before we start reading them.
        {
            final String restartDelay = System.getProperty("worldpainter.restartDelayMillis");
            if ((restartDelay != null) && ! restartDelay.trim().isEmpty()) {
                try {
                    Thread.sleep(Long.parseLong(restartDelay.trim()));
                } catch (NumberFormatException | InterruptedException ignored) {
                    // ignore -- start up immediately
                }
            }
        }

        // Language selection (English / Russian). Priority:
        //   1. -Dworldpainter.language=en|ru system property
        //   2. saved preference (node org/pepsoft/worldpainter, key "language")
        //   3. default: English (en)
        {
            String wpLang = System.getProperty("worldpainter.language");
            if ((wpLang == null) || wpLang.trim().isEmpty()) {
                try {
                    wpLang = Preferences.userRoot().node("org/pepsoft/worldpainter").get("language", "en");
                } catch (Exception e) {
                    wpLang = "en";
                }
            }
            String wpLangCode = org.pepsoft.worldpainter.WPI18n.normalizeLang(wpLang);
            Locale.setDefault("en".equals(wpLangCode) ? Locale.US : new Locale(wpLangCode));
        }

        // Check that this is not a headless runtime. Do it this early because we have seen exceptions from the uncaught
        // exception handler on headless runtimes in the wild, which is the next thing we install after this
        if (GraphicsEnvironment.isHeadless()) {
            System.err.printf("The Java runtime (%s %s, version %s) is headless.%nPlease install a complete Java runtime (or make it the default).%n",
                    System.getProperty("java.vm.vendor"),
                    System.getProperty("java.vm.name"),
                    System.getProperty("java.vm.version"));
            System.exit(1);
        }

        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());

        // Set some hardcoded system properties we always want set:
        if (SystemUtils.isMac()) {
            // Use the Mac style top of screen menu bar
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }
        // Work around a bug in the JIDE Docking Framework which otherwise causes duplicate mouse events on focus
        // switches resulting in uncommanded edits
        System.setProperty("docking.focusWorkaround1", "true");
        // Disable Java2D's automatic UI scaling, as it does not do a good job with the editor view; we want to do it
        // ourselves
        System.setProperty("sun.java2d.uiScale.enabled", "false");
        // Propagate a few system properties to libraries
        final String devMode = System.getProperty("org.pepsoft.worldpainter.devMode");
        if (devMode != null) {
            System.setProperty("org.pepsoft.devMode", devMode);
        }
        boolean safeMode = "true".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.safeMode"));
        for (String arg: args) {
            if (arg.trim().equalsIgnoreCase("--safe")) {
                safeMode = true;
            }
        }
        if (safeMode) {
            logger.info("WorldPainter running in safe mode");
            System.setProperty("org.pepsoft.worldpainter.safeMode", "true");
            System.setProperty("org.pepsoft.util.GUIUtils.disableScaling", "true");
        }
        if (Version.isSnapshot()) {
            System.setProperty("org.pepsoft.snapshotVersion", "true");
        }

        // Use a file lock to make sure only one instance is running with autosave enabled
        File configDir = Configuration.getConfigDir();
        if (! configDir.isDirectory()) {
            configDir.mkdirs();
        }
        Path lockFilePath = new File(configDir, "wpsession.lock").toPath();
        try {
            Files.createFile(lockFilePath);
        } catch (FileAlreadyExistsException e) {
            // We can't yet conclude another instance is running, because it may have crashed and left the lock file
            // behind
        }
        FileChannel lockFileChannel = FileChannel.open(lockFilePath, StandardOpenOption.WRITE);
        FileLock lock = lockFileChannel.tryLock();
        boolean autosaveInhibited;
        if (lock == null) {
            lockFileChannel.close();
            autosaveInhibited = true;
        } else {
            Runtime.getRuntime().addShutdownHook(new Thread("Lock File Eraser") {
                @Override
                public void run() {
                    try {
                        lock.release();
                        lockFileChannel.close();
                        Files.delete(lockFilePath);
                    } catch (IOException e) {
                        logger.error("Could not delete lock file " + lockFilePath, e);
                    }
                }
            });
            autosaveInhibited = false;
        }

        // Configure logging
        String logLevel;
        if ("true".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.debugLogging"))) {
            logLevel = "DEBUG";
        } else if ("extra".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.debugLogging"))) {
            logLevel = "TRACE";
        } else {
            logLevel = "INFO";
        }
        LoggerContext logContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(logContext);
            logContext.reset();
            System.setProperty("org.pepsoft.worldpainter.configDir", configDir.getAbsolutePath());
            System.setProperty("org.pepsoft.worldpainter.logLevel", logLevel);
            configurator.doConfigure(ClassLoader.getSystemResourceAsStream("logback-main.xml"));
        } catch (JoranException e) {
            // StatusPrinter will handle this
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(logContext);
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        logger.info("Starting WorldPainter " + Version.VERSION + " (" + Version.BUILD + ")");
        logger.info("Running on {} version {}; architecture: {}", System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"));
        logger.info("Running on {} Java version {}; maximum heap size: {} MB", System.getProperty("java.vendor"), System.getProperty("java.specification.version"), Runtime.getRuntime().maxMemory() / 1000000);
        if (autosaveInhibited) {
            logger.warn("Another instance of WorldPainter is already running; disabling autosave");
        }

        // Parse the command line
        File myFile = null;
        for (String arg: args) {
            if (new File(arg).isFile() && (myFile == null)) {
                myFile = new File(arg);
            } else {
                throw new IllegalArgumentException("Unrecognised or invalid command line option, or file does not exist: " + arg);
            }
        }
        final File file = myFile;

        // If the config file does not exist, also reset the persistent settings that are not stored in that, since the
        // user may be trying to reset the configuration
        final boolean snapshot = Version.isSnapshot();
        if (! Configuration.getConfigFile().isFile()) {
            try {
                Preferences prefs = Preferences.userNodeForPackage(Main.class);
                prefs.remove((snapshot ? "snapshot." : "") + "accelerationType");
                prefs.flush();
                prefs = Preferences.userNodeForPackage(GUIUtils.class);
                prefs.remove((snapshot ? "snapshot." : "") + "manualUIScale");
                prefs.flush();
            } catch (BackingStoreException e) {
                logger.error("Error resetting user preferences", e);
            }
        }

        // Set the acceleration mode. For some reason we don't fully understand, loading the Configuration from disk
        // initialises Java2D, so we have to do this *before* then.
        AccelerationType accelerationType;
        String accelTypeName = Preferences.userNodeForPackage(Main.class).get((snapshot ? "snapshot." : "") + "accelerationType", null);
        if (accelTypeName != null) {
            accelerationType = AccelerationType.valueOf(accelTypeName);
        } else {
            accelerationType = AccelerationType.DEFAULT;
            // TODO: Experiment with which ones work well and use them by default!
        }
        if (! safeMode) {
            switch (accelerationType) {
                case UNACCELERATED:
                    // Try to disable all accelerated pipelines we know of:
                    System.setProperty("sun.java2d.d3d", "false");
                    System.setProperty("sun.java2d.opengl", "false");
                    System.setProperty("sun.java2d.xrender", "false");
                    System.setProperty("apple.awt.graphics.UseQuartz", "false");
                    logger.info("Hardware acceleration method: unaccelerated");
                    break;
                case DIRECT3D:
                    // Direct3D should already be the default on Windows, but enable a few things which are off by
                    // default:
                    System.setProperty("sun.java2d.translaccel", "true");
                    System.setProperty("sun.java2d.ddscale", "true");
                    logger.info("Hardware acceleration method: Direct3D");
                    break;
                case OPENGL:
                    System.setProperty("sun.java2d.opengl", "True");
                    logger.info("Hardware acceleration method: OpenGL");
                    break;
                case XRENDER:
                    System.setProperty("sun.java2d.xrender", "True");
                    logger.info("Hardware acceleration method: XRender");
                    break;
                case QUARTZ:
                    System.setProperty("apple.awt.graphics.UseQuartz", "true");
                    logger.info("Hardware acceleration method: Quartz");
                    break;
                default:
                    logger.info("Hardware acceleration method: default");
                    break;
            }
        } else {
            logger.info("[SAFE MODE] Hardware acceleration method: default");
        }

        // Load the default platform descriptors so that they don't get blocked by older versions of them which might be
        // contained in the configuration. Do this by loading and initialising (but not instantiating) the DefaultPlugin
        // class
        try {
            Class.forName("org.pepsoft.worldpainter.DefaultPlugin");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        // Load or initialise configuration
        Configuration config = null;
        try {
            config = Configuration.load(); // This will migrate the configuration directory if necessary
        } catch (IOException | Error | RuntimeException | ClassNotFoundException e) {
            configError(e);
        }
        if (config == null) {
            if (! logger.isDebugEnabled()) {
                // If debug logging is on, the Configuration constructor will already log this
                logger.info("Creating new configuration");
            }
            config = new Configuration();
        }
        // Load the transient settings into the config object
        config.setSafeMode(safeMode);
        config.setAutosaveInhibited(autosaveInhibited);
        Configuration.setInstance(config);
        logger.info("Installation ID: " + config.getUuid());

        if (config.getPreviousVersion() >= 0) {
            // Perform legacy migration actions
            if (config.getPreviousVersion() < 18) {
                // The dynmap data may have been copied from Minecraft 1.13, in which case it doesn't work, so delete it
                // if it exists
                File dynmapDir = new File(Configuration.getConfigDir(), "dynmap");
                if (dynmapDir.isDirectory()) {
                    FileUtils.deleteDir(dynmapDir);
                }
            }
        }

        if (config.isAutosaveEnabled() && autosaveInhibited) {
            StartupMessages.addWarning(org.pepsoft.worldpainter.WPI18n.s("startup.warning.anotherInstance"));
        }

        // Store the acceleration type in the config object so the Preferences dialog can edit it
        config.setAccelerationType(accelerationType);

        // Start background scan for Minecraft jars
        BiomeSchemeManager.initialiseInBackground();
        
        // Load and install trusted WorldPainter root certificate
        X509Certificate trustedCert = null;
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            trustedCert = (X509Certificate) certificateFactory.generateCertificate(Main.class.getResourceAsStream("/wproot.pem"));
        } catch (CertificateException e) {
            logger.error("Certificate exception while loading trusted root certificate", e);
        }

        // Load the plugins, checking for updates
        if (! safeMode) {
            if (trustedCert != null) {
                PluginManager.loadPlugins(new File(configDir, "plugins"), trustedCert.getPublicKey(), DESCRIPTOR_PATH, Version.VERSION_OBJ, true);
            } else {
                logger.error("Trusted root certificate not available; not loading plugins");
            }
        } else {
            logger.info("[SAFE MODE] Not loading plugins");
        }
        WPPluginManager.initialise(config.getUuid(), WPContext.INSTANCE);
        // Load all the platform descriptors to ensure that when worlds containing older versions of them are loaded
        // later they are replaced with the current versions, rather than the other way around
        for (Platform platform : PlatformManager.getInstance().getAllPlatforms()) {
            logger.info("Available platform: {}", platform.displayName);
        }
        String httpAgent = "WorldPainter " + Version.VERSION + "; " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch") + ";";
        System.setProperty("http.agent", httpAgent);

        // Load the private context, if any, which provides services which we only want the official distribution of
        // WorldPainter to perform, such as check for updates and submit usage data
        for (PrivateContext aPrivateContextLoader: ServiceLoader.load(PrivateContext.class)) {
            if (privateContext == null) {
                privateContext = aPrivateContextLoader;
            } else {
                throw new IllegalStateException("More than one private context found on classpath");
            }
        }
        if (privateContext == null) {
            logger.debug("No private context found on classpath; update checks and usage data submission disabled");
            config.setPingAllowed(false);
        }

        // Check for updates (if update checker is available)
        if (privateContext != null) {
            privateContext.checkForUpdates();
        }

        final long start = System.currentTimeMillis();
        config.setLaunchCount(config.getLaunchCount() + 1);
        Runtime.getRuntime().addShutdownHook(new Thread("Configuration Saver") {
            @Override
            public void run() {
                try {
                    Configuration config = Configuration.getInstance();
                    MouseOrTabletOperation.flushEvents(config);
                    BetterAction.flushEvents(config);
                    EventVO sessionEvent = new EventVO("worldpainter.session").setAttribute(EventVO.ATTRIBUTE_TIMESTAMP, new Date(start)).duration(System.currentTimeMillis() - start);
                    StringBuilder sb = new StringBuilder();
                    List<Plugin> plugins = WPPluginManager.getInstance().getAllPlugins();
                    plugins.stream()
                            .filter(plugin -> ! plugin.getClass().getName().startsWith("org.pepsoft.worldpainter"))
                            .forEach(plugin -> {
                        if (sb.length() > 0) {
                            sb.append(',');
                        }
                        sb.append("{name=");
                        sb.append(plugin.getName().replaceAll("[ \\t\\n\\x0B\\f\\r\\.]", ""));
                        sb.append(",version=");
                        sb.append(plugin.getVersion());
                        sb.append('}');
                    });
                    if (sb.length() > 0) {
                        sessionEvent.setAttribute(ATTRIBUTE_KEY_PLUGINS, sb.toString());
                    }
                    sessionEvent.setAttribute(ATTRIBUTE_KEY_SAFE_MODE, config.isSafeMode());
                    config.logEvent(sessionEvent);
                    config.save();

                    // Store the acceleration type and manual GUI scale separately, because we need them before we can
                    // load the config:
                    Preferences prefs = Preferences.userNodeForPackage(Main.class);
                    prefs.put((snapshot ? "snapshot." : "") + "accelerationType", config.getAccelerationType().name());
                    prefs.flush();
                    prefs = Preferences.userNodeForPackage(GUIUtils.class);
                    prefs.putFloat((snapshot ? "snapshot." : "") + "manualUIScale", config.getUiScale());
                    prefs.flush();
                } catch (IOException e) {
                    logger.error("I/O error saving configuration", e);
                } catch (BackingStoreException e) {
                    logger.error("Backing store exception saving acceleration type", e);
                }
                logger.info("Shutting down WorldPainter");
            }
        });
        
        // Make the "action:" URLs used in various places work:
        URL.setURLStreamHandlerFactory(protocol -> {
            switch (protocol) {
                case "action":
                    return new URLStreamHandler() {
                        @Override
                        protected URLConnection openConnection(URL u) throws IOException {
                            throw new UnsupportedOperationException("Not supported");
                        }
                    };
                default:
                    return null;
            }
        });

        final World2 world;
        final File autosaveFile = new File(configDir, "autosave.world");
        if ((file == null) && (autosaveInhibited || (! config.isAutosaveEnabled()) || (! autosaveFile.isFile()))) {
            if (! safeMode) {
                world = WorldFactory.createDefaultWorld(config, new Random().nextLong());
//                world = WorldFactory.createFancyWorld(config, new Random().nextLong());
            } else {
                logger.info("[SAFE MODE] Using default configuration for default world");
                world = WorldFactory.createDefaultWorld(new Configuration(), new Random().nextLong());
            }
        } else {
            world = null;
        }

        // Install JIDE licence, if present
        InputStream in = ClassLoader.getSystemResourceAsStream("jide_licence.properties");
        if (in != null) {
            try {
                Properties jideLicenceProps = new Properties();
                jideLicenceProps.load(in);
                Lm.verifyLicense(jideLicenceProps.getProperty("companyName"), jideLicenceProps.getProperty("projectName"), jideLicenceProps.getProperty("licenceKey"));
            } finally {
                in.close();
            }
        }

        final Configuration.LookAndFeel lookAndFeel = (config.getLookAndFeel() != null) ? config.getLookAndFeel() : Configuration.LookAndFeel.FLATLAF_CYAN_LIGHT;
        SwingUtilities.invokeLater(() -> {
            Configuration myConfig = Configuration.getInstance();
            if (myConfig.isSafeMode()) {
                GUIUtils.setUIScale(1.0f);
                logger.info("[SAFE MODE] Not installing visual theme");
            } else {
                // Install configured look and feel
                try {
                    String laf;
                    // WorldPainter Languages fork (L58): explicit icon-theme marker
                    UIManager.getDefaults().remove("WorldPainter.iconTheme");
                    UIManager.getDefaults().remove("WorldPainter.useCustomBiomeIcons");
                    switch (lookAndFeel) {
                        case SYSTEM:
                            laf = UIManager.getSystemLookAndFeelClassName();
                            break;
                        case METAL:
                            laf = "javax.swing.plaf.metal.MetalLookAndFeel";
                            break;
                        case NIMBUS:
                            laf = "javax.swing.plaf.nimbus.NimbusLookAndFeel";
                            break;
                        case DARK_METAL:
                            laf = "org.netbeans.swing.laf.dark.DarkMetalLookAndFeel";
                            IconUtils.setTheme("dark_metal");
                            break;
                        case DARK_NIMBUS:
                            laf = "org.netbeans.swing.laf.dark.DarkNimbusLookAndFeel";
                            IconUtils.setTheme("dark_nimbus");
                            break;
                        case FLAT_LIGHT:
                        case FLAT_DARK:
                            // Legacy values from a rolled back FlatLaf experiment; treat as system look and feel
                            laf = UIManager.getSystemLookAndFeelClassName();
                            break;
                        case FLATLAF_ARC_ORANGE:
                            // L75: removed from the UI; old serialized selections safely use Cyan light.
                        case FLATLAF_CYAN_LIGHT:
                            laf = "com.formdev.flatlaf.intellijthemes.FlatCyanLightIJTheme";
                            IconUtils.setTheme("flatlaf_light");
                            UIManager.put("WorldPainter.iconTheme", "flatlaf_light");
                            UIManager.put("WorldPainter.useCustomBiomeIcons", Boolean.TRUE);
                            javax.swing.JFrame.setDefaultLookAndFeelDecorated(true);
                            javax.swing.JDialog.setDefaultLookAndFeelDecorated(true);
                            break;
                        case DARK_THEME:
                        case RADIANCE_NIGHT_SHADE:
                        case RADIANCE_NIGHT_SHADE_2:
                        case RADIANCE_GRAPHITE:
                        case RADIANCE_GRAPHITE_CHALK:
                        case RADIANCE_TWILIGHT:
                            // L78: removed themes are compatibility aliases for FlatLaf One Dark.
                        case FLATLAF_CARBON:
                        case FLATLAF_DARK_PURPLE:
                        case FLATLAF_ONE_DARK:
                            laf = "com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme";
                            IconUtils.setTheme("flatlaf_dark");
                            UIManager.put("WorldPainter.iconTheme", "flatlaf_dark");
                            UIManager.put("WorldPainter.useCustomBiomeIcons", Boolean.TRUE);
                            javax.swing.JFrame.setDefaultLookAndFeelDecorated(true);
                            javax.swing.JDialog.setDefaultLookAndFeelDecorated(true);
                            break;
                        default:
                            throw new InternalError();
                    }
                    logger.debug("Installing look and feel: " + laf);
                    UIManager.setLookAndFeel(laf);
                    LookAndFeelFactory.installJideExtension();

                    if ((lookAndFeel == Configuration.LookAndFeel.FLATLAF_ONE_DARK)
                            || (lookAndFeel == Configuration.LookAndFeel.FLATLAF_CARBON)
                            || (lookAndFeel == Configuration.LookAndFeel.FLATLAF_DARK_PURPLE)
                            || (lookAndFeel == Configuration.LookAndFeel.DARK_THEME)
                            || lookAndFeel.name().startsWith("RADIANCE_")) {
                        // L69: FlatLaf's docking fallback used near-white outlines and text. Keep the original
                        // theme backgrounds/accents, but soften bright foregrounds and JIDE frame separators.
                        final boolean purple = false; // Dark purple is a compatibility alias for One Dark since L75.
                        final Color softText = purple ? new Color(0xB2, 0xB0, 0xBC) : new Color(0x98, 0xA0, 0xAD);
                        final Color mutedText = purple ? new Color(0x8B, 0x87, 0x9A) : new Color(0x7F, 0x87, 0x94);
                        final Color darkPanel = purple ? new Color(0x2C, 0x2C, 0x3B) : new Color(0x21, 0x25, 0x2B);
                        final Color subtleBorder = purple ? new Color(0x37, 0x34, 0x46) : new Color(0x2F, 0x34, 0x3B);
                        final Color subtleSeparator = purple ? new Color(0x39, 0x36, 0x46) : new Color(0x30, 0x34, 0x3B);
                        final String[] foregroundKeys = {
                            "Label.foreground", "Button.foreground", "ToggleButton.foreground",
                            "CheckBox.foreground", "RadioButton.foreground", "ComboBox.foreground",
                            "List.foreground", "Table.foreground", "Tree.foreground",
                            "TabbedPane.foreground", "Menu.foreground", "MenuItem.foreground",
                            "TextField.foreground", "TextArea.foreground", "TextPane.foreground",
                            "EditorPane.foreground", "TitledBorder.titleColor", "JideLabel.foreground",
                            "DockableFrame.activeTitleForeground", "DockableFrame.inactiveTitleForeground"
                        };
                        for (String key: foregroundKeys) UIManager.put(key, softText);
                        UIManager.put("Label.disabledForeground", mutedText);
                        UIManager.put("Button.disabledText", mutedText);
                        UIManager.put("Separator.foreground", subtleSeparator);
                        UIManager.put("Separator.background", subtleSeparator);
                        UIManager.put("SplitPane.dividerFocusColor", subtleBorder);
                        UIManager.put("control", darkPanel);
                        UIManager.put("controlHighlight", subtleBorder);
                        UIManager.put("controlLtHighlight", subtleBorder);
                        final String[] darkSurfaceKeys = {
                            "DockableFrame.background", "DockableFrameTitlePane.background",
                            "ContentContainer.background", "JideTabbedPane.background",
                            "JideTabbedPane.tabAreaBackground", "SidePane.background",
                            "Workspace.background", "CommandBar.background", "Gripper.background"
                        };
                        for (String key: darkSurfaceKeys) UIManager.put(key, darkPanel);

                        final javax.swing.border.Border lineBorder = javax.swing.BorderFactory.createLineBorder(subtleBorder);
                        final String[] borderKeys = {
                            "DockableFrame.border", "DockableFrameTitlePane.border",
                            "ContentContainer.border", "JideTabbedPane.border", "SidePane.border"
                        };
                        for (String key: borderKeys) UIManager.put(key, lineBorder);

                        final String[] dockingFamilies = {
                            "DockableFrame", "DockableFrameTitlePane", "ContentContainer",
                            "JideTabbedPane", "SidePane", "Workspace", "CommandBar", "Gripper"
                        };
                        final List<Object> flatJideKeys = new ArrayList<>();
                        for (Enumeration<Object> e = UIManager.getDefaults().keys(); e.hasMoreElements(); ) {
                            flatJideKeys.add(e.nextElement());
                        }
                        for (Object key: flatJideKeys) {
                            if (! (key instanceof String)) continue;
                            final String keyName = (String) key;
                            boolean dockingKey = false;
                            for (String family: dockingFamilies) {
                                if (keyName.startsWith(family + ".")) { dockingKey = true; break; }
                            }
                            if (! dockingKey) continue;
                            final Object value = UIManager.get(key);
                            if (value instanceof Color) {
                                final Color colour = (Color) value;
                                final int luminance = (colour.getRed() * 299 + colour.getGreen() * 587 + colour.getBlue() * 114) / 1000;
                                final String lower = keyName.toLowerCase();
                                if ((lower.contains("foreground") || lower.contains("text")) && (luminance > 175)) {
                                    UIManager.put(key, softText);
                                } else if ((lower.contains("border") || lower.contains("separator") || lower.contains("shadow")) && (luminance > 100)) {
                                    UIManager.put(key, subtleBorder);
                                } else if ((lower.contains("background") || lower.contains("highlight")
                                        || lower.contains("light") || lower.contains("control")) && (luminance > 145)) {
                                    UIManager.put(key, darkPanel);
                                }
                            }
                        }
                    }

                    // L68: all custom themes use their dedicated icon override directory.
                    if (UIManager.get("WorldPainter.iconTheme") != null) {
                        UIManager.put("DockableFrameTitlePane.autohideIcon", IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/dock_pin.png"));
                        UIManager.put("DockableFrameTitlePane.stopAutohideIcon", IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/dock_unpin.png"));
                        UIManager.put("DockableFrameTitlePane.hideAutohideIcon", IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/dock_unpin.png"));
                        UIManager.put("DockableFrameTitlePane.floatIcon", IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/dock_float.png"));
                        UIManager.put("DockableFrameTitlePane.unfloatIcon", IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/dock_dock.png"));
                        UIManager.put("DockableFrameTitlePane.hideIcon", IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/dock_hide.png"));
                        UIManager.put("DockableFrameTitlePane.closeIcon", IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/dock_close.png"));
                        UIManager.put("DockableFrameTitlePane.maximizeIcon", IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/dock_maximize.png"));
                        UIManager.put("DockableFrameTitlePane.restoreIcon", IconUtils.loadScaledIcon("org/pepsoft/worldpainter/icons/dock_restore.png"));
                    }

                    // Per-language UI font substitution, driven by the optional
                    // "<code>.font = Font 1, Font 2, ..." entries in
                    // languages.list. The first candidate font that is installed
                    // on the system replaces all default UI fonts, for languages
                    // whose default fallback font renders poorly (e.g. the
                    // bitmap SimSun for Chinese merges strokes at small sizes)
                    // or lacks glyphs for the language's script.
                    final List<String> fontCandidates = WPI18n.getLanguageFontCandidates(Locale.getDefault().getLanguage());
                    if (! fontCandidates.isEmpty()) {
                        String uiFontName = null;
                        final String[] availableFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
                        outer:
                        for (String candidate: fontCandidates) {
                            for (String available: availableFonts) {
                                if (available.equalsIgnoreCase(candidate)) {
                                    uiFontName = available;
                                    break outer;
                                }
                            }
                        }
                        if (uiFontName != null) {
                            logger.debug("Installing configured UI font for language " + Locale.getDefault().getLanguage() + ": " + uiFontName);
                            final List<Object> fontKeys = new ArrayList<>();
                            for (Enumeration<Object> e = UIManager.getDefaults().keys(); e.hasMoreElements(); ) {
                                fontKeys.add(e.nextElement());
                            }
                            for (Object key: fontKeys) {
                                final Object value = UIManager.get(key);
                                if (value instanceof javax.swing.plaf.FontUIResource) {
                                    final javax.swing.plaf.FontUIResource oldFont = (javax.swing.plaf.FontUIResource) value;
                                    UIManager.put(key, new javax.swing.plaf.FontUIResource(uiFontName, oldFont.getStyle(), oldFont.getSize()));
                                }
                            }
                        } else {
                            logger.debug("None of the configured UI fonts for language " + Locale.getDefault().getLanguage() + " are installed; leaving default fonts in place");
                        }
                    }


                    if (((lookAndFeel == Configuration.LookAndFeel.DARK_METAL)
                            || (lookAndFeel == Configuration.LookAndFeel.DARK_NIMBUS)
                            || (lookAndFeel == Configuration.LookAndFeel.FLATLAF_CARBON)
                            || (lookAndFeel == Configuration.LookAndFeel.FLATLAF_DARK_PURPLE)
                            || (lookAndFeel == Configuration.LookAndFeel.FLATLAF_ONE_DARK)
                            || lookAndFeel.name().startsWith("RADIANCE_"))) {
                        // Patch some things to make dark themes look better
                        VoidRenderer.setColour(UIManager.getColor("Panel.background").getRGB());
                        if (lookAndFeel == Configuration.LookAndFeel.DARK_METAL) {
                            UIManager.put("ContentContainer.background", UIManager.getColor("desktop"));
                            UIManager.put("JideTabbedPane.foreground", new Color(222, 222, 222));
                        }
                    }
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
                    logger.warn("Could not install selected look and feel", e);
                }

                if (getUIScale() != 1.0f) {
                    // Scale the look and feel to the UI
                    GUIUtils.scaleLookAndFeel(getUIScale());
                }
            }

            // Don't paint values above sliders in GTK look and feel
            UIManager.put("Slider.paintValue", Boolean.FALSE);
            if (! "en".equals(org.pepsoft.worldpainter.WPI18n.getLanguage())) {
                org.pepsoft.worldpainter.WPI18n.installSwingDialogDefaults();
            }

            final App app = App.getInstance();
            if (JFrame.isDefaultLookAndFeelDecorated()) {
                // L41 (WorldPainter Languages): LAF-decorated frames maximize over the Windows taskbar;
                // constrain maximized bounds to the usable screen area (minus taskbar).
                app.setMaximizedBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
            }
            app.setVisible(true);
            app.localiseDockingButtonToolTips();
            ForkUpdateChecker.checkAtStartup(app);
            // Swing quirk:
            if (myConfig.isMaximised() && (System.getProperty("org.pepsoft.worldpainter.size") == null)) {
                app.setExtendedState(Frame.MAXIMIZED_BOTH);
            }

            // Do this later to give the app the chance to properly set itself up
            SwingUtilities.invokeLater(() -> {
                if (Version.isSnapshot() && ! myConfig.isMessageDisplayed(SNAPSHOT_MESSAGE_KEY)) {
                    String result = JOptionPane.showInputDialog(app, org.pepsoft.worldpainter.WPI18n.s("startup.snapshot.message"), org.pepsoft.worldpainter.WPI18n.s("startup.snapshot.title"), WARNING_MESSAGE);
                    if (result == null) {
                        // Cancel was pressed
                        System.exit(0);
                    }
                    while (! result.toLowerCase().replace(" ", "").equals("iunderstand")) {
                        DesktopUtils.beep();
                        result = JOptionPane.showInputDialog(app, org.pepsoft.worldpainter.WPI18n.s("startup.snapshot.message"), org.pepsoft.worldpainter.WPI18n.s("startup.snapshot.title"), WARNING_MESSAGE);
                        if (result == null) {
                            // Cancel was pressed
                            System.exit(0);
                        }
                    }
                    myConfig.setMessageDisplayed(SNAPSHOT_MESSAGE_KEY);
                }

                if (world != null) {
                    // On a Mac we may be doing this unnecessarily because we may be opening a .world file, but it has
                    // proven difficult to detect that. TODO
                    app.setWorld(world, true);
                } else if ((! autosaveInhibited) && myConfig.isAutosaveEnabled() && autosaveFile.isFile()) {
                    logger.info("Recovering autosaved world");
                    app.open(autosaveFile);
                    StartupMessages.addWarning(org.pepsoft.worldpainter.WPI18n.s("startup.warning.recoveredAutosave"));
                } else {
                    app.open(file);
                }
                for (String error: StartupMessages.getErrors()) {
                    beepAndShowError(app, error, org.pepsoft.worldpainter.WPI18n.s("startup.error.title"));
                }
                for (String warning: StartupMessages.getWarnings()) {
                    beepAndShowWarning(app, warning, org.pepsoft.worldpainter.WPI18n.s("startup.warning.title"));
                }
                for (String message: StartupMessages.getMessages()) {
                    showInfo(app, message, "Startup Message");
                }
                if (StartupMessages.getErrors().isEmpty() && StartupMessages.getWarnings().isEmpty() && StartupMessages.getMessages().isEmpty()) {
                    // Don't bother the user with this if we've already bothered them with errors and/or warnings
                    if (! DonationDialog.maybeShowDonationDialog(app)) {
                        MerchDialog.maybeShowMerchDialog(app);
                    }
                }
            });
        });
    }

    private static void configError(Throwable e) {
        // Try to preserve the config file
        File configFile = Configuration.getConfigFile();
        if (configFile.isFile() && configFile.canRead()) {
            File backupConfigFile = new File(configFile.getParentFile(), configFile.getName() + ".old");
            try {
                FileUtils.copyFileToFile(configFile, backupConfigFile, true);
            } catch (IOException e1) {
                logger.error("I/O error while trying to preserve faulty config file", e1);
            }
        }

        // Report the error
        logger.error("Exception while initialising configuration", e);
        StartupMessages.addError(java.text.MessageFormat.format(org.pepsoft.worldpainter.WPI18n.s("startup.error.configurationReset"), e.getClass().getSimpleName(), e.getMessage()));
    }

    private static final String SNAPSHOT_MESSAGE_KEY = "org.pepsoft.worldpainter.snapshotWarning";

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Main.class);

    static PrivateContext privateContext;
}
