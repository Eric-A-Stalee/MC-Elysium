package dev.elysium.client;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/** Derive an Elysium sprite from the active pack's texture without shipping game pixels. */
public record ToneMappedSpriteSource(ResourceLocation resource, ResourceLocation sprite, double gamma)
        implements SpriteSource {
    public static final MapCodec<ToneMappedSpriteSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("resource").forGetter(ToneMappedSpriteSource::resource),
            ResourceLocation.CODEC.fieldOf("sprite").forGetter(ToneMappedSpriteSource::sprite),
            Codec.doubleRange(0.2, 2.0).fieldOf("gamma").forGetter(ToneMappedSpriteSource::gamma)
    ).apply(instance, ToneMappedSpriteSource::new));
    public static final SpriteSourceType TYPE = new SpriteSourceType(CODEC);

    @Override
    public void run(ResourceManager resources, Output output) {
        // A pack can supply Elysium's sprite explicitly. Respect its finished
        // art rather than running an already-adjusted texture through the curve.
        var explicit = resources.getResource(TEXTURE_ID_CONVERTER.idToFile(sprite));
        if (explicit.isPresent()) {
            output.add(sprite, explicit.get());
            return;
        }
        var source = resources.getResource(TEXTURE_ID_CONVERTER.idToFile(resource));
        if (source.isEmpty()) {
            LogUtils.getLogger().warn("Missing source texture {} for Elysium sprite {}", resource, sprite);
            return;
        }
        var curve = new SpriteToneCurve(gamma);
        output.add(sprite, loader -> {
            // Use Minecraft's loader so pack resolution, frame dimensions and
            // animation metadata survive. Mipmaps are generated after this step.
            var contents = loader.loadSprite(sprite, source.get());
            if (contents != null) contents.getOriginalImage().applyToAllPixels(curve);
            return contents;
        });
    }

    @Override
    public SpriteSourceType type() {
        return TYPE;
    }
}
