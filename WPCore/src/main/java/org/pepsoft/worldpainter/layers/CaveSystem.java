/*
 * WorldPainter Languages, an unofficial localization fork of WorldPainter
 * (https://github.com/saplome/WorldPainter-LANGUAGES).
 * Copyright © 2026 saplome
 * Licensed under the GNU General Public License, version 3.
 */
package org.pepsoft.worldpainter.layers;

public final class CaveSystem extends Layer {
    private CaveSystem() {
        super("org.pepsoft.CaveSystem", "[BETA] Система пещер",
                "Трёхмерная система пещер с крупными залами, тоннелями, аквиферами, пышными и натёчными биомами",
                DataSize.NIBBLE, false, 24);
    }

    public static final CaveSystem INSTANCE = new CaveSystem();
    private static final long serialVersionUID = 1L;
}
