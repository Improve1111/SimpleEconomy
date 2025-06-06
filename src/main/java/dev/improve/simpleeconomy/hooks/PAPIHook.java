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
        if (params.equalsIgnoreCase("balance")) {
            Double balance = plugin.getDatabaseManager().getBalances().get(player.getUniqueId());
            if (balance == null) {
                return "0";
            }
            return String.valueOf(balance);
        }

        if (params.equalsIgnoreCase("balance_formatted")) {
            Double balance = plugin.getDatabaseManager().getBalances().get(player.getUniqueId());
            if (balance == null) {
                return "0.00";
            }

            return formatBalance(balance);
        }

        return super.onRequest(player, params);
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

