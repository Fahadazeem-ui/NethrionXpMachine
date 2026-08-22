package com.nethrion.xpmachine;

import java.util.UUID;

record MachineKey(UUID worldId, int x, int y, int z, int rotation) {}
