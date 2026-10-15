"""Original sparse-footprint landscape geometry; shared NBT serialization lives in generate_worldgen."""


class Template:
    def __init__(self, state, size, name):
        self.state, self.size, self.name = state, size, name
        self.blocks = {}

    def put(self, x, y, z, name, **properties):
        assert all(0 <= value < limit for value, limit in zip((x, y, z), self.size))
        self.blocks[x, y, z] = (self.state(name, **properties), None)

    def fill(self, x1, y1, z1, x2, y2, z2, name, **properties):
        for x in range(x1, x2 + 1):
            for y in range(y1, y2 + 1):
                for z in range(z1, z2 + 1):
                    self.put(x, y, z, name, **properties)

    def ground(self, x, z, surface="grass_block"):
        self.put(x, 0, z, "dirt")
        self.put(x, 1, z, surface, **({"snowy": "false"} if surface == "grass_block" else {}))


def spring(state):
    t = Template(state, (19, 20, 19), "elder_spring")
    # An old asymmetric birch beside a contained spring, rather than another
    # small uniform grove. No blank air cuboid or square ground platform.
    for x in range(4, 11):
        for z in range(4, 11):
            if (x - 7) ** 2 + (z - 7) ** 2 <= 12:
                t.ground(x, z)
    for x in range(9, 18):
        for z in range(8, 15):
            if ((x - 13) / 4) ** 2 + ((z - 11) / 3) ** 2 <= 1:
                t.put(x, 0, z, "smooth_sandstone")
                inside = ((x - 13) / 3) ** 2 + ((z - 11) / 2) ** 2 <= 1
                t.put(x, 1, z, "water" if inside else "smooth_sandstone", **({"level": "0"} if inside else {}))
                t.put(x, 2, z, "air")
    t.fill(7, 2, 7, 7, 8, 7, "birch_log", axis="y")
    t.fill(7, 8, 7, 8, 8, 7, "birch_log", axis="x")
    t.fill(8, 9, 7, 8, 13, 7, "birch_log", axis="y")
    t.fill(8, 13, 7, 10, 13, 7, "birch_log", axis="x")
    t.fill(10, 14, 7, 10, 16, 7, "birch_log", axis="y")
    t.fill(4, 9, 7, 7, 9, 7, "birch_log", axis="x")
    t.fill(4, 9, 7, 4, 9, 10, "birch_log", axis="z")
    t.fill(4, 10, 10, 4, 12, 10, "birch_log", axis="y")
    t.fill(8, 11, 7, 8, 11, 11, "birch_log", axis="z")
    t.fill(8, 11, 11, 10, 11, 11, "birch_log", axis="x")
    t.fill(10, 12, 11, 10, 14, 11, "birch_log", axis="y")
    for cx, cy, cz, radius in ((7, 13, 7, 4), (10, 16, 7, 4), (4, 12, 10, 3), (10, 14, 11, 4)):
        for dy in range(-2, 3):
            width = radius - abs(dy)
            for dx in range(-width, width + 1):
                for dz in range(-width, width + 1):
                    pos = (cx + dx, cy + dy, cz + dz)
                    if dx * dx + dz * dz <= width * width + 1 and pos not in t.blocks:
                        t.put(*pos, "elysium:golden_birch_leaves", distance="1", persistent="true", waterlogged="false")
    # A short invitation to pause; the spring is a landmark, not a loot room.
    for x in range(7, 15):
        for z in (15, 16):
            t.ground(x, z, "dirt_path")
    for x in (9, 10, 11):
        t.ground(x, 17)
        t.put(x, 2, 17, "birch_stairs", facing="south", half="bottom", shape="straight", waterlogged="false")
    t.ground(7, 17, "smooth_sandstone")
    t.put(7, 2, 17, "lantern", hanging="false", waterlogged="false")
    for x, z in ((3, 7), (5, 4), (6, 11), (16, 15)):
        t.ground(x, z)
        t.put(x, 2, z, "oxeye_daisy")
    return t


