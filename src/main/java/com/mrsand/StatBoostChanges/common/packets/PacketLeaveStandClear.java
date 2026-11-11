package com.mrsand.StatBoostChanges.common.packets;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Deprecated: leave-stand clear is server-owned via VExtendedPlayer.syncClient().
 * Kept so old clients do not hard-crash if a stale jar still sends id 8.
 */
public class PacketLeaveStandClear implements IMessage {

	public PacketLeaveStandClear() {}

	@Override
	public void fromBytes(ByteBuf buf) {}

	@Override
	public void toBytes(ByteBuf buf) {}

	public static class Handler implements IMessageHandler<PacketLeaveStandClear, IMessage> {
		@Override
		public IMessage onMessage(PacketLeaveStandClear message, MessageContext ctx) {
			return null;
		}
	}
}
