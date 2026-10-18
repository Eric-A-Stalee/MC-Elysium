package dev.elysium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.elysium.entity.ChainWardenEntity;
import dev.elysium.registry.ModBlocks;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.*;

/** Hollow bell, heavy yoke and dragging linked arms; its chain reaches the same eight-block lane as the strike. */
public final class ChainWardenRenderer extends EntityRenderer<ChainWardenEntity> {
    public ChainWardenRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=1.4f;}
    @Override public ResourceLocation getTextureLocation(ChainWardenEntity e){return TexturedPrimitives.HIDE;}
    @Override public boolean shouldRender(ChainWardenEntity e,Frustum f,double x,double y,double z){return f.isVisible(e.getBoundingBox().inflate(9,3,9));}
    private void box(PoseStack p,MultiBufferSource b,int light,Block m,double x,double y,double z,double w,double h,double d){
        var material=m==Blocks.RED_TERRACOTTA?TexturedPrimitives.EMBER:
            m==Blocks.DRIPSTONE_BLOCK||m==Blocks.CRACKED_DEEPSLATE_TILES?TexturedPrimitives.BONE:
            m==Blocks.OBSIDIAN?TexturedPrimitives.HORN:
            m==Blocks.BASALT||m==Blocks.BLACKSTONE?TexturedPrimitives.HIDE:TexturedPrimitives.BRONZE;
        TexturedPrimitives.box(p,b,light,material,x,y,z,w,h,d);
    }
    @Override public void render(ChainWardenEntity e,float yaw,float partial,PoseStack p,MultiBufferSource b,int light){
        p.pushPose();p.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial,e.yRotO,e.getYRot())));double stride=Math.sin((e.tickCount+partial)*.23)*Math.min(1,e.getDeltaMovement().horizontalDistance()*10);
        for(int side:new int[]{-1,1}){p.pushPose();p.translate(side*.58,1.65,0);p.mulPose(Axis.XP.rotationDegrees((float)(stride*18*side)));box(p,b,light,Blocks.BASALT,0,-1.5,0,.5,1.6,.6);box(p,b,light,Blocks.BLACKSTONE,0,-1.65,.2,.7,.25,1.1);p.popPose();}
        box(p,b,light,Blocks.BLACKSTONE,0,1.55,0,1.7,1.8,1.1);box(p,b,light,ModBlocks.FORGE_IRON.get(),0,3.1,0,3.3,.55,1.3);
        for(int side:new int[]{-1,1})box(p,b,light,Blocks.WAXED_WEATHERED_CUT_COPPER,side*1.42,2.85,0,.6,1,1.5);
        // Bell walls leave an actual dark opening instead of a painted humanoid face.
        for(int side:new int[]{-1,1}){box(p,b,light,Blocks.WAXED_EXPOSED_COPPER,side*.43,3.7,0,.15,.72,.9);box(p,b,light,Blocks.WAXED_WEATHERED_COPPER,0,3.7,side*.43,.9,.72,.15);}
        box(p,b,light,Blocks.WAXED_EXPOSED_COPPER,0,4.42,0,.7,.24,.7);box(p,b,light,Blocks.BLACKSTONE,0,3.6,0,.18,.6,.18);
        box(p,b,LightTexture.pack(5,LightTexture.sky(light)),Blocks.RED_TERRACOTTA,0,2.3,.57,.2,.8,.03);
        for(int side:new int[]{-1,1})for(int i=0;i<9;i++){
            double t=i/8.0,reach=side==1&&e.phase()==2?8:side==1&&e.phase()==3?8*(1-Math.min(1,(e.phaseAge()+partial)/55)):0;
            double lift=side==1&&e.phase()==1?Math.min(1,(e.phaseAge()+partial)/24)*3:0;
            double cx=side*1.55*(1-t*.45),cy=3.2-t*2.8+lift*t,cz=reach*t+.15*Math.sin(i+(e.tickCount+partial)*.04);
            p.pushPose();p.translate(cx,cy,cz);if(i%2==0)p.mulPose(Axis.YP.rotationDegrees(90));
            box(p,b,light,ModBlocks.FORGE_IRON.get(),-.16,0,0,.12,.48,.14);box(p,b,light,ModBlocks.FORGE_IRON.get(),.16,0,0,.12,.48,.14);box(p,b,light,Blocks.WAXED_WEATHERED_COPPER,0,0,0,.34,.10,.14);box(p,b,light,Blocks.WAXED_WEATHERED_COPPER,0,.39,0,.34,.10,.14);p.popPose();
        }
        p.popPose();super.render(e,yaw,partial,p,b,light);
    }
}
