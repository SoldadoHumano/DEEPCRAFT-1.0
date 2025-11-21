/*
Project: DeepCraft
File: TabListCommand.java
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

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Administrative command for vTabList
 * Allows reloading plugin configurations
 */
public class TabListCommand implements CommandExecutor, TabCompleter {

    private static final String RELOAD_PERMISSION = "vtablist.reload";
    private static final String ADMIN_PERMISSION = "vtablist.admin";

    private final vTabList plugin;

    /**
     * Constructor for TabListCommand
     * @param plugin The main plugin instance
     */
    public TabListCommand(vTabList plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the vtablist command
     * @param sender Command sender
     * @param command Command instance
     * @param label Command label
     * @param args Command arguments
     * @return true if command was handled successfully
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, ADMIN_PERMISSION)) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            return executeReload(sender);
        }

        sendHelp(sender);
        return true;
    }

    /**
     * Executes the reload action for configurations
     * @param sender Command sender
     * @return true if reload was successful
     */
    private boolean executeReload(CommandSender sender) {
        if (!hasPermission(sender, RELOAD_PERMISSION)) {
            sender.sendMessage("§cYou don't have permission to reload configurations.");
            return true;
        }

        try {
            plugin.getConfigManager().reloadConfigs();
            plugin.getTabListManager().restart();
            sender.sendMessage("§avTabList reloaded successfully!");
        } catch (Exception e) {
            sender.sendMessage("§cError reloading vTabList. Check console for details.");
            plugin.getLogger().severe("Reload error: " + e.getMessage());
        }

        return true;
    }

    /**
     * Checks if sender has required permission
     * @param sender Command sender
     * @param permission Required permission
     * @return true if sender has permission
     */
    private boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.hasPermission(ADMIN_PERMISSION);
    }

    /**
     * Sends help message to command sender
     * @param sender Command sender
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== vTabList Commands ===");
        sender.sendMessage("§e/vtablist reload §7- Reloads plugin configuration");
        sender.sendMessage("§e/vtablist help §7- Shows this help message");
    }

    /**
     * Provides tab completion for the command
     * @param sender Command sender
     * @param command Command instance
     * @param alias Command alias
     * @param args Command arguments
     * @return List of tab completions
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!hasPermission(sender, ADMIN_PERMISSION)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if ("reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
            if ("help".startsWith(args[0].toLowerCase())) {
                completions.add("help");
            }
            return completions;
        }

        return Collections.emptyList();
    }
}