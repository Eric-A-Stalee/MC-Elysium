package dev.elysium.block;

import dev.elysium.entity.BronzeEagleEntity;
import dev.elysium.forge.*;
import dev.elysium.registry.*;
import java.util.UUID;
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
import net.minecraft.world.phys.*;

/** Saved encounter ownership and personal teaching. Neither disconnects nor a dropped book revoke knowledge. */
public final class PrometheusBlockEntity extends BlockEntity {
    private BlockPos arena;private UUID eagle;private boolean defeated;private int missing;
    public PrometheusBlockEntity(BlockPos p,BlockState s){super(ModBlockEntities.PROMETHEUS.get(),p,s);}
    public void bind(BlockPos p){arena=p.immutable();setChanged();}
    public BlockPos arena(){return arena;}public UUID eagle(){return eagle;}public boolean defeated(){return defeated;}
    public boolean loaded(ServerLevel l){if(arena==null)return false;for(int x=(arena.getX()-32)>>4;x<=(arena.getX()+32)>>4;x++)for(int z=(arena.getZ()-32)>>4;z<=(arena.getZ()+32)>>4;z++)if(l.getChunkSource().getChunkNow(x,z)==null)return false;return true;}
    public boolean summon(){
        if(!(level instanceof ServerLevel l)||defeated||eagle!=null||!loaded(l))return false;
        var mob=ModEntities.BRONZE_EAGLE.get().create(l);if(mob==null)return false;mob.bind(worldPosition,arena);
        if(!l.addFreshEntity(mob))return false;eagle=mob.getUUID();missing=0;setChanged();return true;
    }
    public void defeat(UUID id){
        if(!id.equals(eagle)||defeated||!(level instanceof ServerLevel l))return;
        defeated=true;setChanged();l.playSound(null,worldPosition,SoundEvents.CHAIN_BREAK,SoundSource.BLOCKS,2,.45f);
        for(var p:l.players())if(p.distanceToSqr(Vec3.atCenterOf(worldPosition))<80*80)tell(p,"silence");
    }
    public boolean teach(ServerPlayer p){
        if(!defeated||!(level instanceof ServerLevel l))return false;
        if(ForgeDiscoveries.get(l).discover(p.getUUID(),ForgeDiscoveries.PROMETHEUS)){
            ForgeLore.give(p,ForgeLore.book(ForgeLore.Record.PROMETHEUS));ForgeLore.give(p,new ItemStack(ModItems.BRONZE_PINION.get(),6));ForgeLore.award(p,"promethean_craft");tell(p,"lesson");return true;
        }
        tell(p,"remember");return false;
    }
    public void use(Player player,ItemStack held){
        if(!(player instanceof ServerPlayer p)||arena==null)return;
        if(!defeated){summon();tell(p,"bound");return;}
        if(ForgeDiscoveries.get(p.serverLevel()).has(p.getUUID(),ForgeDiscoveries.PROMETHEUS)&&held.is(Items.BOOK)){
            if(!p.getAbilities().instabuild)held.shrink(1);ForgeLore.give(p,ForgeLore.book(ForgeLore.Record.PROMETHEUS));return;
        }
        teach(p);
    }
    private static void tell(Player p,String key){p.displayClientMessage(Component.translatable("message.elysium.prometheus."+key),false);}
    public static void tick(Level level,BlockPos pos,BlockState state,PrometheusBlockEntity prison){
        if(!(level instanceof ServerLevel l)||prison.arena==null||level.getGameTime()%20!=0)return;
        if(level.getGameTime()%60==0)l.sendParticles(ParticleTypes.SMALL_FLAME,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,2,.15,.1,.15,.002);
        if(prison.defeated)return;
        if(!prison.loaded(l)){prison.missing=0;return;}
        if(prison.eagle!=null){
            if(l.getEntity(prison.eagle)!=null){prison.missing=0;return;}
            if((prison.missing+=20)<200)return;
            var found=l.getEntitiesOfClass(BronzeEagleEntity.class,new AABB(prison.arena).inflate(36,45,36),e->pos.equals(e.control()));
            prison.eagle=found.isEmpty()?null:found.getFirst().getUUID();prison.missing=0;prison.setChanged();
        }
        if(prison.eagle==null&&l.getNearestPlayer(pos.getX(),pos.getY(),pos.getZ(),54,false)!=null)prison.summon();
    }
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider p){super.saveAdditional(t,p);if(arena!=null)t.putLong("Arena",arena.asLong());if(eagle!=null)t.putUUID("Eagle",eagle);t.putBoolean("Defeated",defeated);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider p){super.loadAdditional(t,p);arena=t.contains("Arena")?BlockPos.of(t.getLong("Arena")):null;if(arena!=null&&arena.distSqr(worldPosition)>64*64)arena=null;eagle=t.hasUUID("Eagle")?t.getUUID("Eagle"):null;defeated=t.getBoolean("Defeated");missing=0;}
}
