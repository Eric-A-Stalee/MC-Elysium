package dev.elysium;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ElysiumConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue WHEAT_SHARD_CHANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("entrance");
        WHEAT_SHARD_CHANCE = builder
                .comment("Chance per mature vanilla wheat block's loot roll. 0.0001 = 0.01% (one in 10,000).",
                        "Four shards form a fragment; four fragments form a sigil.",
                        "The default averages 160,000 harvested crops per sigil. Try 0.001 for a gentler progression.",
                        "Wild Elysian grain does not roll this drop. Fortune does not multiply the chance.")
                .defineInRange("wheatShardChance", 0.0001, 0.0, 1.0);
        builder.pop();
        SPEC = builder.build();
    }

    private ElysiumConfig() {}
}
