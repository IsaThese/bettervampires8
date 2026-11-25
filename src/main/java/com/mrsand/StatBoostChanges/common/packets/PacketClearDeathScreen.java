package com.mrsand.StatBoostChanges.common.packets;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Server → client: vampire death was cancelled; close GuiGameOver immediately and
 * suppress reopen for a short window so the death screen does not flash.
 */
public class PacketClearDeathScreen implements IMessage {

	/** Client-only: ticks remaining to block GuiGameOver after a vampire save. */
	@SideOnly(Side.CLIENT)
	private static int suppressTicks;

	public PacketClearDeathScreen() {}

	@Override
	public void toBytes(ByteBuf buf) {}

	@Override
	public void fromBytes(ByteBuf buf) {}

	@SideOnly(Side.CLIENT)
	public static boolean shouldSuppressGameOver() {
		return suppressTicks > 0;
	}

	@SideOnly(Side.CLIENT)
	public static void tickSuppress() {
		if (suppressTicks > 0) {
			suppressTicks--;
		}
	}

	@SideOnly(Side.CLIENT)
	public static void applyClear(EntityPlayer player) {
		suppressTicks = 40;
		if (player != null) {
			player.isDead = false;
			player.deathTime = 0;
		}
		Minecraft mc = Minecraft.getMinecraft();
		if (mc != null && mc.currentScreen instanceof GuiGameOver) {
			mc.displayGuiScreen(null);
		}
	}

	public static class Handler implements IMessageHandler<PacketClearDeathScreen, IMessage> {
		@Override
		public IMessage onMessage(PacketClearDeathScreen message, MessageContext ctx) {
			if (ctx.side == Side.CLIENT) {
				handleClient();
			}
			return null;
		}

		@SideOnly(Side.CLIENT)
		private static void handleClient() {
			Minecraft mc = Minecraft.getMinecraft();
			applyClear(mc != null ? mc.thePlayer : null);
		}
	}
}
