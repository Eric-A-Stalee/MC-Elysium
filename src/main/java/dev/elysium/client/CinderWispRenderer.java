package dev.elysium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.elysium.entity.CinderWispEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;

/** A hollow charred husk around a breathing ember, with loose orbiting splinters. */
public final class CinderWispRenderer extends EntityRenderer<CinderWispEntity> {
    public CinderWispRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=.3f;}
    @Override public ResourceLocation getTextureLocation(CinderWispEntity e){return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;}
    @Override public void render(CinderWispEntity e,float yaw,float partial,PoseStack p,MultiBufferSource b,int light) {
        double t=e.tickCount+partial;p.pushPose();p.translate(0,.4+Math.sin(t*.07)*.06,0);p.mulPose(Axis.YP.rotationDegrees((float)t*1.4f));
        box(p,b,Blocks.MAGMA_BLOCK,-.15,-.15,-.15,.3,.3,.3,LightTexture.FULL_BRIGHT);
        for(int i=0;i<4;i++) {
            p.pushPose();p.mulPose(Axis.YP.rotationDegrees(i*90));
            double spread=e.charging()?.32:.22;
            box(p,b,Blocks.BLACKSTONE,-.22,-.27,spread,.44,.52,.14,light);
            // The warning belongs on the outside of each plate, where a head-on approach can see it.
            double width=e.charging()?.14:.08;
            box(p,b,e.charging()?Blocks.ORANGE_CONCRETE:Blocks.RED_TERRACOTTA,-width/2,-.18,spread+.145,width,.34,.025,LightTexture.FULL_BRIGHT);
            p.popPose();
        }
        for(int i=0;i<3;i++){double a=t*.045+i*2.1;box(p,b,Blocks.BASALT,Math.cos(a)*.54-.06,Math.sin(a*1.7)*.28,Math.sin(a)*.54-.06,.12,.22,.12,light);}
        p.popPose();super.render(e,yaw,partial,p,b,light);
    }
    private static void box(PoseStack p,MultiBufferSource b,Block block,double x,double y,double z,double sx,double sy,double sz,int light){p.pushPose();p.translate(x,y,z);p.scale((float)sx,(float)sy,(float)sz);Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.defaultBlockState(),p,b,light,OverlayTexture.NO_OVERLAY);p.popPose();}
}
