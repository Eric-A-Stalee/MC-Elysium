package local.rtferosion;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

final class ErosionPolicy {
    static final ResourceLocation OVERWORLD = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    private final Set<ResourceLocation> additionalDimensions;

    ErosionPolicy(List<? extends String> dimensions) {
        var parsed = new HashSet<ResourceLocation>();
        for (String value : dimensions) {
            if (!validId(value)) throw new IllegalArgumentException("Expected explicit dimension ID: " + value);
            parsed.add(ResourceLocation.parse(value));
        }
        additionalDimensions = Set.copyOf(parsed);
    }

    boolean allows(ResourceLocation dimension) {
        return OVERWORLD.equals(dimension) || additionalDimensions.contains(dimension);
    }

    static boolean validId(Object value) {
        if (!(value instanceof String id)) return false;
        int colon = id.indexOf(':');
        return colon > 0 && colon < id.length() - 1 && ResourceLocation.tryParse(id) != null;
    }
}
