package dev.elysium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.elysium.entity.BronzeEagleEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.*;

/** Articulated serrated pinions and hooked beak; active-pack materials, original anatomy. */
public final class BronzeEagleRenderer extends EntityRenderer<BronzeEagleEntity> {
    private final BlockRenderDispatcher blocks;
    public BronzeEagleRenderer(EntityRendererProvider.Context c){super(c);blocks=c.getBlockRenderDispatcher();shadowRadius=2;}
    @Override public ResourceLocation getTextureLocation(BronzeEagleEntity e){return TextureAtlas.LOCATION_BLOCKS;}
    @Override public boolean shouldRender(BronzeEagleEntity e,Frustum f,double x,double y,double z){return f.isVisible(e.getBoundingBox().inflate(7,4,5));}
    private void box(PoseStack p,MultiBufferSource b,int light,Block material,double x,double y,double z,double w,double h,double d){p.pushPose();p.translate(x-w/2,y,z-d/2);p.scale((float)w,(float)h,(float)d);blocks.renderSingleBlock(material.defaultBlockState(),p,b,light,OverlayTexture.NO_OVERLAY);p.popPose();}
    @Override public void render(BronzeEagleEntity e,float yaw,float partial,PoseStack p,MultiBufferSource b,int light){
        double t=e.tickCount+partial;boolean rest=e.phase()==BronzeEagleEntity.PERCH||e.phase()==BronzeEagleEntity.RECOVER;
        double flap=rest?8:Math.sin(t*.19)*22,fold=e.phase()==BronzeEagleEntity.DIVE?48:rest?58:e.phase()==BronzeEagleEntity.TELL?-18:0;
        p.pushPose();p.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial,e.yRotO,e.getYRot())));
        box(p,b,light,Blocks.BASALT,0,.55,0,1.65,1.7,2.7);
        for(int i=0;i<5;i++)box(p,b,light,i%2==0?Blocks.WAXED_EXPOSED_COPPER:Blocks.WAXED_WEATHERED_COPPER,0,.6+i*.27,.75-i*.12,1.7-i*.12,.35,1.7);
        box(p,b,light,Blocks.WAXED_EXPOSED_COPPER,0,2.05,1.15,1.05,.95,1.3);
        box(p,b,light,Blocks.BLACKSTONE,0,2.2,2,.65,.48,.9);box(p,b,light,Blocks.OBSIDIAN,0,1.92,2.35,.5,.58,.35);
        for(int side:new int[]{-1,1}){
            box(p,b,LightTexture.FULL_BRIGHT,e.phase()==BronzeEagleEntity.TELL?Blocks.ORANGE_CONCRETE:Blocks.RED_TERRACOTTA,side*.54,2.6,1.53,.065,.17,.28);
            p.pushPose();p.translate(side*.65,1.9,0);p.mulPose(Axis.ZP.rotationDegrees((float)(side*(flap+fold))));
            box(p,b,light,Blocks.WAXED_WEATHERED_COPPER,side*1.7,0,-.25,3.6,.45,1.7);
            for(int i=0;i<9;i++){
                p.pushPose();p.translate(side*(.75+i*.51),-.07,-.40-i*.10);p.mulPose(Axis.YP.rotationDegrees(side*(12+i*3)));
                box(p,b,light,i%3==0?Blocks.WAXED_WEATHERED_COPPER:Blocks.WAXED_EXPOSED_COPPER,0,0,-.7,.58,.19,2.3+i*.14);
                box(p,b,light,Blocks.BLACKSTONE,0,-.07,-1.65-i*.05,.35,.15,.7);p.popPose();
            }
            p.popPose();
            box(p,b,light,Blocks.BASALT,side*.55,.2,.4,.3,.7,.35);
            for(int claw=-1;claw<=1;claw++)box(p,b,light,Blocks.OBSIDIAN,side*.55+claw*.16,.02,.8,.12,.18,.85);
        }
        for(int i=-2;i<=2;i++){p.pushPose();p.mulPose(Axis.YP.rotationDegrees(i*10));box(p,b,light,Blocks.WAXED_EXPOSED_CUT_COPPER,i*.26,.6,-2.3,.44,.2,2.2);p.popPose();}
        p.popPose();super.render(e,yaw,partial,p,b,light);
    }
}
