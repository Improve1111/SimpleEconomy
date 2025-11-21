package dev.improve.simpleeconomy.utils;

import dev.improve.simpleeconomy.SimpleEconomy;
import org.bukkit.configuration.file.FileConfiguration;

public class Config {

    private final SimpleEconomy plugin;

    public static double DEFAULT_BALANCE;
    public static double MIN_BALANCE;
    public static double MAX_BALANCE;
    public static String CURRENCY_SYMBOL;
    public static long SAVE_INTERVAL_TICKS;

    public Config(SimpleEconomy plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfig();

        DEFAULT_BALANCE = cfg.getDouble("settings.start-balance", 100.0);
        MIN_BALANCE = Math.max(0.0, cfg.getDouble("settings.min-balance", 0.0));

        double configuredMax = cfg.getDouble("settings.max-balance", Double.MAX_VALUE);
        if (configuredMax < 0) {
            configuredMax = Double.MAX_VALUE;
        }

        MAX_BALANCE = Math.max(configuredMax, MIN_BALANCE);
        CURRENCY_SYMBOL = cfg.getString("settings.currency-symbol", "$");
        SAVE_INTERVAL_TICKS = Math.max(0L, cfg.getLong("settings.save-interval-ticks", 1200L));
    }
}
