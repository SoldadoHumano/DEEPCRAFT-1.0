/*
Project: DeepCraft
File: PortalMobManager.java
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

package com.vitor.MobPortals;

import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.NamespacedKey;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Manages interactive portal NPCs with animations, sounds, and music systems
 * Handles spawning, animation, music playback, and player interactions
 * Provides immersive portal experience with configurable behaviors
 */
public class PortalMobManager implements Listener {

    private final Plugin plugin;
    private final NamespacedKey PORTAL_COMMAND_KEY;

    // Portal entity management
    private final Map<LivingEntity, String> portalCommands = new HashMap<>();
    private final Map<LivingEntity, List<String>> animatedNames = new HashMap<>();
    private final Map<LivingEntity, Integer> animationIndexes = new HashMap<>();

    // Music system management
    private final Map<LivingEntity, List<String>> musicListMap = new HashMap<>();
    private final Map<LivingEntity, List<Long>> musicIntervalsMap = new HashMap<>();
    private final Map<LivingEntity, List<Float>> musicPitchesMap = new HashMap<>();
    private final Map<LivingEntity, Float> musicRadiusMap = new HashMap<>();
    private final Map<LivingEntity, Integer> currentMusicIndexMap = new HashMap<>();
    private final Map<LivingEntity, Long> nextMusicTimeMap = new HashMap<>();
    private final Map<LivingEntity, Map<Player, Integer>> playerMusicTasksMap = new HashMap<>();

    // Chunk and task management
    private final Map<Chunk, Integer> forcedChunks = new HashMap<>();
    private final Map<LivingEntity, BukkitRunnable> musicRunnables = new HashMap<>();

    public PortalMobManager(Plugin plugin) {
        this.plugin = plugin;
        this.PORTAL_COMMAND_KEY = new NamespacedKey(plugin, "portal_command");
    }

    /**
     * Spawns all configured portals from config
     */
    public void spawnPortals() {
        removeAllPortals(); // Clean up existing portals first

        var portalsSection = plugin.getConfig().getConfigurationSection("portals");
        if (portalsSection == null) {
            plugin.getLogger().info("No portals configured in config.yml");
            return;
        }

        for (String portalKey : portalsSection.getKeys(false)) {
            var portalConfig = portalsSection.getConfigurationSection(portalKey);
            if (portalConfig != null) {
                spawnPortal(portalConfig, portalKey);
            }
        }

        plugin.getLogger().info("Spawned " + portalsSection.getKeys(false).size() + " portal(s)");
    }

    /**
     * Spawns a single portal from configuration
     */
    private void spawnPortal(org.bukkit.configuration.ConfigurationSection portalConfig, String portalKey) {
        // Get portal configuration
        List<String> names = portalConfig.getStringList("names");
        if (names.isEmpty()) {
            names.add(portalConfig.getString("name", "Portal"));
        }

        String command = portalConfig.getString("command", "");
        EntityType entityType = getEntityType(portalConfig.getString("mob", "ZOMBIE"));
        List<Double> locationData = portalConfig.getDoubleList("location");

        if (locationData.size() < 5) {
            plugin.getLogger().warning("Invalid location data for portal: " + portalKey);
            return;
        }

        // Create location from config
        Location spawnLocation = createLocation(locationData);
        if (spawnLocation.getWorld() == null) {
            plugin.getLogger().warning("World not found for portal: " + portalKey);
            return;
        }

        // Force load chunk
        forceLoadChunk(spawnLocation.getChunk());

        // Spawn and configure entity
        LivingEntity portalEntity = spawnPortalEntity(entityType, spawnLocation, names.get(0));
        if (portalEntity == null) return;

        // Configure portal properties
        configurePortalEntity(portalEntity, command);

        // Start animations if multiple names
        if (names.size() > 1) {
            startNameAnimation(portalEntity, names, portalConfig.getInt("animation-speed", 20));
        }

        // Setup music system if configured
        setupPortalMusic(portalEntity, portalConfig);

        plugin.getLogger().info("Spawned portal: " + portalKey + " at " + spawnLocation);
    }

    /**
     * Gets valid entity type or defaults to ZOMBIE
     */
    private EntityType getEntityType(String typeName) {
        try {
            return EntityType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid entity type: " + typeName + ", using ZOMBIE");
            return EntityType.ZOMBIE;
        }
    }

    /**
     * Creates location from double list [x, y, z, yaw, pitch]
     */
    private Location createLocation(List<Double> locationData) {
        return new Location(
                Bukkit.getWorlds().get(0), // Default to first world
                locationData.get(0),
                locationData.get(1),
                locationData.get(2),
                locationData.get(3).floatValue(),
                locationData.get(4).floatValue()
        );
    }

