package org.pepsoft.worldpainter.tools;

import org.pepsoft.worldpainter.Constants;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Pepijn Schmitz on 21-09-16.
 */
public class BiomeAlgorithmListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof Integer) {
            switch ((Integer) value) {
                case Constants.BIOME_ALGORITHM_1_1:
                    setText("Minecraft 1.1");
                    break;
                case Constants.BIOME_ALGORITHM_1_2_AND_1_3_DEFAULT:
                    setText(org.pepsoft.worldpainter.WPI18n.s("ui.t.minecraft_1_6_default_or_1_2_1_5"));
                    break;
                case Constants.BIOME_ALGORITHM_1_3_LARGE:
                    setText(org.pepsoft.worldpainter.WPI18n.s("ui.t.minecraft_1_6_large_biomes_or_1_3_1_5"));
                    break;
                case Constants.BIOME_ALGORITHM_1_7_DEFAULT:
                    setText(org.pepsoft.worldpainter.WPI18n.s("ui.t.minecraft_1_10_default_or_1_7_1_9"));
                    break;
                case Constants.BIOME_ALGORITHM_1_7_LARGE:
                    setText(org.pepsoft.worldpainter.WPI18n.s("ui.t.minecraft_1_10_large_biomes_or_1_7_1_9"));
                    break;
            }
        }
        return this;
    }
}
