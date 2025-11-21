/*
Project: DeepCraft
File: PlayerListener.java
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

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for player events
 * Manages tablist updates when players join/leave
 */
public class PlayerListener implements Listener {

    private final vTabList plugin;

    /**
     * Constructor for PlayerListener
     * @param plugin The main plugin instance
     */
    public PlayerListener(vTabList plugin) {
        this.plugin = plugin;
    }

    /**
     * Updates tablist when a player joins the server
     * @param event PlayerJoinEvent
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Delay to ensure player is fully connected
        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> {
                    TabListManager tabListManager = plugin.getTabListManager();
                    if (tabListManager != null) {
                        tabListManager.updateTabList(player);
                        tabListManager.updateAllTabLists();
                    }
                },
                5L
        );
    }

    /**
     * Updates tablist when a player leaves the server
     * @param event PlayerQuitEvent
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Delay to ensure player is removed from the list
        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> {
                    TabListManager tabListManager = plugin.getTabListManager();
                    if (tabListManager != null) {
                        tabListManager.updateAllTabLists();
                    }
                },
                1L
        );
    }
}