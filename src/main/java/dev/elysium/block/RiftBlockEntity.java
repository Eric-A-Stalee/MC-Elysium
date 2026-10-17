package dev.elysium.block;

import dev.elysium.entity.*;
import dev.elysium.registry.*;
import dev.elysium.story.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Saved warning and local breach; unloading pauses it, and repair leaves the scar in the world. */
public final class RiftBlockEntity extends BlockEntity {
    private RupturePlan plan;private int kind,age,cooldown;private boolean opened;
    public RiftBlockEntity(BlockPos p,BlockState s){super(ModBlockEntities.SURFACE_RIFT.get(),p,s);}
    public void bind(RupturePlan p,int kind){if(!p.core().equals(worldPosition)||kind<0||kind>2)throw new IllegalArgumentException("Foreign rupture");plan=p;this.kind=kind;setChanged();}
    public boolean opened(){return opened;}public int age(){return age;}public int kind(){return kind;}
    public boolean sealed(){return getBlockState().getValue(RiftBlock.SEALED);}
    public void seal(){if(level instanceof ServerLevel l){l.setBlock(worldPosition,getBlockState().setValue(RiftBlock.SEALED,true),3);setChanged();}}
    public void use(Player player,ItemStack stack){
        if(sealed())return;
        if(opened&&stack.is(ModItems.BINDING_NAIL.get())){if(!player.getAbilities().instabuild)stack.shrink(1);seal();player.displayClientMessage(Component.translatable("message.elysium.rift.sealed"),false);}
        else player.displayClientMessage(Component.translatable("message.elysium.rift.nail"),true);
    }
    public void advance(){
        if(!(level instanceof ServerLevel l)||plan==null||sealed()||!plan.loaded(l))return;
        if(RealmCycle.get(l).bindingHeld(kind)){seal();return;}
        var warning=worldPosition.above(6);var player=l.getNearestPlayer(warning.getX(),warning.getY(),warning.getZ(),72,false);if(player==null)return;
        if(!l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)||!l.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)||l.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL)return;
        if(!opened){
            if(Nightfall.protectedAt(l,warning)){seal();return;}
            age++;setChanged();
            if(age%10==0){l.sendParticles(ParticleTypes.ASH,warning.getX()+.5,warning.getY(),warning.getZ()+.5,24,7,.2,6,.025);l.playSound(null,warning,SoundEvents.BASALT_BREAK,SoundSource.HOSTILE,1.6f,.35f);}
            if(age<80)return;
            plan.carve(l);opened=true;cooldown=1200;setChanged();
            l.playSound(null,warning,SoundEvents.GENERIC_EXPLODE.value(),SoundSource.HOSTILE,3,.5f);l.sendParticles(ParticleTypes.LARGE_SMOKE,warning.getX()+.5,warning.getY(),warning.getZ()+.5,80,7,2,7,.08);
            for(var p:l.getEntitiesOfClass(Player.class,new AABB(warning).inflate(8,5,8)))if(!p.isCreative()&&!p.isSpectator()&&!Nightfall.protectedAt(l,p.blockPosition())){
                p.hurt(l.damageSources().inWall(),6);var away=p.position().subtract(Vec3.atCenterOf(warning)).multiply(1,0,1).normalize();p.push(away.x*.8,.5,away.z*.8);
            }
            spawn(l,true);return;
        }
        if(l.getGameTime()%20==0)l.sendParticles(ParticleTypes.SMALL_FLAME,worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5,4,.4,.1,.4,.01);
        if(cooldown>0){cooldown--;if(cooldown%100==0)setChanged();return;}
        spawn(l,false);cooldown=1200;setChanged();
    }
    private void spawn(ServerLevel l,boolean first){
        if(Nightfall.protectedAt(l,worldPosition.above(6)))return;
        if(l.getEntitiesOfClass(AshenHoundEntity.class,new AABB(worldPosition).inflate(48)).size()>=4)return;
        if(kind==2&&first&&l.getEntitiesOfClass(ChainWardenEntity.class,new AABB(worldPosition).inflate(64)).isEmpty()){
            var w=ModEntities.CHAIN_WARDEN.get().create(l);if(w!=null)emerge(l,w,worldPosition);
        }
        for(int i=0;i<(first?2:1);i++){
            var pos=worldPosition.offset(i==0?-2:2,1,0);var h=ModEntities.ASHEN_HOUND.get().create(l);if(h==null)continue;
            emerge(l,h,pos.below());
        }
    }
    /** The complete body must fit above all neighbouring floor columns, not only its centre point. */
    private void emerge(ServerLevel l,net.minecraft.world.entity.Mob mob,BlockPos foot){
        mob.setPersistenceRequired();for(int dy=1;dy<=8;dy++){
            mob.setPos(Vec3.atBottomCenterOf(foot.above(dy)));if(l.noCollision(mob)){l.addFreshEntity(mob);return;}
        }
    }
    public static void tick(Level l,BlockPos p,BlockState s,RiftBlockEntity r){r.advance();}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider p){super.saveAdditional(t,p);if(plan!=null)t.putIntArray("Heights",plan.heights());t.putInt("Kind",kind);t.putInt("Age",age);t.putInt("Cooldown",cooldown);t.putBoolean("Opened",opened);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider p){super.loadAdditional(t,p);int[] a=t.getIntArray("Heights");plan=a.length==441?new RupturePlan(worldPosition.getX(),worldPosition.getZ(),a):null;if(plan!=null&&!plan.core().equals(worldPosition))plan=null;kind=Math.max(0,Math.min(2,t.getInt("Kind")));age=Math.max(0,Math.min(80,t.getInt("Age")));cooldown=Math.max(0,Math.min(1200,t.getInt("Cooldown")));opened=t.getBoolean("Opened");}
}
