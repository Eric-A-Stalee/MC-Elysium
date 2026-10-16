package dev.elysium.client;

import dev.elysium.Elysium;
import dev.elysium.portal.ElysiumTravel;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Distant, unrendered geology fades to warm darkness instead of the Overworld sky. */
@EventBusSubscriber(modid=Elysium.MOD_ID,value=Dist.CLIENT)
public final class TartarusFog {
    @SubscribeEvent public static void color(ViewportEvent.ComputeFogColor event) {
        var level=Minecraft.getInstance().level;
        if(level==null||!level.dimension().equals(ElysiumTravel.DIMENSION)||event.getCamera().getFluidInCamera()!=FogType.NONE)return;
        float weight=Mth.clamp((float)(-event.getCamera().getPosition().y-64)/64,0,1);
        if(weight==0)return;
        event.setRed(Mth.lerp(weight,event.getRed(),.025f));
        event.setGreen(Mth.lerp(weight,event.getGreen(),.018f));
        event.setBlue(Mth.lerp(weight,event.getBlue(),.012f));
    }
    private TartarusFog(){}
}
