package com.mrsand.StatBoostChanges.common.handlers;

import com.mrsand.StatBoostChanges.common.packets.VisualPacketFromServer;
import com.mrsand.StatBoostChanges.util.VisualUtil;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class SyncVisualHandler implements IMessageHandler<VisualPacketFromServer, IMessage> {

	@Override
	public IMessage onMessage(VisualPacketFromServer message, MessageContext ctx) {

		World world = Minecraft.getMinecraft().theWorld;
		Entity entity = world.getEntityByID(message.getPlayerID());
		if(world != null) {	
			if(entity instanceof EntityPlayer) {
				VisualUtil.changeForm((EntityPlayer)entity, message.getBoolean());
			}
		}
		return null;
	}

}
