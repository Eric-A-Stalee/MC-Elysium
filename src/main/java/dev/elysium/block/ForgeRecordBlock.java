package dev.elysium.block;

import dev.elysium.forge.ForgeDiscoveries;
import dev.elysium.forge.ForgeLore;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.registry.ModItems;
import net.minecraft.core.BlockPos;
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

/** Records can be copied once freely per player; a fresh book provides replacement copies. */
public final class ForgeRecordBlock extends Block {
    private final ForgeLore.Record record;
    public ForgeRecordBlock(Properties properties,ForgeLore.Record record) { super(properties);this.record=record; }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(player instanceof ServerPlayer server) read(server,false);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!stack.is(Items.BOOK)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(player instanceof ServerPlayer server && read(server,true) && !player.getAbilities().instabuild)stack.shrink(1);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    public boolean read(ServerPlayer player,boolean copy) {
        if(!player.level().dimension().equals(ElysiumTravel.DIMENSION)) {ForgeLore.tell(player,"outside");return false;}
        boolean first=ForgeDiscoveries.get(player.serverLevel()).discover(player.getUUID(),record.flag);
        if(first||copy) {
            ForgeLore.give(player,ForgeLore.book(record));
            if(record==ForgeLore.Record.SURVEY)ForgeLore.give(player,new ItemStack(ModItems.FORGE_SURVEY.get()));
            player.level().playSound(null,player.blockPosition(),SoundEvents.BOOK_PAGE_TURN,SoundSource.PLAYERS,.8f,1);
        }
        ForgeLore.tell(player,"record."+record.key());
        if(record==ForgeLore.Record.SURVEY)ForgeLore.award(player,"forge_survey");
        return !first && copy;
    }
}
