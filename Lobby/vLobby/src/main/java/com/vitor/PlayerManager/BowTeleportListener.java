/*
Project: DeepCraft
File: BowTeleportListener.java
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

package com.vitor.PlayerManager;

import com.vitor.vLobby;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Handles teleportation when arrows from teleport bows hit surfaces
 * Provides configurable teleport effects, sounds, and arrow refill system
 */
public class BowTeleportListener implements Listener {

    private final NamespacedKey TELEPORT_ARROW_KEY = new NamespacedKey(vLobby.getInstance(), "teleport_arrow");

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        // Validate entity type and shooter
        if (!(event.getEntity() instanceof Arrow) || !(event.getEntity().getShooter() instanceof Player)) {
            return;
        }

        Arrow arrow = (Arrow) event.getEntity();
        Player player = (Player) arrow.getShooter();

        // Check if used bow is teleport bow
        if (!isTeleportBow(player.getInventory().getItemInMainHand())) {
            return;
        }

        executeTeleport(player, arrow);
    }

    /**
     * Executes the teleport sequence for player
     */
    private void executeTeleport(Player player, Arrow arrow) {
        // Teleport player to arrow location
        player.teleport(arrow.getLocation());

        // Play teleport sound
        playTeleportSound(player);

        // Show teleport message if enabled
        showTeleportMessage(player);

        // Remove the arrow entity
        arrow.remove();

        // Handle arrow refill if needed
        scheduleArrowRefill(player);
    }

    /**
     * Plays configured teleport sound
     */
    private void playTeleportSound(Player player) {
        String soundName = getConfigString("lobby.teleport_bow.tp_sound", "entity.enderman.teleport");
        float volume = (float) getConfigDouble("lobby.teleport_bow.tp_sound_volume", 1.0);
        float pitch = (float) getConfigDouble("lobby.teleport_bow.tp_sound_pitch", 1.0);

        try {
            player.playSound(player.getLocation(), soundName, volume, pitch);
        } catch (IllegalArgumentException e) {
            // Fallback to default sound if configured sound is invalid
            vLobby.getInstance().getLogger().warning("Teleport sound not found: " + soundName + ". Using default.");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, volume, pitch);
        }
    }

    /**
     * Shows teleport message if enabled in config
     */
    private void showTeleportMessage(Player player) {
        boolean showMessage = getConfigBoolean("lobby.teleport_bow.show_tp_message", true);
        if (showMessage) {
            String message = getConfigString("lobby.teleport_bow.tp_message", "&aTeleported!").replace("&", "§");
            player.sendMessage(message);
        }
    }

    /**
     * Schedules arrow refill if auto-refill is enabled and infinity is not active
     */
    private void scheduleArrowRefill(Player player) {
        boolean infinity = getConfigBoolean("lobby.teleport_bow.infinity", false);
        boolean autoRefill = getConfigBoolean("lobby.teleport_bow.auto_refill", true);

        if (!infinity && autoRefill) {
            int refillInterval = getConfigInt("lobby.teleport_bow.refill_interval", 5) * 20;

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        giveArrowToPlayer(player);
                    }
                }
            }.runTaskLater(vLobby.getInstance(), refillInterval);
        }
    }

    /**
     * Checks if item is a valid teleport bow
     */
    private boolean isTeleportBow(ItemStack item) {
        if (item == null || item.getType() != Material.BOW || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        String displayName = meta.getDisplayName();
        String configName = getConfigString("lobby.teleport_bow.display_name", "§eTeleport Bow").replace("&", "§");

        return displayName.equals(configName);
    }

    /**
     * Gives teleport arrow to player in configured slot
     */
    private void giveArrowToPlayer(Player player) {
        Material arrowType = Material.valueOf(getConfigString("lobby.teleport_bow.arrow_type", "ARROW"));
        int arrowSlot = getConfigInt("lobby.teleport_bow.arrow_slot", 9);
        int arrowCount = getConfigInt("lobby.teleport_bow.arrows", 1);

        // Validate slot range (inventory: 9-35)
        arrowSlot = Math.max(9, Math.min(35, arrowSlot));

        ItemStack currentItem = player.getInventory().getItem(arrowSlot);

        if (currentItem == null || currentItem.getType() == Material.AIR) {
            // Empty slot, add new arrows
            addNewArrows(player, arrowSlot, arrowType, arrowCount);
        } else if (currentItem.getType() == arrowType) {
            // Existing arrows, add to stack
            addToExistingStack(player, arrowSlot, currentItem, arrowCount);
        }
    }

    /**
     * Adds new arrows to empty slot
     */
    private void addNewArrows(Player player, int slot, Material arrowType, int count) {
        ItemStack arrows = new ItemStack(arrowType, count);
        markAsTeleportArrow(arrows);
        player.getInventory().setItem(slot, arrows);
    }

    /**
     * Adds arrows to existing stack if space available
     */
    private void addToExistingStack(Player player, int slot, ItemStack currentItem, int addCount) {
        int currentAmount = currentItem.getAmount();
        int maxStackSize = currentItem.getType().getMaxStackSize();

        if (currentAmount < maxStackSize) {
            int newAmount = Math.min(currentAmount + addCount, maxStackSize);
            currentItem.setAmount(newAmount);
            markAsTeleportArrow(currentItem);
            player.getInventory().setItem(slot, currentItem);
        }
    }

    /**
     * Marks item as teleport arrow using persistent data
     */
    private void markAsTeleportArrow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(TELEPORT_ARROW_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    // Helper methods for config access with fallbacks
    private String getConfigString(String path, String defaultValue) {
        return vLobby.getInstance().getConfig().getString(path, defaultValue);
    }

    private boolean getConfigBoolean(String path, boolean defaultValue) {
        return vLobby.getInstance().getConfig().getBoolean(path, defaultValue);
    }

    private int getConfigInt(String path, int defaultValue) {
        return vLobby.getInstance().getConfig().getInt(path, defaultValue);
    }

    private double getConfigDouble(String path, double defaultValue) {
        return vLobby.getInstance().getConfig().getDouble(path, defaultValue);
    }
}