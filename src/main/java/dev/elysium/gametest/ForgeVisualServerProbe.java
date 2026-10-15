package dev.elysium.gametest;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import dev.elysium.Elysium;
import dev.elysium.forge.ForgeLighting;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.structure.ForgePlan;
import dev.elysium.structure.HephaestusForgePiece;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Opt-in visual evidence harness. This entire package is excluded from the player jar. */
@EventBusSubscriber(modid=Elysium.MOD_ID)
public final class ForgeVisualServerProbe {
    private static final boolean ENABLED=Boolean.getBoolean("elysium.visualServer");
    // A real surveyed mountain shoulder in the seed-zero Elysium terrain, not a flat test room.
    private static final ForgePlan PLAN=new ForgePlan(-3524,-3804,100,1);

    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if(!ENABLED)return;
        var level=event.getServer().getLevel(ElysiumTravel.DIMENSION);
        if(level==null)throw new IllegalStateException("Missing Elysium dimension");
        var piece=new HephaestusForgePiece(PLAN);var bounds=piece.getBoundingBox();
        for(int x=(bounds.minX()>>4)-1;x<=(bounds.maxX()>>4)+1;x++)
            for(int z=(bounds.minZ()>>4)-1;z<=(bounds.maxZ()>>4)+1;z++) {
                level.setChunkForced(x,z,true);
                level.getChunk(x,z);
            }
        for(int x=bounds.minX()>>4;x<=bounds.maxX()>>4;x++)
            for(int z=bounds.minZ()>>4;z<=bounds.maxZ()>>4;z++) {
                var chunk=new ChunkPos(x,z);
                var clip=new BoundingBox(chunk.getMinBlockX(),level.getMinBuildHeight(),chunk.getMinBlockZ(),
                        chunk.getMaxBlockX(),level.getMaxBuildHeight()-1,chunk.getMaxBlockZ());
                piece.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),level.random,clip,chunk,BlockPos.ZERO);
            }
        LogUtils.getLogger().info("ELYSIUM_FORGE_VISUAL_READY");
    }

    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        if(!ENABLED)return;
        event.getDispatcher().register(Commands.literal("forge_probe")
                .then(Commands.argument("scene",StringArgumentType.word()).executes(context->{
                    scene(context.getSource().getPlayerOrException(),StringArgumentType.getString(context,"scene"));return 1;
                })));
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(ENABLED&&event.getEntity() instanceof ServerPlayer player)scene(player,"entrance");
    }

    private static void scene(ServerPlayer player,String scene) {
        var level=player.getServer().getLevel(ElysiumTravel.DIMENSION);
        if(level==null)return;
        player.setGameMode(GameType.SPECTATOR);
        switch(scene) {
            case "entrance" -> camera(player,level,-7,PLAN.entranceFloor()+5,-18,-2,PLAN.entranceFloor()+3,1);
            case "dark" -> camera(player,level,-11,PLAN.floor()+2.5,57,0,PLAN.floor()+11,76);
            case "foundry" -> {
                ForgeLighting.restore(level,PLAN,0);
                camera(player,level,-11,PLAN.floor()+2.5,57,0,PLAN.floor()+11,76);
            }
            case "workshop" -> {
                ForgeLighting.restore(level,PLAN,1);
                camera(player,level,-36,PLAN.floor()+2,59,-41,PLAN.floor()+3,72);
            }
            case "retorts" -> camera(player,level,34,PLAN.floor()+2,69,43,PLAN.floor()+4,79);
            case "lit" -> {
                for(int i=0;i<ForgeLighting.CIRCUITS;i++)ForgeLighting.restore(level,PLAN,i);
                camera(player,level,-11,PLAN.floor()+2.5,57,0,PLAN.floor()+11,76);
            }
            case "gallery" -> camera(player,level,22,PLAN.floor()+9,70,-10,PLAN.floor()+5,83);
            default -> throw new IllegalArgumentException("Unknown review scene: "+scene);
        }
        LogUtils.getLogger().info("ELYSIUM_FORGE_SCENE {}",scene);
    }

    private static void camera(ServerPlayer player,ServerLevel level,int u,double y,int v,int tu,double ty,int tv) {
        double x=PLAN.worldX(u,v)+.5,z=PLAN.worldZ(u,v)+.5;
        double dx=PLAN.worldX(tu,tv)+.5-x,dz=PLAN.worldZ(tu,tv)+.5-z,dy=ty-y-1.62;
        float yaw=(float)(Math.toDegrees(Math.atan2(dz,dx))-90);
        float pitch=(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));
        player.teleportTo(level,x,y,z,yaw,pitch);
    }
    private ForgeVisualServerProbe() {}
}
