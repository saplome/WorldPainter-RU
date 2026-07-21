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

package org.pepsoft.worldpainter.layers.exporters;

import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.exporting.AbstractLayerExporter;
import org.pepsoft.worldpainter.exporting.Fixup;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;
import org.pepsoft.worldpainter.exporting.SecondPassLayerExporter;
import org.pepsoft.worldpainter.layers.FloodWithLava;
import org.pepsoft.worldpainter.layers.Icebergs;

import java.awt.*;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.pepsoft.minecraft.Material.ICE;
import static org.pepsoft.minecraft.Material.PACKED_ICE;
import static org.pepsoft.minecraft.Material.SNOW_BLOCK;
import static org.pepsoft.worldpainter.Constants.LARGE_BLOBS;
import static org.pepsoft.worldpainter.Constants.SMALL_BLOBS;
import static org.pepsoft.worldpainter.exporting.SecondPassLayerExporter.Stage.ADD_FEATURES;

/**
 * Exporter for the Icebergs layer: generates icebergs of packed ice floating on open water,
 * shaped by two Perlin noise fields. A large-scale noise determines where icebergs occur and
 * how big they are (a dome above the waterline and a keel below it, roughly three times as
 * deep as the dome is high, like real icebergs), and a small-scale noise adds ragged detail
 * and occasional snow-block caps. The brush intensity controls the iceberg density.
 */
public class IcebergsExporter extends AbstractLayerExporter<Icebergs> implements SecondPassLayerExporter {
    public IcebergsExporter(Dimension dimension, Platform platform, ExporterSettings settings) {
        super(dimension, platform, (settings != null) ? settings : new IcebergsSettings(), Icebergs.INSTANCE);
    }

    @Override
    public Set<Stage> getStages() {
        return singleton(ADD_FEATURES);
    }

    @Override
    public List<Fixup> addFeatures(Rectangle area, Rectangle exportedArea, MinecraftWorld minecraftWorld) {
        final long seed = dimension.getSeed();
        if ((seed + SEED_OFFSET_SHAPE) != shapeNoise.getSeed()) {
            shapeNoise.setSeed(seed + SEED_OFFSET_SHAPE);
            detailNoise.setSeed(seed + SEED_OFFSET_DETAIL);
            heightNoise.setSeed(seed + SEED_OFFSET_HEIGHT);
        }
        for (int x = area.x; x < area.x + area.width; x++) {
            for (int y = area.y; y < area.y + area.height; y++) {
                final int value = dimension.getLayerValueAt(Icebergs.INSTANCE, x, y);
                if (value < 1) {
                    continue;
                }
                final int waterLevel = dimension.getWaterLevelAt(x, y);
                final int terrainHeight = dimension.getIntHeightAt(x, y);
                if ((waterLevel <= terrainHeight) || dimension.getBitLayerValueAt(FloodWithLava.INSTANCE, x, y)) {
                    // Only generate icebergs on open water
                    continue;
                }
                // Large-scale noise: where the icebergs are; small-scale noise: ragged detail
                final float shape = shapeNoise.getPerlinNoise(x / LARGE_BLOBS, y / LARGE_BLOBS, 0.0f) + 0.5f;
                final float detail = detailNoise.getPerlinNoise(x / SMALL_BLOBS, y / SMALL_BLOBS, 0.0f);
                final float cutoff = 1.0f - (value / 15.0f) * 0.55f;
                final float strength = shape + detail * 0.15f - cutoff;
                if (strength <= 0.0f) {
                    continue;
                }
                // Steep flanks: the body reaches its full size within a narrow band from the edge
                final float body = Math.min(strength / 0.1f, 1.0f);
                // Per-berg height variation: most bergs are low, some form tall spires
                final float heightVar = heightNoise.getPerlinNoise(x / LARGE_BLOBS, y / LARGE_BLOBS, 0.0f) + 0.5f;
                // Tall spire bonus: where the height noise peaks, bergs get dramatically taller
                final float spire = Math.max(heightVar - 0.65f, 0.0f) * 45.0f;
                final float maxDome = 4.0f + heightVar * heightVar * 24.0f + spire;
                final int domeHeight = Math.max(Math.min(Math.round(body * maxDome + detail * 5.0f), 36), 0);
                final int keelDepth = Math.max(Math.min(Math.round(body * (maxDome * 2.5f + 6.0f) + detail * 8.0f), 55), 1);
                final int top = Math.min(waterLevel + domeHeight, maxZ);
                final int bottom = Math.max(waterLevel - keelDepth, terrainHeight + 1);
                for (int z = bottom; z <= top; z++) {
                    minecraftWorld.setMaterialAt(x, y, z, PACKED_ICE);
                }
                if ((top > waterLevel + 3) && (domeHeight >= maxDome * 0.75f)) {
                    // Snow only on the very tops of the bergs
                    minecraftWorld.setMaterialAt(x, y, top, SNOW_BLOCK);
                } else if ((top == waterLevel) && (detail < -0.15f)) {
                    // Translucent ice patches on the low floes at the waterline
                    minecraftWorld.setMaterialAt(x, y, top, ICE);
                }
            }
        }
        return null;
    }

    private final PerlinNoise shapeNoise = new PerlinNoise(0);
    private final PerlinNoise detailNoise = new PerlinNoise(0);
    private final PerlinNoise heightNoise = new PerlinNoise(0);

    private static final long SEED_OFFSET_SHAPE = 89, SEED_OFFSET_DETAIL = 97, SEED_OFFSET_HEIGHT = 101;

    public static class IcebergsSettings implements ExporterSettings {
        @Override
        public boolean isApplyEverywhere() {
            return false;
        }

        @Override
        public Icebergs getLayer() {
            return Icebergs.INSTANCE;
        }

        @Override
        public boolean equals(Object obj) {
            return (obj != null) && (getClass() == obj.getClass());
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public IcebergsSettings clone() {
            try {
                return (IcebergsSettings) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        private static final long serialVersionUID = 1L;
    }
}
