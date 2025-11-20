/*
Project: DeepCraft
File: HeartEffects.java
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

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Applies permanent regeneration effect to players in lobby
 * Ensures players maintain full health while in lobby area
 */
public class HeartEffects implements Listener {

    /**
     * Applies regeneration effect when player joins
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        applyRegenerationEffect(event.getPlayer());
    }

    /**
     * Applies infinite regeneration effect to player
     */
    private void applyRegenerationEffect(Player player) {
        PotionEffect regeneration = new PotionEffect(
                PotionEffectType.REGENERATION,
                -1, // Infinite duration
                0,  // Amplifier level 0
                false, // No ambient particles
                false, // No icon in inventory
                false  // No icon in HUD
        );

        player.addPotionEffect(regeneration);
    }
}