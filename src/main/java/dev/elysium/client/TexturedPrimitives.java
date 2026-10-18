package dev.elysium.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Authored entity materials with proper face normals, independent of the block atlas. */
public final class TexturedPrimitives {
    private TexturedPrimitives() {}
    public static ResourceLocation texture(String name) {return ResourceLocation.fromNamespaceAndPath("elysium","textures/entity/"+name+".png");}
    public static final ResourceLocation BRONZE=texture("bronze_plate"),FEATHER=texture("bronze_feather"),
        HORN=texture("horn"),HIDE=texture("cinder_hide"),SKIN=texture("titan_skin"),EMBER=texture("ember"),
        CLOTH=texture("mourner_cloth"),WING=texture("moth_wing"),
        BONE=ResourceLocation.fromNamespaceAndPath("elysium","textures/block/charred_bone.png");
    public static void box(PoseStack stack,MultiBufferSource buffers,int light,ResourceLocation texture,
                           double x,double y,double z,double w,double h,double d) {
        var v=buffers.getBuffer(RenderType.entityCutoutNoCull(texture));var p=stack.last();
        float a=(float)(x-w/2),b=(float)(x+w/2),c=(float)y,e=(float)(y+h),f=(float)(z-d/2),g=(float)(z+d/2);
        quad(v,p,light,0,0,-1, a,c,f,a,e,f,b,e,f,b,c,f);
        quad(v,p,light,0,0,1, b,c,g,b,e,g,a,e,g,a,c,g);
        quad(v,p,light,-1,0,0, a,c,g,a,e,g,a,e,f,a,c,f);
        quad(v,p,light,1,0,0, b,c,f,b,e,f,b,e,g,b,c,g);
        quad(v,p,light,0,1,0, a,e,f,a,e,g,b,e,g,b,e,f);
        quad(v,p,light,0,-1,0, a,c,g,a,c,f,b,c,f,b,c,g);
    }
    private static void quad(VertexConsumer v,PoseStack.Pose p,int light,float nx,float ny,float nz,float... xyz) {
        for(int i=0;i<4;i++)v.addVertex(p,xyz[i*3],xyz[i*3+1],xyz[i*3+2]).setColor(255,255,255,255)
            .setUv(i<2?0:1,i==0||i==3?1:0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p,nx,ny,nz);
    }
}
