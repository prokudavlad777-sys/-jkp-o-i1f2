package com.tropicworld.rtp;

import org.bukkit.Material;

import java.util.List;

public record RtpOption(
        String id,
        int slot,
        Material material,
        String name,
        List<String> lore,
        Mode mode,
        Center center,
        int min,
        int max,
        String permission
) {
    public enum Mode { RANDOM, PLAYERS }

    public enum Center { PLAYER, WORLD }
}
