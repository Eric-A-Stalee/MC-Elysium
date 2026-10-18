package dev.elysium.registry;

import dev.elysium.Elysium;
import dev.elysium.entity.SlagChimeraEntity;
import dev.elysium.entity.CinderBoltEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid=Elysium.MOD_ID,bus=EventBusSubscriber.Bus.MOD)
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(Registries.ENTITY_TYPE,Elysium.MOD_ID);
    public static final DeferredHolder<EntityType<?>,EntityType<dev.elysium.entity.CinderWispEntity>> CINDER_WISP=TYPES.register("cinder_wisp",
            ()->EntityType.Builder.<dev.elysium.entity.CinderWispEntity>of(dev.elysium.entity.CinderWispEntity::new,MobCategory.MONSTER).sized(.7f,.8f).fireImmune().clientTrackingRange(8).updateInterval(2).build("elysium:cinder_wisp"));
    public static final DeferredHolder<EntityType<?>,EntityType<CinderBoltEntity>> CINDER_BOLT=TYPES.register("cinder_bolt",
            ()->EntityType.Builder.<CinderBoltEntity>of(CinderBoltEntity::new,MobCategory.MISC).sized(.4f,.4f)
                    .fireImmune().clientTrackingRange(8).updateInterval(1).build("elysium:cinder_bolt"));
    public static final DeferredHolder<EntityType<?>,EntityType<SlagChimeraEntity>> SLAG_CHIMERA=TYPES.register("slag_chimera",
            ()->EntityType.Builder.<SlagChimeraEntity>of(SlagChimeraEntity::new,MobCategory.MISC).sized(11,4)
                    .fireImmune().clientTrackingRange(12).updateInterval(2).build("elysium:slag_chimera"));
    public static final DeferredHolder<EntityType<?>,EntityType<dev.elysium.entity.BronzeEagleEntity>> BRONZE_EAGLE=TYPES.register("bronze_eagle",
            ()->EntityType.Builder.<dev.elysium.entity.BronzeEagleEntity>of(dev.elysium.entity.BronzeEagleEntity::new,MobCategory.MISC).sized(6.4f,5.4f).fireImmune().clientTrackingRange(12).updateInterval(2).build("elysium:bronze_eagle"));
    public static final DeferredHolder<EntityType<?>,EntityType<dev.elysium.entity.BronzeFeatherEntity>> BRONZE_FEATHER=TYPES.register("bronze_feather",
            ()->EntityType.Builder.<dev.elysium.entity.BronzeFeatherEntity>of(dev.elysium.entity.BronzeFeatherEntity::new,MobCategory.MISC).sized(.3f,.3f).fireImmune().clientTrackingRange(8).updateInterval(1).build("elysium:bronze_feather"));
    public static final DeferredHolder<EntityType<?>,EntityType<dev.elysium.entity.AshenHoundEntity>> ASHEN_HOUND=TYPES.register("ashen_hound",()->EntityType.Builder.<dev.elysium.entity.AshenHoundEntity>of(dev.elysium.entity.AshenHoundEntity::new,MobCategory.MONSTER).sized(1.3f,1.25f).fireImmune().clientTrackingRange(10).build("elysium:ashen_hound"));
    public static final DeferredHolder<EntityType<?>,EntityType<dev.elysium.entity.ChainWardenEntity>> CHAIN_WARDEN=TYPES.register("chain_warden",()->EntityType.Builder.<dev.elysium.entity.ChainWardenEntity>of(dev.elysium.entity.ChainWardenEntity::new,MobCategory.MONSTER).sized(1.8f,4.6f).fireImmune().clientTrackingRange(12).updateInterval(2).build("elysium:chain_warden"));
    public static final DeferredHolder<EntityType<?>,EntityType<dev.elysium.entity.MireMournerEntity>> MIRE_MOURNER=TYPES.register("mire_mourner",()->EntityType.Builder.<dev.elysium.entity.MireMournerEntity>of(dev.elysium.entity.MireMournerEntity::new,MobCategory.MONSTER).sized(1.1f,3.5f).clientTrackingRange(10).build("elysium:mire_mourner"));
    public static final DeferredHolder<EntityType<?>,EntityType<dev.elysium.entity.AshMothEntity>> ASH_MOTH=TYPES.register("ash_moth",()->EntityType.Builder.<dev.elysium.entity.AshMothEntity>of(dev.elysium.entity.AshMothEntity::new,MobCategory.AMBIENT).sized(.65f,.4f).clientTrackingRange(8).updateInterval(2).build("elysium:ash_moth"));
    public static final DeferredHolder<EntityType<?>,EntityType<dev.elysium.entity.CrownlessEntity>> CROWNLESS=TYPES.register("crownless",()->EntityType.Builder.<dev.elysium.entity.CrownlessEntity>of(dev.elysium.entity.CrownlessEntity::new,MobCategory.MISC).sized(18,34).fireImmune().clientTrackingRange(16).updateInterval(2).build("elysium:crownless"));
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event){event.put(MIRE_MOURNER.get(),dev.elysium.entity.MireMournerEntity.attributes().build());event.put(ASH_MOTH.get(),dev.elysium.entity.AshMothEntity.attributes().build());event.put(CROWNLESS.get(),dev.elysium.entity.CrownlessEntity.attributes().build());event.put(CHAIN_WARDEN.get(),dev.elysium.entity.ChainWardenEntity.attributes().build());event.put(ASHEN_HOUND.get(),dev.elysium.entity.AshenHoundEntity.attributes().build());event.put(BRONZE_EAGLE.get(),dev.elysium.entity.BronzeEagleEntity.attributes().build());event.put(SLAG_CHIMERA.get(),SlagChimeraEntity.attributes().build());event.put(CINDER_WISP.get(),dev.elysium.entity.CinderWispEntity.attributes().build());}
    private ModEntities(){}
}
