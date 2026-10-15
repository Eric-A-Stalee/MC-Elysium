package dev.elysium.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/** Exercise the actual baking hook and NeoForge quad/model-data path without GL. */
final class GrassModelProbe {
    static void run() {
        List<SpriteContents> allocated = new ArrayList<>();
        try {
            Map<ResourceLocation, TextureAtlasSprite> targets = new HashMap<>();
            List<BakedQuad> faces = new ArrayList<>();
            for (String name : ElysiumGrassModel.SPRITES) {
                var source = sprite("minecraft:block/" + name, 16, 16, allocated);
                var target = sprite("elysium:block/golden_" + name, 96, 144, allocated);
                targets.put(target.contents().name(), target);
                faces.add(quad(source, 0));
            }
            faces.add(quad(faces.getFirst().getSprite(), -1)); // Pack-authored uncoloured face.
            faces.add(quad(sprite("pack:block/custom_grass", 0, 0, allocated), 0));
            var original = new FixtureModel(List.copyOf(faces));
            var enabled = new AtomicBoolean(false);
            var grass = ModelResourceLocation.vanilla("grass_block", "snowy=false");
            var snow = ModelResourceLocation.vanilla("grass_block", "snowy=true");
            var shortGrass = ModelResourceLocation.vanilla("short_grass", "");
            var tallLower = ModelResourceLocation.vanilla("tall_grass", "half=lower");
            var tallUpper = ModelResourceLocation.vanilla("tall_grass", "half=upper");
            var item = ModelResourceLocation.vanilla("grass_block", "inventory");
            var dirt = ModelResourceLocation.vanilla("dirt", "");
            Map<ModelResourceLocation, BakedModel> models = new HashMap<>();
            for (var id : List.of(grass, snow, shortGrass, tallLower, tallUpper, item, dirt)) models.put(id, original);
            ElysiumGrassModel.register(new ModelEvent.ModifyBakingResult(models, material -> targets.get(material.texture()), null), enabled::get);
            var model = models.get(grass);
            check(model instanceof ElysiumGrassModel, "The baking hook must install the grass wrapper");
            for (var id : List.of(snow, shortGrass, tallLower, tallUpper)) {
                check(models.get(id) == model, "All grass state variants must be wrapped, sharing identical original models");
            }
            check(models.get(item) == original && models.get(dirt) == original, "Inventory and unrelated blocks must stay untouched");

            var packProperty = new ModelProperty<String>();
            var packData = ModelData.of(packProperty, "preserved");
            var outside = model.getModelData(null, BlockPos.ZERO, null, packData);
            check(outside == packData, "Non-Elysium model data must pass through unchanged");
            check(model.getQuads(null, Direction.UP, RandomSource.create(17), outside, null) == original.faces(),
                    "Other dimensions must return the original quads");
            enabled.set(true);
            var inside = model.getModelData(null, BlockPos.ZERO, null, packData);
            check("preserved".equals(inside.get(packProperty)), "Pack model data must survive the dimension flag");
            var mapped = model.getQuads(null, Direction.UP, RandomSource.create(17), inside, null);
            check(mapped.size() == faces.size(), "Keep all pack faces");
            for (int i = 0; i < 5; i++) {
                var before = faces.get(i); var after = mapped.get(i);
                check(before != after && after.getSprite() != before.getSprite(), "Tinted vanilla grass faces must use derived sprites");
                check(after.getTintIndex() == before.getTintIndex() && after.getDirection() == before.getDirection()
                                && after.isShade() == before.isShade() && after.hasAmbientOcclusion() == before.hasAmbientOcclusion(),
                        "Tint, face direction, shading and ambient-occlusion flags must survive");
                for (int index = 0; index < before.getVertices().length; index++) {
                    int field = index % IQuadTransformer.STRIDE;
                    if (field == IQuadTransformer.UV0 || field == IQuadTransformer.UV0 + 1) {
                        float a = Float.intBitsToFloat(before.getVertices()[index]);
                        float b = Float.intBitsToFloat(after.getVertices()[index]);
                        float relativeA = field == IQuadTransformer.UV0 ? before.getSprite().getUOffset(a) : before.getSprite().getVOffset(a);
                        float relativeB = field == IQuadTransformer.UV0 ? after.getSprite().getUOffset(b) : after.getSprite().getVOffset(b);
                        check(Math.abs(relativeA - relativeB) < 0.00001, "Atlas relocation must retain normalized UVs, including rotated/subregion UVs");
                    } else check(before.getVertices()[index] == after.getVertices()[index], "Geometry, vertex colours, normals and light must remain exact");
                }
            }
            check(mapped.get(5) == faces.get(5) && mapped.get(6) == faces.get(6), "Uncoloured faces and custom pack sprite paths must pass through");
            check(model.getQuads(null, null, RandomSource.create(17)) == original.faces(), "Legacy/item rendering must retain the original artwork");
            check(model.getQuads(null, null, RandomSource.create(17), ModelData.EMPTY, null) == original.faces(), "Only world model data may enable the grass treatment");
            check(model.getParticleIcon(inside) != original.getParticleIcon() && model.getParticleIcon(outside) == original.getParticleIcon(),
                    "Particle sprite lookup must respect the dimension flag");
            var cached = model.getQuads(null, Direction.UP, RandomSource.create(17), inside, null);
            check(cached.getFirst() == mapped.getFirst(), "Static quads should reuse their transformed result");
            enabled.set(false);
            var returned = model.getModelData(null, BlockPos.ZERO, null, inside);
            check(model.getQuads(null, null, RandomSource.create(17), returned, null) == original.faces(), "Returning from Elysium must clear the treatment");
            Map<ModelResourceLocation, BakedModel> missing = new HashMap<>(Map.of(grass, original));
            ElysiumGrassModel.register(new ModelEvent.ModifyBakingResult(missing, material -> faces.getFirst().getSprite(), null), () -> true);
            check(missing.get(grass) == original, "A missing atlas target must preserve working pack artwork");
            System.out.println("Grass model checks: baking registration, five state variants, dimension changes, UV relocation, pack data/geometry, item isolation and missing-texture fallback passed.");
        } finally {
            allocated.forEach(SpriteContents::close);
        }
    }

