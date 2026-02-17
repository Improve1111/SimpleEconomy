package dev.improve.simpleeconomy.hooks;

import dev.improve.simpleeconomy.SimpleEconomy;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PAPIHook extends PlaceholderExpansion {

    private final SimpleEconomy plugin;

    public PAPIHook(SimpleEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "se";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Improve";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        return switch (params.toLowerCase()) {
            case "balance" -> {
                double balance = plugin.getDatabaseManager().getBalance(player.getUniqueId());
                yield String.valueOf(balance);
            }
            case "balance_formatted" -> {
                double balance = plugin.getDatabaseManager().getBalance(player.getUniqueId());
                yield formatBalance(balance);
            }
            case "rank" -> {
                int rank = plugin.getDatabaseManager().getPlayerRank(player.getUniqueId());
                yield rank > 0 ? String.valueOf(rank) : "?";
            }
            case "total" -> {
                double total = plugin.getDatabaseManager().getTotalEconomy();
                yield formatBalance(total);
            }
            case "players" -> {
                int count = plugin.getDatabaseManager().getPlayerCount();
                yield String.valueOf(count);
            }
            default -> null;
        };
    }

    private String formatBalance(double balance) {
        if (balance < 1_000) {
            return String.format("%.2f", balance);
        } else if (balance < 1_000_000) {
            return String.format("%.0fK", balance / 1_000);
        } else if (balance < 1_000_000_000) {
            return String.format("%.0fM", balance / 1_000_000);
        } else {
            return String.format("%.0fB", balance / 1_000_000_000);
        }
    }

}

