package local.elysiumcompat.mixin;

import net.multiverse.dynamicheight.worldheight.WorldHeightSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = WorldHeightSavedData.class, remap = false)
public interface HeightDataAccessor {
    @Accessor("minY") void elysiumcompat$setMinY(int value);
    @Accessor("maxY") void elysiumcompat$setMaxY(int value);
}
