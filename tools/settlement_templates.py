"""Original reusable house modules. All doors face south and meet a Y=1 path.

The placer owns terrain, spacing and circulation; these templates own the small
authored footprints. Styles change materials without duplicating geometry.
"""
from dataclasses import dataclass
from landmark_templates import Template


@dataclass(frozen=True)
class Style:
    beam: str
    plaster: str
    roof: str
    floor: str = "spruce_planks"


WATERSIDE = Style("birch_log", "smooth_sandstone", "spruce")
NORDIC = Style("stripped_spruce_log", "calcite", "stone_brick")


def house(state, name, style, radius=6, hall=False, variant=0):
    size, c, end = radius * 2 + 1, radius, radius * 2 - 2
    t = Template(state, (size, radius + 11, size), name)
    for x in range(1, size - 1):
        for z in range(1, size - 1):
            t.ground(x, z, "stone_bricks" if hall else "grass_block")
            t.fill(x, 2, z, x, radius + 9, z, "air")
    t.fill(2, 2, 2, end, 2, end, style.floor)
    for x in range(2, end + 1):
        for z in range(2, end + 1):
            if x in (2, end) or z in (2, end):
                t.fill(x, 3, z, x, 6, z, style.plaster)
    for x in (2, c, end):
        for z in (2, end):
            t.fill(x, 3, z, x, 6, z, style.beam, axis="y")
    for z in (2, c, end):
        for x in (2, end):
            t.fill(x, 3, z, x, 6, z, style.beam, axis="y")
    for z in (2, end):
        t.fill(2, 6, z, end, 6, z, style.beam, axis="x")
    for x in (2, end):
        t.fill(x, 6, 2, x, 6, end, style.beam, axis="z")
    # A steep gable has real stair slopes and filled end triangles.
    for x in range(1, size - 1):
        roof_y = 7 + min(x - 1, size - 2 - x)
        for z in range(1, size - 1):
            if x == c:
                t.put(x, roof_y, z, f"{style.roof}_slab", type="bottom", waterlogged="false")
            else:
                t.put(x, roof_y, z, f"{style.roof}_stairs", facing="east" if x < c else "west",
                      half="bottom", shape="straight", waterlogged="false")
        if 2 <= x <= end:
            for z in (2, end):
                t.fill(x, 7, z, x, roof_y - 1, z, style.plaster)
    # Warm windows are illuminated by interior lanterns, not emissive glass.
    for z in (2, end):
        for x in (c - 2, c + 2):
            t.fill(x, 4, z, x, 5, z, "yellow_stained_glass")
    for x in (2, end):
        for z in (c - 2, c + 2):
            t.fill(x, 4, z, x, 5, z, "yellow_stained_glass")
    t.put(c, 3, end, "spruce_door", facing="north", half="lower", hinge="left", open="false", powered="false")
    t.put(c, 4, end, "spruce_door", facing="north", half="upper", hinge="left", open="false", powered="false")
    for x in range(c - 1, c + 2):
        t.put(x, 2, end + 1, "smooth_sandstone_stairs", facing="north", half="bottom", shape="straight", waterlogged="false")
        t.ground(x, end + 2, "stone_bricks")
        t.fill(x, 2, end + 2, x, 5, end + 2, "air")
    for x in (3, end - 1):
        t.put(x, 3, 3, "spruce_fence", north="false", south="false", east="false", west="false", waterlogged="false")
        t.put(x, 4, 3, "lantern", hanging="false", waterlogged="false")
    for x in (3, end - 1):
        t.put(x, 3, end - 2, "yellow_bed", facing="north", part="foot", occupied="false")
        t.put(x, 3, end - 3, "yellow_bed", facing="north", part="head", occupied="false")
    t.put(3, 3, c, "crafting_table")
    t.put(end - 1, 3, c, "cartography_table" if hall else ("barrel" if variant else "composter"))
    if hall:
        # A long communal table leaves two clear aisles to the doorway.
        for z in range(4, end - 3):
            t.put(c, 3, z, "spruce_fence", north="true", south="true", east="false", west="false", waterlogged="false")
            t.put(c, 4, z, "spruce_pressure_plate", powered="false")
        for z in (1, size - 2):
            t.put(c, radius + 6, z, "stone_brick_wall", up="true", north="none", south="none", east="none", west="none", waterlogged="false")
            t.put(c, radius + 7, z, "lantern", hanging="false", waterlogged="false")
    else:
        for x in (2, end):
            t.put(x, 2, size - 2, "flower_pot")
            # Pots are on ground outside the porch rather than in the door route.
    return t


def plaza(state):
    t = Template(state, (9, 9, 9), "mountain_plaza")
    for x in range(9):
        for z in range(9):
            t.ground(x, z, "smooth_sandstone" if 2 <= x <= 6 and 2 <= z <= 6 else "stone_bricks")
            t.fill(x, 2, z, x, 7, z, "air")
    for x, z in ((2, 2), (6, 2), (2, 6), (6, 6)):
        t.fill(x, 2, z, x, 5, z, "stripped_spruce_log", axis="y")
    for x in range(1, 8):
        for z in range(1, 8):
            if x in (1, 7) or z in (1, 7):
                t.put(x, 6, z, "stone_brick_slab", type="bottom", waterlogged="false")
    t.fill(2, 6, 2, 6, 6, 2, "spruce_slab", type="bottom", waterlogged="false")
    t.fill(2, 6, 6, 6, 6, 6, "spruce_slab", type="bottom", waterlogged="false")
    t.put(4, 2, 4, "bell", attachment="floor", facing="south", powered="false")
    for x in (2, 6):
        t.put(x, 2, 4, "lantern", hanging="false", waterlogged="false")
    return t
