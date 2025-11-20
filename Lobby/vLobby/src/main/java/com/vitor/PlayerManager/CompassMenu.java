/*
Project: DeepCraft
File: CompassMenu.java
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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles compass menu for server selection
 * Provides interactive GUI with configurable items, sounds, and commands
 */
public class CompassMenu implements Listener, CommandExecutor {

    /**
     * Opens the compass menu for specified player
     */
    public void open(Player player) {
        Inventory menu = createMenu();
        player.openInventory(menu);
        playOpenSound(player);
    }

    /**
     * Creates and configures the compass menu inventory
     */
    private Inventory createMenu() {
        int size = getConfigInt("compass-menu.size", 27);
        String title = getConfigString("compass-menu.title", "§6Server Menu").replace("&", "§");
        Inventory inventory = Bukkit.createInventory(null, size, title);

        populateMenuItems(inventory);
        return inventory;
    }

    /**
     * Populates menu with configured items
     */
    private void populateMenuItems(Inventory inventory) {
        var itemsSection = vLobby.getInstance().getConfig().getConfigurationSection("lobby.compass.items");
        if (itemsSection == null) return;

        for (String key : itemsSection.getKeys(false)) {
            ItemStack item = createMenuItem(key);
            if (item != null) {
                int slot = getConfigInt("lobby.compass.slots." + key, 0);
                inventory.setItem(slot, item);
            }
        }
    }

    /**
     * Creates a menu item from configuration
     */
    private ItemStack createMenuItem(String itemKey) {
        String materialName = getConfigString("lobby.compass.items." + itemKey + ".material", "STONE");
        Material material = getValidMaterial(materialName);

        if (material == null) {
            vLobby.getInstance().getLogger().warning("Invalid material: " + materialName + " for item " + itemKey);
            return null;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Set display name
        String displayName = getConfigString("lobby.compass.items." + itemKey + ".display_name", "Item").replace("&", "§");
        meta.setDisplayName(displayName);

        // Set lore
        meta.setLore(getItemLore(itemKey));

        // Apply visual effects
        applyItemEffects(meta, itemKey);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Gets valid material or returns null if invalid
     */
    private Material getValidMaterial(String materialName) {
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Gets formatted lore for menu item
     */
    private List<String> getItemLore(String itemKey) {
        List<String> lore = new ArrayList<>();
        List<String> configLore = vLobby.getInstance().getConfig().getStringList("lobby.compass.items." + itemKey + ".lore");

        for (String line : configLore) {
            lore.add(line.replace("&", "§"));
        }
        return lore;
    }

    /**
     * Applies visual effects to menu item
     */
    private void applyItemEffects(ItemMeta meta, String itemKey) {
        // Apply glow effect
        boolean glow = getConfigBoolean("lobby.compass.items." + itemKey + ".glow", false);
        if (glow) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        // Hide all attributes for clean appearance
        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_PLACED_ON,
                ItemFlag.HIDE_DYE
        );

        meta.setUnbreakable(true);
    }

    /**
     * Plays menu open sound
     */
    private void playOpenSound(Player player) {
        String sound = getConfigString("compass-menu.sounds.open", "entity.villager.work_librarian");
        float volume = (float) getConfigDouble("sounds.volume", 1.0);
        float pitch = (float) getConfigDouble("sounds.pitch", 1.0);

        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String menuTitle = getConfigString("compass-menu.title", "§6Server Menu").replace("&", "§");

        if (!event.getView().getTitle().equals(menuTitle)) {
            return;
        }

        event.setCancelled(true); // Always cancel clicks in menu

        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) {
            return;
        }

        handleMenuClick((Player) event.getWhoClicked(), event.getCurrentItem());
    }

    /**
     * Handles menu item clicks
     */
    private void handleMenuClick(Player player, ItemStack clickedItem) {
        var itemsSection = vLobby.getInstance().getConfig().getConfigurationSection("lobby.compass.items");
        if (itemsSection == null) return;

        for (String key : itemsSection.getKeys(false)) {
            String configName = getConfigString("lobby.compass.items." + key + ".display_name", "Item").replace("&", "§");

            if (clickedItem.getItemMeta().getDisplayName().equals(configName)) {
                executeMenuItemAction(player, key);
                break;
            }
        }
    }

    /**
     * Executes actions for clicked menu item
     */
    private void executeMenuItemAction(Player player, String itemKey) {
        // Execute command if specified
        String command = getConfigString("lobby.compass.items." + itemKey + ".command", "").trim();
        if (!command.isEmpty()) {
            player.performCommand(command.replace("/", "").trim());
        }

        // Play click sound
        playClickSound(player, itemKey);

        // Close menu unless keep-open is enabled
        boolean keepOpen = getConfigBoolean("lobby.compass.items." + itemKey + ".keep-open", false);
        if (!keepOpen) {
            player.closeInventory();
        }
    }

    /**
     * Plays appropriate click sound for menu item
     */
    private void playClickSound(Player player, String itemKey) {
        String sound;
        float volume, pitch;

        // Use item-specific sound if configured
        if (vLobby.getInstance().getConfig().contains("lobby.compass.items." + itemKey + ".click_sound")) {
            // CORREÇÃO: Adicionado valor padrão no getConfigString
            sound = getConfigString("lobby.compass.items." + itemKey + ".click_sound", "entity.experience_orb.pickup");
            volume = (float) getConfigDouble("lobby.compass.items." + itemKey + ".sound_volume", 1.0);
            pitch = (float) getConfigDouble("lobby.compass.items." + itemKey + ".sound_pitch", 1.0);
        } else {
            // Use default menu sound
            sound = getConfigString("compass-menu.sounds.click", "entity.experience_orb.pickup");
            volume = (float) getConfigDouble("sounds.volume", 1.0);
            pitch = (float) getConfigDouble("sounds.pitch", 1.0);
        }

        try {
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            // Fallback to default sound
            vLobby.getInstance().getLogger().warning("Click sound not found: " + sound + " for item " + itemKey);
            player.playSound(player.getLocation(), "entity.experience_orb.pickup", volume, pitch);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return false;
        }

        open((Player) sender);
        return true;
    }

    // Helper methods for config access
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