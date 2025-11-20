/*
Project: DeepCraft
File: BiomeUtil.java
Author: Vitor
Date: 11/20/2025

License:
This file is part of the DeepCraft project, licensed under
the GNU General Public License v3.0 (GPLv3).

You may redistribute and/or modify this file under the terms of the GPLv3.
This file is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the LICENSE file in the DeepCraft GitHub repository for more details:
https://github.com/SoldadoHumano/DEEPCRAFT-1.0/blob/main/LICENSE
*/

package com.vitor.BiomeManager;

import com.vitor.vLobby;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Utility class for applying biomes to large areas efficiently using batch processing
 */
public class BiomeUtil {

    /**
     * Applies a biome to a cuboid area in batches to prevent server lag
     *
     * @param world The world to modify
     * @param minX Minimum X coordinate
     * @param minY Minimum Y coordinate
     * @param minZ Minimum Z coordinate
     * @param maxX Maximum X coordinate
     * @param maxY Maximum Y coordinate
     * @param maxZ Maximum Z coordinate
     * @param biome The biome to apply
     * @param batchSize Number of blocks to process per tick
     * @param onFinish Callback to execute when completed
     */
    public static void applyBiomeArea(World world, int minX, int minY, int minZ,
                                      int maxX, int maxY, int maxZ, Biome biome,
                                      int batchSize, Runnable onFinish) {

        new BukkitRunnable() {
            private int currentX = minX;
            private int currentY = minY;
            private int currentZ = minZ;

            @Override
            public void run() {
                int processedCount = 0;

                while (processedCount < batchSize) {
                    // Apply biome to current position
                    world.setBiome(currentX, currentY, currentZ, biome);

                    // Move to next position
                    if (!moveToNextPosition()) {
                        // Finished entire area
                        cancel();
                        if (onFinish != null) onFinish.run();
                        return;
                    }

                    processedCount++;
                }
            }

            /**
             * Advances to the next position in the cuboid
             * @return false if finished entire area
             */
            private boolean moveToNextPosition() {
                currentX++;
                if (currentX > maxX) {
                    currentX = minX;
                    currentY++;
                }
                if (currentY > maxY) {
                    currentY = minY;
                    currentZ++;
                }
                if (currentZ > maxZ) {
                    return false; // Finished
                }
                return true;
            }

        }.runTaskTimer(vLobby.getInstance(), 1L, 1L); // Start after 1 tick, repeat every 1 tick
    }
}