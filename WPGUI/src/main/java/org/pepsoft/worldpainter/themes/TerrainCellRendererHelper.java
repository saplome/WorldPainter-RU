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
package org.pepsoft.worldpainter.themes;

import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Terrain;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author pepijn
 */
class TerrainCellRendererHelper {
    TerrainCellRendererHelper(ColourScheme colourScheme) {
        this.colourScheme = colourScheme;
    }
    
    void configure(JLabel label, Terrain terrain) {
        if (terrain != null) {
            BufferedImage image = terrain.getScaledIcon(16, colourScheme);
            ImageIcon icon = iconCache.get(image);
            if (icon == null) {
                icon = new ImageIcon(image);
                iconCache.put(image, icon);
            }
            label.setIcon(icon);
            label.setText(org.pepsoft.worldpainter.WPI18n.terrainName(terrain));
        }
    }
    
    private final ColourScheme colourScheme;
    private final Map<BufferedImage, ImageIcon> iconCache = new HashMap<>();
}