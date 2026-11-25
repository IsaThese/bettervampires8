package com.mrsand.StatBoostChanges.common.packets;

import com.mrsand.StatBoostChanges.abilities.HypnosisMasteryLogic;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Client → server: use-item while Hypnosis Mastery is channeling.
 * Empty-hand AIR/BLOCK interact often never reaches the server once canceled
 * on the client — this packet carries the enslavement/control toggle.
 */
public class PacketHypnosisToggle implements IMessage {

	public PacketHypnosisToggle() {}

	@Override
	public void fromBytes(ByteBuf buf) {}

	@Override
	public void toBytes(ByteBuf buf) {}

	public static class Handler implements IMessageHandler<PacketHypnosisToggle, IMessage> {
		@Override
		public IMessage onMessage(PacketHypnosisToggle message, MessageContext ctx) {
			EntityPlayer player = ctx.getServerHandler().playerEntity;
			if (player != null) {
				HypnosisMasteryLogic.tryToggleEnslavementFromUse(player);
			}
			return null;
		}
	}
}
