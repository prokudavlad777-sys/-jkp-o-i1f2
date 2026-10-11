package com.tropicworld.rtp;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class RtpCommand implements CommandExecutor, TabCompleter {

    private final TropicRTP plugin;

    public RtpCommand(TropicRTP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages msg = plugin.messages();

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("tropicrtp.admin")) {
                    sender.sendMessage(msg.get("no-permission"));
                    return true;
                }
                plugin.reloadAll();
                sender.sendMessage(plugin.messages().get("reloaded"));
                return true;
            }
            sender.sendMessage(msg.get("usage"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg.get("only-player"));
            return true;
        }
        if (!player.hasPermission("tropicrtp.use")) {
            player.sendMessage(msg.get("no-permission"));
            return true;
        }
        if (!plugin.service().isWorldAllowed(player.getWorld())) {
            player.sendMessage(msg.get("world-disabled"));
            return true;
        }

        player.openInventory(new RtpMenu(plugin, player).getInventory());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("tropicrtp.admin") && "reload".startsWith(args[0].toLowerCase())) {
            return List.of("reload");
        }
        return List.of();
    }
}
