/*
Project: DeepCraft
File: LobbyListener.java
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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Main lobby listener handling player join, protection, and item management
 */
public class LobbyListener implements Listener {

    private final NamespacedKey TELEPORT_ARROW_KEY = new NamespacedKey(vLobby.getInstance(), "teleport_arrow");

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.setJoinMessage(null);

        setupNewPlayer(player);
    }

    /**
     * Sets up a new player in the lobby
     */
    private void setupNewPlayer(Player player) {
        teleportToSpawn(player);
        clearPlayerState(player);
        setupPlayerGameMode(player);
        giveLobbyItems(player);
        applyRankEffects(player);
    }

    /**
     * Teleports player to configured spawn location
     */
    private void teleportToSpawn(Player player) {
        String worldName = vLobby.getInstance().getConfig().getString("lobby.spawn.world", "world");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            vLobby.getInstance().getLogger().warning("Spawn world '" + worldName + "' not found!");
            return;
        }

        Location spawnLocation = new Location(
                world,
                vLobby.getInstance().getConfig().getDouble("lobby.spawn.x", 0.5),
                vLobby.getInstance().getConfig().getDouble("lobby.spawn.y", 64.0),
                vLobby.getInstance().getConfig().getDouble("lobby.spawn.z", 0.5),
                (float) vLobby.getInstance().getConfig().getDouble("lobby.spawn.yaw", 0.0),
                (float) vLobby.getInstance().getConfig().getDouble("lobby.spawn.pitch", 0.0)
        );

        player.teleport(spawnLocation);
    }

    /**
     * Clears and resets player state
     */
    private void clearPlayerState(Player player) {
        player.getInventory().clear();
        player.setHealth(20.0);
        player.setSaturation(20.0f);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.setFallDistance(0);
    }

    /**
     * Sets appropriate game mode based on permissions
     */
    private void setupPlayerGameMode(Player player) {
        if (player.hasPermission("vl.staff")) {
            player.setGameMode(GameMode.CREATIVE);
            notifyStaffMember(player);
        } else {
            player.setGameMode(GameMode.ADVENTURE);
        }
    }

    /**
     * Notifies staff members about creative mode
     */
    private void notifyStaffMember(Player player) {
        String staffMessage = vLobby.getInstance().getConfig().getString("lobby.staff.creative_message", "");
        if (!staffMessage.isEmpty()) {
            player.sendMessage(staffMessage.replace("&", "§"));
        }
    }

    /**
     * Gives lobby items to player
     */
    private void giveLobbyItems(Player player) {
        giveCompass(player);
        giveTeleportBow(player);
    }

    /**
     * Gives compass menu item to player
     */
    private void giveCompass(Player player) {
        Material compassMaterial = Material.valueOf(
                vLobby.getInstance().getConfig().getString("lobby.compass.item", "COMPASS")
        );

        ItemStack compass = new ItemStack(compassMaterial);
        ItemMeta meta = compass.getItemMeta();
        meta.setDisplayName(
                vLobby.getInstance().getConfig().getString("lobby.compass.display_name", "§6Menu")
                        .replace("&", "§")
        );
        compass.setItemMeta(meta);

        player.getInventory().setItem(4, compass); // Slot 4 (fifth slot in hotbar)
    }

    /**
     * Applies special effects and messages based on player's rank
     */
    private void applyRankEffects(Player player) {
        String rank = getPlayerPrimaryGroup(player);
        handleRankJoinEffects(player, rank);
    }

    /**
     * Handles rank-specific join effects (messages, sounds, titles)
     */
    private void handleRankJoinEffects(Player player, String rank) {
        String messagePath = "messages.join." + rank.toLowerCase();
        String soundPath = "sounds.join." + rank.toLowerCase();
        String titlePath = "messages.titles." + rank.toLowerCase();

        // Broadcast join message
        String joinMessage = vLobby.getInstance().getConfig().getString(messagePath);
        if (joinMessage != null && !joinMessage.isEmpty()) {
            String formattedMessage = joinMessage.replace("%player_name%", player.getName()).replace("&", "§");
            Bukkit.broadcastMessage(formattedMessage);
        }

        // Play join sound
        String soundName = vLobby.getInstance().getConfig().getString(soundPath, "entity.experience_orb.pickup");
        float volume = (float) vLobby.getInstance().getConfig().getDouble("sounds.volume", 1.0);
        float pitch = (float) vLobby.getInstance().getConfig().getDouble("sounds.special_pitch." + rank.toLowerCase(),
                vLobby.getInstance().getConfig().getDouble("sounds.pitch", 1.0));

        playGlobalSound(soundName, volume, pitch);

        // Show title if configured
        if (vLobby.getInstance().getConfig().contains(titlePath)) {
            showRankTitle(player, titlePath);
        }
    }

    /**
     * Shows rank-specific title to player
     */
    private void showRankTitle(Player player, String titlePath) {
        String title = vLobby.getInstance().getConfig().getString(titlePath + ".title", "").replace("&", "§");
        String subtitle = vLobby.getInstance().getConfig().getString(titlePath + ".subtitle", "").replace("&", "§");
        int fadeIn = vLobby.getInstance().getConfig().getInt(titlePath + ".fadeIn", 10);
        int stay = vLobby.getInstance().getConfig().getInt(titlePath + ".stay", 70);
        int fadeOut = vLobby.getInstance().getConfig().getInt(titlePath + ".fadeOut", 20);

        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    /**
     * Plays sound to all online players
     */
    private void playGlobalSound(String soundName, float volume, float pitch) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            try {
                onlinePlayer.playSound(onlinePlayer.getLocation(), soundName, volume, pitch);
            } catch (Exception e) {
                vLobby.getInstance().getLogger().warning("Error playing global sound: " + soundName);
            }
        }
    }

    /**
     * Gets player's primary group from LuckPerms
     */
    private String getPlayerPrimaryGroup(Player player) {
        try {
            LuckPerms api = LuckPermsProvider.get();
            User user = api.getUserManager().getUser(player.getUniqueId());
            if (user == null) return "default";
            return user.getPrimaryGroup();
        } catch (Exception e) {
            return "default";
        }
    }

    /**
     * Gives teleport bow to player if enabled in config
     */
    private void giveTeleportBow(Player player) {
        if (!vLobby.getInstance().getConfig().getBoolean("lobby.teleport_bow.enabled", false)) {
            return;
        }

        // Create bow item
        Material bowMaterial = Material.valueOf(
                vLobby.getInstance().getConfig().getString("lobby.teleport_bow.material", "BOW")
        );

        ItemStack bow = new ItemStack(bowMaterial);
        ItemMeta bowMeta = bow.getItemMeta();

        // Set display name
        bowMeta.setDisplayName(
                vLobby.getInstance().getConfig().getString("lobby.teleport_bow.display_name", "§eTeleport Bow")
                        .replace("&", "§")
        );

        // Set lore
        List<String> lore = new ArrayList<>();
        if (vLobby.getInstance().getConfig().contains("lobby.teleport_bow.lore")) {
            List<String> configLore = vLobby.getInstance().getConfig().getStringList("lobby.teleport_bow.lore");
            for (String line : configLore) {
                lore.add(line.replace("&", "§"));
            }
        }
        bowMeta.setLore(lore);

        // Apply enchantments and effects
        applyBowEnchantments(bowMeta);
        applyBowItemFlags(bowMeta);

        bow.setItemMeta(bowMeta);

        // Set bow in configured slot
        int bowSlot = vLobby.getInstance().getConfig().getInt("lobby.teleport_bow.bow_slot", 1);
        player.getInventory().setItem(bowSlot, bow);

        // Give arrows
        giveArrowToPlayer(player);
    }

    /**
     * Applies enchantments to teleport bow
     */
    private void applyBowEnchantments(ItemMeta bowMeta) {
        // Apply glow effect
        boolean glow = vLobby.getInstance().getConfig().getBoolean("lobby.teleport_bow.glow", false);
        if (glow) {
            bowMeta.addEnchant(Enchantment.LURE, 1, true);
        }

        // Apply infinity if enabled
        boolean infinity = vLobby.getInstance().getConfig().getBoolean("lobby.teleport_bow.infinity", false);
        if (infinity) {
            bowMeta.addEnchant(Enchantment.INFINITY, 1, true);
        }

        // Hide enchantments if configured
        boolean hideEnchants = vLobby.getInstance().getConfig().getBoolean("lobby.teleport_bow.hide_enchants", true);
        if (hideEnchants) {
            bowMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
    }

    /**
     * Applies item flags to teleport bow for clean appearance
     */
    private void applyBowItemFlags(ItemMeta bowMeta) {
        bowMeta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_PLACED_ON,
                ItemFlag.HIDE_DYE
        );
        bowMeta.setUnbreakable(true);
    }

    /**
     * Gives teleport arrows to player
     */
    private void giveArrowToPlayer(Player player) {
        Material arrowType = Material.valueOf(
                vLobby.getInstance().getConfig().getString("lobby.teleport_bow.arrow_type", "ARROW")
        );
        int arrowSlot = vLobby.getInstance().getConfig().getInt("lobby.teleport_bow.arrow_slot", 9);
        int arrowCount = vLobby.getInstance().getConfig().getInt("lobby.teleport_bow.arrows", 1);

        // Validate slot range
        arrowSlot = Math.max(9, Math.min(35, arrowSlot));

        ItemStack arrows = new ItemStack(arrowType, arrowCount);
        markAsTeleportArrow(arrows);
        player.getInventory().setItem(arrowSlot, arrows);
    }

    /**
     * Marks item as teleport arrow using persistent data
     */
    private void markAsTeleportArrow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(TELEPORT_ARROW_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    // Event handlers for item protection and interactions

    @EventHandler
    public void onCompassClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        // Handle compass click
        if (item.getType() == Material.COMPASS &&
                (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {

            String compassName = vLobby.getInstance().getConfig().getString("lobby.compass.display_name", "§6Menu").replace("&", "§");
            if (item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(compassName)) {
                new CompassMenu().open(player);
                event.setCancelled(true);
            }
        }
        // Handle teleport bow protection
        else if (item.getType() == Material.BOW) {
            String bowName = vLobby.getInstance().getConfig().getString("lobby.teleport_bow.display_name", "§eTeleport Bow").replace("&", "§");
            if (item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(bowName)) {
                // Only allow shooting actions
                if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        if (isProtectedItem(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() != null && isProtectedItem(event.getCurrentItem())) {
            event.setCancelled(true);
        }
    }

    /**
     * Checks if item is protected (compass, teleport bow, or teleport arrow)
     */
    private boolean isProtectedItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        // Check compass
        if (item.getType() == Material.COMPASS) {
            String compassName = vLobby.getInstance().getConfig().getString("lobby.compass.display_name", "§6Menu").replace("&", "§");
            return item.getItemMeta().getDisplayName().equals(compassName);
        }

        // Check teleport bow
        if (item.getType() == Material.BOW) {
            String bowName = vLobby.getInstance().getConfig().getString("lobby.teleport_bow.display_name", "§eTeleport Bow").replace("&", "§");
            return item.getItemMeta().getDisplayName().equals(bowName);
        }

        // Check teleport arrow
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(TELEPORT_ARROW_KEY, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true); // Full damage protection in lobby
        }
    }

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        // Cancel wither effects in lobby
        if (event.getNewEffect() != null &&
                event.getNewEffect().getType().equals(org.bukkit.potion.PotionEffectType.WITHER)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        event.setCancelled(true); // Prevent hunger in lobby
    }
}