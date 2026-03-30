package me.aris.arisauthme.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.*;
import me.aris.arisauthme.ArisAuthme;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthListener implements Listener {
    private final ArisAuthme plugin;
    private final Map<UUID, Location> lastLoc = new HashMap<>();

    public AuthListener(ArisAuthme plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.getAuthManager().setLoggedIn(p.getUniqueId(), false);
        plugin.getAuthManager().setAdminVerified(p.getUniqueId(), false);
        if (plugin.getConfig().getBoolean("settings.teleport_to_spawn_on_join") && plugin.getArisLocation() != null) {
            lastLoc.put(p.getUniqueId(), p.getLocation());
            p.teleportAsync(plugin.getArisLocation());
        }
        startAuthTask(p);
    }

    private void startAuthTask(Player p) {
        boolean isReg = new File(plugin.getAuthManager().getPlayerFolder(), p.getUniqueId() + ".yml").exists();
        boolean hasAdminFile = new File(plugin.getAuthManager().getAdminFolder(), p.getUniqueId() + ".yml").exists();
        boolean needsAdmin = p.isOp() || hasAdminFile;
        plugin.getAuthManager().getTimeLeft().put(p.getUniqueId(), plugin.getConfig().getInt("settings.timeout_seconds"));

        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> {
            if (!p.isOnline()) { t.cancel(); return; }
            boolean logged = plugin.getAuthManager().isLoggedIn(p.getUniqueId());
            boolean adminOk = !needsAdmin || plugin.getAuthManager().isAdminVerified(p.getUniqueId());
            if (logged && adminOk) { t.cancel(); return; }

            int time = plugin.getAuthManager().getTimeLeft().get(p.getUniqueId());
            if (time <= 0) {
                Bukkit.getRegionScheduler().run(plugin, p.getLocation(), task -> {
                    if (needsAdmin && !plugin.getAuthManager().isAdminVerified(p.getUniqueId())) p.setOp(false);
                    p.kickPlayer(plugin.getAuthManager().getMsg("messages.timeout_kick"));
                });
                t.cancel(); return;
            }
            plugin.getAuthManager().sendAuthVisuals(p, !isReg, needsAdmin, time);
            plugin.getAuthManager().getTimeLeft().put(p.getUniqueId(), time - 1);
        }, 1L, 20L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent e) { if (!check(e.getPlayer())) e.setTo(e.getFrom()); }
    @EventHandler public void onBreak(BlockBreakEvent e) { if (!check(e.getPlayer())) e.setCancelled(true); }
    @EventHandler public void onPlace(BlockPlaceEvent e) { if (!check(e.getPlayer())) e.setCancelled(true); }
    @EventHandler public void onChat(AsyncPlayerChatEvent e) { if (!check(e.getPlayer())) e.setCancelled(true); }

    private boolean check(Player p) {
        return plugin.getAuthManager().isLoggedIn(p.getUniqueId()) && (!p.isOp() || plugin.getAuthManager().isAdminVerified(p.getUniqueId()));
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        String[] args = e.getMessage().split(" ");
        String cmd = args[0].toLowerCase();
        if (cmd.equals("/dangky") || cmd.equals("/dangnhap") || cmd.equals("/adminpass")) {
            e.setCancelled(true);
            if (cmd.equals("/dangky")) handleReg(p, args);
            else if (cmd.equals("/dangnhap")) handleLogin(p, args);
            else handleAdminAuth(p, args);
            return;
        }
        if (!check(p)) e.setCancelled(true);
    }

    private void handleReg(Player p, String[] a) {
        if (plugin.getAuthManager().isLoggedIn(p.getUniqueId()) || a.length < 3 || !a[1].equals(a[2])) return;
        File f = new File(plugin.getAuthManager().getPlayerFolder(), p.getUniqueId() + ".yml");
        if (f.exists()) return;
        YamlConfiguration c = new YamlConfiguration();
        c.set("pass", plugin.getAuthManager().hash(a[1]));
        try { c.save(f); } catch (Exception ignored) {}
        p.sendTitle(plugin.getAuthManager().getMsg("messages.success_register_title"), "", 10, 40, 10);
        done(p);
    }

    private void handleLogin(Player p, String[] a) {
        if (plugin.getAuthManager().isLoggedIn(p.getUniqueId()) || a.length < 2) return;
        File f = new File(plugin.getAuthManager().getPlayerFolder(), p.getUniqueId() + ".yml");
        if (f.exists() && plugin.getAuthManager().check(a[1], YamlConfiguration.loadConfiguration(f).getString("pass"))) {
            p.sendTitle(plugin.getAuthManager().getMsg("messages.success_login_title"), "", 10, 40, 10);
            done(p);
        } else p.sendMessage(plugin.getAuthManager().getMsg("messages.wrong_password"));
    }

    private void handleAdminAuth(Player p, String[] a) {
        if (plugin.getAuthManager().isAdminVerified(p.getUniqueId()) || a.length < 2) return;
        File f = new File(plugin.getAuthManager().getAdminFolder(), p.getUniqueId() + ".yml");
        if (!f.exists()) return;
        if (plugin.getAuthManager().check(a[1], YamlConfiguration.loadConfiguration(f).getString("pass"))) {
            plugin.getAuthManager().setAdminVerified(p.getUniqueId(), true);
            p.sendMessage(plugin.getAuthManager().getMsg("messages.admin_success"));
            if (lastLoc.containsKey(p.getUniqueId())) p.teleportAsync(lastLoc.remove(p.getUniqueId()));
        } else {
            int att = plugin.getAuthManager().getAdminAttempts().getOrDefault(p.getUniqueId(), 0) + 1;
            if (att >= 5) Bukkit.getRegionScheduler().run(plugin, p.getLocation(), t -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban-ip " + p.getName()));
            else plugin.getAuthManager().getAdminAttempts().put(p.getUniqueId(), att);
        }
    }

    private void done(Player p) {
        plugin.getAuthManager().setLoggedIn(p.getUniqueId(), true);
        plugin.getAuthManager().playSound(p, "sounds.success");
        boolean hasAdminFile = new File(plugin.getAuthManager().getAdminFolder(), p.getUniqueId() + ".yml").exists();
        if (!(p.isOp() || hasAdminFile) && lastLoc.containsKey(p.getUniqueId())) p.teleportAsync(lastLoc.remove(p.getUniqueId()));
    }
    }
