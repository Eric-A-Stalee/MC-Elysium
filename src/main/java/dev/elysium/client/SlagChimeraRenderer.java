package dev.elysium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.elysium.entity.SlagChimeraEntity;
import dev.elysium.registry.ModBlocks;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Authored voxel anatomy, sharing the eye's pose with its actual damage hitbox. */
public final class SlagChimeraRenderer extends EntityRenderer<SlagChimeraEntity> {
    private final BlockRenderDispatcher blocks;
    public SlagChimeraRenderer(EntityRendererProvider.Context context){super(context);blocks=context.getBlockRenderDispatcher();shadowRadius=5;}
    @Override public ResourceLocation getTextureLocation(SlagChimeraEntity entity){return TextureAtlas.LOCATION_BLOCKS;}
    @Override public boolean shouldRender(SlagChimeraEntity e,Frustum frustum,double x,double y,double z){return frustum.isVisible(e.getBoundingBox().inflate(8,14,12));}
    private void box(PoseStack p,MultiBufferSource buffer,int light,Block block,double x,double y,double z,double w,double h,double d) {
        p.pushPose();p.translate(x-w/2,y,z-d/2);p.scale((float)w,(float)h,(float)d);
        blocks.renderSingleBlock(block.defaultBlockState(),p,buffer,light,OverlayTexture.NO_OVERLAY);p.popPose();
    }
    @Override public void render(SlagChimeraEntity e,float yaw,float partial,PoseStack p,MultiBufferSource buffer,int light) {
        p.pushPose();p.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        double breath=Math.sin((e.tickCount+partial)*.035)*.08;
        box(p,buffer,light,Blocks.BLACKSTONE,0,.1,0,9,2.6+breath,10);
        box(p,buffer,light,Blocks.OBSIDIAN,-.6,1.5,-.8,6.5,2.5,7.5);
        // Unequal dragging lobes, with tapering tar ribbons rather than humanoid limbs.
        for(int side:new int[]{-1,1})for(int i=0;i<5;i++) {
            double z=-4+i*2.05,x=side*(4.4+Math.sin(i*2.3+side)*.6),w=2.1+i%2*.6;
            box(p,buffer,light,i%2==0?Blocks.BLACKSTONE:ModBlocks.BURNT_CLINKER.get(),x,.12,z,w,1.4+(i%3)*.35,2.8);
            box(p,buffer,light,Blocks.BLACKSTONE,x+side*.8,.05,z-1.8,1.1,.5,3.2);
        }
        for(int i=0;i<7;i++)box(p,buffer,light,Blocks.BASALT,(i%2-.5)*.5,3.3,-4+i*1.15,1.5,1.2+(i%3)*.35,1.1);
        double head=e.eyeHeightOffset(partial);
        for(int i=0;i<7;i++) {
            double t=i/6.0,y=2.7+(head-3.3)*t,z=1.4+3.1*t;
            double w=3.2-1.0*t;
            box(p,buffer,light,i%2==0?Blocks.BLACKSTONE:Blocks.BASALT,0,y,z,w,1.35,w*.85);
        }
        // One alien eye, encased in slag; no mouth, second eye or human face.
        box(p,buffer,light,Blocks.OBSIDIAN,0,head-1.8,4.3,4.3,3.6,3.0);
        box(p,buffer,LightTexture.FULL_BRIGHT,Blocks.ORANGE_CONCRETE,0,head-1.5,5.84,3.1,3,0.18);
        box(p,buffer,LightTexture.FULL_BRIGHT,Blocks.YELLOW_TERRACOTTA,0,head-1.12,5.95,2.5,2.24,.14);
        box(p,buffer,LightTexture.FULL_BRIGHT,Blocks.YELLOW_CONCRETE,0,head-.8,6.03,1.9,1.6,.1);
        box(p,buffer,light,Blocks.BLACK_CONCRETE,0,head-1.3,6.12,.48,2.6,.12);
        // Segmented scorpion curl rises behind the body and hooks forward again.
        for(int i=0;i<12;i++) {
            double t=i/11.0,z=-4.4-5.8*Math.sin(t*Math.PI*.85),y=1.4+8.5*t;
            double size=2.3-1.35*t;
            box(p,buffer,light,i%2==0?Blocks.BLACKSTONE:Blocks.BASALT,.6+Math.sin(t*2)*.7,y,z,size,1.25,size);
        }
        box(p,buffer,LightTexture.FULL_BRIGHT,Blocks.ORANGE_TERRACOTTA,1.2,9.2,-6.7,.8,1.6,1.2);
        box(p,buffer,LightTexture.FULL_BRIGHT,Blocks.MAGMA_BLOCK,1.2,8.3,-6.1,.45,1.1,.65);
        p.popPose();super.render(e,yaw,partial,p,buffer,light);
    }
}
