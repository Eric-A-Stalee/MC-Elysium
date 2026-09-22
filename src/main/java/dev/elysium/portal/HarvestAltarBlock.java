package dev.elysium.portal;

import dev.elysium.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A persistent, two-bit ritual altar; no block entity, inventory, or ticking is needed. */
public final class HarvestAltarBlock extends Block {
    public static final BooleanProperty HAS_SIGIL = BooleanProperty.create("has_sigil");
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 3, 16),
            Block.box(3, 3, 3, 13, 11, 13),
            Block.box(0, 11, 0, 16, 14, 16));

    public HarvestAltarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HAS_SIGIL, false).setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_SIGIL, ACTIVE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Someone who packs up the arrival altar can always put it down again and get home.
        // This does not provide a way into Elysium, and the empty stone never contains a sigil.
        return defaultBlockState().setValue(ACTIVE, context.getLevel().dimension().equals(ElysiumTravel.DIMENSION));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(ACTIVE)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        boolean sigil = stack.is(ModItems.ELYSIUM_SIGIL.get());
        boolean offering = stack.is(Items.WHEAT);
        if (!sigil && !offering) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!level.dimension().equals(Level.OVERWORLD)) {
            tell(player, "message.elysium.altar.overworld_only");
            return ItemInteractionResult.CONSUME;
        }
        if (sigil) {
            if (state.getValue(HAS_SIGIL)) {
                tell(player, "message.elysium.altar.sigil_present");
            } else {
                level.setBlock(pos, state.setValue(HAS_SIGIL, true), Block.UPDATE_ALL);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.7F);
                tell(player, "message.elysium.altar.sigil_inserted");
            }
            return ItemInteractionResult.CONSUME;
        }
        if (!state.getValue(HAS_SIGIL)) {
            tell(player, "message.elysium.altar.needs_sigil");
            return ItemInteractionResult.CONSUME;
        }
        AltarPattern.Problem problem = AltarPattern.validateActivation(level, pos);
        if (problem != AltarPattern.Problem.NONE) {
            tell(player, problem.translationKey());
            return ItemInteractionResult.CONSUME;
        }
        // Validate the destination before accepting an offering on a broken datapack installation.
        if (level.getServer().getLevel(ElysiumTravel.DIMENSION) == null) {
            tell(player, "message.elysium.travel.unavailable");
            return ItemInteractionResult.CONSUME;
        }
        level.setBlock(pos, state.setValue(ACTIVE, true), Block.UPDATE_ALL);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5F, 1.2F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2,
                    pos.getZ() + 0.5, 60, 2.0, 1.0, 2.0, 0.025);
        }
        tell(player, "message.elysium.altar.awakened");
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!state.getValue(ACTIVE)) {
            tell(player, state.getValue(HAS_SIGIL)
                    ? "message.elysium.altar.offer_wheat" : "message.elysium.altar.needs_sigil");
            return InteractionResult.CONSUME;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (level.dimension().equals(ElysiumTravel.DIMENSION)) {
                ElysiumTravel.returnHome(serverPlayer);
            } else {
                AltarPattern.Problem problem = AltarPattern.validateStructure(level, pos);
                if (problem == AltarPattern.Problem.NONE) {
                    ElysiumTravel.enter(serverPlayer, pos);
                } else {
                    level.setBlock(pos, state.setValue(ACTIVE, false), Block.UPDATE_ALL);
                    tell(player, problem.translationKey());
                }
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(ACTIVE) && random.nextInt(3) == 0) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            level.addParticle(ParticleTypes.END_ROD,
                    pos.getX() + 0.5 + Math.cos(angle) * 1.1,
                    pos.getY() + 1.0 + random.nextDouble() * 0.7,
                    pos.getZ() + 0.5 + Math.sin(angle) * 1.1, 0.0, 0.025, 0.0);
        }
    }

    static void tell(Player player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
