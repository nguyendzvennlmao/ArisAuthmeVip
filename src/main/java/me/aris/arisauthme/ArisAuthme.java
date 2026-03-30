package me.aris.arisauthme;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import me.aris.arisauthme.commands.AuthCommand;
import me.aris.arisauthme.commands.AdminCommand;
import me.aris.arisauthme.listeners.AuthListener;
import me.aris.arisauthme.manager.AuthManager;

public class ArisAuthme extends JavaPlugin {

    private static ArisAuthme instance;
    private AuthManager authManager;
    private Location arisLocation;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.authManager = new AuthManager(this);
        loadArisLocation();
        
        getServer().getPluginManager().registerEvents(new AuthListener(this), this);
        
        AuthCommand authCmd = new AuthCommand(this);
        getCommand("arislocation").setExecutor(authCmd);
        getCommand("arislocation").setTabCompleter(authCmd);

        AdminCommand adminCmd = new AdminCommand(this);
        getCommand("setpass").setExecutor(adminCmd);
        getCommand("setpass").setTabCompleter(adminCmd);
        getCommand("deletepass").setExecutor(adminCmd);
        getCommand("deletepass").setTabCompleter(adminCmd);
    }

    public void loadArisLocation() {
        if (getConfig().contains("arislocation.world")) {
            String worldName = getConfig().getString("arislocation.world");
            if (Bukkit.getWorld(worldName) != null) {
                this.arisLocation = new Location(
                    Bukkit.getWorld(worldName),
                    getConfig().getDouble("arislocation.x"),
                    getConfig().getDouble("arislocation.y"),
                    getConfig().getDouble("arislocation.z"),
                    (float) getConfig().getDouble("arislocation.yaw"),
                    (float) getConfig().getDouble("arislocation.pitch")
                );
            }
        }
    }

    public static ArisAuthme getInstance() { return instance; }
    public AuthManager getAuthManager() { return authManager; }
    public Location getArisLocation() { return arisLocation; }
          }
