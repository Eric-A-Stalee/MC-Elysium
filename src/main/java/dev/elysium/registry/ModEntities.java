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
            ()->EntityType.Builder.<dev.elysium.entity.BronzeEagleEntity>of(dev.elysium.entity.BronzeEagleEntity::new,MobCategory.MISC).sized(3.6f,2.8f).fireImmune().clientTrackingRange(12).updateInterval(2).build("elysium:bronze_eagle"));
    public static final DeferredHolder<EntityType<?>,EntityType<dev.elysium.entity.BronzeFeatherEntity>> BRONZE_FEATHER=TYPES.register("bronze_feather",
            ()->EntityType.Builder.<dev.elysium.entity.BronzeFeatherEntity>of(dev.elysium.entity.BronzeFeatherEntity::new,MobCategory.MISC).sized(.3f,.3f).fireImmune().clientTrackingRange(8).updateInterval(1).build("elysium:bronze_feather"));
    @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event){event.put(BRONZE_EAGLE.get(),dev.elysium.entity.BronzeEagleEntity.attributes().build());event.put(SLAG_CHIMERA.get(),SlagChimeraEntity.attributes().build());event.put(CINDER_WISP.get(),dev.elysium.entity.CinderWispEntity.attributes().build());}
    private ModEntities(){}
}
