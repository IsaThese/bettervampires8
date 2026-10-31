package com.mrsand.StatBoostChanges.common.packets;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

public class PacketVignette implements IMessage {
    private float intensity;

    public PacketVignette() {}
    public PacketVignette(float intensity) {
        this.intensity = MathHelper.clamp_float(intensity, 0.0F, 1.0F);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeFloat(intensity);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.intensity = buf.readFloat();
    }

    public float getIntensity() { return intensity; }

    // --- Static client-side state ---
    @SideOnly(Side.CLIENT)
    private static float currentIntensity = 0.0F;

    @SideOnly(Side.CLIENT)
    public static float getCurrentIntensity() { return currentIntensity; }

    public static class Handler implements IMessageHandler<PacketVignette, IMessage> {
        @Override
        public IMessage onMessage(PacketVignette message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                currentIntensity = message.intensity;
            }
            return null;
        }
    }

    @SideOnly(Side.CLIENT)
    public static void render(ScaledResolution res) {
        if (currentIntensity <= 0.0F) return;

        Minecraft mc = Minecraft.getMinecraft();
        int width = res.getScaledWidth();
        int height = res.getScaledHeight();

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4f(0.0F, 0.0F, 0.0F, currentIntensity);

//        mc.getTextureManager().bindTexture(new ResourceLocation("textures/misc/vignette.png"));
        mc.getTextureManager().bindTexture(new ResourceLocation("bettervampires", "textures/misc/vignette.png"));

        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(0,     height, -90, 0, 1);
        tess.addVertexWithUV(width, height, -90, 1, 1);
        tess.addVertexWithUV(width, 0,      -90, 1, 0);
        tess.addVertexWithUV(0,     0,      -90, 0, 0);
        tess.draw();

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
    }
}