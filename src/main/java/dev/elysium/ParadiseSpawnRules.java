package dev.elysium;

import dev.elysium.portal.ElysiumTravel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * Elysium's dimension type is natural so beds and compasses work, which also lets lit nether portals
 * spawn zombified piglins. Block only those portal spawns; eggs, spawners and commands still work.
 * The first night has its own cinders; releasing the sky does not import Nether portal mobs.
 */
@EventBusSubscriber(modid = Elysium.MOD_ID)
public final class ParadiseSpawnRules {
    @SubscribeEvent
    public static void blockPortalPiglins(FinalizeSpawnEvent event) {
        var level = event.getLevel().getLevel();
        if (event.getSpawnType() == MobSpawnType.STRUCTURE
                && event.getEntity().getType() == EntityType.ZOMBIFIED_PIGLIN
                && level.dimension().equals(ElysiumTravel.DIMENSION)) {
            event.setSpawnCancelled(true);
        }
    }

    private ParadiseSpawnRules() {}
}
