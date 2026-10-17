package dev.elysium.block;

import dev.elysium.forge.ForgeLore;
import dev.elysium.registry.*;
import dev.elysium.story.*;
import dev.elysium.portal.ElysiumTravel;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class WatchfireBlockEntity extends BlockEntity {
    private int watch=-1;
    private boolean restored;
    public WatchfireBlockEntity(BlockPos p,BlockState s){super(ModBlockEntities.WATCHFIRE.get(),p,s);}
    public void bind(int kind){if(kind<0||kind>2)throw new IllegalArgumentException("Unknown watch");watch=kind;setChanged();}
    public int watch(){return watch;}
    public boolean restored(){return restored;}
    public boolean repair(ServerPlayer player,ItemStack seal) {
        if(watch<0||restored||!player.serverLevel().dimension().equals(ElysiumTravel.DIMENSION)||!seal.is(ModItems.WATCHFIRE_SEAL.get()))return false;
        var cycle=RealmCycle.get(player.serverLevel());if(!cycle.unsettled())return false;
        Item offering=watch==0?Items.WHEAT:watch==1?Items.WATER_BUCKET:Items.AMETHYST_SHARD;int cost=watch==0?8:watch==1?1:4;
        int total=0;for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(offering))total+=player.getInventory().getItem(i).getCount();
        if(total<cost&&!player.getAbilities().instabuild)return false;
        if(!player.getAbilities().instabuild) {
            for(int i=0;i<player.getInventory().getContainerSize()&&cost>0;i++){var stack=player.getInventory().getItem(i);if(stack.is(offering)){int n=Math.min(cost,stack.getCount());stack.shrink(n);cost-=n;}}
            seal.shrink(1);if(watch==1)ForgeLore.give(player,new ItemStack(Items.BUCKET));
        }
        cycle.answerWatch(watch,worldPosition);restored=true;setChanged();update((ServerLevel)level);
        level.playSound(null,worldPosition,SoundEvents.BELL_RESONATE,SoundSource.BLOCKS,1,.65f+watch*.12f);
        ((ServerLevel)level).sendParticles(ParticleTypes.FLAME,worldPosition.getX()+.5,worldPosition.getY()+1.3,worldPosition.getZ()+.5,35,.4,.5,.4,.02);
        ForgeLore.award(player,"watch_"+watch);WatchLore.give(player,watch);return true;
    }
    public void use(Player player,ItemStack held) {
        if(!(player instanceof ServerPlayer sp)||watch<0||!sp.level().dimension().equals(ElysiumTravel.DIMENSION))return;
        if(held.is(Items.BOOK)){ForgeLore.give(sp,WatchLore.book(watch));if(!player.getAbilities().instabuild)held.shrink(1);return;}
        var cycle=RealmCycle.get(sp.serverLevel());
        if(restored){WatchLore.give(sp,watch);player.displayClientMessage(Component.translatable("message.elysium.watch.answered",Integer.bitCount(cycle.watches())),false);return;}
        boolean success=repair(sp,held);
        if(!success){WatchLore.give(sp,watch);player.displayClientMessage(Component.translatable(cycle.unsettled()?"message.elysium.watch.offer_"+watch:"message.elysium.watch.sleeping"),false);}
        else player.displayClientMessage(Component.translatable(cycle.watches()==7?"message.elysium.watch.complete":"message.elysium.watch.answered",Integer.bitCount(cycle.watches())),false);
    }
    public void update(ServerLevel server) {
        if(watch<0||!server.dimension().equals(ElysiumTravel.DIMENSION))return;
        var cycle=RealmCycle.get(server);int ember=restored?2:cycle.unsettled()?1:0;
        if(getBlockState().getValue(WatchfireBlock.EMBER)!=ember)server.setBlock(worldPosition,getBlockState().setValue(WatchfireBlock.EMBER,ember),3);
        if(restored)cycle.addWard(worldPosition);
        if(ember>0&&server.players().stream().anyMatch(p->p.distanceToSqr(worldPosition.getCenter())<4096))server.sendParticles(ember==2?ParticleTypes.FLAME:ParticleTypes.SMALL_FLAME,worldPosition.getX()+.5,worldPosition.getY()+1.2,worldPosition.getZ()+.5,ember==2?5:1,.2,.15,.2,.003);
    }
    public static void tick(Level l,BlockPos p,BlockState s,WatchfireBlockEntity b){if(l.getGameTime()%40==0)b.update((ServerLevel)l);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider){super.saveAdditional(tag,provider);tag.putInt("Watch",watch);tag.putBoolean("Restored",restored);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider provider){super.loadAdditional(tag,provider);watch=tag.contains("Watch")?tag.getInt("Watch"):-1;if(watch<0||watch>2)watch=-1;restored=tag.getBoolean("Restored")&&watch>=0;}
}
