package dev.improve.simpleeconomy.listeners;

import dev.improve.simpleeconomy.SimpleEconomy;
import dev.improve.simpleeconomy.managers.DatabaseManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final DatabaseManager databaseManager;

    public PlayerListener(SimpleEconomy plugin) {
        this.databaseManager = plugin.getDatabaseManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Preload balance asynchronously to avoid main thread DB access
        databaseManager.loadBalanceAsync(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        databaseManager.evictFromCache(player.getUniqueId());
    }
}
