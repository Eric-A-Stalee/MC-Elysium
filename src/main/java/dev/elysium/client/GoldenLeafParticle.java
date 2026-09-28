package dev.elysium.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CherryParticle;
import net.minecraft.client.particle.SpriteSet;

/** Vanilla's gentle leaf motion with our original tiny golden-birch sprite. */
public final class GoldenLeafParticle extends CherryParticle {
    public GoldenLeafParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, sprites);
    }
}
