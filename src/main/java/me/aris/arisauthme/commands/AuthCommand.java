package me.aris.arisauthme.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import me.aris.arisauthme.ArisAuthme;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AuthCommand implements CommandExecutor, TabCompleter {
    private final ArisAuthme plugin;
    public AuthCommand(ArisAuthme plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (s instanceof Player p && p.hasPermission("arisauthme.admin")) {
            if (a.length > 0 && a[0].equalsIgnoreCase("reload")) {
                plugin.reloadConfig();
                plugin.getAuthManager().loadMessages();
                plugin.loadArisLocation();
                s.sendMessage(plugin.getAuthManager().color("&#facc15đã Reload toàn bộ cấu hình!"));
            } else {
                plugin.getConfig().set("arislocation.world", p.getWorld().getName());
                plugin.getConfig().set("arislocation.x", p.getLocation().getX());
                plugin.getConfig().set("arislocation.y", p.getLocation().getY());
                plugin.getConfig().set("arislocation.z", p.getLocation().getZ());
                plugin.getConfig().set("arislocation.yaw", p.getLocation().getYaw());
                plugin.getConfig().set("arislocation.pitch", p.getLocation().getPitch());
                plugin.saveConfig();
                plugin.loadArisLocation();
                s.sendMessage(plugin.getAuthManager().getMsg("messages.location_set"));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 1) return Arrays.asList("set", "reload").stream().filter(i -> i.startsWith(a[0].toLowerCase())).collect(Collectors.toList());
        return null;
    }
        }
