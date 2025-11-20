/*
Project: DeepCraft
File: SetBiomeCmd.java
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

package com.vitor.BiomeManager;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command executor for biome setting functionality
 * Allows admins to set biomes in large areas with batch processing
 */
public class SetBiomeCmd implements CommandExecutor {

    private static final String PERMISSION = "vl.admin";
    private static final String USAGE_MESSAGE = "§cUsage: /setbiome <world> <biome> <x1> <y1> <z1> <x2> <y2> <z2>";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Validate command sender and permissions
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        // Validate argument count
        if (args.length != 8) {
            player.sendMessage(USAGE_MESSAGE);
            return true;
        }

        // Parse and validate arguments
        CommandArguments parsedArgs = parseArguments(player, args);
        if (parsedArgs == null) {
            return true;
        }

        // Execute biome setting
        executeBiomeSet(player, parsedArgs);
        return true;
    }

    /**
     * Parses and validates command arguments
     */
    private CommandArguments parseArguments(Player player, String[] args) {
        try {
            World world = Bukkit.getWorld(args[0]);
            if (world == null) {
                player.sendMessage("§cInvalid world: " + args[0]);
                return null;
            }

            Biome biome = Biome.valueOf(args[1].toUpperCase());

            // Parse coordinates
            double[] coords = new double[6];
            for (int i = 0; i < 6; i++) {
                coords[i] = Double.parseDouble(args[i + 2]);
            }

            return new CommandArguments(world, biome, coords);

        } catch (IllegalArgumentException e) {
            player.sendMessage("§cInvalid biome or coordinates.");
            return null;
        }
    }

    /**
     * Executes the biome setting process
     */
    private void executeBiomeSet(Player player, CommandArguments args) {
        // Calculate area bounds
        int minX = (int) Math.min(args.coords[0], args.coords[3]);
        int minY = (int) Math.min(args.coords[1], args.coords[4]);
        int minZ = (int) Math.min(args.coords[2], args.coords[5]);

        int maxX = (int) Math.max(args.coords[0], args.coords[3]);
        int maxY = (int) Math.max(args.coords[1], args.coords[4]);
        int maxZ = (int) Math.max(args.coords[2], args.coords[5]);

        player.sendMessage("§aApplying biome... This may take a few seconds.");

        // Apply biome using batch processing
        BiomeUtil.applyBiomeArea(
                args.world,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                args.biome,
                2000, // Blocks per batch
                () -> player.sendMessage("§aBiome " + args.biome.name() + " applied successfully!")
        );
    }

    /**
     * Helper class for storing parsed command arguments
     */
    private static class CommandArguments {
        final World world;
        final Biome biome;
        final double[] coords;

        CommandArguments(World world, Biome biome, double[] coords) {
            this.world = world;
            this.biome = biome;
            this.coords = coords;
        }
    }
}