package com.nethrion.xpmachine;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Barrel;
import org.bukkit.block.Hopper;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class MachineListener implements Listener {

    private static final long TICKS_PER_SECOND = 20L;

    private final JavaPlugin plugin;
    private final MachinePattern pattern = new MachinePattern();
    private final int xpPerCopperPair;
    private final long depositorMemoryMillis;

    private final Map<MachineKey, DepositorMemory> depositors = new HashMap<>();

    MachineListener(JavaPlugin plugin, int xpPerCopperPair, long depositorMemorySeconds) {
        this.plugin = plugin;
        this.xpPerCopperPair = Math.max(1, xpPerCopperPair);
        this.depositorMemoryMillis = Math.max(1L, depositorMemorySeconds) * 1000L;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHopperOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof Hopper hopper)) {
            return;
        }
        MachineKey machine = findMachine(hopper.getBlock());
        if (machine != null) {
            depositors.put(machine, new DepositorMemory(player.getUniqueId(), System.currentTimeMillis()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHopperClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof Hopper hopper)) {
            return;
        }

        MachineKey machine = findMachine(hopper.getBlock());
        if (machine == null) {
            return;
        }

        boolean insertingCopper = isCopper(event.getCursor())
                || (event.isShiftClick() && isCopper(event.getCurrentItem()));

        if (insertingCopper) {
            depositors.put(machine, new DepositorMemory(player.getUniqueId(), System.currentTimeMillis()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHopperDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof Hopper hopper)) {
            return;
        }

        if (!isCopper(event.getOldCursor())) {
            return;
        }

        MachineKey machine = findMachine(hopper.getBlock());
        if (machine != null) {
            depositors.put(machine, new DepositorMemory(player.getUniqueId(), System.currentTimeMillis()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCopperMoved(InventoryMoveItemEvent event) {
        if (!isCopper(event.getItem())) {
            return;
        }

        if (!(event.getDestination().getHolder() instanceof Barrel barrel)) {
            return;
        }

        MachineKey machine = findMachine(barrel.getBlock());
        if (machine == null) {
            return;
        }

        int movedAmount = Math.max(1, event.getItem().getAmount());

        // Let the hopper perform its normal vanilla transfer first.
        // Then consume exactly the transferred amount from the machine's barrel.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            processTransferredCopper(machine, barrel, movedAmount);
        });
    }

    private void processTransferredCopper(MachineKey machine, Barrel barrel, int maximumToProcess) {
        DepositorMemory memory = depositors.get(machine);
        if (memory == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - memory.timestampMillis > depositorMemoryMillis) {
            depositors.remove(machine);
            return;
        }

        Player player = plugin.getServer().getPlayer(memory.playerId);
        if (player == null || !player.isOnline() || player.isDead()) {
            return;
        }

        Inventory inventory = barrel.getInventory();
        int available = countCopper(inventory);
        int amount = Math.min(maximumToProcess, available);

        // Copper only pays out in pairs: 2 copper ingots = the XP that 1 diamond
        // used to award. Any odd leftover ingot stays in the barrel until its pair arrives.
        int usable = amount - (amount % 2);
        if (usable <= 0) {
            return;
        }

        int pairs = usable / 2;
        removeCopper(inventory, usable);
        giveExperienceOrbs(player, pairs * xpPerCopperPair, barrel.getLocation().add(0.5, 0.5, 0.5));
    }

    private void giveExperienceOrbs(Player player, int amount, Location source) {
        // Split the payout into several normal-looking XP orbs so the reward
        // visibly travels into the player instead of appearing as a level jump.
        int remaining = amount;
        int[] orbValues = {37, 37, 37, 37, 37, 37, 30, 27};
        int delay = 0;

        for (int baseValue : orbValues) {
            if (remaining <= 0) {
                break;
            }

            int value = Math.min(baseValue, remaining);
            remaining -= value;

            int tickDelay = delay;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> spawnXpOrb(player, source, value), tickDelay);
            delay += 2;
        }

        while (remaining > 0) {
            int value = Math.min(37, remaining);
            remaining -= value;

            int tickDelay = delay;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> spawnXpOrb(player, source, value), tickDelay);
            delay += 2;
        }
    }

    private void spawnXpOrb(Player player, Location source, int value) {
        if (!player.isOnline() || player.isDead()) {
            return;
        }

        Location target = player.getLocation().add(0.0, 0.4, 0.0);
        Location spawn = target.clone().add(
                (Math.random() - 0.5) * 0.8,
                0.35 + Math.random() * 0.35,
                (Math.random() - 0.5) * 0.8
        );

        Vector velocity = target.toVector().subtract(spawn.toVector());
        if (velocity.lengthSquared() > 0.0001) {
            velocity.normalize().multiply(0.20);
        }

        World world = player.getWorld();
        ExperienceOrb orb = world.spawn(spawn, ExperienceOrb.class);
        orb.setExperience(value);
        orb.setVelocity(velocity);
    }

    private int countCopper(Inventory inventory) {
        int total = 0;
        for (ItemStack item : inventory.getContents()) {
            if (isCopper(item)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private void removeCopper(Inventory inventory, int amount) {
        int remaining = amount;
        ItemStack[] contents = inventory.getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (!isCopper(item)) {
                continue;
            }

            int remove = Math.min(remaining, item.getAmount());
            int left = item.getAmount() - remove;
            if (left <= 0) {
                contents[i] = null;
            } else {
                item.setAmount(left);
                contents[i] = item;
            }
            remaining -= remove;
        }

        inventory.setContents(contents);
    }

    private boolean isCopper(ItemStack item) {
        return item != null && item.getType() == Material.COPPER_INGOT && item.getAmount() > 0;
    }

    private MachineKey findMachine(Block anchor) {
        if (anchor == null) {
            return null;
        }

        Material type = anchor.getType();
        if (type != Material.HOPPER && type != Material.BARREL) {
            return null;
        }

        int expectedX = 1;
        int expectedY = type == Material.HOPPER ? 1 : 0;
        int expectedZ = 0;

        Location base = anchor.getLocation();
        for (int rotation = 0; rotation < 4; rotation++) {
            int[] rotated = MachinePattern.rotate(expectedX, expectedZ, rotation);
            int originX = base.getBlockX() - rotated[0];
            int originY = base.getBlockY() - expectedY;
            int originZ = base.getBlockZ() - rotated[1];

            MachineKey key = new MachineKey(anchor.getWorld().getUID(), originX, originY, originZ, rotation);
            if (matches(key, anchor.getWorld())) {
                return key;
            }
        }

        return null;
    }

    private boolean matches(MachineKey key, World world) {
        if (world == null) {
            return false;
        }

        for (var entry : pattern.entries()) {
            int[] rotated = MachinePattern.rotate(entry.x(), entry.z(), key.rotation());
            Block block = world.getBlockAt(
                    key.x() + rotated[0],
                    key.y() + entry.y(),
                    key.z() + rotated[1]
            );

            if (block.getType() != entry.material()) {
                return false;
            }
        }

        return true;
    }

    private record DepositorMemory(UUID playerId, long timestampMillis) {}
}
