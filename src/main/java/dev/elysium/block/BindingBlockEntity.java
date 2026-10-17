package dev.elysium.block;

import dev.elysium.entity.ChainWardenEntity;
import dev.elysium.forge.ForgeLore;
import dev.elysium.registry.*;
import dev.elysium.story.RealmCycle;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Hunger accepts an offering; Sorrow needs patient light; Dominion still obeys its jailer. */
public final class BindingBlockEntity extends BlockEntity {
    private int quiet,attention,missing;private UUID listener,warden;private boolean defeated;
    public BindingBlockEntity(BlockPos p,BlockState s){super(ModBlockEntities.DEEP_BINDING.get(),p,s);}
    public int kind(){return getBlockState().getValue(BindingBlock.KIND);}public boolean sealed(){return getBlockState().getValue(BindingBlock.SEALED);}
    public UUID warden(){return warden;}public boolean defeated(){return defeated;}public int quiet(){return quiet;}
    public BlockPos resting(){return worldPosition.offset(8,0,-3);}
    private boolean loaded(ServerLevel l){for(int x=(worldPosition.getX()-28)>>4;x<=(worldPosition.getX()+28)>>4;x++)for(int z=(worldPosition.getZ()-28)>>4;z<=(worldPosition.getZ()+28)>>4;z++)if(l.getChunkSource().getChunkNow(x,z)==null)return false;return true;}
    public boolean summon(){
        if(!(level instanceof ServerLevel l)||kind()!=2||sealed()||defeated||warden!=null||!RealmCycle.get(l).released()||!loaded(l)||l.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL)return false;
        var mob=ModEntities.CHAIN_WARDEN.get().create(l);if(mob==null)return false;mob.bind(worldPosition,resting());
        if(!l.noCollision(mob)){com.mojang.logging.LogUtils.getLogger().warn("Binding jailer has no space at {}",resting());return false;}
        if(!l.addFreshEntity(mob))return false;warden=mob.getUUID();missing=0;setChanged();return true;
    }
    public void wardenDefeated(UUID id){if(id.equals(warden)&&!defeated){defeated=true;setChanged();}}
    public boolean offer(ItemStack stack,Player p){
        if(kind()!=0||sealed()||!(level instanceof ServerLevel l)||!RealmCycle.get(l).released()||!stack.is(Items.WHEAT)||stack.getCount()<8)return false;
        if(!p.getAbilities().instabuild)stack.shrink(8);quiet=240;setChanged();tell(p,"fed");return true;
    }
    /** Called every five ticks. Losing the light or rising breaks concentration, not an already earned opening. */
    public void attend(Player p){
        boolean hope=p.getMainHandItem().is(ModItems.HOPES_EMBER.get())||p.getOffhandItem().is(ModItems.HOPES_EMBER.get());
        if(kind()!=1||sealed()||!(level instanceof ServerLevel l)||!RealmCycle.get(l).released()||!hope||!p.isShiftKeyDown()||p.getDeltaMovement().horizontalDistanceSqr()>.0004||p.distanceToSqr(Vec3.atCenterOf(worldPosition))>16||l.getBrightness(LightLayer.BLOCK,worldPosition.above())<8){attention=0;listener=null;return;}
        if(!p.getUUID().equals(listener)){listener=p.getUUID();attention=0;}
        attention+=5;
        if(attention>=80){quiet=200;attention=0;tell(p,"heard");l.sendParticles(ParticleTypes.SMALL_FLAME,worldPosition.getX()+.5,worldPosition.getY()+1.1,worldPosition.getZ()+.5,8,.4,.1,.4,.005);setChanged();}
        else if(attention%20==0)tell(p,"listening");
    }
    public boolean seal(Player player,ItemStack nail){
        if(!(level instanceof ServerLevel l)||sealed()||!RealmCycle.get(l).released()||!nail.is(ModItems.BINDING_NAIL.get()))return false;
        if(kind()==2&&!defeated||kind()!=2&&quiet<=0){tell(player,kind()==0?"hungry":kind()==1?"sorrow":"sentence");return false;}
        if(!RealmCycle.get(l).bindDeep(kind()))return false;
        if(!player.getAbilities().instabuild)nail.shrink(1);
        l.setBlock(worldPosition,getBlockState().setValue(BindingBlock.SEALED,true),3);quiet=0;setChanged();
        l.playSound(null,worldPosition,SoundEvents.CHAIN_BREAK,SoundSource.BLOCKS,2,.5f);
        l.sendParticles(ParticleTypes.ASH,worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5,36,2,1,2,.02);
        tell(player,"sealed");if(player instanceof ServerPlayer sp)ForgeLore.award(sp,"binding_"+kind());return true;
    }
    public void use(Player p,ItemStack stack){
        if(!(level instanceof ServerLevel l))return;
        if(sealed()||RealmCycle.get(l).bindingHeld(kind())){tell(p,"held");return;}
        if(!RealmCycle.get(l).released()){tell(p,"intact");return;}
        if(offer(stack,p)||seal(p,stack))return;
        if(kind()==2&&l.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL){defeated=true;setChanged();tell(p,"peaceful");return;}
        if(kind()==2)summon();tell(p,kind()==0?"hungry":kind()==1?"sorrow":defeated?"nail":"sentence");
    }
    private static void tell(Player p,String key){p.displayClientMessage(Component.translatable("message.elysium.binding."+key),true);}
    public static void tick(Level level,BlockPos pos,BlockState state,BindingBlockEntity core){
        if(!(level instanceof ServerLevel l)||level.getGameTime()%5!=0||core.sealed())return;
        if(RealmCycle.get(l).bindingHeld(core.kind())){l.setBlock(pos,state.setValue(BindingBlock.SEALED,true),3);return;}
        if(!RealmCycle.get(l).released())return;
        if(core.quiet>0){core.quiet=Math.max(0,core.quiet-5);core.setChanged();}
        if(core.kind()==1){var p=l.getNearestPlayer(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,4,false);if(p!=null)core.attend(p);else{core.attention=0;core.listener=null;}}
        if(core.kind()!=2||core.defeated||level.getGameTime()%20!=0)return;
        if(!core.loaded(l)){core.missing=0;return;}
        if(core.warden!=null){
            if(l.getEntity(core.warden)!=null){core.missing=0;return;}
            if((core.missing+=20)<200)return;
            var found=l.getEntitiesOfClass(ChainWardenEntity.class,new AABB(pos).inflate(32),e->pos.equals(e.control()));core.warden=found.isEmpty()?null:found.getFirst().getUUID();core.missing=0;core.setChanged();
        }
        if(core.warden==null&&l.getNearestPlayer(pos.getX(),pos.getY(),pos.getZ(),26,false)!=null)core.summon();
    }
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider p){super.saveAdditional(t,p);t.putInt("Quiet",quiet);t.putBoolean("Defeated",defeated);if(warden!=null)t.putUUID("Warden",warden);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider p){super.loadAdditional(t,p);quiet=Math.max(0,Math.min(240,t.getInt("Quiet")));defeated=t.getBoolean("Defeated");warden=t.hasUUID("Warden")?t.getUUID("Warden"):null;attention=0;listener=null;missing=0;}
}
