package dev.elysium.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.neoforged.neoforge.entity.PartEntity;

public final class ChimeraEyePart extends PartEntity<SlagChimeraEntity> {
    public ChimeraEyePart(SlagChimeraEntity parent){super(parent);refreshDimensions();}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public EntityDimensions getDimensions(Pose pose){return EntityDimensions.scalable(3,3);}
    @Override public boolean isPickable(){return true;}
    @Override public boolean hurt(DamageSource source,float amount){return !isInvulnerableTo(source)&&getParent().hurtEye(source,amount);}
    @Override public boolean is(Entity other){return this==other||getParent()==other;}
}
