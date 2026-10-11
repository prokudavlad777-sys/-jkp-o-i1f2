package com.tropicworld.rtp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RtpMenu implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, RtpOption> bySlot = new HashMap<>();

    public RtpMenu(TropicRTP plugin, Player player) {
        Messages m = plugin.messages();
        FileConfiguration cfg = plugin.getConfig();

        int rows = Math.max(1, Math.min(6, cfg.getInt("menu.rows", 3)));
        inventory = Bukkit.createInventory(this, rows * 9, m.parse(cfg.getString("menu.title", "<prefix>")));

        Material fillerMat = Material.matchMaterial(cfg.getString("menu.filler", "GRAY_STAINED_GLASS_PANE"));
        if (fillerMat != null && !fillerMat.isAir()) {
            ItemStack filler = new ItemStack(fillerMat);
            filler.editMeta(meta -> meta.displayName(Component.text(" ")));
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, filler);
            }
        }

        for (RtpOption opt : plugin.options()) {
            if (opt.slot() < 0 || opt.slot() >= inventory.getSize()) {
                continue;
            }
            boolean allowed = plugin.service().hasAccess(player, opt);
            TagResolver[] res = {Messages.num("min", opt.min()), Messages.num("max", opt.max())};

            Component name = m.parse(opt.name(), res)
                    .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);

            List<Component> lore = new ArrayList<>();
            for (String line : opt.lore()) {
                lore.add(m.parse(line, res).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            }
            lore.add(Component.empty());
            lore.add(m.get(allowed ? "menu-available" : "menu-locked")
                    .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));

            ItemStack item = new ItemStack(opt.material());
            item.editMeta(meta -> {
                meta.displayName(name);
                meta.lore(lore);
            });
            inventory.setItem(opt.slot(), item);
            bySlot.put(opt.slot(), opt);
        }
    }

    public RtpOption optionAt(int slot) {
        return bySlot.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