    /**
     * Forces chunk to be loaded and kept loaded
     */
    private void forceLoadChunk(Chunk chunk) {
        if (!chunk.isLoaded()) {
            chunk.load();
        }
        chunk.setForceLoaded(true);
        forcedChunks.put(chunk, forcedChunks.getOrDefault(chunk, 0) + 1);
    }

    /**
     * Spawns and returns portal entity
     */
    private LivingEntity spawnPortalEntity(EntityType type, Location location, String displayName) {
        try {
            LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, type);

            // Configure entity properties
            entity.setPersistent(true);
            entity.setRemoveWhenFarAway(false);
            entity.setCustomName(ChatColor.translateAlternateColorCodes('&', displayName));
            entity.setCustomNameVisible(true);
            entity.setAI(false);
            entity.setInvulnerable(true);
            entity.setCollidable(false);
            entity.setSilent(true);
            entity.setGravity(false);
            entity.setFireTicks(0);
            entity.setVisualFire(false);

            // Special handling for villagers
            if (entity instanceof Villager) {
                ((Villager) entity).setProfession(Villager.Profession.NONE);
            }

            return entity;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to spawn portal entity: " + e.getMessage());
            return null;
        }
    }

    /**
     * Configures portal entity with command data and other properties
     */
    private void configurePortalEntity(LivingEntity entity, String command) {
        entity.getPersistentDataContainer().set(PORTAL_COMMAND_KEY, PersistentDataType.STRING, command);
        portalCommands.put(entity, command);
    }

    /**
     * Starts name animation for portal entity
     */
    private void startNameAnimation(LivingEntity entity, List<String> names, int animationSpeed) {
        animatedNames.put(entity, names);
        animationIndexes.put(entity, 0);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (entity.isDead() || !entity.isValid()) {
                    this.cancel();
                    animatedNames.remove(entity);
                    animationIndexes.remove(entity);
                    return;
                }

                int currentIndex = animationIndexes.get(entity);
                String nextName = ChatColor.translateAlternateColorCodes('&', names.get(currentIndex));
                entity.setCustomName(nextName);

                // Move to next index, loop back to 0 if at end
                int nextIndex = (currentIndex + 1) % names.size();
                animationIndexes.put(entity, nextIndex);
            }
        }.runTaskTimer(plugin, 0, animationSpeed);
    }

    /**
     * Sets up music system for portal entity
     */
    private void setupPortalMusic(LivingEntity entity, org.bukkit.configuration.ConfigurationSection portalConfig) {
        List<String> musicList = portalConfig.getStringList("music");
        if (musicList.isEmpty()) {
            return; // No music configured
        }

        List<Long> musicIntervals = portalConfig.getLongList("music-intervals");
        List<Float> musicPitches = new ArrayList<>();
        float musicRadius = (float) portalConfig.getDouble("music-radius", 1.0);

        // Load music pitches or use defaults
        if (portalConfig.contains("music-pitches")) {
            for (Object pitchObj : portalConfig.getList("music-pitches", new ArrayList<Double>())) {
                if (pitchObj instanceof Double) {
                    musicPitches.add(((Double) pitchObj).floatValue());
                } else if (pitchObj instanceof Integer) {
                    musicPitches.add(((Integer) pitchObj).floatValue());
                }
            }
        } else {
            // Default pitch 1.0 for all music
            for (int i = 0; i < musicList.size(); i++) {
                musicPitches.add(1.0f);
            }
        }

        // Store music configuration
        musicListMap.put(entity, musicList);
        musicPitchesMap.put(entity, musicPitches);
        musicRadiusMap.put(entity, musicRadius);
        playerMusicTasksMap.put(entity, new HashMap<>());

        // Adjust intervals to match music list
        List<Long> adjustedIntervals = new ArrayList<>();
        for (int i = 0; i < musicList.size(); i++) {
            if (i < musicIntervals.size()) {
                adjustedIntervals.add(musicIntervals.get(i));
            } else if (!musicIntervals.isEmpty()) {
                adjustedIntervals.add(musicIntervals.get(musicIntervals.size() - 1));
            } else {
                adjustedIntervals.add(3780L); // Default interval
            }
        }
        musicIntervalsMap.put(entity, adjustedIntervals);

        // Initialize music state
        currentMusicIndexMap.put(entity, 0);
        nextMusicTimeMap.put(entity, System.currentTimeMillis() + (adjustedIntervals.get(0) * 50L));

        // Start music management task
        BukkitRunnable musicRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (entity.isDead() || !entity.isValid()) {
                    this.cancel();
                    cleanupPortalMusic(entity);
                    return;
                }

                long currentTime = System.currentTimeMillis();
                if (currentTime >= nextMusicTimeMap.get(entity)) {
                    advanceToNextMusic(entity);
                }

                playMusicForNearbyPlayers(entity);
            }
        };

        musicRunnable.runTaskTimer(plugin, 0L, 10L);
        musicRunnables.put(entity, musicRunnable);
    }

    /**
     * Advances to the next music track in sequence
     */
    private void advanceToNextMusic(LivingEntity entity) {
        List<String> musicList = musicListMap.get(entity);
        List<Long> intervals = musicIntervalsMap.get(entity);
        int currentIndex = currentMusicIndexMap.get(entity);

        int nextIndex = (currentIndex + 1) % musicList.size();
        currentMusicIndexMap.put(entity, nextIndex);

        long nextInterval = intervals.get(nextIndex) * 50L;
        nextMusicTimeMap.put(entity, System.currentTimeMillis() + nextInterval);
    }

    /**
     * Plays music for players within portal radius
     */
    private void playMusicForNearbyPlayers(LivingEntity entity) {
        List<String> musicList = musicListMap.get(entity);
        List<Float> pitches = musicPitchesMap.get(entity);
        int currentMusicIndex = currentMusicIndexMap.get(entity);

        if (currentMusicIndex >= musicList.size()) return;

        String currentMusic = musicList.get(currentMusicIndex);
        float currentPitch = pitches.get(currentMusicIndex);
        float radius = musicRadiusMap.get(entity);

        for (Player player : entity.getWorld().getPlayers()) {
            if (!player.getWorld().equals(entity.getWorld())) continue;

            double distance = player.getLocation().distance(entity.getLocation());
            Map<Player, Integer> playerTasks = playerMusicTasksMap.get(entity);

            if (distance <= radius) {
                // Player is within range, play music
                if (!playerTasks.containsKey(player) || playerTasks.get(player) != currentMusicIndex) {
                    // Stop previous music if different
                    if (playerTasks.containsKey(player)) {
                        String previousMusic = musicList.get(playerTasks.get(player));
                        player.stopSound(previousMusic);
                    }

                    // Play current music
                    player.playSound(entity.getLocation(), currentMusic, 1.0f, currentPitch);
                    playerTasks.put(player, currentMusicIndex);
                }
            } else {
                // Player left radius, stop music
                stopMusicForPlayer(entity, player);
            }
        }
    }

    /**
     * Stops music for specific player
     */
    private void stopMusicForPlayer(LivingEntity entity, Player player) {
        Map<Player, Integer> playerTasks = playerMusicTasksMap.get(entity);
        if (playerTasks.containsKey(player)) {
            int musicIndex = playerTasks.get(player);
            List<String> musicList = musicListMap.get(entity);

            if (musicIndex < musicList.size()) {
                String music = musicList.get(musicIndex);
                player.stopSound(music);
            }

            playerTasks.remove(player);
        }
    }

    /**
     * Cleans up music system for portal entity
     */
    private void cleanupPortalMusic(LivingEntity entity) {
        // Stop music for all players
        Map<Player, Integer> playerTasks = playerMusicTasksMap.get(entity);
        if (playerTasks != null) {
            List<String> musicList = musicListMap.get(entity);
            for (Player player : new ArrayList<>(playerTasks.keySet())) {
                int musicIndex = playerTasks.get(player);
                if (musicIndex < musicList.size()) {
                    String music = musicList.get(musicIndex);
                    player.stopSound(music);
                }
            }
            playerTasks.clear();
        }

        // Cancel music task
        BukkitRunnable musicRunnable = musicRunnables.get(entity);
        if (musicRunnable != null) {
            musicRunnable.cancel();
            musicRunnables.remove(entity);
        }

        // Clean up collections
        musicListMap.remove(entity);
        musicIntervalsMap.remove(entity);
        musicPitchesMap.remove(entity);
        musicRadiusMap.remove(entity);
        currentMusicIndexMap.remove(entity);
        nextMusicTimeMap.remove(entity);
        playerMusicTasksMap.remove(entity);
    }

    /**
     * Removes all portals and cleans up resources
     */
    private void removeAllPortals() {
        // Create copy to avoid ConcurrentModificationException
        List<LivingEntity> portalsToRemove = new ArrayList<>();

        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity.getPersistentDataContainer().has(PORTAL_COMMAND_KEY, PersistentDataType.STRING)) {
                    portalsToRemove.add(entity);
                }
            }
        }

        // Remove entities and cleanup
        for (LivingEntity entity : portalsToRemove) {
            cleanupPortalEntity(entity);
        }

        // Clear collections
        portalCommands.clear();
        animatedNames.clear();
        animationIndexes.clear();

        // Cleanup music systems
        for (LivingEntity entity : new ArrayList<>(musicListMap.keySet())) {
            cleanupPortalMusic(entity);
        }

        musicRunnables.clear();

        plugin.getLogger().info("Removed " + portalsToRemove.size() + " portal(s)");
    }

    /**
     * Cleans up individual portal entity and associated resources
     */
    private void cleanupPortalEntity(LivingEntity entity) {
        Chunk chunk = entity.getLocation().getChunk();
        removeForcedChunk(chunk);
        cleanupPortalMusic(entity);
        entity.remove();
    }

    /**
     * Removes forced chunk loading reference
     */
    private void removeForcedChunk(Chunk chunk) {
        int count = forcedChunks.getOrDefault(chunk, 0) - 1;
        if (count <= 0) {
            forcedChunks.remove(chunk);
            chunk.setForceLoaded(false);
            if (chunk.getEntities().length == 0) {
                chunk.unload();
            }
        } else {
            forcedChunks.put(chunk, count);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Update name animations for joined player
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (LivingEntity entity : new ArrayList<>(animatedNames.keySet())) {
                if (!entity.isValid() || !entity.getWorld().equals(player.getWorld())) continue;

                int index = animationIndexes.getOrDefault(entity, 0);
                String currentName = ChatColor.translateAlternateColorCodes('&', animatedNames.get(entity).get(index));
                entity.setCustomName(currentName);
            }
        }, 20L);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Only process if player actually moved blocks
        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        // Check music radius for all portals
        for (LivingEntity entity : new ArrayList<>(musicListMap.keySet())) {
            if (entity.isValid() && entity.getWorld().equals(player.getWorld())) {
                double distance = player.getLocation().distance(entity.getLocation());
                float radius = musicRadiusMap.get(entity);

                if (distance > radius) {
                    stopMusicForPlayer(entity, player);
                }
            }
        }
    }

    @EventHandler
    public void onPortalClick(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof LivingEntity entity)) return;

        if (!entity.getPersistentDataContainer().has(PORTAL_COMMAND_KEY, PersistentDataType.STRING)) return;

        String command = entity.getPersistentDataContainer().get(PORTAL_COMMAND_KEY, PersistentDataType.STRING);
        Player player = event.getPlayer();

        // Execute portal command
        if (command != null && !command.trim().isEmpty()) {
            String formattedCommand = command.replace("%player_name%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand);
        }

        // Play click effects
        playPortalClickEffects(entity, player);
        event.setCancelled(true);
    }

    /**
     * Plays click effects for portal interaction
     */
    private void playPortalClickEffects(LivingEntity entity, Player player) {
        var portalsSection = plugin.getConfig().getConfigurationSection("portals");
        if (portalsSection == null) return;

        for (String portalKey : portalsSection.getKeys(false)) {
            var portalConfig = portalsSection.getConfigurationSection("portals." + portalKey);
            if (portalConfig == null) continue;

            List<Double> locList = portalConfig.getDoubleList("location");
            if (locList.size() < 3) continue;

            Location portalLoc = new Location(Bukkit.getWorlds().get(0), locList.get(0), locList.get(1), locList.get(2));

            // Check if this is the clicked portal
            if (portalLoc.distanceSquared(entity.getLocation()) <= 2.25) { // 1.5 blocks squared
                String clickSound = portalConfig.getString("click-sound", "entity.player.levelup");
                float clickPitch = (float) portalConfig.getDouble("click-pitch", 1.0);

                try {
                    player.playSound(entity.getLocation(), clickSound, 1.0f, clickPitch);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid click sound: " + clickSound + " for portal " + portalKey);
                    player.playSound(entity.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, clickPitch);
                }

                String clickMessage = portalConfig.getString("click-message", "");
                if (!clickMessage.trim().isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', clickMessage));
                }
                break;
            }
        }
    }

    @EventHandler
    public void onPortalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        if (entity.getPersistentDataContainer().has(PORTAL_COMMAND_KEY, PersistentDataType.STRING)) {
            event.setCancelled(true); // Portals are invulnerable
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Stop all music for quitting player
        for (LivingEntity entity : new ArrayList<>(playerMusicTasksMap.keySet())) {
            stopMusicForPlayer(entity, player);
        }
    }

    /**
     * Emergency cleanup method for plugin disable
     */
    public void emergencyCleanup() {
        removeAllPortals();

        // Force unload all chunks
        for (Chunk chunk : new ArrayList<>(forcedChunks.keySet())) {
            chunk.setForceLoaded(false);
            if (chunk.getEntities().length == 0) {
                chunk.unload();
            }
        }
        forcedChunks.clear();
    }
}