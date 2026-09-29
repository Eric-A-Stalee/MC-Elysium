package local.elysiumcompat.mixin;

import local.elysiumcompat.ElysiumPackCompat;
import net.multiverse.dynamicheight.network.WorldHeightNetwork;
import net.multiverse.dynamicheight.worldheight.WorldHeightData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.multiverse.dynamicheight.client.WorldHeightClientNetworking", remap = false)
public abstract class ElysiumClientHeightMixin {
    @Redirect(method = "handleApplied", at = @At(value = "INVOKE", target = "Lnet/multiverse/dynamicheight/worldheight/WorldHeightData;applyServerRange(II)V"), require = 1)
    private static void retainExactReceivedRange(int minY, int maxY, WorldHeightNetwork.AppliedPayload payload) {
        WorldHeightData.applyServerRange(minY, maxY);
        if (payload.dimensionId().equals(ElysiumPackCompat.DIMENSION)) {
            ClientHeightDataAccessor.elysiumcompat$setMinY(minY);
            ClientHeightDataAccessor.elysiumcompat$setMaxY(maxY);
        }
    }
}
