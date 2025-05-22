package dev.improve.simpleeconomy.managers;

import dev.improve.simpleeconomy.SimpleEconomy;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final SimpleEconomy plugin;

    public ConfigManager(SimpleEconomy plugin) {
        this.plugin = plugin;
        setupDefaults();
    }

    private void setupDefaults() {
        FileConfiguration config = plugin.getConfig();

        config.addDefault("settings.default-balance", 100.0);

        // Add more settings with documentation-like comments in config.yml

        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    public String color(String msg){
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

}