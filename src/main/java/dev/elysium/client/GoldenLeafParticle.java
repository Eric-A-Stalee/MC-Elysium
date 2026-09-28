package dev.elysium.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CherryParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;

/** Vanilla's gentle leaf motion with our original tiny golden-birch sprite. */
public final class GoldenLeafParticle extends CherryParticle {
    public GoldenLeafParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, sprites);
        int color = ElysiumClient.foliageColor(level, BlockPos.containing(x, y, z));
        setColor(((color >> 16) & 255) / 255.0F, ((color >> 8) & 255) / 255.0F, (color & 255) / 255.0F);
    }
}
