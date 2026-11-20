package com.shop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class ShopPlugin extends JavaPlugin implements Listener {

    private Economy econ;

    static class ShopHolder implements InventoryHolder {
        public Inventory getInventory() { return null; }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().severe("No economy plugin found (install Vault + EssentialsX). Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        econ = rsp.getProvider();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player p)) { s.sendMessage("Players only."); return true; }
        open(p);
        return true;
    }

    private void open(Player p) {
        Inventory inv = Bukkit.createInventory(new ShopHolder(), 9, ChatColor.DARK_GREEN + "Shop");
        ConfigurationSection sec = getConfig().getConfigurationSection("items");
        int slot = 0;
        for (String key : sec.getKeys(false)) {
            Material m = Material.matchMaterial(key);
            if (m == null || slot >= 9) continue;
            int amount = sec.getInt(key + ".amount", 1);
            double price = sec.getDouble(key + ".price");
            ItemStack it = new ItemStack(m, amount);
            ItemMeta meta = it.getItemMeta();
            meta.setLore(List.of(ChatColor.GOLD + "Price: " + price,
                                 ChatColor.GRAY + "Click to buy"));
            it.setItemMeta(meta);
            inv.setItem(slot++, it);
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof ShopHolder)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getInventory()) return;
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        Player p = (Player) e.getWhoClicked();

        double price = getConfig().getDouble("items." + clicked.getType().name() + ".price", -1);
        if (price < 0) return;
        if (!econ.has(p, price)) {
            p.sendMessage(ChatColor.RED + "You don't have enough money.");
            return;
        }
        econ.withdrawPlayer(p, price);
        ItemStack give = new ItemStack(clicked.getType(), clicked.getAmount());
        Map<Integer, ItemStack> left = p.getInventory().addItem(give);
        left.values().forEach(i -> p.getWorld().dropItem(p.getLocation(), i));
        p.sendMessage(ChatColor.GREEN + "Purchased " + give.getAmount() + "x "
                + give.getType().name() + " for " + price);
    }
}
