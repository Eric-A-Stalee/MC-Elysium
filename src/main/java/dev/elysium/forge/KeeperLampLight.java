package dev.elysium.forge;

import dev.elysium.Elysium;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** A modest, throttled vanilla light in the depths, with expiry owned by its block. */
@EventBusSubscriber(modid=Elysium.MOD_ID)
public final class KeeperLampLight {
    public static boolean carrying(Player player) {
        return player.isAlive()&&!player.isSpectator()&&player.level().dimension().equals(ElysiumTravel.DIMENSION)
                &&player.getY()<0&&(player.getMainHandItem().is(ModBlocks.KEEPERS_LAMP.get().asItem())
                ||player.getOffhandItem().is(ModBlocks.KEEPERS_LAMP.get().asItem()));
    }
    public static void illuminate(ServerPlayer player) {
        if(!carrying(player))return;
        var level=player.serverLevel();var pos=BlockPos.containing(player.getEyePosition());
        if(level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null&&level.getBlockState(pos).isAir())
            level.setBlock(pos,ModBlocks.CHIMERA_GLOW.get().defaultBlockState(),3);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if(event.getEntity() instanceof ServerPlayer player&&player.tickCount%10==0)illuminate(player);
    }
    private KeeperLampLight(){}
}
