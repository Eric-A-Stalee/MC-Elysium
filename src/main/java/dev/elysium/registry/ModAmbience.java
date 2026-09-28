package dev.elysium.registry;

import dev.elysium.Elysium;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModAmbience {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Elysium.MOD_ID);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GOLDEN_LEAF =
            PARTICLES.register("golden_leaf", () -> new SimpleParticleType(false));
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Elysium.MOD_ID);
    public static final DeferredHolder<SoundEvent, SoundEvent> WOODLAND_BREEZE = SOUNDS.register("woodland_breeze",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Elysium.MOD_ID, "woodland_breeze")));
    private ModAmbience() {}
}
