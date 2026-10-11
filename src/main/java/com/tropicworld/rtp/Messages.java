package com.tropicworld.rtp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public final class Messages {

    private final TropicRTP plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private FileConfiguration cfg;

    public Messages(TropicRTP plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        cfg = YamlConfiguration.loadConfiguration(file);
    }

    /** Сообщение из messages.yml по ключу. */
    public Component get(String key, TagResolver... resolvers) {
        String raw = cfg.getString(key, "<prefix> <red>Сообщение не найдено: " + key + "</red>");
        return parse(raw, resolvers);
    }

    /** Произвольная строка MiniMessage с поддержкой <prefix>. */
    public Component parse(String raw, TagResolver... resolvers) {
        String prefix = plugin.getConfig().getString("prefix", "");
        return mm.deserialize(raw, TagResolver.resolver(
                Placeholder.parsed("prefix", prefix),
                TagResolver.resolver(resolvers)
        ));
    }

    public static TagResolver num(String name, long value) {
        return Placeholder.parsed(name, String.valueOf(value));
    }
}
