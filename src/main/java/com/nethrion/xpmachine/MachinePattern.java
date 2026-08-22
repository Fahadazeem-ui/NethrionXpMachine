package com.nethrion.xpmachine;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

final class MachinePattern {

    static record Entry(int x, int y, int z, Material material) {}

    private final List<Entry> entries;

    MachinePattern() {
        List<Entry> e = new ArrayList<>();
        Material d = Material.POLISHED_DEEPSLATE;
        Material c = Material.COPPER_BLOCK;

        // Layer 0
        e.add(new Entry(0, 0, 0, d));
        e.add(new Entry(1, 0, 0, Material.BARREL));
        e.add(new Entry(2, 0, 0, d));
        e.add(new Entry(0, 0, 1, d));
        e.add(new Entry(1, 0, 1, d));
        e.add(new Entry(2, 0, 1, d));
        e.add(new Entry(0, 0, 2, d));
        e.add(new Entry(1, 0, 2, d));
        e.add(new Entry(2, 0, 2, d));

        // Layer 1
        e.add(new Entry(0, 1, 0, d));
        e.add(new Entry(1, 1, 0, Material.HOPPER));
        e.add(new Entry(2, 1, 0, d));
        e.add(new Entry(0, 1, 1, d));
        e.add(new Entry(1, 1, 1, Material.COMPARATOR));
        e.add(new Entry(2, 1, 1, d));
        e.add(new Entry(0, 1, 2, d));
        e.add(new Entry(1, 1, 2, Material.REDSTONE_WIRE));
        e.add(new Entry(2, 1, 2, d));

        // Layer 2
        e.add(new Entry(0, 2, 0, c));
        e.add(new Entry(1, 2, 0, d));
        e.add(new Entry(2, 2, 0, c));
        e.add(new Entry(0, 2, 1, d));
        e.add(new Entry(1, 2, 1, d));
        e.add(new Entry(2, 2, 1, d));
        e.add(new Entry(0, 2, 2, d));
        e.add(new Entry(1, 2, 2, Material.REDSTONE_LAMP));
        e.add(new Entry(2, 2, 2, d));

        // Layer 3
        e.add(new Entry(0, 3, 0, d));
        e.add(new Entry(1, 3, 0, c));
        e.add(new Entry(2, 3, 0, d));
        e.add(new Entry(0, 3, 1, d));
        e.add(new Entry(1, 3, 1, d));
        e.add(new Entry(2, 3, 1, d));
        e.add(new Entry(0, 3, 2, d));
        e.add(new Entry(1, 3, 2, d));
        e.add(new Entry(2, 3, 2, d));

        this.entries = List.copyOf(e);
    }

    List<Entry> entries() {
        return entries;
    }

    static int[] rotate(int x, int z, int rotation) {
        return switch (rotation & 3) {
            case 0 -> new int[]{x, z};
            case 1 -> new int[]{z, 2 - x};
            case 2 -> new int[]{2 - x, 2 - z};
            default -> new int[]{2 - z, x};
        };
    }
}
