package com.tropicworld.rtp;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.EnumSet;
import java.util.Set;

public final class LocationFinder {

    private static final Set<Material> UNSAFE = EnumSet.of(
            Material.LAVA, Material.WATER, Material.FIRE, Material.SOUL_FIRE,
            Material.MAGMA_BLOCK, Material.CACTUS, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
            Material.SWEET_BERRY_BUSH, Material.POWDER_SNOW, Material.WITHER_ROSE,
            Material.BUBBLE_COLUMN
    );

    private LocationFinder() {
    }

    private static boolean unsafe(Block block) {
        return block.isLiquid() || UNSAFE.contains(block.getType());
    }

    private static boolean standable(Block ground) {
        Block feet = ground.getRelative(0, 1, 0);
        Block head = ground.getRelative(0, 2, 0);
        return !ground.isPassable()
                && !unsafe(ground)
                && feet.isPassable() && !unsafe(feet)
                && head.isPassable() && !unsafe(head);
    }

    /** Ищет безопасную точку в колонке x/z. Чанк должен быть уже загружен. */
    public static Location findSafe(World world, int x, int z) {
        Block ground = null;

        if (world.getEnvironment() == World.Environment.NETHER) {
            int minY = Math.max(33, world.getMinHeight() + 1);
            for (int y = 120; y >= minY; y--) {
                Block b = world.getBlockAt(x, y, z);
                if (standable(b)) {
                    ground = b;
                    break;
                }
            }
        } else {
            Block top = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (standable(top) && top.getY() + 2 < world.getMaxHeight()) {
                ground = top;
            }
        }

        if (ground == null) {
            return null;
        }
        return new Location(world, x + 0.5, ground.getY() + 1, z + 0.5);
    }
}
