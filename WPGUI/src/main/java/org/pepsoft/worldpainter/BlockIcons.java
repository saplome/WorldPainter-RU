/*
 * WorldPainter Languages, an unofficial localization fork of WorldPainter
 * (https://github.com/saplome/WorldPainter-LANGUAGES).
 * Copyright © 2026 saplome
 *
 * WorldPainter itself is Copyright © pepsoft.org, The Netherlands.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.pepsoft.worldpainter;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Small isometric icons for Minecraft blocks, shown in the block selection
 * UI. Icons are stored as 32x32 PNGs on the classpath under
 * {@code /org/pepsoft/worldpainter/blockicons/&lt;simple_name&gt;.png} and
 * rendered at 16x16. Missing icons are cached as {@code null} so lookups
 * stay cheap.
 */
public final class BlockIcons {
    private BlockIcons() {
        // Do nothing
    }

    /**
     * Get the icon for a block by its simple name (the part of the block
     * identifier after the namespace, e.g. {@code acacia_door}).
     *
     * @param simpleName The simple name of the block.
     * @return The icon for the block, or {@code null} if there is none.
     */
    public static synchronized Icon get(String simpleName) {
        if ((simpleName == null) || simpleName.isEmpty()) {
            return null;
        }
        if (CACHE.containsKey(simpleName)) {
            return CACHE.get(simpleName);
        }
        Icon icon = null;
        try (InputStream in = BlockIcons.class.getResourceAsStream("/org/pepsoft/worldpainter/blockicons/" + simpleName + ".png")) {
            if (in != null) {
                final BufferedImage image = ImageIO.read(in);
                if (image != null) {
                    icon = new ImageIcon(image.getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH));
                }
            }
        } catch (IOException e) {
            // No icon then
        }
        CACHE.put(simpleName, icon);
        return icon;
    }

    private static final Map<String, Icon> CACHE = new HashMap<>();
    private static final int ICON_SIZE = 16;
}
