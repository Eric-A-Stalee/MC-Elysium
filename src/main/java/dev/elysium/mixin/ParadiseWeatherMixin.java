package dev.elysium.mixin;

import dev.elysium.portal.ElysiumTravel;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Custom dimensions share the Overworld's DerivedLevelData weather flags. Setting those flags
 * is either ignored or changes the wrong world; suppress only this level's weather update instead.
 * Keep this hook separate from the dimension clock so a later story phase can opt into weather.
 */
@Mixin(ServerLevel.class)
public abstract class ParadiseWeatherMixin {
    @Inject(method = "advanceWeatherCycle", at = @At("HEAD"), cancellable = true)
    private void elysium$keepParadiseClear(CallbackInfo callback) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (!level.dimension().equals(ElysiumTravel.DIMENSION)) {
            return;
        }
        boolean changed = level.getRainLevel(1.0F) > 0 || level.getThunderLevel(1.0F) > 0;
        level.setRainLevel(0.0F);
        level.setThunderLevel(0.0F);
        if (changed) {
            var players = level.getServer().getPlayerList();
            players.broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0), level.dimension());
            players.broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 0), level.dimension());
            players.broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 0), level.dimension());
        }
        callback.cancel();
    }
}
