package dev.elysium.block;

import dev.elysium.forge.ForgeDiscoveries;
import dev.elysium.forge.ForgeLore;
import dev.elysium.item.ResonanceItem;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Examining the cold hearth cannot advance the sun or start a world-wide phase. */
public final class DormantHearthBlock extends Block {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty FLAME=net.minecraft.world.level.block.state.properties.BooleanProperty.create("flame");
    public DormantHearthBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FLAME,false));}
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> builder){builder.add(FLAME);}
    public boolean receiveFlame(ServerPlayer player,BlockPos pos,ItemStack stack) {
        var level=player.serverLevel();var state=level.getBlockState(pos);
        if(!level.dimension().equals(ElysiumTravel.DIMENSION)||!state.is(this)||state.getValue(FLAME)||!stack.is(ModItems.FIRST_FLAME.get()))return false;
        var type=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("elysium","hephaestus_forge"));
        var start=level.structureManager().getStructureAt(pos,type);
        if(!start.isValid()||start.getPieces().stream().noneMatch(piece->piece instanceof dev.elysium.structure.HephaestusForgePiece forge&&forge.hearth().equals(pos)))return false;
        level.setBlock(pos,state.setValue(FLAME,true),3);if(!player.getAbilities().instabuild)stack.shrink(1);
        ForgeLore.award(player,"flame_returned");ForgeLore.tell(player,"flame_returned");
        level.playSound(null,pos,SoundEvents.BEACON_ACTIVATE,SoundSource.BLOCKS,.8f,.55f);return true;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(player instanceof ServerPlayer server) examine(server,pos,false);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(stack.is(ModItems.FIRST_FLAME.get())) {
            if(player instanceof ServerPlayer server&&!receiveFlame(server,pos,stack))ForgeLore.tell(server,"flame_socket");
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if(!stack.is(Items.AMETHYST_SHARD)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(player instanceof ServerPlayer server && examine(server,pos,true) && !player.getAbilities().instabuild)stack.shrink(1);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    /** Returns whether a replacement crystal was accepted. */
    public boolean examine(ServerPlayer player,BlockPos pos,boolean replace) {
        var level=player.serverLevel();
        if(!level.dimension().equals(ElysiumTravel.DIMENSION)){ForgeLore.tell(player,"outside");return false;}
        boolean first=ForgeDiscoveries.get(level).discover(player.getUUID(),ForgeDiscoveries.HEARTH);
        boolean missing=true;
        for(int i=0;i<player.getInventory().getContainerSize();i++)
            if(player.getInventory().getItem(i).is(ModItems.CHAIN_RESONATOR.get()))missing=false;
        if(first || replace&&missing) {
            var relic=new ItemStack(ModItems.CHAIN_RESONATOR.get());ResonanceItem.rememberForge(relic,pos);
            ForgeLore.give(player,relic);
            ForgeLore.tell(player,"relic");
        }
        ForgeLore.award(player,"cold_hearth");
        ForgeLore.tell(player,level.getBlockState(pos).is(this)&&level.getBlockState(pos).getValue(FLAME)?"hearth_restored":"hearth");
        if(first){
            level.playSound(null,pos,SoundEvents.BEACON_DEACTIVATE,SoundSource.BLOCKS,1,.65f);
            level.sendParticles(ParticleTypes.END_ROD,pos.getX()+.5,pos.getY()+1.1,pos.getZ()+.5,18,.6,.15,.6,.005);
        }
        return !first&&replace&&missing;
    }
}
