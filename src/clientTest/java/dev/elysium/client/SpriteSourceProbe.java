package dev.elysium.client;

import com.google.common.collect.HashBiMap;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;

/** Real Minecraft sprite decoding and mip generation; no window or GL context. */
public final class SpriteSourceProbe {
    private static final SpriteResourceLoader LOADER = SpriteResourceLoader.create(List.of(AnimationMetadataSection.SERIALIZER));

    public static void main(String[] args) throws Exception {
        verifyPixelContract();
        var types = HashBiMap.<ResourceLocation, SpriteSourceType>create();
        ElysiumClient.spriteSources(new RegisterSpriteSourceTypesEvent(types));
        var atlas = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/minecraft/atlases/blocks.json")))
                .getAsJsonObject().getAsJsonArray("sources");
        var definitions = Stream.iterate(0, i -> i + 1).limit(atlas.size()).map(atlas::get)
                .map(value -> value.getAsJsonObject())
                .filter(value -> value.get("type").getAsString().equals("elysium:tone_mapped")).toList();
        check(definitions.size() == 6, "All five grass sprites and the leaf sprite must be generated");
        for (var definition : definitions) verifySource(definition, types);
        verifyBiomeTints();
        GrassModelProbe.run();
        System.out.println("Client sprite/model checks passed: approved B foliage/G4.1, six real sprites, alpha, mipmaps, animation, pack overrides and dimension isolation.");
    }

