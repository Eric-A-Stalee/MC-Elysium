package dev.elysium.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.elysium.Elysium;
import dev.elysium.ElysiumConfig;
import dev.elysium.registry.ModItems;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** A single additive roll after vanilla wheat loot; conditions live in the data pack. */
public final class WheatShardModifier extends LootModifier {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Elysium.MOD_ID);
    public static final MapCodec<WheatShardModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance).apply(instance, WheatShardModifier::new));
    static { SERIALIZERS.register("wheat_shard", () -> CODEC); }

    public WheatShardModifier(LootItemCondition[] conditions) { super(conditions); }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextDouble() < ElysiumConfig.WHEAT_SHARD_CHANCE.get()) {
            generatedLoot.add(new ItemStack(ModItems.ELYSIUM_SHARD.get()));
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
