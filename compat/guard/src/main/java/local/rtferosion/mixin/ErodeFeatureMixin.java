package local.rtferosion.mixin;

import local.rtferosion.ErosionGuard;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "raccoonman.reterraforged.world.worldgen.feature.ErodeFeature", remap = false)
public abstract class ErodeFeatureMixin {
    @Inject(method = "place(Lnet/minecraft/world/level/levelgen/feature/FeaturePlaceContext;)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void restrictDimensions(FeaturePlaceContext<?> context, CallbackInfoReturnable<Boolean> callback) {
        if (!ErosionGuard.allows(context.level().getLevel().dimension().location())) callback.setReturnValue(false);
    }
}
