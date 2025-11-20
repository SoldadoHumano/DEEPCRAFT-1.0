/*
Project: DeepCraft
File: vLobby.java
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

import com.vitor.BiomeManager.SetBiomeCmd;
import com.vitor.MobPortals.PortalMobManager;
import com.vitor.PlayerManager.BowTeleportListener;
import com.vitor.PlayerManager.CompassMenu;
import com.vitor.PlayerManager.HeartEffects;
import com.vitor.PlayerManager.LobbyListener;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class for vLobby plugin - Provides comprehensive lobby management system
 * including spawn protection, server menus, teleport bows, and interactive portals.
 */
public class vLobby extends JavaPlugin {

    private static vLobby instance;
    private PortalMobManager portalMobManager;

    @Override
    public void onEnable() {
        instance = this;

        initializePlugin();
        registerEventListeners();
        registerCommands();
        startBackgroundTasks();

        getLogger().info("vLobby loaded successfully!");
    }

    /**
     * Initializes plugin configuration and core components
     */
    private void initializePlugin() {
        saveDefaultConfig();
        reloadConfig();

        portalMobManager = new PortalMobManager(this);

        // Set main world to hardcore mode if exists
        World world = getServer().getWorld("world");
        if (world != null) {
            world.setHardcore(true);
        }
    }

    /**
     * Registers all event listeners
     */
    private void registerEventListeners() {
        getServer().getPluginManager().registerEvents(new LobbyListener(), this);
        getServer().getPluginManager().registerEvents(new CompassMenu(), this);
        getServer().getPluginManager().registerEvents(new HeartEffects(), this);
        getServer().getPluginManager().registerEvents(portalMobManager, this);
        getServer().getPluginManager().registerEvents(new BowTeleportListener(), this);
    }

    /**
     * Registers all command executors
     */
    private void registerCommands() {
        getCommand("lobbymenu").setExecutor(new CompassMenu());
        getCommand("vlb").setExecutor(this);
        getCommand("setbiome").setExecutor(new SetBiomeCmd());
    }

    /**
     * Starts repeating background tasks
     */
    private void startBackgroundTasks() {
        startWorldTask();

        // Spawn all portals on startup
        if (portalMobManager != null) {
            portalMobManager.spawnPortals();
        }
    }

    /**
     * Maintains the lobby world always at day and clear weather.
     *
     * @deprecated Use the alternative of disabling the day cycle in the world settings
     *             instead of running a repeating task. This method may be removed in a future version.
     */
    @Deprecated(since="3.1.2", forRemoval=true)
    private void startWorldTask() {
        getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (World world : getServer().getWorlds()) {
                if (world.getName().equals(getConfig().getString("lobby.spawn.world", "world"))) {
                    world.setTime(1000);
                    world.setStorm(false);
                    world.setThundering(false);
                }
            }
        }, 0L, 100L);
    }

    /**
     * Reloads plugin configuration and respawns portals
     */
    public void reloadPlugin() {
        reloadConfig();
        if (portalMobManager != null) {
            portalMobManager.spawnPortals();
        }
        getLogger().info("vLobby configuration reloaded successfully!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("vlb")) {
            return false;
        }

        if (!sender.hasPermission("vl.admin")) {
            sender.sendMessage("§cYou do not have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            sendCommandHelp(sender);
            return true;
        }

        handleCommand(sender, args);
        return true;
    }

    /**
     * Displays command help information
     */
    private void sendCommandHelp(CommandSender sender) {
        sender.sendMessage("§eAvailable commands:");
        sender.sendMessage("§e/vlb reload §7- Reloads plugin configuration");
        sender.sendMessage("§e/vlb version §7- Shows plugin version");
    }

    /**
     * Handles plugin commands
     */
    private void handleCommand(CommandSender sender, String[] args) {
        switch (args[0].toLowerCase()) {
            case "reload":
            case "rl":
                reloadPlugin();
                sender.sendMessage("§avLobby configuration reloaded successfully!");
                break;

            case "version":
            case "ver":
            case "v":
                sender.sendMessage("§6vLobby §eVersion 3.21");
                sender.sendMessage("§6Author: §bvitor1227_OP");
                break;

            default:
                sender.sendMessage("§cUnknown command. Use §e/vlb §cfor help.");
                break;
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("vLobby disabled!");
    }

    /**
     * Returns the plugin instance
     * @return vLobby instance
     */
    public static vLobby getInstance() {
        return instance;
    }
}