"""Original, furnished mountain-town modules with connected two-storey plans.

Profiles select materials and wings; common geometry owns doors, stairs, room
partitions and roofs. Each south-facing connector meets the town at local Y=1.
"""
from dataclasses import dataclass
from landmark_templates import Template


@dataclass(frozen=True)
class Residence:
    name: str
    radius: int = 11
    roof: str = "deepslate_tile"
    plaster: str = "calcite"
    wing: bool = True
    hall: bool = False
    workshop: bool = False


RESIDENCES = (
    Residence("mountain_manor"),
    Residence("mountain_manor_workshop", roof="stone_brick", workshop=True),
    Residence("mountain_manor_gabled", plaster="smooth_sandstone", wing=False),
    Residence("mountain_great_hall", radius=13, roof="deepslate_brick", hall=True),
)


def stair(t, x, y, z, facing, material="spruce"):
    t.put(x, y, z, material + "_stairs", facing=facing, half="bottom", shape="straight", waterlogged="false")


def door(t, x, y, z, facing="north"):
    for dy, half in ((0, "lower"), (1, "upper")):
        t.put(x, y + dy, z, "spruce_door", facing=facing, half=half,
              hinge="left", open="false", powered="false")


def lamp(t, x, y, z):
    t.put(x, y, z, "spruce_fence", north="false", south="false", east="false", west="false", waterlogged="false")
    t.put(x, y + 1, z, "lantern", hanging="false", waterlogged="false")


def bed(t, x, y, z):
    for dz, part in ((0, "foot"), (-1, "head")):
        t.put(x, y, z + dz, "yellow_bed", facing="north", part=part, occupied="false")


