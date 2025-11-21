/*
Project: DeepCraft
File: vTabList.java
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

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Main vTabList plugin - Customizable tablist system for Minecraft

 */
public class vTabList extends JavaPlugin {

    private ConfigManager configManager;
    private TabListManager tabListManager;

    private static vTabList instance;

    /**
     * Returns the singleton plugin instance
     * @return vTabList instance
     */
    public static vTabList getInstance() {
        return instance;
    }

    /**
     * Called when plugin is enabled
     */
    @Override
    public void onEnable() {
        instance = this;

        try {
            initializeManagers();
            registerEvents();
            registerCommands();
            startTasks();

            getLogger().info("vTabList enabled successfully! Version: " + getDescription().getVersion());
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error enabling vTabList", e);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Called when plugin is disabled
     */
    @Override
    public void onDisable() {
        if (tabListManager != null) {
            tabListManager.stop();
        }

        getLogger().info("vTabList disabled!");
    }

    /**
     * Initializes main managers
     */
    private void initializeManagers() {
        this.configManager = new ConfigManager(this);
        this.tabListManager = new TabListManager(this, configManager);
    }

    /**
     * Registers event listeners
     */
    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    /**
     * Registers commands
     */
    private void registerCommands() {
        getCommand("vtablist").setExecutor(new TabListCommand(this));
    }

    /**
     * Starts periodic tasks
     */
    private void startTasks() {
        tabListManager.start();
    }

    // Getters

    /**
     * Gets the configuration manager
     * @return ConfigManager instance
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Gets the tablist manager
     * @return TabListManager instance
     */
    public TabListManager getTabListManager() {
        return tabListManager;
    }

    /**
     * Gets the local server online player count
     * @return Number of players online on this server
     */
    public int getServerOnline() {
        return Bukkit.getOnlinePlayers().size();
    }
}