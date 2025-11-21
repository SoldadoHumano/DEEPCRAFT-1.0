/*
Project: DeepCraft
File: ConfigManager.java
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

package com.vitor;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;

/**
 * Manages configurations for the vTabList plugin
 * Responsible for loading, accessing, and reloading plugin configurations
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private final Map<String, Map<String, LineConfig>> lineCache;

    /**
     * Constructor for ConfigManager
     * @param plugin The main plugin instance
     */
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.lineCache = new HashMap<>();
        loadConfigs();
    }

    /**
     * Loads configurations from config.yml file
     */
    public void loadConfigs() {
        try {
            plugin.saveDefaultConfig();
            config = plugin.getConfig();
            lineCache.clear(); // Clear cache on reload
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading configurations", e);
        }
    }

    /**
     * Reloads configurations from file
     */
    public void reloadConfigs() {
        try {
            plugin.reloadConfig();
            config = plugin.getConfig();
            lineCache.clear();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error reloading configurations", e);
        }
    }

    /**
     * Gets the update interval in ticks
     * @return Update interval, minimum 1 tick
     */
    public int getUpdateInterval() {
        return Math.max(1, config.getInt("update-interval", 20));
    }

    /**
     * Checks if header is enabled
     * @return true if header is enabled
     */
    public boolean isHeaderEnabled() {
        return config.getBoolean("header.enabled", true);
    }

    /**
     * Checks if footer is enabled
     * @return true if footer is enabled
     */
    public boolean isFooterEnabled() {
        return config.getBoolean("footer.enabled", true);
    }

    /**
     * Gets header lines with cache for better performance
     * @return Map of header line configurations
     */
    public Map<String, LineConfig> getHeaderLines() {
        return getCachedLines("header");
    }

    /**
     * Gets footer lines with cache for better performance
     * @return Map of footer line configurations
     */
    public Map<String, LineConfig> getFooterLines() {
        return getCachedLines("footer");
    }

    /**
     * Gets lines from configuration with caching
     * @param section Configuration section name
     * @return Map of line configurations
     */
    private Map<String, LineConfig> getCachedLines(String section) {
        return lineCache.computeIfAbsent(section, s -> loadLinesFromConfig(s));
    }

    /**
     * Loads lines from configuration without caching
     * @param section Configuration section name
     * @return Map of line configurations
     */
    private Map<String, LineConfig> loadLinesFromConfig(String section) {
        Map<String, LineConfig> lines = new LinkedHashMap<>();

        try {
            ConfigurationSection linesSection = config.getConfigurationSection(section + ".lines");
            if (linesSection == null) {
                plugin.getLogger().warning("Section " + section + ".lines not found in configuration");
                return lines;
            }

            for (String key : linesSection.getKeys(false)) {
                ConfigurationSection lineSection = linesSection.getConfigurationSection(key);
                if (lineSection != null) {
                    LineConfig lineConfig = createLineConfig(lineSection);
                    if (lineConfig != null) {
                        lines.put(key, lineConfig);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading lines from section: " + section, e);
        }

        return lines;
    }

    /**
     * Factory method for creating line configurations
     * @param section Configuration section for the line
     * @return LineConfig instance or null if invalid
     */
    private LineConfig createLineConfig(ConfigurationSection section) {
        try {
            List<String> texts = section.getStringList("texts");
            int interval = Math.max(1, section.getInt("animation-interval", 20));

            if (texts.isEmpty()) {
                plugin.getLogger().warning("Line without defined texts: " + section.getName());
                return null;
            }

            return new LineConfig(Collections.unmodifiableList(texts), interval);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating LineConfig", e);
            return null;
        }
    }

    /**
     * Configuration for individual line with texts and animation interval
     */
    public static class LineConfig {
        private final List<String> texts;
        private final int animationInterval;

        /**
         * Constructor for LineConfig
         * @param texts List of text frames
         * @param animationInterval Animation interval in ticks
         */
        public LineConfig(List<String> texts, int animationInterval) {
            this.texts = texts;
            this.animationInterval = animationInterval;
        }

        /**
         * Gets the text frames
         * @return Unmodifiable list of text frames
         */
        public List<String> getTexts() {
            return texts;
        }

        /**
         * Gets the animation interval
         * @return Animation interval in ticks
         */
        public int getAnimationInterval() {
            return animationInterval;
        }

        /**
         * Checks if the line is animated
         * @return true if multiple frames exist
         */
        public boolean isAnimated() {
            return texts.size() > 1;
        }
    }
}