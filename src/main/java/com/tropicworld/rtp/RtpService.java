package com.tropicworld.rtp;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.metadata.MetadataValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class RtpService {

    private final TropicRTP plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> searching = ConcurrentHashMap.newKeySet();

    public RtpService(TropicRTP plugin) {
        this.plugin = plugin;
    }

    /** Кулдаун игрока в секундах (учитывает группы LuckPerms через group.<имя>). */
    public int cooldownFor(Player player) {
        int base = plugin.getConfig().getInt("cooldown.default", 30);
        ConfigurationSection groups = plugin.getConfig().getConfigurationSection("cooldown.groups");
        if (groups != null) {
            for (String group : groups.getKeys(false)) {
                if (player.hasPermission("group." + group.toLowerCase())) {
                    base = Math.min(base, groups.getInt(group, base));
                }
            }
        }
        return Math.max(0, base);
    }

    public long remainingSeconds(Player player) {
        Long until = cooldowns.get(player.getUniqueId());
        if (until == null) {
            return 0;
        }
        long left = until - System.currentTimeMillis();
        return left <= 0 ? 0 : (left + 999) / 1000;
    }

    public boolean isWorldAllowed(World world) {
        return plugin.getConfig().getStringList("worlds").contains(world.getName());
    }

    public boolean hasAccess(Player player, RtpOption option) {
        String perm = option.permission();
        return perm == null || perm.isBlank()
                || player.hasPermission(perm)
                || player.hasPermission("tropicrtp.menu.all");
    }

    /** Игрок нажал на вариант в меню. */
    public void choose(Player player, RtpOption option) {
        Messages msg = plugin.messages();
        plugin.getServer().getScheduler().runTask(plugin, () -> player.closeInventory());

        if (!hasAccess(player, option)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            player.sendMessage(msg.get("no-group"));
            return;
        }
        if (!isWorldAllowed(player.getWorld())) {
            player.sendMessage(msg.get("world-disabled"));
            return;
        }
        if (!player.hasPermission("tropicrtp.bypass")) {
            long left = remainingSeconds(player);
            if (left > 0) {
                player.sendMessage(msg.get("cooldown", Messages.num("seconds", left)));
                return;
            }
        }
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        start(player, option);
    }

    private void start(Player player, RtpOption option) {
        UUID id = player.getUniqueId();
        Messages msg = plugin.messages();
        if (!searching.add(id)) {
            player.sendMessage(msg.get("already-searching"));
            return;
        }

        World world = player.getWorld();
        Location center;
        boolean clamp = false;

        if (option.mode() == RtpOption.Mode.PLAYERS) {
            Player target = pickTarget(player, world);
            if (target == null) {
                searching.remove(id);
                player.sendMessage(msg.get("no-players"));
                return;
            }
            center = target.getLocation();
        } else if (option.center() == RtpOption.Center.WORLD) {
            center = world.getWorldBorder().getCenter();
            clamp = true;
        } else {
            center = player.getLocation();
        }

        player.sendMessage(msg.get("searching"));
        attempt(player, world, option, center, clamp, plugin.getConfig().getInt("max-attempts", 15));
    }

    private Player pickTarget(Player self, World world) {
        List<Player> candidates = new ArrayList<>();
        for (Player p : world.getPlayers()) {
            if (!p.equals(self) && p.getGameMode() != GameMode.SPECTATOR && !vanished(p)) {
                candidates.add(p);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private boolean vanished(Player p) {
        for (MetadataValue v : p.getMetadata("vanished")) {
            if (v.asBoolean()) {
                return true;
            }
        }
        return false;
    }

    private void attempt(Player player, World world, RtpOption option, Location center, boolean clamp, int left) {
        UUID id = player.getUniqueId();
        if (!player.isOnline()) {
            searching.remove(id);
            return;
        }
        if (left <= 0) {
            searching.remove(id);
            player.sendMessage(plugin.messages().get("failed"));
            return;
        }

        int[] xz = randomXZ(world, option, center, clamp);
        if (!world.getWorldBorder().isInside(new Location(world, xz[0] + 0.5, 64, xz[1] + 0.5))) {
            attempt(player, world, option, center, clamp, left - 1);
            return;
        }

        world.getChunkAtAsync(xz[0] >> 4, xz[1] >> 4).whenComplete((chunk, error) ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (error != null || chunk == null) {
                        attempt(player, world, option, center, clamp, left - 1);
                        return;
                    }
                    Location target = LocationFinder.findSafe(world, xz[0], xz[1]);
                    if (target == null || !world.getWorldBorder().isInside(target)) {
                        attempt(player, world, option, center, clamp, left - 1);
                        return;
                    }
                    player.teleportAsync(target, TeleportCause.PLUGIN).whenComplete((ok, tpError) ->
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                searching.remove(id);
                                if (tpError != null || !Boolean.TRUE.equals(ok)) {
                                    player.sendMessage(plugin.messages().get("failed"));
                                    return;
                                }
                                int cd = cooldownFor(player);
                                if (cd > 0) {
                                    cooldowns.put(id, System.currentTimeMillis() + cd * 1000L);
                                }
                                player.setFallDistance(0f);
                                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                                player.sendMessage(plugin.messages().get("success",
                                        Messages.num("x", target.getBlockX()),
                                        Messages.num("y", target.getBlockY()),
                                        Messages.num("z", target.getBlockZ())));
                            }));
                }));
    }

    private int[] randomXZ(World world, RtpOption option, Location center, boolean clamp) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        double maxR = option.max();
        double minR = option.min();
        if (clamp) {
            maxR = Math.min(maxR, world.getWorldBorder().getSize() / 2.0 - 16);
            minR = Math.min(minR, maxR / 2.0);
        }
        maxR = Math.max(maxR, 1);
        minR = Math.max(0, Math.min(minR, maxR - 1));

        // равномерное распределение по площади кольца
        double dist = Math.sqrt(rnd.nextDouble() * (maxR * maxR - minR * minR) + minR * minR);
        double angle = rnd.nextDouble() * Math.PI * 2;

        int x = (int) Math.floor(center.getX() + Math.cos(angle) * dist);
        int z = (int) Math.floor(center.getZ() + Math.sin(angle) * dist);
        return new int[]{x, z};
    }
}
