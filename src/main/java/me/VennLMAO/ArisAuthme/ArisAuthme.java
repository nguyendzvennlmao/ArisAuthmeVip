package me.VennLMAO.ArisAuthme;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ArisAuthme extends JavaPlugin implements Listener, CommandExecutor {

    private final Set<UUID> authenticated = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("dangky").setExecutor(this);
        getCommand("dangnhap").setExecutor(this);

        Bukkit.getAsyncScheduler().runAtFixedRate(this, (task) -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!authenticated.contains(p.getUniqueId())) {
                    String cmd = isRegistered(p) ? "/dangnhap <matkhau>" : "/dangky <mk> <mk>";
                    String msg = color(getConfig().getString("messages.require-auth").replace("%cmd%", cmd));
                    p.sendTitle("", msg, 0, 40, 10);
                }
            }
        }, 1, 2, TimeUnit.SECONDS);
    }

    private boolean isRegistered(Player p) {
        return getConfig().contains("players." + p.getName().toLowerCase());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        String name = p.getName().toLowerCase();

        if (label.equalsIgnoreCase("dangky")) {
            if (isRegistered(p)) {
                p.sendMessage(color(getConfig().getString("messages.already-registered")));
                return true;
            }
            if (args.length < 2) return false;

            String ip = p.getAddress().getAddress().getHostAddress().replace(".", "_");
            int count = getConfig().getInt("ips." + ip, 0);
            int max = getConfig().getInt("max-acc-per-ip");
            if (count >= max) {
                p.sendMessage(color(getConfig().getString("messages.max-ip-reached").replace("%limit%", String.valueOf(max))));
                return true;
            }

            if (!args[0].equals(args[1])) {
                p.sendMessage(color(getConfig().getString("messages.password-not-match")));
                return true;
            }

            getConfig().set("players." + name, args[0]);
            getConfig().set("ips." + ip, count + 1);
            saveConfig();
            authenticated.add(p.getUniqueId());
            p.sendMessage(color(getConfig().getString("messages.register-success")));
            p.resetTitle();

        } else if (label.equalsIgnoreCase("dangnhap")) {
            if (!isRegistered(p)) {
                p.sendMessage(color(getConfig().getString("messages.not-registered")));
                return true;
            }
            if (args.length < 1) return false;

            if (getConfig().getString("players." + name).equals(args[0])) {
                authenticated.add(p.getUniqueId());
                p.sendMessage(color(getConfig().getString("messages.login-success")));
                p.resetTitle();
            } else {
                p.sendMessage(color(getConfig().getString("messages.wrong-password")));
            }
        }
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent e) {
        if (!authenticated.contains(e.getPlayer().getUniqueId())) {
            if (e.getFrom().getX() != e.getTo().getX() || e.getFrom().getZ() != e.getTo().getZ()) {
                e.setTo(e.getFrom());
            }
        }
    }

    @EventHandler public void onBreak(BlockBreakEvent e) { if (!authenticated.contains(e.getPlayer().getUniqueId())) e.setCancelled(true); }
    @EventHandler public void onPlace(BlockPlaceEvent e) { if (!authenticated.contains(e.getPlayer().getUniqueId())) e.setCancelled(true); }
    @EventHandler public void onDamage(EntityDamageByEntityEvent e) { if (!authenticated.contains(e.getDamager().getUniqueId())) e.setCancelled(true); }
    @EventHandler public void onChat(AsyncPlayerChatEvent e) { if (!authenticated.contains(e.getPlayer().getUniqueId())) e.setCancelled(true); }
    
    @EventHandler
    public void onCommandPre(PlayerCommandPreprocessEvent e) {
        if (!authenticated.contains(e.getPlayer().getUniqueId())) {
            String msg = e.getMessage().toLowerCase();
            if (!msg.startsWith("/dangky") && !msg.startsWith("/dangnhap")) e.setCancelled(true);
        }
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) { authenticated.remove(e.getPlayer().getUniqueId()); }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
  }
