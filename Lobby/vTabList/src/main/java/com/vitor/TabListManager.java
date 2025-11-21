/*
Project: DeepCraft
File: TabListManager.java
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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optimized tablist manager with performance improvements
 */
public class TabListManager {

    private final vTabList plugin;
    private final ConfigManager configManager;
    private final Map<String, AnimationTask> animationTasks;
    private final LegacyComponentSerializer textSerializer;
    private BukkitTask updateTask;

    // Enhanced cache with TTL and reduced memory footprint
    private final Map<UUID, CachedComponents> playerComponentCache;
    private long lastConfigUpdate = System.currentTimeMillis();
    private final Component[] emptyComponents = new Component[]{Component.empty(), Component.empty()};

    // Pre-compiled component cache for static content
    private final Map<String, Component> preCompiledComponents = new ConcurrentHashMap<>();

    /**
     * Enhanced component cache with timestamp
     */
    private static class CachedComponents {
        final Component header;
        final Component footer;
        final long timestamp;
        final int serverOnline; // Track server state when cached

        CachedComponents(Component header, Component footer, int serverOnline) {
            this.header = header;
            this.footer = footer;
            this.timestamp = System.currentTimeMillis();
            this.serverOnline = serverOnline;
        }
    }

    public TabListManager(vTabList plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.animationTasks = new ConcurrentHashMap<>();
        this.textSerializer = LegacyComponentSerializer.legacySection();
        this.playerComponentCache = new ConcurrentHashMap<>();
    }

    /**
     * Starts update tasks and animations
     */
    public void start() {
        initializeAnimations();
        startUpdateTask();
        startCacheCleanupTask();
    }

    /**
     * Stops all running tasks
     */
    public void stop() {
        stopUpdateTask();
        stopAllAnimations();
        playerComponentCache.clear();
        preCompiledComponents.clear();
    }

    /**
     * Restarts the manager (useful for reloads)
     */
    public void restart() {
        stop();
        start();
    }

    /**
     * Initialize animations more efficiently
     */
    private void initializeAnimations() {
        initializeSectionAnimations("header", configManager.getHeaderLines());
        initializeSectionAnimations("footer", configManager.getFooterLines());
    }

    private void initializeSectionAnimations(String sectionName, Map<String, ConfigManager.LineConfig> lines) {
        for (Map.Entry<String, ConfigManager.LineConfig> entry : lines.entrySet()) {
            String lineId = sectionName + "_" + entry.getKey();
            ConfigManager.LineConfig lineConfig = entry.getValue();

            if (lineConfig.isAnimated()) {
                AnimationTask task = new AnimationTask(lineConfig.getTexts(), lineConfig.getAnimationInterval());
                task.start();
                animationTasks.put(lineId, task);
            } else {
                // Pre-compile static components
                preCompiledComponents.put(lineId, textSerializer.deserialize(
                        ChatColor.translateAlternateColorCodes('&', lineConfig.getTexts().get(0))
                ));
            }
        }
    }

