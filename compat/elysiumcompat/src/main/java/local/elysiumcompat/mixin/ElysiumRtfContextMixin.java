package local.elysiumcompat.mixin;

import local.elysiumcompat.ElysiumPackCompat;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChunkMap.class, priority = 900)
public abstract class ElysiumRtfContextMixin {
    @Shadow @Final private RandomState randomState;
    @Shadow @Final private ServerLevel level;

    @Inject(method = "<init>", at = @At("TAIL"), require = 1)
    private void clearForeignTerrainContext(CallbackInfo callback) {
        if (level.dimension().location().equals(ElysiumPackCompat.DIMENSION)) {
            ((local.elysiumcompat.ElysiumRandomState) (Object) randomState).elysiumcompat$protect();
            com.mojang.logging.LogUtils.getLogger().info("[elysium-compat] RTF context cleared for {}", level.dimension().location());
        }
    }
}
