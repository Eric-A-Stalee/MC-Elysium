package dev.elysium.portal;

import dev.elysium.Elysium;
import dev.elysium.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Optional;
import java.util.Set;

/** Server-only, player-only travel. Every search is bounded and runs in response to an interaction. */
@EventBusSubscriber(modid = Elysium.MOD_ID)
public final class ElysiumTravel {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Elysium.MOD_ID, "elysium"));
    private static final String RETURN_KEY = "elysium_return";
    private static final String LAST_TRAVEL_KEY = "elysium_last_travel";
    private static final long COOLDOWN_TICKS = 60;

    private ElysiumTravel() {}

    /** The caller checks the ritual. This method additionally enforces the source dimension. */
    public static boolean enter(ServerPlayer player, BlockPos sourceAltar) {
        if (!player.serverLevel().dimension().equals(Level.OVERWORLD)) {
            HarvestAltarBlock.tell(player, "message.elysium.altar.overworld_only");
            return false;
        }
        if (!mayTravel(player)) {
            return false;
        }
        ServerLevel destination = player.serverLevel().getServer().getLevel(DIMENSION);
        if (destination == null) {
            HarvestAltarBlock.tell(player, "message.elysium.travel.unavailable");
            return false;
        }
        Optional<Vec3> arrival = prepareArrival(destination);
        if (arrival.isEmpty()) {
            HarvestAltarBlock.tell(player, "message.elysium.travel.no_landing");
            return false;
        }
        // Keep the exact feet position when it is safe, otherwise a nearby supported standing spot.
        Vec3 origin = findSafeNear(player.serverLevel(), player.position(), 4, 3)
                .or(() -> findSafeNear(player.serverLevel(), Vec3.atBottomCenterOf(sourceAltar.above()), 6, 4))
                .orElse(player.position());
        ReturnPoint returnPoint = new ReturnPoint(player.serverLevel().dimension(), origin, player.getYRot(), player.getXRot());
        if (!teleport(player, destination, arrival.get(), 180.0F, 0.0F)) {
            return false;
        }
        player.getPersistentData().put(RETURN_KEY, returnPoint.save());
        player.connection.send(new ClientboundSetTitlesAnimationPacket(20, 80, 35));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.translatable("title.elysium.arrival.subtitle").withStyle(ChatFormatting.YELLOW)));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable("title.elysium.arrival").withStyle(ChatFormatting.GOLD)));
        return true;
    }

    public static boolean returnHome(ServerPlayer player) {
        if (!player.serverLevel().dimension().equals(DIMENSION) || !mayTravel(player)) {
            return false;
        }
        Optional<ReturnPoint> saved = ReturnPoint.load(player.getPersistentData().getCompound(RETURN_KEY));
        if (saved.isPresent()) {
            ReturnPoint point = saved.get();
            ServerLevel target = player.serverLevel().getServer().getLevel(point.dimension());
            if (target != null && !target.dimension().equals(DIMENSION)) {
                Optional<Vec3> feet = findSafeNear(target, point.feet(), 8, 6);
                if (feet.isPresent()) {
                    return teleport(player, target, feet.get(), point.yaw(), point.pitch());
                }
            }
        }
        // Removed dimensions, demolished entrances and command-based entries always have a way out.
        ServerLevel overworld = player.serverLevel().getServer().overworld();
        BlockPos respawn = player.getRespawnDimension().equals(Level.OVERWORLD) && player.getRespawnPosition() != null
                ? player.getRespawnPosition() : overworld.getSharedSpawnPos();
        Optional<Vec3> safe = findSafeNear(overworld, Vec3.atBottomCenterOf(respawn), 8, 6)
                .or(() -> findSurfaceSpot(overworld, respawn, 16))
                .or(() -> buildLanding(overworld, respawn, 1, false));
        if (safe.isEmpty()) {
            HarvestAltarBlock.tell(player, "message.elysium.travel.no_landing");
            return false;
        }
        boolean returned = teleport(player, overworld, safe.get(), 0.0F, 0.0F);
        if (returned) {
            HarvestAltarBlock.tell(player, "message.elysium.travel.fallback");
        }
        return returned;
    }

    private static boolean mayTravel(ServerPlayer player) {
        if (player.isPassenger() || player.isVehicle()) {
            HarvestAltarBlock.tell(player, "message.elysium.travel.dismount");
            return false;
        }
        long now = player.serverLevel().getServer().overworld().getGameTime();
        CompoundTag data = player.getPersistentData();
        if (data.contains(LAST_TRAVEL_KEY, Tag.TAG_LONG)) {
            long elapsed = now - data.getLong(LAST_TRAVEL_KEY);
            if (elapsed >= 0 && elapsed < COOLDOWN_TICKS) {
                HarvestAltarBlock.tell(player, "message.elysium.travel.cooldown");
                return false;
            }
        }
        return true;
    }

    private static boolean teleport(ServerPlayer player, ServerLevel destination, Vec3 feet, float yaw, float pitch) {
        // In 1.21.1 this teleportTo overload returns true even when NeoForge's inner
        // changeDimension call is canceled. Confirm the actual level before success side effects.
        if (!player.teleportTo(destination, feet.x, feet.y, feet.z, Set.of(), yaw, pitch)
                || player.serverLevel() != destination) {
            HarvestAltarBlock.tell(player, "message.elysium.travel.prevented");
            return false;
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.getPersistentData().putLong(LAST_TRAVEL_KEY, destination.getServer().overworld().getGameTime());
        destination.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.7F, 1.3F);
        return true;
    }

    private static Optional<Vec3> prepareArrival(ServerLevel destination) {
        ArrivalSavedData saved = ArrivalSavedData.get(destination);
        BlockPos existing = saved.altar();
        if (existing != null && restoreReturnAltar(destination, existing)) {
            Optional<Vec3> nearby = findSafeNear(destination, Vec3.atBottomCenterOf(existing.south()), 6, 4);
            if (nearby.isPresent()) {
                return nearby;
            }
        }
        BlockPos anchor = existing == null ? BlockPos.ZERO : existing;
        Optional<Vec3> natural = findNaturalArrival(destination, anchor);
        if (natural.isPresent()) {
            return natural;
        }
        // A destroyed/occupied arrival is repaired with a small 3x3 landing, never a repeated 5x5 rebuild.
        return buildLanding(destination, anchor, existing == null ? 2 : 1, true);
    }

    /** Prefer a little naturally flat clearing; not even its grass/flower blocks are removed. */
    private static Optional<Vec3> findNaturalArrival(ServerLevel level, BlockPos near) {
        for (int ring = 0; ring <= 4; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    BlockPos column = near.offset(dx * 4, 0, dz * 4);
                    if (!level.getWorldBorder().isWithinBounds(column)) {
                        continue;
                    }
                    BlockPos altar = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
                    BlockState ground = level.getBlockState(altar.below());
                    if (!(ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.DIRT)
                            || ground.is(Blocks.COARSE_DIRT) || ground.is(Blocks.PODZOL) || ground.is(Blocks.STONE))
                            || !level.getBlockState(altar).isAir()
                            || level.getHeight(Heightmap.Types.WORLD_SURFACE, altar.getX(), altar.getZ()) > altar.getY()) {
                        continue;
                    }
                    boolean clear = true;
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        if (!isSafeStandingPosition(level, Vec3.atBottomCenterOf(altar.relative(direction)))) {
                            clear = false;
                            break;
                        }
                    }
                    if (clear && isSafeStandingPosition(level, Vec3.atBottomCenterOf(altar))) {
                        placeReturnAltar(level, altar);
                        ArrivalSavedData.get(level).setAltar(altar);
                        return Optional.of(Vec3.atBottomCenterOf(altar.south()));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean restoreReturnAltar(ServerLevel level, BlockPos altar) {
        if (!level.getWorldBorder().isWithinBounds(altar)) {
            return false;
        }
        BlockState state = level.getBlockState(altar);
        if (state.is(ModBlocks.HARVEST_ALTAR.get())) {
            if (!state.getValue(HarvestAltarBlock.ACTIVE)) {
                level.setBlock(altar, state.setValue(HarvestAltarBlock.ACTIVE, true), Block.UPDATE_ALL);
            }
            return true;
        }
        if (state.isAir() && safeFloor(level, altar.below())) {
            placeReturnAltar(level, altar);
            return true;
        }
        return false;
    }

    /**
     * Adds an entirely new landing in empty space above the local surface. The preflight checks every
     * cell before writing any; even the emergency path never clears terrain or an existing build.
     */
    private static Optional<Vec3> buildLanding(ServerLevel level, BlockPos preferred, int radius, boolean withAltar) {
        int originX = preferred.getX();
        int originZ = preferred.getZ();
        if (!level.getWorldBorder().isWithinBounds(preferred)) {
            originX = (int) Math.floor(level.getWorldBorder().getCenterX());
            originZ = (int) Math.floor(level.getWorldBorder().getCenterZ());
        }
        for (int ring = 0; ring <= 4; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    int x = originX + dx * 8;
                    int z = originZ + dz * 8;
                    int floorY = level.getMinBuildHeight() + 4;
                    boolean inBounds = true;
                    for (int px = -radius; px <= radius; px++) {
                        for (int pz = -radius; pz <= radius; pz++) {
                            BlockPos column = new BlockPos(x + px, floorY, z + pz);
                            if (!level.getWorldBorder().isWithinBounds(column)) {
                                inBounds = false;
                                break;
                            }
                            floorY = Math.max(floorY,
                                    level.getHeight(Heightmap.Types.WORLD_SURFACE, x + px, z + pz));
                        }
                    }
                    if (!inBounds || floorY + 4 >= level.getMaxBuildHeight()) {
                        continue;
                    }
                    BlockPos floor = new BlockPos(x, floorY, z);
                    if (!emptyLandingVolume(level, floor, radius)) {
                        continue;
                    }
                    for (int px = -radius; px <= radius; px++) {
                        for (int pz = -radius; pz <= radius; pz++) {
                            BlockState stone = Math.abs(px) == radius && Math.abs(pz) == radius
                                    ? Blocks.CHISELED_SANDSTONE.defaultBlockState() : Blocks.SMOOTH_SANDSTONE.defaultBlockState();
                            level.setBlock(floor.offset(px, 0, pz), stone, Block.UPDATE_ALL);
                        }
                    }
                    BlockPos altar = floor.above();
                    if (withAltar) {
                        placeReturnAltar(level, altar);
                        ArrivalSavedData.get(level).setAltar(altar);
                    }
                    return Optional.of(Vec3.atBottomCenterOf(withAltar ? altar.south() : altar));
                }
            }
        }
        return Optional.empty();
    }

    private static boolean emptyLandingVolume(ServerLevel level, BlockPos floor, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = 0; y < 4; y++) {
                    if (!level.getBlockState(floor.offset(x, y, z)).isAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void placeReturnAltar(ServerLevel level, BlockPos pos) {
        // This naturally active return stone contains no item: breaking it must never mint a Sigil.
        level.setBlock(pos, ModBlocks.HARVEST_ALTAR.get().defaultBlockState()
                .setValue(HarvestAltarBlock.ACTIVE, true)
                .setValue(HarvestAltarBlock.HAS_SIGIL, false), Block.UPDATE_ALL);
    }

    private static Optional<Vec3> findSafeNear(ServerLevel level, Vec3 preferred, int radius, int verticalRange) {
        if (isSafeStandingPosition(level, preferred)) {
            return Optional.of(preferred);
        }
        BlockPos center = BlockPos.containing(preferred);
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    for (int step = 0; step <= verticalRange * 2; step++) {
                        int dy = step == 0 ? 0 : ((step + 1) / 2) * (step % 2 == 1 ? 1 : -1);
                        Vec3 candidate = Vec3.atBottomCenterOf(center.offset(dx, dy, dz));
                        if (isSafeStandingPosition(level, candidate)) {
                            return Optional.of(candidate);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Vec3> findSurfaceSpot(ServerLevel level, BlockPos near, int radius) {
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    BlockPos feet = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, near.offset(dx, 0, dz));
                    Vec3 point = Vec3.atBottomCenterOf(feet);
                    if (isSafeStandingPosition(level, point)) {
                        return Optional.of(point);
                    }
                }
            }
        }
        return Optional.empty();
    }

    /** Kept public for integration tests and future commands which need the same conservative policy. */
    public static boolean isSafeStandingPosition(ServerLevel level, Vec3 feet) {
        if (!Double.isFinite(feet.x) || !Double.isFinite(feet.y) || !Double.isFinite(feet.z)
                || feet.y <= level.getMinBuildHeight() || feet.y + 1.8 >= level.getMaxBuildHeight()) {
            return false;
        }
        BlockPos footBlock = BlockPos.containing(feet);
        if (!level.getWorldBorder().isWithinBounds(footBlock)
                || !safeFloor(level, BlockPos.containing(feet.x, feet.y - 0.01, feet.z))) {
            return false;
        }
        AABB body = new AABB(feet.x - 0.31, feet.y + 0.001, feet.z - 0.31,
                feet.x + 0.31, feet.y + 1.81, feet.z + 0.31);
        return level.noCollision(body) && !level.containsAnyLiquid(body)
                && !dangerous(level.getBlockState(footBlock))
                && !dangerous(level.getBlockState(footBlock.above()));
    }

    private static boolean safeFloor(ServerLevel level, BlockPos floor) {
        BlockState state = level.getBlockState(floor);
        return state.isFaceSturdy(level, floor, Direction.UP) && state.getFluidState().isEmpty() && !dangerous(state);
    }

    private static boolean dangerous(BlockState state) {
        return state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS) || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag previous = event.getOriginal().getPersistentData();
        if (previous.contains(RETURN_KEY, Tag.TAG_COMPOUND)) {
            event.getEntity().getPersistentData().put(RETURN_KEY, previous.getCompound(RETURN_KEY).copy());
        }
        if (previous.contains(LAST_TRAVEL_KEY, Tag.TAG_LONG)) {
            event.getEntity().getPersistentData().putLong(LAST_TRAVEL_KEY, previous.getLong(LAST_TRAVEL_KEY));
        }
    }

    private record ReturnPoint(ResourceKey<Level> dimension, Vec3 feet, float yaw, float pitch) {
        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dimension", dimension.location().toString());
            tag.putDouble("X", feet.x);
            tag.putDouble("Y", feet.y);
            tag.putDouble("Z", feet.z);
            tag.putFloat("Yaw", yaw);
            tag.putFloat("Pitch", pitch);
            return tag;
        }

        static Optional<ReturnPoint> load(CompoundTag tag) {
            ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
            if (dimension == null || !tag.contains("X", Tag.TAG_ANY_NUMERIC)
                    || !tag.contains("Y", Tag.TAG_ANY_NUMERIC) || !tag.contains("Z", Tag.TAG_ANY_NUMERIC)) {
                return Optional.empty();
            }
            Vec3 feet = new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
            float yaw = tag.getFloat("Yaw");
            float pitch = tag.getFloat("Pitch");
            if (!Double.isFinite(feet.x) || !Double.isFinite(feet.y) || !Double.isFinite(feet.z)
                    || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
                return Optional.empty();
            }
            return Optional.of(new ReturnPoint(ResourceKey.create(Registries.DIMENSION, dimension), feet, yaw, pitch));
        }
    }
}
