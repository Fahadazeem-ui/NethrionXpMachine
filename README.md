# Nethrion XP Machine

A compact, vanilla-style 3x3x4 physical machine for Paper 1.21.11.

## Exact machine
The plugin recognizes only the exact block pattern used by `Nethrion_XP_Exchange_Machine.schem`.
It supports the same build rotated horizontally in 90-degree steps.

Front-left-bottom is the pattern origin. The machine contains:
- Polished Deepslate
- Copper Block
- Barrel
- Hopper (input)
- Comparator
- Redstone Wire
- Redstone Lamp

No mob capture or mob processing is used.

## How players use it
1. Build the exact machine.
2. Open the machine's hopper.
3. Put copper ingots into the hopper.
4. Vanilla hopper transfer moves copper into the barrel.
5. The plugin consumes the transferred copper ingot(s) immediately.
6. The player receives the payout as real Minecraft experience orbs that visibly fly into them.

Each single copper ingot grants exactly 139.5 raw XP — double the price of the old diamond (1 diamond used to give 279 XP, so 2 copper ingots now give that same 279 XP total). There is no pairing requirement: even a single copper ingot pays out immediately.

Copper placed directly into the barrel is not processed unless it arrives through a matching hopper transfer.

## No command is required.
## No GUI is used.
