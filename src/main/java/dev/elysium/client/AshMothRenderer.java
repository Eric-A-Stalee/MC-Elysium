package dev.elysium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.elysium.entity.AshMothEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import static dev.elysium.client.TexturedPrimitives.*;

/** Split UVs use both halves of the actual authored wing sprite. Wings fold around the thorax. */
public final class AshMothRenderer extends EntityRenderer<AshMothEntity> {
    public AshMothRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=.2f;}
    @Override public ResourceLocation getTextureLocation(AshMothEntity e){return WING;}
    @Override public void render(AshMothEntity e,float yaw,float partial,PoseStack p,MultiBufferSource b,int light){
        p.pushPose();p.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial,e.yRotO,e.getYRot())));p.translate(0,.17,0);
        for(int side:new int[]{-1,1}){
            p.pushPose();p.mulPose(Axis.ZP.rotationDegrees((float)(side*(23+Math.sin((e.tickCount+partial)*.75)*34))));
            var v=b.getBuffer(RenderType.entityCutoutNoCull(WING));var pose=p.last();
            float[] xs={0,side*1.05f,side*1.05f,0},zs={-.5f,-.5f,.5f,.5f};
            for(int i=0;i<4;i++)v.addVertex(pose,xs[i],0,zs[i]).setColor(255,255,255,255).setUv(i==0||i==3?.5f:side<0?0:1,i<2?0:1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,0,1,0);
            p.popPose();
        }
        box(p,b,light,BONE,0,-.06,0,.13,.13,.54);box(p,b,light,HORN,0,0,.29,.15,.13,.16);
        for(int side:new int[]{-1,1}){p.pushPose();p.translate(side*.06,.08,.32);p.mulPose(Axis.YP.rotationDegrees(side*30));box(p,b,light,HORN,0,0,.1,.025,.025,.27);p.popPose();}
        p.popPose();super.render(e,yaw,partial,p,b,light);
    }
}
