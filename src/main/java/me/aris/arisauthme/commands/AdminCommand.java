package me.aris.arisauthme.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import me.aris.arisauthme.ArisAuthme;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminCommand implements CommandExecutor, TabCompleter {
    private final ArisAuthme plugin;
    public AdminCommand(ArisAuthme plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!s.hasPermission("arisauth.admin") || a.length < 1) return true;
        Player t = Bukkit.getPlayer(a[0]);
        if (t == null) return true;
        File f = new File(plugin.getAuthManager().getAdminFolder(), t.getUniqueId() + ".yml");
        if (l.equalsIgnoreCase("setpass") && a.length > 1) {
            YamlConfiguration conf = new YamlConfiguration();
            conf.set("pass", plugin.getAuthManager().hash(a[1]));
            try { conf.save(f); } catch (Exception ignored) {}
            s.sendMessage(plugin.getAuthManager().getMsg("messages.admin_setpass").replace("%player%", t.getName()));
        } else if (l.equalsIgnoreCase("deletepass")) {
            f.delete();
            s.sendMessage(plugin.getAuthManager().getMsg("messages.admin_delpass").replace("%player%", t.getName()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(a[0].toLowerCase())).collect(Collectors.toList());
        return new ArrayList<>();
    }
}
