package dev.elysium.block;

import dev.elysium.forge.ForgeLighting;
import dev.elysium.forge.ForgeLore;
import dev.elysium.portal.ElysiumTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Lamps and handwheel stations share the same small persisted binding. */
public final class ForgeServiceBlock extends Block implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final boolean controller;

    public ForgeServiceBlock(Properties properties, boolean controller) {
        super(properties);
        this.controller = controller;
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(FACING, Direction.NORTH));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(LIT, FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ForgeServiceBlockEntity(pos, state); }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!controller) return InteractionResult.PASS;
        if (player instanceof ServerPlayer server) {
            if (!level.dimension().equals(ElysiumTravel.DIMENSION)) { ForgeLore.tell(server, "outside"); return InteractionResult.SUCCESS; }
            if (!(level.getBlockEntity(pos) instanceof ForgeServiceBlockEntity service) || service.plan() == null) {
                ForgeLore.tell(server, "service.disconnected"); return InteractionResult.SUCCESS;
            }
            boolean restored = ForgeLighting.restore(server.serverLevel(), service.plan(), service.circuit());
            ForgeLore.tell(server, restored ? "service.restored" : "service.already",
                    Component.translatable("message.elysium.forge.service.circuit." + service.circuit()));
            if (restored) {
                level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, .8f, .5f);
                level.playSound(null, pos, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.BLOCKS, .8f, .65f);
                server.serverLevel().sendParticles(ParticleTypes.WAX_OFF, pos.getX()+.5, pos.getY()+.7, pos.getZ()+.5, 8, .25, .25, .25, 0);
                ForgeLore.award(server, "borrowed_light");
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof ForgeServiceBlockEntity service) service.reconcile(level);
    }

    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!level.dimension().equals(ElysiumTravel.DIMENSION)) return;
        if (!controller && random.nextInt(90) == 0)
            level.addParticle(ParticleTypes.WHITE_ASH, pos.getX()+random.nextDouble(), pos.getY()-.05, pos.getZ()+random.nextDouble(), 0, -.015, 0);
        if (!controller && random.nextInt(2400) == 0)
            level.playLocalSound(pos.getX()+.5, pos.getY()+.5, pos.getZ()+.5, SoundEvents.CHAIN_STEP, SoundSource.AMBIENT, .12f, .45f, false);
    }
}
