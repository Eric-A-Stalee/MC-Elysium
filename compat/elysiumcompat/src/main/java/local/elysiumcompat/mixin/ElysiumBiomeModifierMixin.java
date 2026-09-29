package local.elysiumcompat.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "raccoonman.reterraforged.world.worldgen.biome.modifier.neoforge.AddModifier", remap = false)
public abstract class ElysiumBiomeModifierMixin {
    @Inject(method = "modify", at = @At("HEAD"), cancellable = true, require = 1)
    private void keepElysiumFeatureOrder(Holder<Biome> biome, BiomeModifier.Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder, CallbackInfo callback) {
        if (biome.unwrapKey().map(key -> key.location().getNamespace().equals("elysium")).orElse(false)) callback.cancel();
    }
}
