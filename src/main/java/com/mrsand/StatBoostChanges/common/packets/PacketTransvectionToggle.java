package com.mrsand.StatBoostChanges.common.packets;

import com.mrsand.StatBoostChanges.util.LegendsUtil;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Client → server: double-tap Space toggles Legends transvection
 * (creative-style fly activation).
 */
public class PacketTransvectionToggle implements IMessage {

	public PacketTransvectionToggle() {}

	@Override
	public void fromBytes(ByteBuf buf) {}

	@Override
	public void toBytes(ByteBuf buf) {}

	public static class Handler implements IMessageHandler<PacketTransvectionToggle, IMessage> {
		@Override
		public IMessage onMessage(PacketTransvectionToggle message, MessageContext ctx) {
			EntityPlayer player = ctx.getServerHandler().playerEntity;
			if (player != null) {
				LegendsUtil.tryToggleDoubleJumpTransvection(player);
			}
			return null;
		}
	}
}
