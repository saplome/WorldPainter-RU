/*
 * WorldPainter i18n helper.
 *
 * Part of the community EN/RU localization pack. Provides a single static
 * accessor for externalized UI strings. Strings live in the resource bundle
 * org.pepsoft.worldpainter.resources.strings (strings.properties = English
 * base, strings_ru.properties = Russian), which is shipped in the WPGUI module
 * and available on the runtime classpath.
 *
 * This class lives in WPCore (not WPGUI) so that both WPCore and WPGUI classes
 * can reference it: WPGUI depends on WPCore, but not the other way around.
 *
 * This class never throws: if a key is missing it returns the key itself so
 * the UI keeps working even with an incomplete bundle.
 */
package org.pepsoft.worldpainter;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static org.pepsoft.worldpainter.Constants.DIM_END;
import static org.pepsoft.worldpainter.Constants.DIM_NETHER;
import static org.pepsoft.worldpainter.Constants.DIM_NORMAL;

public final class WPI18n {
    private static final String BUNDLE_NAME = "org.pepsoft.worldpainter.resources.strings";

    private WPI18n() {
    }

    /**
     * Look up a UI string by key for the current default Locale.
     * ResourceBundle caches per (base name, locale), so this is cheap.
     *
     * @param key the externalized string key
     * @return the localized string, or the key itself if not found
     */
    public static String s(String key) {
        if (key == null) {
            return "";
        }
        try {
            return ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault()).getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Persist the desired language ("en" or "ru") and update the running JVM's
     * default Locale. A restart is recommended for the change to fully apply to
     * every already-built window.
     *
     * @param lang "ru" for Russian, anything else for English
     */
    public static String biome(String englishName) {
        if (englishName == null) {
            return null;
        }
        final String key = "biome." + englishName.replaceAll("[^A-Za-z0-9]+", "_");
        final String v = s(key);
        return v.equals(key) ? englishName : v;
    }

    public static String plant(String englishName) {
        if (englishName == null) {
            return null;
        }
        final String key = "plant." + englishName.replaceAll("[^A-Za-z0-9]+", "_");
        final String v = s(key);
        return v.equals(key) ? englishName : v;
    }

    public static String colour(String englishName) {
        if (englishName == null) {
            return null;
        }
        final String key = "colour." + englishName.replaceAll("[^A-Za-z0-9]+", "_");
        final String v = s(key);
        return v.equals(key) ? englishName : v;
    }

    public static String dimensionName(Dimension dimension) {
        if (dimension == null) {
            return "";
        }
        final Dimension.Anchor anchor = dimension.getAnchor();
        final String name = dimension.getName();
        return ((anchor != null) && ((name == null) || name.equals(anchor.getDefaultName())))
                ? dimensionName(anchor)
                : name;
    }

    public static String dimensionName(Dimension.Anchor anchor) {
        final StringBuilder sb = new StringBuilder();
        switch (anchor.dim) {
            case DIM_NORMAL:
                sb.append(s("ui.dimension.surface"));
                break;
            case DIM_NETHER:
                sb.append(s("ui.dimension.nether"));
                break;
            case DIM_END:
                sb.append(s("ui.dimension.end"));
                break;
            default:
                sb.append(MessageFormat.format(s("ui.dimension.generic"), anchor.dim));
                break;
        }
        switch (anchor.role) {
            case MASTER:
                sb.append(s("ui.dimension.masterSuffix"));
                break;
            case CAVE_FLOOR:
                sb.append(s("ui.dimension.caveFloorSuffix"));
                break;
            case FLOATING_FLOOR:
                sb.append(s("ui.dimension.floatingFloorSuffix"));
                break;
        }
        if (anchor.invert) {
            sb.append(s("ui.dimension.ceilingSuffix"));
        }
        if (anchor.id != 0) {
            sb.append(' ').append(anchor.id);
        }
        return sb.toString();
    }

    /**
     * Install Russian translations for the standard Swing dialogs (file chooser,
     * colour chooser, option pane). Call once at startup after the look-and-feel
     * has been set, only when the Russian locale is active.
     */
    public static void installSwingRussianDefaults() {
        javax.swing.UIManager.put("FileChooser.lookInLabelText", "\u041f\u0430\u043f\u043a\u0430:");
        javax.swing.UIManager.put("FileChooser.saveInLabelText", "\u041f\u0430\u043f\u043a\u0430:");
        javax.swing.UIManager.put("FileChooser.fileNameLabelText", "\u0418\u043c\u044f \u0444\u0430\u0439\u043b\u0430:");
        javax.swing.UIManager.put("FileChooser.filesOfTypeLabelText", "\u0422\u0438\u043f \u0444\u0430\u0439\u043b\u043e\u0432:");
        javax.swing.UIManager.put("FileChooser.upFolderToolTipText", "\u041d\u0430 \u0443\u0440\u043e\u0432\u0435\u043d\u044c \u0432\u0432\u0435\u0440\u0445");
        javax.swing.UIManager.put("FileChooser.homeFolderToolTipText", "\u0414\u043e\u043c\u0430\u0448\u043d\u044f\u044f \u043f\u0430\u043f\u043a\u0430");
        javax.swing.UIManager.put("FileChooser.newFolderToolTipText", "\u0421\u043e\u0437\u0434\u0430\u0442\u044c \u043d\u043e\u0432\u0443\u044e \u043f\u0430\u043f\u043a\u0443");
        javax.swing.UIManager.put("FileChooser.listViewButtonToolTipText", "\u0421\u043f\u0438\u0441\u043e\u043a");
        javax.swing.UIManager.put("FileChooser.detailsViewButtonToolTipText", "\u0422\u0430\u0431\u043b\u0438\u0446\u0430");
        javax.swing.UIManager.put("FileChooser.viewMenuButtonToolTipText", "\u041c\u0435\u043d\u044e \u0432\u0438\u0434\u0430");
        javax.swing.UIManager.put("FileChooser.viewMenuButtonAccessibleName", "\u041c\u0435\u043d\u044e \u0432\u0438\u0434\u0430");
        javax.swing.UIManager.put("FileChooser.fileNameHeaderText", "\u0418\u043c\u044f");
        javax.swing.UIManager.put("FileChooser.fileSizeHeaderText", "\u0420\u0430\u0437\u043c\u0435\u0440");
        javax.swing.UIManager.put("FileChooser.fileTypeHeaderText", "\u0422\u0438\u043f");
        javax.swing.UIManager.put("FileChooser.fileDateHeaderText", "\u0418\u0437\u043c\u0435\u043d\u0451\u043d");
        javax.swing.UIManager.put("FileChooser.fileAttrHeaderText", "\u0410\u0442\u0440\u0438\u0431\u0443\u0442\u044b");
        javax.swing.UIManager.put("FileChooser.openButtonText", "\u041e\u0442\u043a\u0440\u044b\u0442\u044c");
        javax.swing.UIManager.put("FileChooser.openButtonToolTipText", "\u041e\u0442\u043a\u0440\u044b\u0442\u044c \u0432\u044b\u0431\u0440\u0430\u043d\u043d\u044b\u0439 \u0444\u0430\u0439\u043b");
        javax.swing.UIManager.put("FileChooser.cancelButtonText", "\u041e\u0442\u043c\u0435\u043d\u0430");
        javax.swing.UIManager.put("FileChooser.cancelButtonToolTipText", "\u0417\u0430\u043a\u0440\u044b\u0442\u044c \u0434\u0438\u0430\u043b\u043e\u0433");
        javax.swing.UIManager.put("FileChooser.saveButtonText", "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c");
        javax.swing.UIManager.put("FileChooser.saveButtonToolTipText", "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0432\u044b\u0431\u0440\u0430\u043d\u043d\u044b\u0439 \u0444\u0430\u0439\u043b");
        javax.swing.UIManager.put("FileChooser.updateButtonText", "\u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c");
        javax.swing.UIManager.put("FileChooser.helpButtonText", "\u0421\u043f\u0440\u0430\u0432\u043a\u0430");
        javax.swing.UIManager.put("FileChooser.directoryOpenButtonText", "\u041e\u0442\u043a\u0440\u044b\u0442\u044c");
        javax.swing.UIManager.put("FileChooser.directoryOpenButtonToolTipText", "\u041e\u0442\u043a\u0440\u044b\u0442\u044c \u0432\u044b\u0431\u0440\u0430\u043d\u043d\u0443\u044e \u043f\u0430\u043f\u043a\u0443");
        javax.swing.UIManager.put("FileChooser.openDialogTitleText", "\u041e\u0442\u043a\u0440\u044b\u0442\u044c");
        javax.swing.UIManager.put("FileChooser.saveDialogTitleText", "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c");
        javax.swing.UIManager.put("FileChooser.acceptAllFileFilterText", "\u0412\u0441\u0435 \u0444\u0430\u0439\u043b\u044b");
        javax.swing.UIManager.put("FileChooser.newFolderButtonText", "\u0421\u043e\u0437\u0434\u0430\u0442\u044c \u043f\u0430\u043f\u043a\u0443");
        javax.swing.UIManager.put("FileChooser.renameFileButtonText", "\u041f\u0435\u0440\u0435\u0438\u043c\u0435\u043d\u043e\u0432\u0430\u0442\u044c");
        javax.swing.UIManager.put("FileChooser.deleteFileButtonText", "\u0423\u0434\u0430\u043b\u0438\u0442\u044c");
        javax.swing.UIManager.put("FileChooser.filterLabelText", "\u0422\u0438\u043f \u0444\u0430\u0439\u043b\u043e\u0432:");
        javax.swing.UIManager.put("FileChooser.foldersLabelText", "\u041f\u0430\u043f\u043a\u0438");
        javax.swing.UIManager.put("FileChooser.filesLabelText", "\u0424\u0430\u0439\u043b\u044b");
        javax.swing.UIManager.put("FileChooser.pathLabelText", "\u041f\u0443\u0442\u044c:");
        javax.swing.UIManager.put("FileChooser.newFolderErrorText", "\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u0440\u0438 \u0441\u043e\u0437\u0434\u0430\u043d\u0438\u0438 \u043f\u0430\u043f\u043a\u0438");
        javax.swing.UIManager.put("FileChooser.other.newFolder", "\u041d\u043e\u0432\u0430\u044f \u043f\u0430\u043f\u043a\u0430");
        javax.swing.UIManager.put("FileChooser.win32.newFolder", "\u041d\u043e\u0432\u0430\u044f \u043f\u0430\u043f\u043a\u0430");
        javax.swing.UIManager.put("ColorChooser.okText", "OK");
        javax.swing.UIManager.put("ColorChooser.cancelText", "\u041e\u0442\u043c\u0435\u043d\u0430");
        javax.swing.UIManager.put("ColorChooser.resetText", "\u0421\u0431\u0440\u043e\u0441\u0438\u0442\u044c");
        javax.swing.UIManager.put("ColorChooser.previewText", "\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440");
        javax.swing.UIManager.put("ColorChooser.sampleText", "\u041f\u0440\u0438\u043c\u0435\u0440 \u0442\u0435\u043a\u0441\u0442\u0430");
        javax.swing.UIManager.put("ColorChooser.swatchesNameText", "\u041e\u0431\u0440\u0430\u0437\u0446\u044b");
        javax.swing.UIManager.put("ColorChooser.swatchesRecentText", "\u041d\u0435\u0434\u0430\u0432\u043d\u0438\u0435:");
        javax.swing.UIManager.put("ColorChooser.rgbNameText", "RGB");
        javax.swing.UIManager.put("ColorChooser.hsvNameText", "HSV");
        javax.swing.UIManager.put("ColorChooser.hslNameText", "HSL");
        javax.swing.UIManager.put("ColorChooser.cmykNameText", "CMYK");
        javax.swing.UIManager.put("ColorChooser.rgbRedText", "\u041a\u0440\u0430\u0441\u043d\u044b\u0439");
        javax.swing.UIManager.put("ColorChooser.rgbGreenText", "\u0417\u0435\u043b\u0451\u043d\u044b\u0439");
        javax.swing.UIManager.put("ColorChooser.rgbBlueText", "\u0421\u0438\u043d\u0438\u0439");
        javax.swing.UIManager.put("ColorChooser.rgbAlphaText", "\u041f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c");
        javax.swing.UIManager.put("ColorChooser.rgbHexCodeText", "\u041a\u043e\u0434 \u0446\u0432\u0435\u0442\u0430");
        javax.swing.UIManager.put("ColorChooser.hsvHueText", "\u041e\u0442\u0442\u0435\u043d\u043e\u043a");
        javax.swing.UIManager.put("ColorChooser.hsvSaturationText", "\u041d\u0430\u0441\u044b\u0449\u0435\u043d\u043d\u043e\u0441\u0442\u044c");
        javax.swing.UIManager.put("ColorChooser.hsvValueText", "\u042f\u0440\u043a\u043e\u0441\u0442\u044c");
        javax.swing.UIManager.put("ColorChooser.hsvTransparencyText", "\u041f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c");
        javax.swing.UIManager.put("ColorChooser.hslHueText", "\u041e\u0442\u0442\u0435\u043d\u043e\u043a");
        javax.swing.UIManager.put("ColorChooser.hslSaturationText", "\u041d\u0430\u0441\u044b\u0449\u0435\u043d\u043d\u043e\u0441\u0442\u044c");
        javax.swing.UIManager.put("ColorChooser.hslLightnessText", "\u0421\u0432\u0435\u0442\u043b\u043e\u0442\u0430");
        javax.swing.UIManager.put("ColorChooser.hslTransparencyText", "\u041f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c");
        javax.swing.UIManager.put("ColorChooser.cmykCyanText", "\u0411\u0438\u0440\u044e\u0437\u043e\u0432\u044b\u0439");
        javax.swing.UIManager.put("ColorChooser.cmykMagentaText", "\u041f\u0443\u0440\u043f\u0443\u0440\u043d\u044b\u0439");
        javax.swing.UIManager.put("ColorChooser.cmykYellowText", "\u0416\u0451\u043b\u0442\u044b\u0439");
        javax.swing.UIManager.put("ColorChooser.cmykBlackText", "\u0427\u0451\u0440\u043d\u044b\u0439");
        javax.swing.UIManager.put("ColorChooser.cmykAlphaText", "\u041f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c");
        javax.swing.UIManager.put("OptionPane.yesButtonText", "\u0414\u0430");
        javax.swing.UIManager.put("OptionPane.noButtonText", "\u041d\u0435\u0442");
        javax.swing.UIManager.put("OptionPane.okButtonText", "OK");
        javax.swing.UIManager.put("OptionPane.cancelButtonText", "\u041e\u0442\u043c\u0435\u043d\u0430");
    }

    public static void setLanguage(String lang) {
        try {
            java.util.prefs.Preferences.userRoot().node("org/pepsoft/worldpainter")
                    .put("language", "ru".equalsIgnoreCase(lang) ? "ru" : "en");
        } catch (Exception e) {
            // ignore; language simply won't persist
        }
        Locale.setDefault("ru".equalsIgnoreCase(lang) ? new Locale("ru") : Locale.US);
    }

    /**
     * @return the currently saved language code ("en" or "ru")
     */
    public static String getLanguage() {
        try {
            return java.util.prefs.Preferences.userRoot().node("org/pepsoft/worldpainter")
                    .get("language", "en");
        } catch (Exception e) {
            return "en";
        }
    }
}
