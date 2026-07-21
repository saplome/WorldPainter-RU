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

package org.pepsoft.worldpainter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

import static java.awt.Color.GRAY;

public class NoiseSettingsTableCellRenderer extends DefaultTableCellRenderer {
    public NoiseSettingsTableCellRenderer() {
        setHorizontalAlignment(TRAILING);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value instanceof NoiseSettings) {
            if (! isSelected) {
                setForeground(table.getForeground());
            }
            NoiseSettings noiseSettings = (NoiseSettings) value;
            setText(noiseSettings.getRange() + ", " + Math.round(noiseSettings.getScale() * 100) + ", " + noiseSettings.getRoughness());
        } else {
            if (! isSelected) {
                setForeground(GRAY);
            }
            setText(org.pepsoft.worldpainter.WPI18n.s("ui.label.noVariation"));
        }
        return this;
    }
}