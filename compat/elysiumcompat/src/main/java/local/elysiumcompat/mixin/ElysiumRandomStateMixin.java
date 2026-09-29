package local.elysiumcompat.mixin;

import local.elysiumcompat.ElysiumRandomState;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RandomState.class, priority = 800)
public abstract class ElysiumRandomStateMixin implements ElysiumRandomState {
    @Unique private boolean elysiumcompat$protected;

    public void elysiumcompat$protect() {
        elysiumcompat$protected = true;
        var state = (RtfStateAccessor) (Object) this;
        state.elysiumcompat$setContext(null);
        state.elysiumcompat$setPreset(null);
    }

    @Inject(method = "initialize", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void preventForeignReinitialization(CallbackInfo callback) {
        if (elysiumcompat$protected) callback.cancel();
    }
}
