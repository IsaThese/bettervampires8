package com.mrsand.StatBoostChanges.common.packets;

import com.mrsand.StatBoostChanges.util.CoffinDebug;
import com.mrsand.StatBoostChanges.util.CoffinSleepUtil;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;

/** Client → server: Space while sealed in a Legends coffin to pass the day. */
public class PacketCoffinPassDay implements IMessage {

	public PacketCoffinPassDay() {}

	@Override
	public void fromBytes(ByteBuf buf) {}

	@Override
	public void toBytes(ByteBuf buf) {}

	public static class Handler implements IMessageHandler<PacketCoffinPassDay, IMessage> {
		@Override
		public IMessage onMessage(PacketCoffinPassDay message, MessageContext ctx) {
			EntityPlayer player = ctx.getServerHandler().playerEntity;
			if (player != null) {
				CoffinDebug.log(player, "RECV PacketCoffinPassDay");
				CoffinSleepUtil.tryPassDay(player);
			}
			return null;
		}
	}
}
