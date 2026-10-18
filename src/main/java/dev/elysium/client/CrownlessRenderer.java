package dev.elysium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.elysium.entity.CrownlessEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import static dev.elysium.client.TexturedPrimitives.*;

/** A living 34-block seated prisoner, not another block statue. Every cuff has a visible tether. */
public final class CrownlessRenderer extends EntityRenderer<CrownlessEntity> {
    public CrownlessRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=10;}
    @Override public ResourceLocation getTextureLocation(CrownlessEntity e){return SKIN;}
    @Override public boolean shouldRender(CrownlessEntity e,Frustum f,double x,double y,double z){return f.isVisible(e.getBoundingBox().inflate(30,18,18));}
    @Override public void render(CrownlessEntity e,float yaw,float partial,PoseStack p,MultiBufferSource b,int light){
        double t=e.tickCount+partial,breath=Math.sin(t*.022)*.18;
        double inhaling=e.phase()==CrownlessEntity.INHALE?Math.min(1,(e.age()+partial)/60):0;
        p.pushPose();
        // Bent knees and splayed feet keep the mass low and recognisably captive.
        for(int side:new int[]{-1,1}){
            p.pushPose();p.translate(side*6,6,4);p.mulPose(Axis.YP.rotationDegrees(side*-16));
            box(p,b,light,SKIN,0,0,0,5,8,8);box(p,b,light,HIDE,0,-5,4,4,6,5);
            box(p,b,light,SKIN,0,-6,7,5,2,8);for(int toe=0;toe<4;toe++)box(p,b,light,HORN,(toe-1.5)*1.05,-6,11,.8,1.1,2);p.popPose();
        }
        box(p,b,light,HIDE,0,6,-1,12,7,8);
        p.pushPose();p.translate(0,12+breath,-1);p.scale(1,(float)(1+inhaling*.035),1);
        box(p,b,light,SKIN,0,0,0,12,12,7);
        // Stacked asymmetric muscle planes, tapering chest, and exposed charcoal ribs.
        for(int side:new int[]{-1,1})for(int i=0;i<5;i++){
            p.pushPose();p.translate(side*(5.5-i*.35),2+i*1.8,3);p.mulPose(Axis.ZP.rotationDegrees(side*(16-i*3)));
            box(p,b,light,SKIN,0,0,0,2.8,2.1,2.8);box(p,b,light,BONE,side*-.45,.3,1.45,2.6,.45,.36);p.popPose();
        }
        box(p,b,light,BRONZE,0,10.8,0,17,1.6,7.8);
        // A seam holds a little fire. It is not a lamp lighting the whole hall.
        box(p,b,LightTexture.pack(e.phase()==CrownlessEntity.INHALE?12:5,0),EMBER,0,2,3.53,.3,8,.05);
        p.popPose();
        for(int side:new int[]{-1,1}){
            p.pushPose();p.translate(side*8.2,21.8+breath,0);p.mulPose(Axis.ZP.rotationDegrees(side*32));
            box(p,b,light,SKIN,0,-10,0,4.2,10,5);p.popPose();
            p.pushPose();p.translate(side*13.5,13.7,0);p.mulPose(Axis.ZP.rotationDegrees(side*42));
            box(p,b,light,SKIN,0,-7.4,0,3.5,8,4);box(p,b,light,BRONZE,0,-7,0,4.2,1.6,4.7);p.popPose();
            box(p,b,light,SKIN,side*18.6,6.4,1.5,4.8,2.5,5);
            for(int finger=0;finger<4;finger++){double len=3.1-Math.abs(finger-1.5)*.6;box(p,b,light,SKIN,side*(17+finger*1.05),5.6,4.2,.85,1.8,len);box(p,b,light,HORN,side*(17+finger*1.05),5.5,4.2+len/2,.72,.7,.8);}
            // Catenary links connect the wrist to the generated anchor's bronze collar.
            for(int i=0;i<22;i++){
                double f=i/21.0,cx=side*(18.5+15.5*f),cy=8+34*f-5*Math.sin(f*Math.PI),cz=1-4*f;
                p.pushPose();p.translate(cx,cy,cz);p.mulPose(Axis.ZP.rotationDegrees(-side*24));if(i%2==0)p.mulPose(Axis.YP.rotationDegrees(90));
                link(p,b,light);p.popPose();
            }
        }
        p.pushPose();p.translate(0,24+breath,0);p.mulPose(Axis.YP.rotationDegrees(Mth.clamp(Mth.rotLerp(partial,e.yHeadRotO,e.getYHeadRot()),-18,18)));
        p.mulPose(Axis.XP.rotationDegrees(e.phase()==CrownlessEntity.DORMANT?17:(float)(9-inhaling*12)));
        double[] skullWidth={4.8,6.3,7,6.8,5.7},skullDepth={4.6,5.6,6,5.4,4.7};
        for(int row=0;row<5;row++)box(p,b,light,SKIN,0,row*1.3,-.1+row*.08,skullWidth[row],1.4,skullDepth[row]);
        box(p,b,light,HIDE,0,.15,2.5,4.8,1.7,1.4);
        for(int side:new int[]{-1,1}){
            p.pushPose();p.translate(side*1.8,3.9,3.02);p.mulPose(Axis.ZP.rotationDegrees(side*9));box(p,b,light,HORN,0,0,0,2.3,.65,.6);p.popPose();
            if(e.phase()!=CrownlessEntity.DORMANT)box(p,b,LightTexture.FULL_BRIGHT,EMBER,side*1.8,3.65,3.2,1.1,.18,.06);
            box(p,b,light,BONE,side*2.6,1.4,3.1,1,2,.5);
        }
        box(p,b,light,HORN,0,1.6,3.25,3,.45+inhaling*.8,.12);
        for(int i=-2;i<=2;i++){p.pushPose();p.translate(i*.7,.3,2.8);p.mulPose(Axis.XP.rotationDegrees(12+i*2));box(p,b,light,HORN,0,-1.8+Math.abs(i)*.3,0,.48,2.0-Math.abs(i)*.3,.65);p.popPose();}
        for(int i=0;i<7;i++){double a=i*Math.PI*2/7;p.pushPose();p.translate(Math.cos(a)*3.4,5.5,Math.sin(a)*2.7);p.mulPose(Axis.ZP.rotationDegrees((float)(Math.cos(a)*-15)));box(p,b,light,BRONZE,0,0,0,.75,2.3+(i*7%5)*.5,.8);box(p,b,light,HORN,0,2.2+(i*7%5)*.5,0,.4,.65,.4);p.popPose();}
        p.popPose();p.popPose();super.render(e,yaw,partial,p,b,light);
    }
    private void link(PoseStack p,MultiBufferSource b,int light){
        box(p,b,light,BRONZE,-.6,0,0,.35,1.8,.38);box(p,b,light,BRONZE,.6,0,0,.35,1.8,.38);
        box(p,b,light,HORN,0,0,0,1.2,.35,.38);box(p,b,light,HORN,0,1.45,0,1.2,.35,.38);
    }
}
