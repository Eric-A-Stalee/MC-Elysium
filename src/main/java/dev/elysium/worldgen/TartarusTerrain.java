package dev.elysium.worldgen;

import dev.elysium.registry.ModBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/** Seeded, continuous deep geology. Columns are pure values: generation order cannot change a seam. */
public final class TartarusTerrain {
    public static final int MIN_Y = -640, TOP = -248, WATERLINE = -530;
    public enum Region { ASH_WASTES, CHAIN_FOREST, HOLLOW_MIRE, TITAN_GRAVE }
    private final SimplexNoise relief, detail, fracture;
    private final DensityFunction humidity;
    private final long seed;

    public TartarusTerrain(RandomState state) {
        seed = state.getOrCreateRandomFactory(ResourceLocation.fromNamespaceAndPath("elysium", "tartarus_geology"))
                .at(0, 0, 0).nextLong();
        var random = RandomSource.create(seed);
        relief = new SimplexNoise(random);
        detail = new SimplexNoise(random);
        fracture = new SimplexNoise(random);
        humidity = state.router().vegetation();
    }

    public Region region(int x, int z) {
        double h = humidity(x, z);
        return h < -.30 ? Region.ASH_WASTES : h < .10 ? Region.CHAIN_FOREST
                : h < .45 ? Region.HOLLOW_MIRE : Region.TITAN_GRAVE;
    }

    private double humidity(int x, int z) {
        return humidity.compute(new DensityFunction.SinglePointContext(x, -400, z));
    }

    public int floor(int x, int z) {
        double h = humidity(x, z);
        double mire = Math.max(0, 1 - Math.abs(h - .275) / .175);
        double cut = Math.max(0, 1 - Math.abs(fracture.getValue(x / 190.0, z / 190.0)) / .075);
        return Math.max(MIN_Y + 22, -494 + (int)Math.round(42 * relief.getValue(x / 235.0, z / 235.0)
                + 14 * detail.getValue(x / 59.0, z / 59.0) - 48 * mire - 34 * cut));
    }

    public int roof(int x, int z) {
        return Math.max(floor(x,z)+144,-296 + (int)Math.round(25 * relief.getValue((x + 710) / 310.0, (z - 430) / 310.0)
                + 9 * detail.getValue(x / 83.0, z / 83.0)));
    }

    public Column column(int x, int z) {
        int floor = floor(x, z), roof = roof(x, z);
        Region region = region(x, z);
        // Shared cell centres use world coordinates, including negative chunks.
        int cellX = Math.floorDiv(x, 112), cellZ = Math.floorDiv(z, 112);
        int cell = hash(cellX, 0, cellZ);
        int cx = cellX * 112 + 25 + cell % 62, cz = cellZ * 112 + 25 + (cell >>> 7) % 62;
        int dx = x - cx, dz = z - cz;
        double distance = Math.hypot(dx, dz);
        int pillar = cell % 4 == 0 ? (int)(Math.max(0, 1 - distance / 17) * (roof - floor)) : 0;
        boolean chain = region == Region.CHAIN_FOREST && cell % 3 != 0;
        int gx = Math.floorDiv(x, 192), gz = Math.floorDiv(z, 192);
        int grave = hash(gx, 43, gz);
        int boneX = gx * 192 + 64 + grave % 64, boneZ = gz * 192 + 64 + (grave >>> 8) % 64;
        int ribX = x - boneX, ribZ = z - boneZ;
        int boneBase = floor(boneX, boneZ);
        return new Column(x, z, floor, roof, region, dx, dz, pillar, chain, ribX, ribZ, boneBase);
    }

    public final class Column {
        public final int x, z, floor, roof;
        public final Region region;
        private final int dx, dz, pillar, ribX, ribZ, boneBase;
        private final boolean chain;
        private Column(int x, int z, int floor, int roof, Region region, int dx, int dz, int pillar,
                       boolean chain, int ribX, int ribZ, int boneBase) {
            this.x=x;this.z=z;this.floor=floor;this.roof=roof;this.region=region;
            this.dx=dx;this.dz=dz;this.pillar=pillar;this.chain=chain;
            this.ribX=ribX;this.ribZ=ribZ;this.boneBase=boneBase;
        }