def residence(state, profile):
    c, r = profile.radius, profile.radius
    size = r * 2 + 1
    t = Template(state, (size, 29 if profile.hall else 25, size), profile.name)
    a, b, back, front = c - (9 if profile.hall else 7), c + (9 if profile.hall else 7), c - 7, c + 5
    boxes = [(a, back, b, front, 11)]
    if profile.wing:
        boxes.append((b - 2, c + 1, size - 3, size - 4, 7))
    footprint = set()
    for x1, z1, x2, z2, height in boxes:
        for x in range(x1 - 1, x2 + 2):
            for z in range(z1 - 1, z2 + 2):
                footprint.add((x, z))
                t.fill(x, 2, z, x, t.size[1] - 1, z, "air")
    for x1, z1, x2, z2, height in boxes:
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                t.put(x, 2, z, "spruce_planks")
                if x in (x1, x2) or z in (z1, z2):
                    t.fill(x, 3, z, x, height, z, profile.plaster)
                    t.put(x, 3, z, "stone_bricks" if (x + z) % 3 else "andesite")
    for x1, z1, x2, z2, height in boxes:
        t.fill(x1 + 1, 3, z1 + 1, x2 - 1, height, z2 - 1, "air")
    for x, z in footprint:
        t.put(x, 0, z, "stone_bricks")
        t.put(x, 1, z, "stone_bricks")
    # Continuous first-floor deck and substantial horizontal framing.
    t.fill(a + 1, 7, back + 1, b - 1, 7, front - 1, "spruce_planks")
    for y in (7, 11):
        for z in (back, front):
            t.fill(a, y, z, b, y, z, "stripped_spruce_log", axis="x")
        for x in (a, b):
            t.fill(x, y, back, x, y, front, "stripped_spruce_log", axis="z")
    for x in (a, c - 2, c + 2, b):
        for z in (back, front):
            t.fill(x, 3, z, x, 11, z, "stripped_spruce_log", axis="y")
    for z in (back, c - 2, c + 2, front):
        for x in (a, b):
            t.fill(x, 3, z, x, 11, z, "stripped_spruce_log", axis="y")
    for y in (4, 9):
        for z in (back, front):
            for x in (a + 2, a + 3, b - 3, b - 2):
                t.fill(x, y, z, x, y + 1, z, "yellow_stained_glass_pane",
                       north="false", south="false", east="true", west="true", waterlogged="false")
                stair(t, x, y - 1, z + (-1 if z == back else 1), "south" if z == back else "north", "stone_brick")
        for x in (a, b):
            for z in (back + 2, back + 3, front - 3, front - 2):
                # The lower side wing receives its own exterior windows.
                if x == b and profile.wing and z >= c + 1 and y == 4:
                    continue
                t.fill(x, y, z, x, y + 1, z, "yellow_stained_glass_pane",
                       north="true", south="true", east="false", west="false", waterlogged="false")
    # Three connected rooms per storey beside the entrance/stair hall.
    left_wall, right_wall = c - (4 if profile.hall else 2), c + (4 if profile.hall else 2)
    for y in (3, 8):
        t.fill(left_wall, y, back + 1, left_wall, y + 2, front - 1, profile.plaster)
        t.fill(a + 1, y, c - 1, left_wall - 1, y + 2, c - 1, profile.plaster)
        door(t, left_wall, y, back + 3, "east")
        door(t, left_wall, y, front - 2, "east")
        t.fill(right_wall, y, back + 1, right_wall, y + 2, c - 1, profile.plaster)
        door(t, right_wall, y, back + 3, "west")
        lamp(t, c - 1, y, back + 1)
        lamp(t, a + 1, y, c - 3)
        lamp(t, b - 1, y, back + 1)
    # Two-block stairs: five rises, a full landing, and an actual opening through
    # the upper floor. Clear all three blocks of headroom above every tread.
    for step in range(5):
        z, y = front - 2 - step, 3 + step
        for x in (b - 2, b - 1):
            t.fill(x, y + 1, z, x, y + 3, z, "air")
            stair(t, x, y, z, "north")
    door(t, c, 3, front)
    t.fill(c - 1, 2, front + 1, c + 1, 2, front + 1, "spruce_planks")
    for x in range(c - 1, c + 2):
        stair(t, x, 2, front + 2, "north", "stone_brick")
        for z in range(front + 3, size):
            t.put(x, 0, z, "stone_bricks")
            t.put(x, 1, z, "stone_bricks")
            t.fill(x, 2, z, x, 6, z, "air")
    # A supported entrance canopy with a different ridge height.
    for x in (c - 2, c + 2):
        t.fill(x, 2, front + 2, x, 5, front + 2, "stripped_spruce_log", axis="y")
    for x in range(c - 2, c + 3):
        for z in (front + 1, front + 2, front + 3):
            stair(t, x, 6 + 2 - abs(x - c), z, "east" if x <= c else "west", profile.roof)
    # Gables and intersecting roof surfaces share a highest-surface envelope;
    # the lower wing cannot leave a second roof embedded in the main rooms.
    roofs = {}
    def gable(x1, z1, x2, z2, base, along_z):
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                v, lo, hi = (x, x1, x2) if along_z else (z, z1, z2)
                y = base + min(v - lo, hi - v)
                facing = ("east" if v < (lo + hi) / 2 else "west") if along_z else ("south" if v < (lo + hi) / 2 else "north")
                if y >= roofs.get((x, z), (-1, ""))[0]:
                    roofs[x, z] = (y, facing)
                if (along_z and z in (z1 + 1, z2 - 1)) or (not along_z and x in (x1 + 1, x2 - 1)):
                    if lo < v < hi and not (base < 12 and a <= x <= b and back <= z <= front):
                        t.fill(x, base, z, x, y - 1, z, profile.plaster)
        return roofs
    gable(a - 1, back - 1, b + 1, front + 1, 12, True)
    if profile.wing:
        gable(b - 3, c, size - 2, size - 3, 8, False)
        t.fill(b, 3, c + 2, b, 4, c + 3, "air")
        for z in (c + 3, c + 4):
            t.fill(size - 3, 4, z, size - 3, 5, z, "yellow_stained_glass")
        lamp(t, size - 4, 3, size - 5)
        t.put(size - 4, 3, c + 1, "loom" if profile.workshop else "barrel")
    for (x, z), (y, facing) in roofs.items():
        stair(t, x, y, z, facing, profile.roof)
    # The front gable's exposed framing and attic window break up the plaster.
    ridge = roofs[c, front][0]
    t.fill(c, 12, front, c, ridge - 1, front, "stripped_spruce_log", axis="y")
    for x in (c - 1, c + 1):
        t.fill(x, 13, front, x, 14, front, "yellow_stained_glass")
    t.fill(a + 1, 12, back + 1, b - 1, 12, front - 1, "spruce_planks")
    # The attic has a supported ladder from the upper corridor and a lit landing.
    for y in range(8, 14):
        t.put(c, y, back + 1, "ladder", facing="south", waterlogged="false")
    lamp(t, c + 1, 13, back + 2)
    # A proper small dormer on the west roof slope.
    dx, dz = a, c - 2
    t.fill(dx, 14, dz - 1, dx + 2, 16, dz + 1, profile.plaster)
    t.fill(dx, 14, dz, dx + 1, 15, dz, "air")
    t.put(dx, 15, dz, "yellow_stained_glass")
    for x in range(dx - 1, dx + 4):
        for z in range(dz - 2, dz + 3):
            stair(t, x, 17 + 2 - abs(z - dz), z, "south" if z <= dz else "north", profile.roof)
    # Furnish each room without putting furniture in the corridor or stairs.
    bed(t, a + 2, 8, back + 3)
    bed(t, a + 2, 8, front - 2)
    bed(t, b - 3, 8, back + 3)
    t.put(a + 1, 3, front - 1, "smoker", facing="east", lit="false")
    t.put(a + 2, 3, front - 1, "barrel", facing="up", open="false")
    t.put(a + 1, 3, back + 1, "crafting_table")
    t.put(b - 1, 3, back + 3, "cartography_table" if profile.hall else "bookshelf")
    for y in range(4, min(t.size[1] - 1, ridge + 2)):
        t.put(a + 1, y, front - 1, "stone_bricks")
    if profile.hall:
        # Double-height communal space with an upper gallery around the void.
        t.fill(c - 1, 7, c - 2, c + 1, 7, front - 2, "air")
        for x in (c - 2, c + 2):
            t.fill(x, 8, c - 2, x, 8, front - 2, "spruce_fence", north="true", south="true", east="false", west="false", waterlogged="false")
        for z in (c - 3, front - 1):
            t.fill(c - 1, 8, z, c + 1, 8, z, "spruce_fence", north="false", south="false", east="true", west="true", waterlogged="false")
        for z in range(c - 1, front - 2):
            t.put(c, 3, z, "spruce_fence", north="true", south="true", east="false", west="false", waterlogged="false")
            t.put(c, 4, z, "spruce_pressure_plate", powered="false")
    t.residents = [(c - 1, 3, front - 1), (c - 1, 8, back + 3)]
    t.room_targets = [(a + 3, y, z) for y in (3, 8) for z in (back + 3, front - 2)] + [(b - 3, y, back + 2) for y in (3, 8)]
    return t


