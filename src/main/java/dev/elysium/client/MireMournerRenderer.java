package dev.elysium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.elysium.entity.MireMournerEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import static dev.elysium.client.TexturedPrimitives.*;

/** A hollow hood, drooping woven shroud and a false hearth cradled where a face should be. */
public final class MireMournerRenderer extends EntityRenderer<MireMournerEntity> {
    public MireMournerRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=.6f;}
    @Override public ResourceLocation getTextureLocation(MireMournerEntity e){return CLOTH;}
    @Override public void render(MireMournerEntity e,float yaw,float partial,PoseStack p,MultiBufferSource b,int light){
        double t=e.tickCount+partial,breath=Math.sin(t*.045)*.035;
        p.pushPose();p.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial,e.yRotO,e.getYRot())));
        box(p,b,light,HIDE,0,.6,0,.55,1.9,.5);
        for(int i=0;i<7;i++){
            double a=i*Math.PI*2/7;p.pushPose();p.translate(Math.cos(a)*.35,2.5,Math.sin(a)*.3);p.mulPose(Axis.ZP.rotationDegrees((float)(Math.cos(a)*(8+3*Math.sin(t*.04+i)))));
            box(p,b,light,CLOTH,0,-2.5,0,.48,2.6,.15);p.popPose();
        }
        p.pushPose();p.translate(0,2.4+breath,.08);p.mulPose(Axis.XP.rotationDegrees(e.chant()>0?-18:12));
        box(p,b,light,CLOTH,0,.7,0,1.15,.3,.85);box(p,b,light,CLOTH,0,0,-.32,1.1,.8,.2);
        for(int side:new int[]{-1,1})box(p,b,light,CLOTH,side*.48,0,0,.23,.85,.85);
        box(p,b,light,HORN,0,.15,0,.55,.45,.3);
        box(p,b,LightTexture.FULL_BRIGHT,EMBER,0,.21,.18,e.chant()>0?.4:.22,.18,.035);
        p.popPose();
        for(int side:new int[]{-1,1}){
            p.pushPose();p.translate(side*.48,2.3,0);p.mulPose(Axis.ZP.rotationDegrees(side*(e.chant()>0?42:15)));
            box(p,b,light,BONE,0,-1.6,0,.16,1.6,.2);box(p,b,light,CLOTH,0,-.8,-.05,.42,.95,.32);
            for(int i=0;i<3;i++)box(p,b,light,HORN,(i-1)*.09,-1.85,.07,.06,.4,.09);p.popPose();
        }
        p.popPose();super.render(e,yaw,partial,p,b,light);
    }
}
