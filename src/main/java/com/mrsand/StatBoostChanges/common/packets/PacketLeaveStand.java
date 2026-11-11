package com.mrsand.StatBoostChanges.common.packets;

import com.mrsand.StatBoostChanges.util.CoffinPoseDebug;
import com.mrsand.StatBoostChanges.util.CoffinSleepUtil;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChunkCoordinates;

/**
 * Server → client: authoritative leave-stand feet-air cell after nest exit.
 * Client must not independently search/eject for a stand.
 */
public class PacketLeaveStand implements IMessage {

	private int x;
	private int y;
	private int z;

	public PacketLeaveStand() {}

	public PacketLeaveStand(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public PacketLeaveStand(ChunkCoordinates stand) {
		this(stand.posX, stand.posY, stand.posZ);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
	}

	public static class Handler implements IMessageHandler<PacketLeaveStand, IMessage> {
		@Override
		public IMessage onMessage(PacketLeaveStand message, MessageContext ctx) {
			if (ctx.side == Side.CLIENT) {
				handleClient(message);
			}
			return null;
		}

		@SideOnly(Side.CLIENT)
		private void handleClient(PacketLeaveStand message) {
			EntityPlayer player = Minecraft.getMinecraft().thePlayer;
			if (player == null) return;
			ChunkCoordinates stand = new ChunkCoordinates(message.x, message.y, message.z);
			CoffinSleepUtil.applyLeaveStandFromServer(player, stand);
			CoffinPoseDebug.dump(player, "RECV_LEAVE_STAND stand=" + stand.posX + "," + stand.posY + "," + stand.posZ);
		}
	}
}
