package dev.elysium.block;

import dev.elysium.forge.ForgeLore;
import dev.elysium.registry.ModBlockEntities;
import dev.elysium.structure.ForgeFixtures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/** Fuelled brazier, ember channels and terminal braziers share a small structure binding. */
public final class ForgeServiceBlock extends Block implements EntityBlock {
    public static final BooleanProperty LIT=BlockStateProperties.LIT;
    public static final IntegerProperty DECAY=IntegerProperty.create("decay",0,2);
    public static final BooleanProperty[] CONNECTIONS={BooleanProperty.create("down"),BooleanProperty.create("up"),
            BooleanProperty.create("north"),BooleanProperty.create("south"),BooleanProperty.create("west"),BooleanProperty.create("east")};
    public static BooleanProperty connection(Direction d) { return CONNECTIONS[d.get3DDataValue()]; }
    private final ForgeFixtures.Kind kind;
    public ForgeServiceBlock(Properties properties,ForgeFixtures.Kind kind) {
        super(properties);this.kind=kind;
        var state=stateDefinition.any().setValue(LIT,false).setValue(DECAY,0);
        for(var property:CONNECTIONS)state=state.setValue(property,false);
        registerDefaultState(state);
    }
    public boolean controller() { return kind==ForgeFixtures.Kind.BRAZIER; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(LIT,DECAY);builder.add(CONNECTIONS); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var state=defaultBlockState();
        for(var d:Direction.values())state=state.setValue(connection(d),context.getLevel().getBlockState(context.getClickedPos().relative(d)).getBlock() instanceof ForgeServiceBlock);
        return state;
    }
    @Override protected BlockState updateShape(BlockState state,Direction d,BlockState neighbour,LevelAccessor level,BlockPos pos,BlockPos other) {
        return state.setValue(connection(d),neighbour.getBlock() instanceof ForgeServiceBlock);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity entity,ItemStack stack) {
        if(!(level.getBlockEntity(pos) instanceof ForgeServiceBlockEntity placed))return;
        for(var d:Direction.values())if(level.getBlockEntity(pos.relative(d)) instanceof ForgeServiceBlockEntity neighbour&&neighbour.plan()!=null) {
            var fixture=ForgeFixtures.at(neighbour.plan(),pos);
            if(fixture!=null&&fixture.kind()==kind) {placed.bind(neighbour.plan());return;}
        }
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new ForgeServiceBlockEntity(pos,state); }
    @Override @SuppressWarnings("unchecked") public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return !level.isClientSide&&controller()&&type==ModBlockEntities.FORGE_SERVICE.get()
                ?(world,pos,s,be)->ForgeServiceBlockEntity.serverTick(world,pos,s,(ForgeServiceBlockEntity)be):null;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(player instanceof ServerPlayer server) {
            if(state.getValue(DECAY)==1) {
                level.setBlock(pos,state.setValue(DECAY,0),3);
                ForgeLore.tell(server,"service.cleared");
                server.serverLevel().sendParticles(ParticleTypes.ASH,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,12,.3,.1,.3,.01);
            } else if(state.getValue(DECAY)==2)ForgeLore.tell(server,"service.cracked");
            else if(controller()) {
                if(!(level.getBlockEntity(pos) instanceof ForgeServiceBlockEntity service)||!service.source())ForgeLore.tell(server,"service.disconnected");
                else {
                    service.toggleAirflow();
                    level.playSound(null,pos,SoundEvents.CHAIN_PLACE,SoundSource.BLOCKS,.8f,.5f);
                    ForgeLore.tell(server,service.airflow()?"service.open":"service.closed",(service.fuelTicks()+1199)/1200);
                    if(service.burning())ForgeLore.award(server,"borrowed_light");
                }
            } else return InteractionResult.PASS;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(state.getValue(DECAY)==2&&stack.is(Items.GUNPOWDER)) {
            if(player instanceof ServerPlayer server) {
                level.setBlock(pos,state.setValue(DECAY,0),3);
                if(!player.getAbilities().instabuild)stack.shrink(1);
                ForgeLore.tell(server,"service.bridged");
                level.playSound(null,pos,SoundEvents.FIRECHARGE_USE,SoundSource.BLOCKS,.4f,.7f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if(!controller())return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        boolean fuel=stack.is(Items.CHARCOAL)||stack.is(Items.COAL);
        boolean fire=stack.is(Items.FLINT_AND_STEEL)||stack.is(Items.FIRE_CHARGE);
        if(!fuel&&!fire)return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(player instanceof ServerPlayer server) {
            if(!(level.getBlockEntity(pos) instanceof ForgeServiceBlockEntity service)||!service.source())ForgeLore.tell(server,"service.disconnected");
            else if(fuel) {
                if(service.addFuel(ForgeServiceBlockEntity.FUEL_UNIT)) {
                    if(!player.getAbilities().instabuild)stack.shrink(1);
                    ForgeLore.tell(server,"service.fuel",(service.fuelTicks()+1199)/1200);
                } else ForgeLore.tell(server,"service.full");
            } else if(service.ignite()) {
                if(!player.getAbilities().instabuild) {
                    if(stack.is(Items.FIRE_CHARGE))stack.shrink(1);
                    else stack.hurtAndBreak(1,player,LivingEntity.getSlotForHand(hand));
                }
                level.playSound(null,pos,SoundEvents.FLINTANDSTEEL_USE,SoundSource.BLOCKS,.8f,.8f);
                ForgeLore.tell(server,service.airflow()?"service.ignited":"service.banked");
                if(service.airflow())ForgeLore.award(server,"borrowed_light");
            } else ForgeLore.tell(server,service.fuelTicks()==0?"service.empty":"service.banked");
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(level.getBlockEntity(pos) instanceof ForgeServiceBlockEntity service)service.reconcile(level);
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) {
        if(!state.getValue(LIT))return;
        boolean channel=kind==ForgeFixtures.Kind.CHANNEL;
        if(random.nextInt(channel?5:2)==0)level.addParticle(ParticleTypes.SMALL_FLAME,pos.getX()+.3+random.nextDouble()*.4,pos.getY()+1.03,pos.getZ()+.3+random.nextDouble()*.4,0,.01,0);
        if(!channel&&random.nextInt(18)==0)level.addParticle(ParticleTypes.SMOKE,pos.getX()+.5,pos.getY()+1.1,pos.getZ()+.5,0,.02,0);
        if(random.nextInt(channel?500:90)==0)level.playLocalSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,SoundEvents.FIRE_AMBIENT,SoundSource.BLOCKS,.2f,.7f,false);
    }
}