def lookout(state):
    t = Template(state, (13, 9, 13), "sunlit_lookout")
    # North is the open viewing face. The site placer rotates it toward a
    # measured fall in the terrain; the entry and benches remain behind it.
    for x in range(1, 12):
        for z in range(1, 12):
            if abs(x - 6) + abs(z - 6) <= 8:
                t.ground(x, z, "smooth_sandstone")
                t.fill(x, 2, z, x, 7, z, "air")
    for x in range(5, 8):
        for z in (11, 12):
            t.ground(x, z, "dirt_path")
    for x, z in ((3, 3), (9, 3), (3, 9), (9, 9)):
        t.put(x, 2, z, "chiseled_sandstone")
        t.fill(x, 3, z, x, 5, z, "smooth_sandstone")
        t.put(x, 6, z, "chiseled_sandstone")
        t.put(x, 7, z, "smooth_sandstone_slab", type="bottom", waterlogged="false")
    # An open pergola: pale cornices and a few birch slats let golden light in.
    t.fill(2, 7, 2, 10, 7, 2, "smooth_sandstone_slab", type="bottom", waterlogged="false")
    t.fill(2, 7, 10, 10, 7, 10, "smooth_sandstone_slab", type="bottom", waterlogged="false")
    t.fill(2, 7, 3, 2, 7, 9, "smooth_sandstone_slab", type="bottom", waterlogged="false")
    t.fill(10, 7, 3, 10, 7, 9, "smooth_sandstone_slab", type="bottom", waterlogged="false")
    for x in (4, 6, 8):
        t.fill(x, 7, 3, x, 7, 9, "birch_slab", type="bottom", waterlogged="false")
    for x in (4, 5, 7, 8):
        t.put(x, 2, 9, "birch_stairs", facing="south", half="bottom", shape="straight", waterlogged="false")
    for x in (2, 10):
        t.put(x, 2, 5, "smooth_sandstone_slab", type="bottom", waterlogged="false")
        t.put(x, 2, 6, "smooth_sandstone_slab", type="bottom", waterlogged="false")
    for x in (3, 9):
        t.put(x, 6, 6, "lantern", hanging="true", waterlogged="false")
        t.put(x, 7, 6, "birch_slab", type="bottom", waterlogged="false")
    return t


def forge_waystation(state):
    """An abandoned survey workshop: a surviving archive beneath a partial copper roof."""
    t = Template(state, (17, 11, 17), "forge_waystation")
    for x in range(3, 14):
        for z in range(3, 13):
            t.ground(x, z, "stone_bricks" if (x + z) % 5 else "cracked_stone_bricks")
            t.fill(x, 2, z, x, 8, z, "air")
    for x, z in ((3, 3), (13, 3), (3, 12), (13, 12)):
        t.fill(x, 2, z, x, 6 if z == 3 else 4, z, "polished_basalt", axis="y")
    for z in range(4, 12):
        for x in (3, 13):
            t.fill(x, 2, z, x, 3 if z > 7 else 5, z, "stone_bricks")
    t.fill(4, 2, 3, 12, 4, 3, "stone_bricks")
    t.fill(4, 5, 3, 12, 5, 3, "waxed_exposed_cut_copper")
    # The rear half retains its roof; the front opens to the sky and golden plants.
    for x in range(2, 15):
        for z in range(2, 8):
            t.put(x, 6 + max(0, 3 - abs(x - 8) // 2), z, "waxed_exposed_cut_copper_slab", type="bottom", waterlogged="false")
    t.put(8, 2, 4, "elysium:forge_archive")
    t.put(5, 2, 4, "smithing_table")
    t.put(11, 2, 4, "chipped_anvil", facing="north")
    t.put(4, 2, 7, "blast_furnace", facing="east", lit="false")
    t.put(12, 2, 7, "crafting_table")
    t.put(6, 2, 4, "waxed_cut_copper")
    t.put(6, 3, 4, "lantern", hanging="false", waterlogged="false")
    for x in range(7, 10):
        for z in range(12, 17):
            t.ground(x, z, "dirt_path")
            t.fill(x, 2, z, x, 4, z, "air")
    for x, z in ((4, 10), (11, 11), (5, 13), (12, 14)):
        t.ground(x, z)
        t.put(x, 2, z, "elysium:leaf_litter")
    return t
