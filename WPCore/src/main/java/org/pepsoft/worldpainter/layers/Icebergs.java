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

package org.pepsoft.worldpainter.layers;

/**
 * Noise-based icebergs of packed ice floating on water.
 */
public class Icebergs extends Layer {
    private Icebergs() {
        super("org.pepsoft.Icebergs", "[BETA] \u0410\u0439\u0441\u0431\u0435\u0440\u0433\u0438", "\u0413\u0435\u043d\u0435\u0440\u0438\u0440\u0443\u0435\u0442 \u0430\u0439\u0441\u0431\u0435\u0440\u0433\u0438 \u0438\u0437 \u043f\u043b\u043e\u0442\u043d\u043e\u0433\u043e \u043b\u044c\u0434\u0430 \u043d\u0430 \u0432\u043e\u0434\u0435", DataSize.NIBBLE, false, 61);
    }

    public static final Icebergs INSTANCE = new Icebergs();

    private static final long serialVersionUID = 1L;
}
