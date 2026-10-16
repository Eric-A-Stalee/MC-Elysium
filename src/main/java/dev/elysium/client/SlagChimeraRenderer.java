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
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Articulated slag anatomy; the socket pose is shared with the server-side eye. */
public final class SlagChimeraRenderer extends EntityRenderer<SlagChimeraEntity> {
    private final BlockRenderDispatcher blocks;
    public SlagChimeraRenderer(EntityRendererProvider.Context context){super(context);blocks=context.getBlockRenderDispatcher();shadowRadius=5;}
    @Override public ResourceLocation getTextureLocation(SlagChimeraEntity entity){return TextureAtlas.LOCATION_BLOCKS;}
    @Override public boolean shouldRender(SlagChimeraEntity e,Frustum frustum,double x,double y,double z){return frustum.isVisible(e.getBoundingBox().inflate(12,14,15));}
    private void box(PoseStack p,MultiBufferSource b,int light,Block block,double x,double y,double z,double w,double h,double d) {
        p.pushPose();p.translate(x-w/2,y,z-d/2);p.scale((float)w,(float)h,(float)d);
        blocks.renderSingleBlock(block.defaultBlockState(),p,b,light,OverlayTexture.NO_OVERLAY);p.popPose();
    }
    private void plate(PoseStack p,MultiBufferSource b,int light,Block block,double x,double y,double z,double w,double h,double d,double roll,double pitch) {
        p.pushPose();p.translate(x,y,z);p.mulPose(Axis.ZP.rotationDegrees((float)roll));p.mulPose(Axis.XP.rotationDegrees((float)pitch));
        box(p,b,light,block,0,-h/2,0,w,h,d);p.popPose();
    }
    private void limb(PoseStack p,MultiBufferSource b,int light,Block block,Vec3 a,Vec3 end,double w,double d) {
        var delta=end.subtract(a);double length=delta.length();if(length<.001)return;
        p.pushPose();p.translate(a.x,a.y,a.z);
        p.mulPose(new Quaternionf().rotationTo(0,1,0,(float)(delta.x/length),(float)(delta.y/length),(float)(delta.z/length)));
        box(p,b,light,block,0,0,0,w,length,d);p.popPose();
    }
    private static int ember(int light,int strength){return LightTexture.pack(Math.max(LightTexture.block(light),strength),LightTexture.sky(light));}
    @Override public void render(SlagChimeraEntity e,float yaw,float partial,PoseStack p,MultiBufferSource b,int light) {
        p.pushPose();p.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial,e.yRotO,e.getYRot())));
        double time=e.tickCount+partial,breath=Math.sin(time*.035)*.10;
        body(e,p,b,light,time,breath);
        neck(e,p,b,light,partial,breath);
        eye(e,p,b,light,partial,time);
        tail(e,p,b,light,partial,time);
        p.popPose();super.render(e,yaw,partial,p,b,light);
    }
    private void body(SlagChimeraEntity e,PoseStack p,MultiBufferSource b,int light,double time,double breath) {
        // Unequal shoulder and haunch plates sit above separated dragging limbs.
        plate(p,b,light,Blocks.BLACKSTONE,-.6,2.5+breath,-.7,6.4,4.6,7.8,-8,5);
        plate(p,b,light,Blocks.OBSIDIAN,2.2,3.2+breath,-2.3,4.5,4.5,5.2,17,-9);
        plate(p,b,light,ModBlocks.BURNT_CLINKER.get(),-2.5,3.0+breath,1.7,3.7,3.8,4.7,-18,12);
        plate(p,b,light,Blocks.BLACKSTONE,.4,4.2+breath,1.1,4.3,3,4.5,7,-18);
        for(int side:new int[]{-1,1})for(int n=0;n<3;n++) {
            double z=-3.9+n*3.3,drag=Math.sin(time*.022+n*2+side)*.16;
            Vec3 root=new Vec3(side*2.5,2.8+breath,z);
            Vec3 elbow=new Vec3(side*(5.4+(n==1?.9:0)),1.9+(n==0?.65:0),z-1.1+drag);
            Vec3 end=new Vec3(side*(7.1+(side<0&&n==2?.9:0)),.48,z+1.7+drag);
            limb(p,b,light,n%2==0?Blocks.BLACKSTONE:Blocks.OBSIDIAN,root,elbow,2.0,2.3);
            limb(p,b,light,Blocks.BLACKSTONE,elbow,end,1.2,1.6);
            for(int toe=0;toe<2;toe++)limb(p,b,light,Blocks.OBSIDIAN,end.add(toe*.5,0,0),end.add(side*(1.5-toe*.35),-.28,1.4+toe*.5),.46,.7);
            // A hairline of heat reveals the joint, not the whole appendage.
            limb(p,b,ember(light,8),Blocks.RED_TERRACOTTA,elbow.add(-side*.08,.35,0),elbow.add(side*.7,.1,.7),.09,.16);
        }
        for(int n=0;n<7;n++) {
            double z=-4.3+n*1.15,x=Math.sin(n*1.4)*.45;
            plate(p,b,light,Blocks.BASALT,x,4.5+breath+(n%3)*.4,z,.9,1.6,1.2,(n%2==0?16:-13),-20);
        }
        for(int n=0;n<4;n++) {
            double x=-2.7+n*1.6;
            limb(p,b,ember(light,9),n%2==0?Blocks.ORANGE_TERRACOTTA:Blocks.RED_TERRACOTTA,
                    new Vec3(x,3.9+breath,3.36),new Vec3(x+.36,2.4+breath,3.65),.10,.10);
        }
    }
    private void neck(SlagChimeraEntity e,PoseStack p,MultiBufferSource b,int light,float partial,double breath) {
        var head=e.eyeLocal(partial);
        Vec3 previous=new Vec3(-.65,3.2+breath,1.3);
        for(int i=1;i<=10;i++) {
            double t=i/10.0;
            // The bowed, off-centre neck retains its curve when the eye lowers.
            Vec3 next=new Vec3(head.x*t-1.15*Math.sin(t*Math.PI),3.2+(head.y-3.2)*t,
                    1.3+(head.z-1.4-1.3)*t-1.1*Math.sin(t*Math.PI));
            double width=2.9-1.2*t;
            limb(p,b,light,i%3==0?Blocks.OBSIDIAN:Blocks.BLACKSTONE,previous,next,width,width*.8);
            if(i==3||i==6||i==8)limb(p,b,ember(light,10),Blocks.ORANGE_TERRACOTTA,
                    previous.add(width*.45,0,.1),next.add(width*.46,0,.12),.09,.1);
            previous=next;
        }
    }
    private void eye(SlagChimeraEntity e,PoseStack p,MultiBufferSource b,int light,float partial,double time) {
        var c=e.eyeLocal(partial);
        double charge=e.phase()==1?Mth.clamp(e.phaseAge()/45.0,0,1):0;
        double blink=(time%173)<7?Math.sin((time%173)*Math.PI/7):0;
        double opening=Math.max(.13,1-.8*blink-.2*charge);
        p.pushPose();p.translate(c.x,c.y,c.z);
        double[] widths={1.6,2.8,3.5,3.9,3.65,2.9,1.7};
        for(int row=0;row<widths.length;row++) {
            double y=(row-3)*.43,w=widths[row];
            box(p,b,light,row%3==0?Blocks.BLACKSTONE:Blocks.OBSIDIAN,0,y-.28,-.75,w+.85,.62,2.05);
            // Stepped socket edges and irregular amber bands preserve a single alien eye.
            box(p,b,ember(light,11),Blocks.RED_TERRACOTTA,0,y*opening-.23, .31,w,.46*opening,.15);
            box(p,b,ember(light,13),row%2==0?Blocks.ORANGE_CONCRETE:Blocks.ORANGE_TERRACOTTA,
                    .04,y*opening-.18,.41,Math.max(.6,w-.45),.36*opening,.12);
            if(row>0&&row<6)box(p,b,LightTexture.FULL_BRIGHT,row==3?Blocks.YELLOW_CONCRETE:Blocks.YELLOW_TERRACOTTA,
                    -.12,y*opening-.16,.49,Math.max(.4,w-1.35),.32*opening,.12);
        }
        // An uneven vertical pupil, inset in the hot iris rather than a panel-wide stripe.
        for(int row=-2;row<=2;row++)box(p,b,light,Blocks.BLACK_CONCRETE,.10+row*.025,row*.43*opening-.24,.58,
                .27+(2-Math.abs(row))*.105,.51*opening,.13);
        plate(p,b,light,Blocks.BLACKSTONE,-1.55,1.5,-.25,1.3,.65,1.8,-25,-12);
        plate(p,b,light,Blocks.OBSIDIAN,1.6,1.28,-.4,1.3,.75,2.1,28,15);
        plate(p,b,light,Blocks.BLACKSTONE,-.5,-1.65,-.3,2.6,.65,1.6,12,-8);
        p.popPose();
    }
    private void tail(SlagChimeraEntity e,PoseStack p,MultiBufferSource b,int light,float partial,double time) {
        p.pushPose();p.mulPose(Axis.YP.rotationDegrees((float)-Math.toDegrees(e.tailAngle(partial))));
        Vec3 previous=new Vec3(.6,2.8,-3.2);
        for(int i=1;i<=15;i++) {
            double t=i/15.0;
            Vec3 next=e.tailJoint(t,partial);
            double width=2.2-1.5*t;
            limb(p,b,light,i%3==0?Blocks.OBSIDIAN:Blocks.BLACKSTONE,previous,next,width,width*.82);
            if(i%4==0)limb(p,b,ember(light,9),Blocks.ORANGE_TERRACOTTA,previous.add(width*.44,0,0),next.add(width*.44,0,0),.08,.14);
            previous=next;
        }
        limb(p,b,light,Blocks.OBSIDIAN,previous,previous.add(-.3,-1.8,1.65),.75,.65);
        limb(p,b,ember(light,12),Blocks.MAGMA_BLOCK,previous.add(-.3,-1.4,1.2),previous.add(-.55,-2.2,2.0),.20,.30);
        p.popPose();
    }
}
