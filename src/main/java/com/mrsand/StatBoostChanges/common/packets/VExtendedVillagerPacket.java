package com.mrsand.StatBoostChanges.common.packets;

import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.common.management.VExtendedVillager;
import com.mrsand.StatBoostChanges.common.management.VExtendedVillager.VillagerKeys;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;

public class VExtendedVillagerPacket implements IMessage{

	private int entityID;
	private int[] dataValues;
	
	public VExtendedVillagerPacket() {}

	public VExtendedVillagerPacket(EntityVillager villager, VExtendedVillager vVillager) {
		this.entityID = villager.getEntityId();
		VillagerKeys[] keys = VillagerKeys.values();
		this.dataValues = new int[keys.length];

		for (int i = 0; i < keys.length; i++) {
			String keyName = VExtendedPlayer.NBT_PREFIX + keys[i].name();
			this.dataValues[i] = vVillager.getData().getInteger(keyName);

		}
	}
	@Override
	public void toBytes(ByteBuf buffer) {
		buffer.writeInt(entityID); // MUST send the ID
		buffer.writeInt(dataValues.length); // Good practice to send length
		for (int value : this.dataValues) {
			buffer.writeInt(value);
		}
	}

	@Override
	public void fromBytes(ByteBuf buffer) {
		this.entityID = buffer.readInt(); 
		int len = buffer.readInt();
		this.dataValues = new int[len];
		for (int i = 0; i < len; i++) {
			this.dataValues[i] = buffer.readInt();
		}
	}



	public int getVillagerID() { return entityID; }
	public int[] getDataValues() { return dataValues; }

	public static class VExtendedVillagerHandler implements IMessageHandler<VExtendedVillagerPacket, IMessage> {

		@Override
		public IMessage onMessage(VExtendedVillagerPacket message, MessageContext ctx) {
			// Safety check to ensure we only run this on the client thread
			if (ctx.side == Side.CLIENT) {
				Entity entity = Minecraft.getMinecraft().theWorld.getEntityByID(message.entityID);

				if (entity instanceof EntityVillager) {
					VExtendedVillager props = VExtendedVillager.get((EntityVillager) entity);
					if (props != null) {
						VillagerKeys[] keys = VillagerKeys.values();
						for (int i = 0; i < Math.min(keys.length, message.dataValues.length); i++) {
							props.setInteger(keys[i], message.dataValues[i]);
						}
					}
				}
			}
			return null;
		}
	}

}
