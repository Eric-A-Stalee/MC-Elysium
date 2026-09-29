package local.elysiumcompat.mixin;

import local.elysiumcompat.ElysiumPackCompat;
import net.minecraft.server.level.ServerLevel;
import net.multiverse.dynamicheight.network.WorldHeightNetwork;
import net.multiverse.dynamicheight.worldheight.WorldHeightSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.multiverse.dynamicheight.worldheight.WorldHeightManager", remap = false)
public abstract class ElysiumHeightMixin {
    @Inject(method = "applyDimensionSettings", at = @At("HEAD"), cancellable = true, require = 1)
    private static void keepDeclaredHeight(ServerLevel level, WorldHeightSavedData data, CallbackInfo callback) {
        if (level.dimension().location().equals(ElysiumPackCompat.DIMENSION)) {
            var settings = level.dimensionType();
            var exact = (HeightDataAccessor) (Object) data;
            exact.elysiumcompat$setMinY(settings.minY());
            exact.elysiumcompat$setMaxY(settings.minY() + settings.height());
            data.setDirty();
            WorldHeightNetwork.broadcast(level, data);
            com.mojang.logging.LogUtils.getLogger().info("[elysium-compat] Declared height retained: minY={} height={}", settings.minY(), settings.height());
            callback.cancel();
        }
    }
}