        public BlockState at(int y) {
            if(y <= MIN_Y + 4) return Blocks.BEDROCK.defaultBlockState();
            if(y <= floor || y >= roof || y <= floor + pillar) return rock(y);
            if(chain && Math.abs(dx) <= 5 && Math.abs(dz) <= 5) {
                int link = Math.floorDiv(y, 14), along = Math.floorMod(y, 14) - 7;
                int across = (link & 1) == 0 ? dx : dz, plane = (link & 1) == 0 ? dz : dx;
                double ring = Math.pow(across / 4.0, 4) + Math.pow(along / 7.0, 4);
                if(Math.abs(plane) <= 1 && ring >= .48 && ring <= 1.6)
                    return ModBlocks.TARTARUS_CHAIN_BLOCK.get().defaultBlockState();
            }
            if(region == Region.TITAN_GRAVE && Math.abs(ribZ) <= 54) {
                if(Math.abs(ribX) <= 3 && Math.abs(y - boneBase - 4) <= 3)
                    return Blocks.BONE_BLOCK.defaultBlockState();
                if(Math.floorMod(ribZ + 54, 12) <= 2 && Math.abs(ribX) <= 29) {
                    double arch = ribX * ribX / (29.0 * 29.0) + (y - boneBase - 6.0) * (y - boneBase - 6.0) / (43.0 * 43.0);
                    if(y >= boneBase + 4 && arch >= .84 && arch <= 1.16)
                        return hash(x, y, z) % 7 == 0 ? Blocks.BASALT.defaultBlockState() : Blocks.BONE_BLOCK.defaultBlockState();
                }
            }
            if(y <= WATERLINE) return Blocks.WATER.defaultBlockState();
            if(y == floor + 1 && region == Region.HOLLOW_MIRE && hash(x, 0, z) % 67 == 0)
                return Blocks.BROWN_MUSHROOM.defaultBlockState();
            return Blocks.CAVE_AIR.defaultBlockState();
        }

        private BlockState rock(int y) {
            int h = hash(x, y, z);
            if(y == floor) {
                if(region == Region.ASH_WASTES && h % 73 == 0) return Blocks.MAGMA_BLOCK.defaultBlockState();
                return switch(region) {
                    case ASH_WASTES -> (h % 8 == 0 ? Blocks.SOUL_SOIL : Blocks.BLACKSTONE).defaultBlockState();
                    case CHAIN_FOREST -> (h % 7 == 0 ? Blocks.BASALT : Blocks.DEEPSLATE).defaultBlockState();
                    case HOLLOW_MIRE -> (h % 6 == 0 ? Blocks.PACKED_MUD : Blocks.MUD).defaultBlockState();
                    case TITAN_GRAVE -> (h % 11 == 0 ? Blocks.BONE_BLOCK : Blocks.SOUL_SOIL).defaultBlockState();
                };
            }
            if(Math.floorMod(y + (h >>> 4) % 3, 23) < 3) return Blocks.BASALT.defaultBlockState();
            return (h % 13 == 0 ? Blocks.TUFF : region == Region.ASH_WASTES ? Blocks.BLACKSTONE : Blocks.DEEPSLATE).defaultBlockState();
        }
    }

    private int hash(int x, int y, int z) {
        long n = seed ^ x * 0x632BE59BD9B4E019L ^ z * 0x9E3779B97F4A7C15L ^ y * 0x85157AF5L;
        n=(n^(n>>>30))*0xBF58476D1CE4E5B9L;n=(n^(n>>>27))*0x94D049BB133111EBL;
        return (int)(n^(n>>>31)) & 0x7fffffff;
    }
}
