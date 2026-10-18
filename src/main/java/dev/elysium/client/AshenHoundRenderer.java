package dev.elysium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.elysium.entity.AshenHoundEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.*;

/** Eyeless charcoal anatomy, a hollow rib cage and a jaw that opens before the lunge. */
public final class AshenHoundRenderer extends EntityRenderer<AshenHoundEntity> {
    public AshenHoundRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=.7f;}
    @Override public ResourceLocation getTextureLocation(AshenHoundEntity e){return TexturedPrimitives.HIDE;}
    private void box(PoseStack p,MultiBufferSource b,int light,Block m,double x,double y,double z,double w,double h,double d){
        var material=m==Blocks.RED_TERRACOTTA?TexturedPrimitives.EMBER:
            m==Blocks.DRIPSTONE_BLOCK||m==Blocks.CRACKED_DEEPSLATE_TILES?TexturedPrimitives.BONE:
            m==Blocks.OBSIDIAN?TexturedPrimitives.HORN:
            m==Blocks.BASALT||m==Blocks.BLACKSTONE?TexturedPrimitives.HIDE:TexturedPrimitives.BRONZE;
        TexturedPrimitives.box(p,b,light,material,x,y,z,w,h,d);
    }
    @Override public void render(AshenHoundEntity e,float yaw,float partial,PoseStack p,MultiBufferSource b,int light){
        p.pushPose();p.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial,e.yRotO,e.getYRot())));double stride=Math.sin((e.tickCount+partial)*.6)*Math.min(1,e.getDeltaMovement().horizontalDistance()*8);
        box(p,b,light,Blocks.BASALT,0,.64,-.2,.45,.3,1.9);box(p,b,light,Blocks.BLACKSTONE,0,.6,.50,1.05,.65,.7);
        for(int i=0;i<5;i++)for(int side:new int[]{-1,1}){p.pushPose();p.translate(side*.35,.73,-.95+i*.29);p.mulPose(Axis.ZP.rotationDegrees(side*-22));box(p,b,light,Blocks.CRACKED_DEEPSLATE_TILES,0,-.3,0,.12,.65,.17);p.popPose();}
        for(int side:new int[]{-1,1})for(int leg=0;leg<2;leg++){p.pushPose();p.translate(side*.43,.73,leg==0?.48:-.82);p.mulPose(Axis.XP.rotationDegrees((float)(stride*25*side*(leg==0?1:-1))));box(p,b,light,Blocks.BASALT,0,-.64,0,.18,.7,.22);box(p,b,light,Blocks.OBSIDIAN,0,-.72,.13,.26,.15,.42);p.popPose();}
        box(p,b,light,Blocks.BLACKSTONE,0,.84,1,.62,.45,.9);box(p,b,light,Blocks.OBSIDIAN,0,.95,1.4,.55,.19,.4);
        double gape=e.jaw()/18.0;
        box(p,b,LightTexture.pack(6,LightTexture.sky(light)),Blocks.RED_TERRACOTTA,0,.68,1.12,.30,.20,.62);
        p.pushPose();p.translate(0,.70,.78);p.mulPose(Axis.XP.rotationDegrees((float)(gape*34)));box(p,b,light,Blocks.BASALT,0,-.05,.45,.61,.14,1);
        for(int side:new int[]{-1,1})for(int i=0;i<3;i++)box(p,b,light,Blocks.DRIPSTONE_BLOCK,side*.24,.05,.20+i*.26,.08,.18,.09);p.popPose();
        for(int i=0;i<4;i++){p.pushPose();p.translate(0,.72-i*.08,-1.1-i*.25);p.mulPose(Axis.YP.rotationDegrees((float)(Math.sin((e.tickCount+partial)*.07)*i*5)));box(p,b,light,Blocks.BASALT,0,0,0,.17,.18,.35);p.popPose();}
        p.popPose();super.render(e,yaw,partial,p,b,light);
    }
}
