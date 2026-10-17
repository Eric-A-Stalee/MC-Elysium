package dev.elysium.mixin;

import dev.elysium.portal.ElysiumTravel;
import dev.elysium.story.RealmClock;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Both vanilla sky rendering and server daylight logic read the same independent realm time. */
@Mixin(Level.class)
public abstract class RealmTimeMixin implements net.minecraft.world.level.LevelAccessor {
    // Sky angle, moon phase and sky darkness call LevelAccessor.dayTime(), whose default
    // bypasses Level.getDayTime(). Route both public read paths through the realm clock.
    @Override public long dayTime(){return ((Level)(Object)this).getDayTime();}
    @Inject(method="getDayTime",at=@At("HEAD"),cancellable=true)
    private void elysium$ownClock(CallbackInfoReturnable<Long> callback) {
        var level=(Level)(Object)this;
        if(level.dimension().equals(ElysiumTravel.DIMENSION))callback.setReturnValue(RealmClock.time(level));
    }
}