def tower(state):
    t = Template(state, (13, 35, 13), "mountain_watchtower")
    for x in range(2, 11):
        for z in range(2, 11):
            t.put(x, 0, z, "stone_bricks")
            t.put(x, 1, z, "stone_bricks")
            t.fill(x, 2, z, x, 33, z, "air")
    for y in range(2, 28):
        for x in range(3, 10):
            for z in range(3, 10):
                if x in (3, 9) or z in (3, 9):
                    t.put(x, y, z, "stripped_spruce_log" if (x in (3, 9) and z in (3, 9)) else ("stone_bricks" if y < 7 else "calcite"))
        if y in (2, 8, 14, 20, 26):
            t.fill(4, y, 4, 8, y, 8, "spruce_planks")
    for y in (5, 11, 17, 23):
        for x, z in ((6, 3), (6, 9), (3, 6), (9, 6)):
            t.fill(x, y, z, x, y + 1, z, "yellow_stained_glass")
    for y in range(3, 28):
        t.put(4, y, 4, "ladder", facing="south", waterlogged="false")
    for y in (3, 9, 15, 21, 27):
        lamp(t, 8, y, 4)
    door(t, 6, 3, 9)
    for z in range(10, 13):
        for x in range(5, 8):
            t.put(x, 0, z, "stone_bricks")
            t.put(x, 1, z, "stone_bricks")
    for x in range(5, 8):
        stair(t, x, 2, 10, "north", "stone_brick")
    for ring in range(5):
        for x in range(2 + ring, 11 - ring):
            for z in range(2 + ring, 11 - ring):
                if x in (2 + ring, 10 - ring) or z in (2 + ring, 10 - ring):
                    facing = "east" if x == 2 + ring else "west" if x == 10 - ring else "south" if z == 2 + ring else "north"
                    stair(t, x, 28 + ring, z, facing, "deepslate_brick")
    t.put(6, 33, 6, "lantern", hanging="false", waterlogged="false")
    t.residents, t.room_targets = [], [(6, 3, 6), (6, 27, 6)]
    return t


def square(state):
    t = Template(state, (11, 9, 11), "grand_mountain_plaza")
    for x in range(11):
        for z in range(11):
            t.put(x, 0, z, "stone_bricks")
            t.put(x, 1, z, "polished_andesite" if x in (1, 9) or z in (1, 9) else "stone_bricks")
            t.fill(x, 2, z, x, 8, z, "air")
    for x in (1, 9):
        for z in (1, 9):
            t.fill(x, 2, z, x, 4, z, "stone_brick_wall", up="true", north="none", south="none", east="none", west="none", waterlogged="false")
            t.put(x, 5, z, "lantern", hanging="false", waterlogged="false")
    for x in (3, 4, 6, 7):
        stair(t, x, 2, 2, "north")
    t.put(5, 2, 3, "bell", attachment="floor", facing="south", powered="false")
    t.residents, t.room_targets = [], []
    return t