    /**
     * Cache cleanup task to prevent memory leaks
     */
    private void startCacheCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupOldCacheEntries();
            }
        }.runTaskTimer(plugin, 6000L, 6000L); // Run every 5 minutes
    }

    private void cleanupOldCacheEntries() {
        long now = System.currentTimeMillis();
        long cacheTTL = 300000; // 5 minutes TTL

        playerComponentCache.entrySet().removeIf(entry ->
                (now - entry.getValue().timestamp) > cacheTTL
        );
    }

    /**
     * Optimized update task with player count check
     */
    private void startUpdateTask() {
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
        }

        final int[] lastPlayerCount = {-1};

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                int currentPlayerCount = Bukkit.getOnlinePlayers().size();

                // Only update if player count changed or config was updated recently
                if (currentPlayerCount != lastPlayerCount[0] ||
                        System.currentTimeMillis() - lastConfigUpdate < 5000) {
                    updateAllTabLists();
                    lastPlayerCount[0] = currentPlayerCount;
                }
            }
        }.runTaskTimer(plugin, 0L, configManager.getUpdateInterval());
    }

    private void stopUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void stopAllAnimations() {
        for (AnimationTask task : animationTasks.values()) {
            task.stop();
        }
        animationTasks.clear();
    }

    /**
     * Batch update for better performance
     */
    public void updateAllTabLists() {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

        // Process in small batches to avoid server lag
        for (int i = 0; i < onlinePlayers.size(); i += 10) {
            int end = Math.min(i + 10, onlinePlayers.size());
            List<Player> batch = onlinePlayers.subList(i, end);

            for (Player player : batch) {
                updateTabList(player);
            }

            // Small delay between batches
            if (end < onlinePlayers.size()) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Optimized single player update
     */
    public void updateTabList(Player player) {
        if (player == null || !player.isOnline()) return;

        try {
            Component[] components = getCachedComponents(player);
            if (components[0] != null || components[1] != null) {
                player.sendPlayerListHeaderAndFooter(components[0], components[1]);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error updating tablist for " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Enhanced caching with server state awareness
     */
    private Component[] getCachedComponents(Player player) {
        UUID playerId = player.getUniqueId();
        int currentServerOnline = plugin.getServerOnline();

        CachedComponents cached = playerComponentCache.get(playerId);

        // Check if cache is still valid
        if (cached != null && cached.serverOnline == currentServerOnline) {
            return new Component[]{cached.header, cached.footer};
        }

        // Build new components
        Component header = buildComponent(player, configManager.getHeaderLines(), true);
        Component footer = buildComponent(player, configManager.getFooterLines(), false);

        // Update cache
        CachedComponents newCache = new CachedComponents(header, footer, currentServerOnline);
        playerComponentCache.put(playerId, newCache);

        return new Component[]{header, footer};
    }

    /**
     * Optimized component building
     */
    private Component buildComponent(Player player, Map<String, ConfigManager.LineConfig> lines, boolean isHeader) {
        if (lines.isEmpty()) {
            return Component.empty();
        }

        // Use StringBuilder for better performance
        StringBuilder content = new StringBuilder(128); // Pre-allocate reasonable size
        String sectionPrefix = isHeader ? "header_" : "footer_";
        boolean firstLine = true;

        for (Map.Entry<String, ConfigManager.LineConfig> entry : lines.entrySet()) {
            if (!firstLine) {
                content.append("\n");
            }

            String lineId = sectionPrefix + entry.getKey();
            String processedLine = processSingleLine(player, entry.getValue(), lineId);
            content.append(processedLine);
            firstLine = false;
        }

        String finalContent = content.toString();
        if (finalContent.isEmpty()) {
            return Component.empty();
        }

        return textSerializer.deserialize(finalContent);
    }

    /**
     * Optimized line processing
     */
    private String processSingleLine(Player player, ConfigManager.LineConfig lineConfig, String lineId) {
        String text = getCurrentFrame(lineConfig, lineId);

        // Skip processing for empty lines
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        text = replacePlaceholders(player, text);
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private String getCurrentFrame(ConfigManager.LineConfig lineConfig, String lineId) {
        if (lineConfig.isAnimated() && animationTasks.containsKey(lineId)) {
            return animationTasks.get(lineId).getCurrentFrame();
        }
        return lineConfig.getTexts().get(0);
    }

    /**
     * Optimized placeholder replacement
     */
    private String replacePlaceholders(Player player, String text) {
        if (player == null || !text.contains("%")) {
            return text;
        }

        // Only process if placeholders are present
        if (text.contains("%player_name%")) {
            text = text.replace("%player_name%", player.getName());
        }
        if (text.contains("%server_online%")) {
            text = text.replace("%server_online%", String.valueOf(plugin.getServerOnline()));
        }
        if (text.contains("%player_displayname%")) {
            text = text.replace("%player_displayname%", player.getDisplayName());
        }
        if (text.contains("%player_world%")) {
            text = text.replace("%player_world%", player.getWorld().getName());
        }

        return text;
    }

    /**
     * Optimized AnimationTask with reduced overhead
     */
    private class AnimationTask {
        private final List<String> frames;
        private final int interval;
        private volatile int currentFrameIndex = 0;
        private BukkitTask animationTask;

        public AnimationTask(List<String> frames, int interval) {
            this.frames = Collections.unmodifiableList(frames);
            this.interval = Math.max(1, interval);
        }

        public void start() {
            if (animationTask != null && !animationTask.isCancelled()) {
                return;
            }

            animationTask = new BukkitRunnable() {
                @Override
                public void run() {
                    currentFrameIndex = (currentFrameIndex + 1) % frames.size();
                }
            }.runTaskTimer(plugin, 0L, interval);
        }

        public void stop() {
            if (animationTask != null) {
                animationTask.cancel();
                animationTask = null;
            }
        }

        public String getCurrentFrame() {
            return frames.get(currentFrameIndex);
        }
    }
}