package com.nethrion.xpmachine;

import org.bukkit.plugin.java.JavaPlugin;

public final class NethrionXpMachinePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        MachineListener listener = new MachineListener(
                this,
                getConfig().getDouble("experience-per-copper", 139.5),
                getConfig().getLong("depositor-memory-seconds", 20L)
        );

        getServer().getPluginManager().registerEvents(listener, this);
        getLogger().info("Nethrion XP Machine enabled. Exact 3x3x4 machine detection is active.");
    }
}
