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

	/**
	 * Client: arm suppress as soon as local HP hits 0 (before GuiGameOver),
	 * so S06(0) cannot flash the death screen ahead of the death-save packet.
	 */
	@SideOnly(Side.CLIENT)
	public static void armSuppressClient(int ticks) {
		if (ticks > suppressTicks) {
			suppressTicks = ticks;
		}
	}

	@SideOnly(Side.CLIENT)
	public static int getSuppressTicks() {
		return suppressTicks;
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
		boolean hadGameOver = false;
		float hp = player != null ? player.getHealth() : -1.0F;
		if (player != null) {
			player.isDead = false;
			player.deathTime = 0;
		}
		Minecraft mc = Minecraft.getMinecraft();
		if (mc != null && mc.currentScreen instanceof GuiGameOver) {
			hadGameOver = true;
			mc.displayGuiScreen(null);
		}
		// #region agent log
		try {
			java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(
					"c:/Users/isaac/IdeaProjects/bettervampires8/.cursor/debug-9ecb1a.log", true));
			pw.println("{\"sessionId\":\"9ecb1a\",\"runId\":\"death-gui\",\"hypothesisId\":\"H1\","
					+ "\"location\":\"PacketClearDeathScreen.applyClear\",\"message\":\"packet_clear\","
					+ "\"timestamp\":" + System.currentTimeMillis()
					+ ",\"data\":{\"suppressTicks\":" + suppressTicks + ",\"hadGameOver\":" + hadGameOver
					+ ",\"hp\":" + hp + "}}");
			pw.close();
		} catch (Throwable ignored) {
		}
		// #endregion
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
