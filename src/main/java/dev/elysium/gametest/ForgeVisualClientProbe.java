package dev.elysium.gametest;

import com.mojang.logging.LogUtils;
import dev.elysium.Elysium;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Captures actual default-texture Minecraft frames; no shader, image synthesis or fullbright. */
@EventBusSubscriber(modid=Elysium.MOD_ID,value=Dist.CLIENT)
public final class ForgeVisualClientProbe {
    private static final boolean ENABLED=Boolean.getBoolean("elysium.visualClient");
    private static final String[] SCENES={"descent","chimera_dark","chimera_lamp","chimera_eye","chimera_attack",
            "dark","forge_service","forge_restored","forge_full_hall","forge_banked",
            "hammer_strike","harvest_sunset","harvest_night","harvest_answered","water_answered","high_answered","governor","first_dawn","chosen_hour"};
    private static boolean connecting,requested,captured;
    private static int index;
    private static long next;
    private static long attackDeadline;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!ENABLED)return;
        var mc=Minecraft.getInstance();
        if(!connecting&&mc.screen instanceof TitleScreen) {
            connecting=true;
            mc.options.hideGui=true;
            mc.options.renderDistance().set(10);
            mc.options.gamma().set(.5);
            mc.options.fov().set(70);
            ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString("127.0.0.1:25565"),
                    new ServerData("Forge visual review","127.0.0.1:25565",ServerData.Type.OTHER),false,null);
            return;
        }
        if(mc.player==null||mc.level==null||mc.screen!=null)return;
        long now=System.nanoTime();
        if(index>=SCENES.length) {
            if(now>=next) {LogUtils.getLogger().info("ELYSIUM_FORGE_VISUAL_COMPLETE");mc.stop();}
            return;
        }
        if(!requested) {
            mc.player.connection.sendCommand("forge_probe "+SCENES[index]);
            next=now+(index==0?90_000_000_000L:20_000_000_000L);attackDeadline=next+30_000_000_000L;requested=true;captured=false;
        } else if(now>=next&&!captured) {
            if(SCENES[index].equals("chimera_attack")) {
                boolean bolt=mc.level.getEntitiesOfClass(dev.elysium.entity.CinderBoltEntity.class,mc.player.getBoundingBox().inflate(40),e->e.tickCount>=5&&e.tickCount<=18).size()>0;
                if(!bolt) {
                    if(now>=attackDeadline)throw new IllegalStateException("No visible cinder projectile reached the client during the combat capture");
                    return;
                }
            }
            String name=String.format("%02d-%s.png",index+1,SCENES[index]);
            long time=Math.floorMod(mc.level.getDayTime(),24000);
            String scene=SCENES[index];
            if(mc.level.dayTime()!=mc.level.getDayTime()||mc.level.getTimeOfDay(1)!=mc.level.dimensionType().timeOfDay(mc.level.getDayTime()))
                throw new IllegalStateException("Sky angle is not using the synchronized realm clock in "+scene);
            if(scene.equals("harvest_sunset")&&(time<11500||time>=14000)
                    ||scene.equals("harvest_night")&&(time<18000||time>22000)
                    ||scene.equals("first_dawn")&&time>=4000
                    ||scene.equals("chosen_hour")&&time!=11500)
                throw new IllegalStateException("Client sky is out of sync in "+scene+": "+time);
            LogUtils.getLogger().info("ELYSIUM_CLIENT_CLOCK scene={} time={}",scene,time);
            Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->LogUtils.getLogger().info("Forge capture: {}",message.getString()));
            captured=true;next=now+3_000_000_000L;
        } else if(now>=next) {
            index++;requested=false;next=now+5_000_000_000L;
        }
    }
    private ForgeVisualClientProbe() {}
}
