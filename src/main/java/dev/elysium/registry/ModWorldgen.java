package dev.elysium.registry;

import dev.elysium.Elysium;
import dev.elysium.worldgen.NearWaterFilter;
import dev.elysium.worldgen.BirchCrownPlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModWorldgen {
    public static final DeferredRegister<FoliagePlacerType<?>> FOLIAGE =
            DeferredRegister.create(Registries.FOLIAGE_PLACER_TYPE, Elysium.MOD_ID);
    public static final DeferredHolder<FoliagePlacerType<?>, FoliagePlacerType<BirchCrownPlacer>> BIRCH_CROWN =
            FOLIAGE.register("birch_crown", () -> new FoliagePlacerType<>(BirchCrownPlacer.CODEC));
    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENTS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, Elysium.MOD_ID);
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<NearWaterFilter>> NEAR_WATER =
            PLACEMENTS.register("near_surface_water", () -> () -> NearWaterFilter.CODEC);
    private ModWorldgen() {}
}
