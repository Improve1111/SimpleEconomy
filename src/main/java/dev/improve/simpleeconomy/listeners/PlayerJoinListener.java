package dev.improve.simpleeconomy.listeners;

import dev.improve.simpleeconomy.SimpleEconomy;
import dev.improve.simpleeconomy.managers.DatabaseManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final DatabaseManager databaseManager;

    public PlayerJoinListener() {
        this.databaseManager = SimpleEconomy.getInstance().getDatabaseManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        SimpleEconomy.getInstance().getServer().getScheduler().runTaskAsynchronously(SimpleEconomy.getInstance(), () -> {
            if (!databaseManager.hasBalance(player.getUniqueId())) {
                double defaultBalance = 100.0;
                databaseManager.setBalance(player.getUniqueId(), defaultBalance);

                SimpleEconomy.getInstance().getLogger().info("Created new balance for player: " + player.getName() + " with balance: " + defaultBalance);
            }
        });
    }
}