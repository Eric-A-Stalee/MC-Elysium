package local.rtferosion;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ErosionPolicyTest {
    @Test void overworldAlwaysAllowed() {
        assertTrue(new ErosionPolicy(List.of()).allows(ResourceLocation.parse("minecraft:overworld")));
        assertTrue(new ErosionPolicy(List.of("twilightforest:twilight_forest")).allows(ResourceLocation.parse("minecraft:overworld")));
    }

    @ParameterizedTest @ValueSource(strings={"twilightforest:twilight_forest","minecraft:the_nether","minecraft:the_end","alpha_below:alpha_world","example:overworld"})
    void foreignDimensionsAreOptIn(String dimension) {
        assertFalse(new ErosionPolicy(List.of()).allows(ResourceLocation.parse(dimension)));
        assertTrue(new ErosionPolicy(List.of(dimension)).allows(ResourceLocation.parse(dimension)));
    }

    @Test void optInDoesNotEnableOtherDimensions() {
        assertFalse(new ErosionPolicy(List.of("twilightforest:twilight_forest")).allows(ResourceLocation.parse("minecraft:the_nether")));
    }

    @ParameterizedTest @ValueSource(strings={"overworld",":overworld","minecraft:","minecraft:Overworld","minecraft:the nether","minecraft:overworld:extra","*"," minecraft:overworld",""})
    void rejectsInvalidIds(String dimension) {
        assertFalse(ErosionPolicy.validId(dimension));
        assertThrows(IllegalArgumentException.class, () -> new ErosionPolicy(List.of(dimension)));
    }

    @Test void rejectsNonStrings() {
        assertFalse(ErosionPolicy.validId(null));
        assertFalse(ErosionPolicy.validId(1));
    }

    @Test void snapshotIsImmutable() {
        var input = new ArrayList<>(List.of("twilightforest:twilight_forest"));
        var policy = new ErosionPolicy(input);
        input.clear();
        input.add("minecraft:the_nether");
        assertTrue(policy.allows(ResourceLocation.parse("twilightforest:twilight_forest")));
        assertFalse(policy.allows(ResourceLocation.parse("minecraft:the_nether")));
    }
}
