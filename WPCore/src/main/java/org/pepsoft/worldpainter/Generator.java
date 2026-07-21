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

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 *
 * @author pepijn
 */
public enum Generator {
    DEFAULT("Default"), FLAT("Superflat"), LARGE_BIOMES("Large Biomes"), AMPLIFIED("Amplified"), BUFFET("Buffet"), CUSTOM("Custom"), CUSTOMIZED("Customized"), UNKNOWN("Unknown"), NETHER("The Nether"), END("The End");

    Generator(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        try {
            return ResourceBundle.getBundle("org.pepsoft.worldpainter.resources.Generator", Locale.getDefault(), Utf8Control.INSTANCE).getString(name());
        } catch (MissingResourceException e) {
            return displayName;
        }
    }

    private final String displayName;
}
