package dev.elysium.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

/** Harmless ember-feeders. They recognize emitting blocks, not the mourner's counterfeit glow. */
public final class AshMothEntity extends PathfinderMob {
    private BlockPos light;private Vec3 destination;
    public AshMothEntity(EntityType<? extends AshMothEntity> type,Level level){super(type,level);setNoGravity(true);}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,4).add(Attributes.MOVEMENT_SPEED,.18);}
    @Override protected void registerGoals(){}
    public BlockPos light(){return light;}
    public boolean genuineLight(BlockPos p){return level().hasChunkAt(p)&&level().getBlockState(p).getLightEmission()>=5;}
    public void findLight(){
        BlockPos best=null;double distance=Double.MAX_VALUE;
        // A bounded local scan, staggered among the small ambient population.
        for(var p:BlockPos.betweenClosed(blockPosition().offset(-7,-4,-7),blockPosition().offset(7,4,7))){
            double d=p.distSqr(blockPosition());if(d>=distance||!genuineLight(p))continue;
            if(level().clip(new ClipContext(position(),Vec3.atCenterOf(p),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getBlockPos().equals(p)){
                best=p.immutable();distance=d;
            }
        }
        light=best;
    }
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel)||isNoAi())return;
        if(Math.floorMod(tickCount+getId(),60)==0)findLight();
        if(light!=null&&!genuineLight(light))light=null;
        if(light!=null){double a=(tickCount+getId()*13)*.035;destination=Vec3.atCenterOf(light).add(Math.cos(a)*1.3,1.1+Math.sin(a*.7)*.4,Math.sin(a)*1.3);}
        else if(destination==null||position().distanceToSqr(destination)<1||horizontalCollision||tickCount%90==0){
            var probe=blockPosition().offset(random.nextInt(9)-4,random.nextInt(5)-2,random.nextInt(9)-4);
            if(level().hasChunkAt(probe)&&level().getBlockState(probe).isAir()&&!level().getBlockState(probe.below()).isAir())probe=probe.above();
            if(level().hasChunkAt(probe)&&level().getBlockState(probe).isAir())destination=Vec3.atCenterOf(probe);
        }
        if(destination!=null){var d=destination.subtract(position());setDeltaMovement(getDeltaMovement().scale(.8).add(d.normalize().scale(.025)));setYRot(Mth.approachDegrees(getYRot(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),9));}
        if(tickCount>12000&&!hasCustomName()&&!isPersistenceRequired()&&level().getNearestPlayer(this,40)==null)discard();
    }
    @Override public boolean causeFallDamage(float distance,float multiplier,net.minecraft.world.damagesource.DamageSource source){return false;}
}
