package dev.elysium.block;

import dev.elysium.forge.*;
import dev.elysium.registry.ModItems;
import dev.elysium.structure.ForgePlan;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** The sealed vessel is present before the choice. What remains inside can still be carried upward. */
public final class PandoraVesselBlock extends Block {
    public static final BooleanProperty OPEN=BooleanProperty.create("open");
    public PandoraVesselBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(OPEN,false));}
    public static BlockPos position(ForgePlan p){return ForgeHammer.world(p,7,1,104);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(OPEN);}
    private void use(BlockState s,Player player,ItemStack held){
        if(!(player instanceof ServerPlayer p))return;
        if(!s.getValue(OPEN)){tell(p,"sealed");return;}
        var discoveries=ForgeDiscoveries.get(p.serverLevel());boolean first=!discoveries.has(p.getUUID(),ForgeDiscoveries.HOPE);
        boolean carrying=false;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(ModItems.HOPES_EMBER.get()))carrying=true;
        if(!first&&(carrying||!held.is(ModItems.ELYSIUM_FRAGMENT.get()))){tell(p,"empty");return;}
        if(!first&&!p.getAbilities().instabuild)held.shrink(1);
        discoveries.discover(p.getUUID(),ForgeDiscoveries.HOPE);ForgeLore.give(p,new ItemStack(ModItems.HOPES_EMBER.get()));ForgeLore.award(p,"hope_remains");tell(p,"hope");
    }
    private static void tell(Player p,String key){p.displayClientMessage(Component.translatable("message.elysium.vessel."+key),false);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){use(s,p,ItemStack.EMPTY);return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){use(s,p,stack);return ItemInteractionResult.sidedSuccess(l.isClientSide);}
}
