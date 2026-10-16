package dev.elysium.gametest;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** A normal damageable player; only the absent network transport is stubbed. */
final class CombatTestPlayer {
    static ServerPlayer create(ServerLevel level,BlockPos pos) {
        var profile=new GameProfile(UUID.randomUUID(),"CombatProbe");
        var player=new ServerPlayer(level.getServer(),level,profile,ClientInformation.createDefault());
        var connection=new Connection(PacketFlow.SERVERBOUND) {
            @Override public void setListenerForServerboundHandshake(PacketListener listener) {}
        };
        player.connection=new ServerGamePacketListenerImpl(level.getServer(),connection,player,
                CommonListenerCookie.createInitial(profile,false));
        player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);
        player.setPos(Vec3.atBottomCenterOf(pos));level.addNewPlayer(player);
        // A socket-less player has no connection tick to expire its spawn protection.
        for(int n=0;n<70;n++){player.tick();player.doTick();}
        player.setPos(Vec3.atBottomCenterOf(pos));player.setHealth(20);player.invulnerableTime=0;
        return player;
    }
    private CombatTestPlayer() {}
}
