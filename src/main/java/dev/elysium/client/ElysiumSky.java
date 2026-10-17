package dev.elysium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;

/** The surface retains its normal sky. The deep horizon is geology, never a distant sun. */
public final class ElysiumSky extends DimensionSpecialEffects.OverworldEffects {
    @Override public boolean renderSky(ClientLevel l,int ticks,float partial,Matrix4f model,Camera camera,Matrix4f projection,boolean foggy,Runnable fog){return camera.getPosition().y < -64;}
    @Override public boolean renderClouds(ClientLevel l,int ticks,float partial,PoseStack pose,double x,double y,double z,Matrix4f model,Matrix4f projection){return y < -64;}
    @Override public boolean renderSnowAndRain(ClientLevel l,int ticks,float partial,LightTexture light,double x,double y,double z){return y < -64;}
}
