package dev.elysium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.elysium.entity.BronzeEagleEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import static dev.elysium.client.TexturedPrimitives.*;

/** Approximately twenty-six-block flight silhouette, layered coverts and individually splayed primary feathers. */
public final class BronzeEagleRenderer extends EntityRenderer<BronzeEagleEntity> {
    public static final float SCALE=1.9f;
    public BronzeEagleRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=3.4f;}
    @Override public ResourceLocation getTextureLocation(BronzeEagleEntity e){return FEATHER;}
    @Override public boolean shouldRender(BronzeEagleEntity e,Frustum f,double x,double y,double z){return f.isVisible(e.getBoundingBox().inflate(13,5,9));}
    @Override public void render(BronzeEagleEntity e,float yaw,float partial,PoseStack p,MultiBufferSource b,int light){
        double t=e.tickCount+partial;boolean rest=e.phase()==BronzeEagleEntity.PERCH||e.phase()==BronzeEagleEntity.RECOVER;
        double flap=rest?Math.sin(t*.055)*2:Math.sin(t*.12)*19;
        double fold=e.phase()==BronzeEagleEntity.DIVE?-28:rest?-50:e.phase()==BronzeEagleEntity.TELL?-14:0;
        p.pushPose();p.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial,e.yRotO,e.getYRot())));p.scale(SCALE,SCALE,SCALE);
        box(p,b,light,HIDE,0,.55,-.05,1.5,1.5,2.3);
        for(int row=0;row<5;row++)for(int side:new int[]{-1,1}){
            p.pushPose();p.translate(side*(.40+row*.045),.64+row*.25,.78-row*.08);p.mulPose(Axis.ZP.rotationDegrees(side*22));
            box(p,b,light,FEATHER,0,0,0,.65,.48,1.55-row*.12);p.popPose();
        }
        box(p,b,light,BRONZE,0,1.92,1.04,.95,.85,1.16);
        box(p,b,light,HORN,0,2.07,1.94,.57,.42,.94);
        p.pushPose();p.translate(0,2.1,2.3);p.mulPose(Axis.XP.rotationDegrees(-24));box(p,b,light,HORN,0,-.42,0,.38,.53,.3);p.popPose();
        for(int i=-1;i<=1;i++){p.pushPose();p.translate(i*.25,2.6,.87);p.mulPose(Axis.XP.rotationDegrees(-32));box(p,b,light,FEATHER,0,0,0,.18,.45,.17);p.popPose();}
        for(int side:new int[]{-1,1}){
            box(p,b,LightTexture.FULL_BRIGHT,EMBER,side*.48,2.41,1.35,.045,.13,.32);
            box(p,b,light,HORN,side*.48,2.55,1.37,.17,.13,.54);
            p.pushPose();p.translate(side*.65,1.82,0);
            if(rest){p.mulPose(Axis.YP.rotationDegrees(side*70));p.scale(.55f,.55f,.55f);}
            p.mulPose(Axis.ZP.rotationDegrees((float)(side*(flap+fold))));
            box(p,b,light,BRONZE,side*1.45,0,-.12,2.9,.33,1.2);
            box(p,b,light,HORN,side*3.7,-.04,-.4,1.7,.19,.58);
            for(int row=0;row<2;row++)for(int i=0;i<7;i++){
                p.pushPose();p.translate(side*(.55+i*.51),.20-row*.13,-.02-row*.52);p.mulPose(Axis.YP.rotationDegrees(side*(13+i*2)));
                box(p,b,light,FEATHER,0,0,-.12,.64,.14,1.15);p.popPose();
            }
            for(int i=0;i<10;i++){
                p.pushPose();p.translate(side*(.78+i*.51),-.07,-.46-i*.085);p.mulPose(Axis.YP.rotationDegrees(side*(10+i*3.1f)));
                box(p,b,light,FEATHER,0,0,-.8,.55,.13,2.4+i*.12);
                box(p,b,light,HORN,0,.02,-2.02-i*.035,.24,.075,.35);p.popPose();
            }
            p.popPose();
            box(p,b,light,BRONZE,side*.5,.2,.38,.29,.65,.35);
            for(int claw=-1;claw<=1;claw++){
                box(p,b,light,HORN,side*.5+claw*.17,.08,.82,.12,.17,.9);
                box(p,b,light,HORN,side*.5+claw*.17,.01,1.2,.09,.18,.18);
            }
        }
        for(int i=-3;i<=3;i++){p.pushPose();p.mulPose(Axis.YP.rotationDegrees(i*8));box(p,b,light,FEATHER,i*.22,.6,-2.2,.46,.12,2.5);p.popPose();}
        p.popPose();super.render(e,yaw,partial,p,b,light);
    }
}
