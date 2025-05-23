package dev.improve.simpleeconomy.utils;

import dev.improve.simpleeconomy.SimpleEconomy;

public class Config {

    private final SimpleEconomy plugin;
    public static double DEFAULT_BALANCE;

    public Config(SimpleEconomy plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload(){
        DEFAULT_BALANCE = plugin.getConfig().getDouble("settings.start-balance", 100.0);
    }

}
