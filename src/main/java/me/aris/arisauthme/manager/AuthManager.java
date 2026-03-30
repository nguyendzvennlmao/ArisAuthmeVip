package me.aris.arisauthme.manager;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.mindrot.jbcrypt.BCrypt;
import me.aris.arisauthme.ArisAuthme;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthManager {
    private final ArisAuthme plugin;
    private final Map<UUID, Boolean> loggedIn = new HashMap<>();
    private final Map<UUID, Boolean> adminVerified = new HashMap<>();
    private final Map<UUID, Integer> timeLeft = new HashMap<>();
    private final Map<UUID, Integer> adminAttempts = new HashMap<>();
    private final File playerFolder;
    private final File adminFolder;
    private FileConfiguration msgConfig;

    public AuthManager(ArisAuthme plugin) {
        this.plugin = plugin;
        this.playerFolder = new File(plugin.getDataFolder(), "pasplayer");
        this.adminFolder = new File(plugin.getDataFolder(), "adminpass");
        if (!playerFolder.exists()) playerFolder.mkdirs();
        if (!adminFolder.exists()) adminFolder.mkdirs();
        loadMessages();
    }

    public void loadMessages() {
        File msgFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!msgFile.exists()) plugin.saveResource("messages.yml", false);
        msgConfig = YamlConfiguration.loadConfiguration(msgFile);
    }

    public String getMsg(String path) {
        return color(msgConfig.getString(path, "&c" + path));
    }

    public String color(String msg) {
        if (msg == null) return "";
        Matcher m = Pattern.compile("&#([a-fA-F0-9]{6})").matcher(msg);
        while (m.find()) msg = msg.replace(m.group(), ChatColor.of("#" + m.group(1)).toString());
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public void sendAuthVisuals(Player p, boolean isReg, boolean needsAdmin, int time) {
        String actionPath = needsAdmin && loggedIn.getOrDefault(p.getUniqueId(), false) ? "messages.admin_actionbar" : (isReg ? "messages.register_actionbar" : "messages.login_actionbar");
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(getMsg(actionPath).replace("%time%", String.valueOf(time))));
        String titlePath = needsAdmin && loggedIn.getOrDefault(p.getUniqueId(), false) ? "titles.admin_main" : (isReg ? "titles.register_main" : "titles.login_main");
        String subPath = needsAdmin && loggedIn.getOrDefault(p.getUniqueId(), false) ? "titles.admin_sub" : (isReg ? "titles.register_sub" : "titles.login_sub");
        p.sendTitle(getMsg(titlePath), getMsg(subPath), 0, 21, 0);
    }

    public String hash(String password) { return BCrypt.hashpw(password, BCrypt.gensalt(10)); }
    public boolean check(String password, String hashed) { try { return BCrypt.checkpw(password, hashed); } catch (Exception e) { return false; } }
    public void playSound(Player p, String path) {
        String s = plugin.getConfig().getString(path);
        if (s != null) p.playSound(p.getLocation(), Sound.valueOf(s.toUpperCase()), 1.0f, 1.0f);
    }

    public boolean isLoggedIn(UUID id) { return loggedIn.getOrDefault(id, false); }
    public void setLoggedIn(UUID id, boolean s) { loggedIn.put(id, s); }
    public boolean isAdminVerified(UUID id) { return adminVerified.getOrDefault(id, false); }
    public void setAdminVerified(UUID id, boolean s) { adminVerified.put(id, s); }
    public Map<UUID, Integer> getTimeLeft() { return timeLeft; }
    public Map<UUID, Integer> getAdminAttempts() { return adminAttempts; }
    public File getPlayerFolder() { return playerFolder; }
    public File getAdminFolder() { return adminFolder; }
  }