    private static TextureAtlasSprite sprite(String name, int x, int y, List<SpriteContents> allocated) {
        var contents = new SpriteContents(ResourceLocation.parse(name), new FrameSize(16, 16),
                new NativeImage(16, 16, false), ResourceMetadata.EMPTY);
        allocated.add(contents);
        return new FixtureSprite(contents, x, y);
    }

    private static BakedQuad quad(TextureAtlasSprite sprite, int tint) {
        int[] vertices = new int[4 * IQuadTransformer.STRIDE];
        for (int i = 0; i < vertices.length; i++) vertices[i] = 0x12340000 + i;
        for (int i = 0; i < 4; i++) {
            int uv = i * IQuadTransformer.STRIDE + IQuadTransformer.UV0;
            vertices[uv] = Float.floatToRawIntBits(sprite.getU((i + 1) / 5.0f));
            vertices[uv + 1] = Float.floatToRawIntBits(sprite.getV((4 - i) / 5.0f));
        }
        return new BakedQuad(vertices, tint, Direction.UP, sprite, true, false);
    }

    private record FixtureModel(List<BakedQuad> faces) implements BakedModel {
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) { return faces; }
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType type) { return faces; }
        public boolean useAmbientOcclusion() { return true; }
        public boolean isGui3d() { return true; }
        public boolean usesBlockLight() { return true; }
        public boolean isCustomRenderer() { return false; }
        public TextureAtlasSprite getParticleIcon() { return faces.getFirst().getSprite(); }
        public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
    }

    private static final class FixtureSprite extends TextureAtlasSprite {
        FixtureSprite(SpriteContents contents, int x, int y) { super(TextureAtlas.LOCATION_BLOCKS, contents, 256, 256, x, y); }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
