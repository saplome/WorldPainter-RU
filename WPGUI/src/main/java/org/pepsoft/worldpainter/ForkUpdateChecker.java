/*
 * This file is part of WorldPainter Languages, an unofficial localization
 * fork of WorldPainter (https://github.com/saplome/WorldPainter-LANGUAGES).
 *
 * Copyright © 2026 saplome.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 */
package org.pepsoft.worldpainter;

import com.fasterxml.jackson.databind.JsonNode;
import org.pepsoft.util.DesktopUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.pepsoft.util.ObjectMapperHolder.OBJECT_MAPPER;

/** Checks GitHub Releases for newer WorldPainter Languages builds. */
public final class ForkUpdateChecker {
    private ForkUpdateChecker() {
    }

    public static void checkAtStartup(Window parent) {
        final Configuration config = Configuration.getInstance();
        if ((! config.isCheckForUpdates())
                || "true".equalsIgnoreCase(System.getProperty("org.pepsoft.worldpainter.disableUpdateCheck"))) {
            return;
        }
        check(parent, false);
    }

    public static void checkManually(Window parent) {
        check(parent, true);
    }

    private static void check(Window parent, boolean manual) {
        final Thread thread = new Thread(() -> {
            try {
                final Release release = loadLatestRelease();
                if (release.revision > CURRENT_FORK_REVISION) {
                    final Configuration config = Configuration.getInstance();
                    if (manual || (config.getDismissedForkUpdateRevision() != release.revision)) {
                        SwingUtilities.invokeLater(() -> showUpdateAvailable(parent, release));
                    }
                } else if (manual) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                            WPI18n.s("ui.update.upToDate.message"), WPI18n.s("ui.update.upToDate.title"),
                            JOptionPane.INFORMATION_MESSAGE));
                }
            } catch (Exception e) {
                logger.warn("Could not check WorldPainter Languages updates", e);
                if (manual) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                            WPI18n.s("ui.update.failed.message"), WPI18n.s("ui.update.failed.title"),
                            JOptionPane.WARNING_MESSAGE));
                }
            }
        }, "WorldPainter Languages Update Checker");
        thread.setDaemon(true);
        thread.start();
    }

    private static Release loadLatestRelease() throws Exception {
        final HttpURLConnection connection = (HttpURLConnection) new URL(LATEST_RELEASE_API).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "WorldPainter-Languages-L" + CURRENT_FORK_REVISION);
        try {
            final int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("GitHub returned HTTP " + responseCode);
            }
            try (InputStream in = connection.getInputStream()) {
                final JsonNode json = OBJECT_MAPPER.readTree(in);
                final String tag = json.path("tag_name").asText();
                final String pageUrl = json.path("html_url").asText();
                final Matcher matcher = FORK_REVISION_PATTERN.matcher(tag);
                if ((! matcher.find()) || pageUrl.isEmpty()) {
                    throw new IllegalStateException("Unsupported GitHub release: " + tag);
                }
                return new Release(Integer.parseInt(matcher.group(1)), tag, pageUrl);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static void showUpdateAvailable(Window parent, Release release) {
        final JLabel message = new JLabel(MessageFormat.format(WPI18n.s("ui.update.available.message"),
                "L" + CURRENT_FORK_REVISION, release.tag));
        final JCheckBox dontRemind = new JCheckBox(WPI18n.s("ui.update.dontRemindThisVersion"));
        final JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.add(message, BorderLayout.CENTER);
        panel.add(dontRemind, BorderLayout.SOUTH);
        final Object[] options = {WPI18n.s("ui.update.downloadNow"), WPI18n.s("ui.update.notNow")};
        final int answer = JOptionPane.showOptionDialog(parent, panel, WPI18n.s("ui.update.available.title"),
                JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
        if (dontRemind.isSelected()) {
            Configuration.getInstance().setDismissedForkUpdateRevision(release.revision);
        }
        if (answer == JOptionPane.YES_OPTION) {
            try {
                DesktopUtils.open(new URL(release.pageUrl));
            } catch (Exception e) {
                logger.error("Could not open release page " + release.pageUrl, e);
                JOptionPane.showMessageDialog(parent, WPI18n.s("ui.update.failed.message"),
                        WPI18n.s("ui.update.failed.title"), JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private static final class Release {
        private Release(int revision, String tag, String pageUrl) {
            this.revision = revision;
            this.tag = tag;
            this.pageUrl = pageUrl;
        }

        final int revision;
        final String tag, pageUrl;
    }

    public static final int CURRENT_FORK_REVISION = 2;
    private static final String LATEST_RELEASE_API = "https://api.github.com/repos/saplome/WorldPainter-LANGUAGES/releases/latest";
    private static final Pattern FORK_REVISION_PATTERN = Pattern.compile("(?:^|-)L(\\d+)(?:$|[^0-9])", Pattern.CASE_INSENSITIVE);
    private static final Logger logger = LoggerFactory.getLogger(ForkUpdateChecker.class);
}
