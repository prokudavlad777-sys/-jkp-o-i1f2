package com.tropicworld.rtp;

import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TropicRTP extends JavaPlugin {

    private Messages messages;
    private RtpService service;
    private List<RtpOption> options = List.of();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        messages = new Messages(this);
        service = new RtpService(this);
        loadOptions();

        PluginCommand cmd = getCommand("rtp");
        if (cmd != null) {
            RtpCommand handler = new RtpCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getLogger().info("TropicRTP включён.");
    }

    public void reloadAll() {
        reloadConfig();
        messages.reload();
        loadOptions();
    }

    private void loadOptions() {
        List<RtpOption> list = new ArrayList<>();
        ConfigurationSection section = getConfig().getConfigurationSection("options");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection s = section.getConfigurationSection(key);
                if (s == null) {
                    continue;
                }
                Material material = Material.matchMaterial(s.getString("material", "STONE"));
                if (material == null) {
                    material = Material.STONE;
                }
                list.add(new RtpOption(
                        key,
                        s.getInt("slot", 13),
                        material,
                        s.getString("name", key),
                        s.getStringList("lore"),
                        parse(RtpOption.Mode.class, s.getString("mode"), RtpOption.Mode.RANDOM),
                        parse(RtpOption.Center.class, s.getString("center"), RtpOption.Center.PLAYER),
                        s.getInt("min", 0),
                        s.getInt("max", 250),
                        s.getString("permission", "tropicrtp.use")
                ));
            }
        }
        options = List.copyOf(list);
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, E fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    public Messages messages() {
        return messages;
    }

    public RtpService service() {
        return service;
    }

    public List<RtpOption> options() {
        return options;
    }
}