    private static void verifySource(com.google.gson.JsonObject definition,
                                     Map<ResourceLocation, SpriteSourceType> types) throws Exception {
        var type = types.get(ResourceLocation.parse(definition.get("type").getAsString()));
        check(type == ToneMappedSpriteSource.TYPE, "Atlas type must be registered by the real client handler");
        var source = (ToneMappedSpriteSource) type.codec().codec().parse(JsonOps.INSTANCE, definition).getOrThrow();
        var input = SpriteSource.TEXTURE_ID_CONVERTER.idToFile(source.resource());
        var target = SpriteSource.TEXTURE_ID_CONVERTER.idToFile(source.sprite());

        byte[] vanilla;
        try (var stream = SpriteSourceProbe.class.getResourceAsStream("/assets/minecraft/" + input.getPath())) {
            check(stream != null, "Minecraft's actual source texture must be available");
            vanilla = stream.readAllBytes();
        }
        var curve = new SpriteToneCurve(source.gamma(), source.contrast(), source.pivot());
        byte[] lut = new byte[256];
        for (int value = 0; value < 256; value++) lut[value] = (byte) curve.applyAsInt(0xFF000000 | value);
        // Golden values exported from the approved Python preview, independently of Java's implementation.
        String expectedCurveHash = source.resource().getPath().endsWith("birch_leaves")
                ? "f3e63d863e6c1e2795f5e8792f0ad585296d91b2e16f787a376898e4e9cb5ec8"
                : "6f27e8ace6b640dbebe7b1af1f8149fe3c286a48aa7c0a33522352f994525868";
        check(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(lut)).equals(expectedCurveHash),
                "Every channel value must match the approved preview curve: " + source.sprite());
        var resource = resource(vanilla, "{}");
        try (var original = LOADER.loadSprite(source.resource(), resource);
             var mapped = load(source, Map.of(input, resource))) {
            check(mapped != null && mapped.name().equals(source.sprite()), "Derived sprite must resolve under Elysium's identifier");
            check(mapped.width() == 16 && mapped.height() == 16, "Vanilla sprite resolution must be retained");
            double before = 0, after = 0; int opaque = 0;
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                int a = original.getOriginalImage().getPixelRGBA(x, y), b = mapped.getOriginalImage().getPixelRGBA(x, y);
                check((a >>> 24) == (b >>> 24), "Leaf cutout alpha must remain exact");
                check(b == curve.applyAsInt(a), "The real loader must apply the approved curve to each source pixel");
                if ((a >>> 24) == 0) check(a == b, "Transparent RGB padding must remain exact");
                else { before += a & 255; after += b & 255; opaque++; }
            }
            check(opaque > 0 && after > before, "Approved source values must be lifted");
            mapped.increaseMipLevel(4);
            check(mapped.byMipLevel.length == 5 && mapped.byMipLevel[4].getWidth() == 1,
                    "The generated sprite must survive real Minecraft mip generation");
            System.out.printf("%s: %d visible pixels; mean red %.1f -> %.1f; approved curve, alpha and five mip levels preserved.%n",
                    source.resource(), opaque, before / opaque, after / opaque);
        }

        // A non-vanilla pack supplies a larger, coloured, animated texture with
        // partial alpha. Both frames must be processed without losing metadata.
        byte[] packed = fixture();
        String metadata = "{\"animation\":{\"width\":32,\"height\":32,\"frametime\":3,\"interpolate\":true,\"frames\":[1,0]}}";
        var animated = resource(packed, metadata);
        try (var original = LOADER.loadSprite(source.resource(), animated);
             var mapped = load(source, Map.of(input, animated))) {
            check(mapped != null && mapped.width() == 32 && mapped.height() == 32, "Pack frame dimensions must survive reload");
            check(mapped.getOriginalImage().getHeight() == 64 && mapped.getUniqueFrames().count() == 2,
                    "Both animation frames must be retained");
            var animation = mapped.metadata().getSection(AnimationMetadataSection.SERIALIZER).orElseThrow();
            check(animation.getDefaultFrameTime() == 3 && animation.isInterpolatedFrames(), "Animation timing must remain intact");
            int altered = 0;
            for (int y = 0; y < 64; y++) for (int x = 0; x < 32; x++) {
                int a = original.getOriginalImage().getPixelRGBA(x, y), b = mapped.getOriginalImage().getPixelRGBA(x, y);
                check((a >>> 24) == (b >>> 24), "Pack partial alpha must remain exact in every frame");
                check(b == curve.applyAsInt(a), "Each pack frame must use the same approved curve");
                if (a != b) altered++;
            }
            check(altered > 1000, "A reload must process the newly supplied resource pack texture");
            mapped.increaseMipLevel(4);
        }
        // Explicit Elysium artwork is already authored: it must bypass the curve.
        try (var expected = LOADER.loadSprite(source.sprite(), animated);
             var overridden = load(source, Map.of(input, resource, target, animated))) {
            check(overridden != null && overridden.width() == 32 && overridden.getUniqueFrames().count() == 2,
                    "Explicit Elysium texture overrides must retain their own dimensions and animation");
            check(java.util.Arrays.equals(expected.getOriginalImage().getPixelsRGBA(), overridden.getOriginalImage().getPixelsRGBA()),
                    "Explicit Elysium texture overrides must not be recoloured a second time");
        }
        check(load(source, Map.of()) == null, "An absent source must fall back to Minecraft's missing-sprite path");
        for (String field : List.of("gamma", "contrast", "pivot")) {
            var invalid = definition.deepCopy(); invalid.addProperty(field, -1);
            check(type.codec().codec().parse(JsonOps.INSTANCE, invalid).error().isPresent(), "Invalid atlas " + field + " must be rejected");
        }
        var legacy = definition.deepCopy(); legacy.remove("contrast"); legacy.remove("pivot");
        var legacySource = (ToneMappedSpriteSource) type.codec().codec().parse(JsonOps.INSTANCE, legacy).getOrThrow();
        check(legacySource.contrast() == 1 && legacySource.pivot() == 0, "Gamma-only pack sources must remain compatible");
    }

    private static void verifyBiomeTints() throws Exception {
        for (String biome : List.of("golden_fields", "elysian_highlands", "ivory_peaks")) {
            var effects = JsonParser.parseString(Files.readString(Path.of(
                    "src/main/resources/data/elysium/worldgen/biome/" + biome + ".json")))
                    .getAsJsonObject().getAsJsonObject("effects");
            check(effects.get("foliage_color").getAsInt() == 0xFFB947, "Reference biomes must use selected warmer-amber B foliage");
            check(effects.get("grass_color").getAsInt() == 0xF2C846, "Reference biomes must use G4.1 grass");
        }
        check(dev.elysium.registry.ModBlocks.GOLDEN_LEAF_COLOR == 0xFFB947, "Inventory foliage must use selected warmer-amber B too");
        int[] original = {255, 211, 74}, folded = {242, 200, 70};
        int largestDifference = 0;
        for (int channel = 0; channel < 3; channel++) for (int value = 0; value < 256; value++) {
            long preview = Math.round(value * original[channel] / 255.0 * 0.95);
            long runtime = Math.round(value * folded[channel] / 255.0);
            largestDifference = Math.max(largestDifference, (int) Math.abs(preview - runtime));
        }
        check(largestDifference <= 1, "Folding 0.95 gain into the biome tint must stay within one 8-bit channel value");
        System.out.println("G4.1 tint quantization: maximum difference from approved preview = " + largestDifference + "/255.");
    }

    private static void verifyPixelContract() {
        var curve = new SpriteToneCurve(0.72);
        var identity = new SpriteToneCurve(1);
        int previous = -1;
        for (int value = 0; value < 256; value++) {
            int gray = 0xFF000000 | value * 0x010101, result = curve.applyAsInt(gray);
            check((result & 255) >= previous, "Tonal order must remain monotone");
            previous = result & 255;
            check(identity.applyAsInt(gray) == gray, "Gamma one must be an exact identity");
            for (int alpha = 0; alpha < 256; alpha++) {
                int pixel = alpha << 24 | value << 16 | (255 - value) << 8 | 73;
                check((curve.applyAsInt(pixel) >>> 24) == alpha, "The curve must preserve every possible alpha");
                if (alpha == 0) check(curve.applyAsInt(pixel) == pixel, "Invisible texels must be untouched");
            }
        }
        for (int primary : new int[]{0xFF0000FF, 0xFF00FF00, 0xFFFF0000, 0xFF000000, 0xFFFFFFFF}) {
            check(curve.applyAsInt(primary) == primary, "Primary channels and black/white endpoints must not shift or swap");
        }
    }

    private static SpriteContents load(ToneMappedSpriteSource source, Map<ResourceLocation, Resource> resources) {
        var suppliers = new HashMap<ResourceLocation, SpriteSource.SpriteSupplier>();
        source.run(new MemoryResources(resources), new SpriteSource.Output() {
            public void add(ResourceLocation id, SpriteSource.SpriteSupplier supplier) { suppliers.put(id, supplier); }
            public void removeAll(Predicate<ResourceLocation> predicate) { suppliers.keySet().removeIf(predicate); }
        });
        var supplier = suppliers.get(source.sprite());
        return supplier == null ? null : supplier.apply(LOADER);
    }

    private static Resource resource(byte[] png, String metadata) {
        return new Resource(null, () -> new ByteArrayInputStream(png),
                () -> ResourceMetadata.fromJsonStream(new ByteArrayInputStream(metadata.getBytes(StandardCharsets.UTF_8))));
    }

    private static byte[] fixture() throws Exception {
        var image = new BufferedImage(32, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 64; y++) for (int x = 0; x < 32; x++) {
            image.setRGB(x, y, ((x * 13 + y * 7) & 255) << 24 | 0x4070A0);
        }
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private record MemoryResources(Map<ResourceLocation, Resource> values) implements ResourceManager {
        public Set<String> getNamespaces() { return Set.of("minecraft", "elysium"); }
        public Optional<Resource> getResource(ResourceLocation id) { return Optional.ofNullable(values.get(id)); }
        public List<Resource> getResourceStack(ResourceLocation id) { return getResource(id).stream().toList(); }
        public Map<ResourceLocation, Resource> listResources(String path, Predicate<ResourceLocation> filter) { return Map.of(); }
        public Map<ResourceLocation, List<Resource>> listResourceStacks(String path, Predicate<ResourceLocation> filter) { return Map.of(); }
        public Stream<PackResources> listPacks() { return Stream.empty(); }
    }
}
