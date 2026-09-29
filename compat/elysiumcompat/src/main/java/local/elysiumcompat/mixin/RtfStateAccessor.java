package local.elysiumcompat.mixin;

import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;

@Mixin(value = RandomState.class, priority = 900)
public interface RtfStateAccessor {
    @Accessor(value = "generatorContext", remap = false)
    void elysiumcompat$setContext(GeneratorContext context);

    @Accessor(value = "preset", remap = false)
    void elysiumcompat$setPreset(Preset preset);
}
