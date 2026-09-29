package local.elysiumcompat.mixin;

import local.elysiumcompat.ElysiumPackCompat;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "raccoonman.reterraforged.world.worldgen.feature.DecorateSnowFeature", remap = false)
public abstract class ElysiumSnowMixin {
    @Inject(method = "place(Lnet/minecraft/world/level/levelgen/feature/FeaturePlaceContext;)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void keepElysiumSnowIndependent(FeaturePlaceContext<?> context, CallbackInfoReturnable<Boolean> callback) {
        if (context.level().getLevel().dimension().location().equals(ElysiumPackCompat.DIMENSION)) callback.setReturnValue(false);
    }
}
