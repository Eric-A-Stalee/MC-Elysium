package dev.elysium.client;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

/** Retexture vanilla grass in Elysium, keeping pack geometry and vanilla block behaviour. */
public final class ElysiumGrassModel extends BakedModelWrapper<BakedModel> {
    static final List<String> SPRITES = List.of("grass_block_top", "grass_block_side_overlay",
            "short_grass", "tall_grass_top", "tall_grass_bottom");
    private static final Set<ResourceLocation> BLOCKS = Set.of(
            ResourceLocation.withDefaultNamespace("grass_block"),
            ResourceLocation.withDefaultNamespace("short_grass"),
            ResourceLocation.withDefaultNamespace("tall_grass"));
    private static final ModelProperty<Boolean> IN_ELYSIUM = new ModelProperty<>();
    private static final ModelData ELYSIUM_DATA = ModelData.of(IN_ELYSIUM, true);
    private final Map<ResourceLocation, TextureAtlasSprite> sprites;
    private final BooleanSupplier inElysium;
    // Reload creates new wrappers; a bounded, thread-safe cache also accommodates
    // packs whose models produce new quads on every call without retaining them forever.
    private final LoadingCache<BakedQuad, BakedQuad> quads = CacheBuilder.newBuilder()
            .maximumSize(512).weakKeys().build(CacheLoader.from(this::remap));

    ElysiumGrassModel(BakedModel original, Map<ResourceLocation, TextureAtlasSprite> sprites,
                      BooleanSupplier inElysium) {
        super(original);
        this.sprites = Map.copyOf(sprites);
        this.inElysium = inElysium;
    }

    static void register(ModelEvent.ModifyBakingResult event, BooleanSupplier inElysium) {
        Map<ResourceLocation, TextureAtlasSprite> sprites = new HashMap<>();
        for (String name : SPRITES) {
            var target = ResourceLocation.fromNamespaceAndPath("elysium", "block/golden_" + name);
            var sprite = event.getTextureGetter().apply(new Material(TextureAtlas.LOCATION_BLOCKS, target));
            // An absent source/atlas entry must not turn working pack artwork into checkerboards.
            if (sprite != null && sprite.contents().name().equals(target)) {
                sprites.put(ResourceLocation.withDefaultNamespace("block/" + name), sprite);
            }
        }
        if (sprites.isEmpty()) return;
        Map<BakedModel, BakedModel> wrappers = new IdentityHashMap<>();
        event.getModels().replaceAll((id, model) -> BLOCKS.contains(id.id())
                && !id.variant().equals(ModelResourceLocation.INVENTORY_VARIANT)
                && !id.variant().equals(ModelResourceLocation.STANDALONE_VARIANT)
                && !model.isCustomRenderer()
                ? wrappers.computeIfAbsent(model, original -> new ElysiumGrassModel(original, sprites, inElysium))
                : model);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
        var original = originalModel.getModelData(level, pos, state, data);
        boolean enabled = inElysium.getAsBoolean();
        if (original == ModelData.EMPTY) return enabled ? ELYSIUM_DATA : original;
        if (!enabled && !original.has(IN_ELYSIUM)) return original;
        return original.derive().with(IN_ELYSIUM, enabled).build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                    ModelData data, @Nullable RenderType renderType) {
        var original = originalModel.getQuads(state, side, random, data, renderType);
        if (!Boolean.TRUE.equals(data.get(IN_ELYSIUM))) return original;
        List<BakedQuad> mapped = null;
        for (int i = 0; i < original.size(); i++) {
            var quad = original.get(i);
            var replacement = quads.getUnchecked(quad);
            if (replacement != quad) {
                if (mapped == null) mapped = new ArrayList<>(original);
                mapped.set(i, replacement);
            }
        }
        return mapped == null ? original : mapped;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        var original = originalModel.getParticleIcon(data);
        return Boolean.TRUE.equals(data.get(IN_ELYSIUM))
                ? sprites.getOrDefault(original.contents().name(), original) : original;
    }

    private BakedQuad remap(BakedQuad quad) {
        var source = quad.getSprite();
        var target = sprites.get(source.contents().name());
        // Uncoloured pack faces, dirt, snow and custom sprite paths remain as authored.
        if (target == null || !quad.isTinted()) return quad;
        int[] vertices = quad.getVertices().clone();
        for (int i = 0; i < vertices.length; i += IQuadTransformer.STRIDE) {
            int uv = i + IQuadTransformer.UV0;
            vertices[uv] = Float.floatToRawIntBits(target.getU(source.getUOffset(Float.intBitsToFloat(vertices[uv]))));
            vertices[uv + 1] = Float.floatToRawIntBits(target.getV(source.getVOffset(Float.intBitsToFloat(vertices[uv + 1]))));
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), target,
                quad.isShade(), quad.hasAmbientOcclusion());
    }
}
