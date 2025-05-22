package dev.improve.simpleeconomy.utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtil {

    private final FileConfiguration config;
    private final Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public MessageUtil(FileConfiguration config) {
        this.config = config;
    }

    public String getMessage(String path) {
        String message = config.getString("messages." + path);
        if (message == null) {
            return ChatColor.RED + "Message not found: " + path;
        }
        return colorize(message);
    }

    public String getMessage(String path, String def) {
        return colorize(config.getString("messages." + path, def));
    }

    public String colorize(String input) {
        Matcher matcher = hexPattern.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            String replacement = net.md_5.bungee.api.ChatColor.of("#" + hexCode).toString();
            matcher.appendReplacement(buffer, replacement);
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

}
