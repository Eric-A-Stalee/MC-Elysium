package local.elysiumcompat.mixin;

import net.multiverse.dynamicheight.worldheight.WorldHeightData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = WorldHeightData.class, remap = false)
public interface ClientHeightDataAccessor {
    @Accessor("minY") static void elysiumcompat$setMinY(int value) { throw new AssertionError(); }
    @Accessor("maxY") static void elysiumcompat$setMaxY(int value) { throw new AssertionError(); }
}
