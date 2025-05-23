package dev.improve.simpleeconomy.listeners;

import dev.improve.simpleeconomy.SimpleEconomy;
import dev.improve.simpleeconomy.managers.DatabaseManager;
import dev.improve.simpleeconomy.utils.Config;
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

        SimpleEconomy.getInstance().getServer().getScheduler().runTaskAsynchronously(SimpleEconomy.getInstance(), () -> {
            if (!databaseManager.hasBalance(player.getUniqueId())) {
                databaseManager.setBalance(player.getUniqueId(), Config.DEFAULT_BALANCE);
                //SimpleEconomy.getInstance().getLogger().info("Created new balance for player: " + player.getName() + " with balance: " + defaultBalance);
            }
            databaseManager.getBalances().put(player.getUniqueId(), databaseManager.getBalance(player.getUniqueId()));
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        SimpleEconomy.getInstance().getServer().getScheduler().runTaskAsynchronously(SimpleEconomy.getInstance(), () -> databaseManager.getBalances().remove(player.getUniqueId()));
    }


}