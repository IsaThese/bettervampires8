package com.mrsand.StatBoostChanges.common.packets;

import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.StringPlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.util.HypnosisDebug;
import com.mrsand.StatBoostChanges.util.VampireEyesDebug;
import com.mrsand.StatBoostChanges.util.VisualUtil;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class VExtendedPlayerPacket implements IMessage {
	private int entityID;
	private int[] dataValues;
	private String[] stringValues;

	public VExtendedPlayerPacket() {}
	public VExtendedPlayerPacket(EntityPlayer player, VExtendedPlayer vExtended) {
		this.entityID = player.getEntityId();
		PlayerKeys[] keys = PlayerKeys.values();
		this.dataValues = new int[keys.length];

		for (int i = 0; i < keys.length; i++) {
			if (VExtendedPlayer.isBooleanKey(keys[i])) {
				this.dataValues[i] = vExtended.getBoolean(keys[i]) ? 1 : 0;
			} else {
				this.dataValues[i] = vExtended.getInteger(keys[i]);
			}
		}
		StringPlayerKeys[] strings = StringPlayerKeys.values();
		this.stringValues = new String[strings.length];

		for (int i = 0; i < strings.length; i++) {
			this.stringValues[i] = vExtended.getString(strings[i]);
		}

	}

	@Override
	public void toBytes(ByteBuf buffer) {
		buffer.writeInt(entityID); // MUST send the ID
		buffer.writeInt(dataValues.length); // Good practice to send length
		for (int value : this.dataValues) {
			buffer.writeInt(value);
		}
		buffer.writeInt(stringValues.length);
		for (String value : this.stringValues) {
			// ByteBufUtils handles string length prefixes automatically
			ByteBufUtils.writeUTF8String(buffer, value != null ? value : "");
		}
	}

	@Override
	public void fromBytes(ByteBuf buffer) {
		this.entityID = buffer.readInt(); // MUST read the ID first
		int len = buffer.readInt();
		this.dataValues = new int[len]; // Initialize the array properly
		for (int i = 0; i < len; i++) {
			this.dataValues[i] = buffer.readInt();
		}

		int stringLen = buffer.readInt();
		this.stringValues = new String[stringLen];
		for (int i = 0; i < stringLen; i++) {
			this.stringValues[i] = ByteBufUtils.readUTF8String(buffer);
		}
	}
	public int getPlayerID() { return entityID; }
	public int[] getDataValues() { return dataValues; }
	public String[] getStringValues() { return stringValues; }


	public static class SyncVExtendedPlayerHandler implements IMessageHandler<VExtendedPlayerPacket, IMessage> {


		@Override
		public IMessage onMessage(VExtendedPlayerPacket message, MessageContext ctx) {

			if (ctx.side == Side.SERVER) {
				EntityPlayerMP serverPlayer = ctx.getServerHandler().playerEntity;
				if (serverPlayer != null) {
					VExtendedPlayer props = VExtendedPlayer.get(serverPlayer);
					boolean hypnoBefore = props != null && props.getBoolean(PlayerKeys.HYPNOTIZING);
					handleSync(serverPlayer, props, message.getDataValues(), message.getStringValues(), Side.SERVER);
					// Mesmerize is client-toggled; rebroadcast only when HYPNOTIZING actually changes.
					if (props != null) {
						boolean hypnoAfter = props.getBoolean(PlayerKeys.HYPNOTIZING);
						if (hypnoBefore != hypnoAfter) {
							props.syncClient();
							VampireEyesDebug.logSyncClient(serverPlayer, true, hypnoAfter);
						}
					}
				}
			}
			else if (ctx.side == Side.CLIENT) {

				EntityPlayer targetPlayer = (EntityPlayer) Minecraft.getMinecraft().theWorld.getEntityByID(message.getPlayerID());
				if (targetPlayer == null) return null;
				VExtendedPlayer props = VExtendedPlayer.get(targetPlayer);
				// Replay spawn order can deliver BV packets before EntityConstructing finishes.
				if (props == null) {
					VExtendedPlayer.register(targetPlayer);
					props = VExtendedPlayer.get(targetPlayer);
					VampireEyesDebug.logState(targetPlayer, "packet-register-ieep");
				}
				if (props == null) return null;
				boolean hypnoBefore = props.getBoolean(PlayerKeys.HYPNOTIZING);
				boolean otherBefore = props.getBoolean(PlayerKeys.OTHERFORM);
				handleSync(targetPlayer, props, message.getDataValues(), message.getStringValues(), Side.CLIENT);
				boolean hypnoAfter = props.getBoolean(PlayerKeys.HYPNOTIZING);
				boolean otherAfter = props.getBoolean(PlayerKeys.OTHERFORM);
				if (hypnoBefore != hypnoAfter || otherBefore != otherAfter) {
					VampireEyesDebug.logState(targetPlayer, "packet-apply-client");
				}
			}

			return null;
		}

		private void handleSync(EntityPlayer player, VExtendedPlayer props, int[] receivedData, String[] receivedStrings, Side side) {
			if (props != null && receivedData != null) {
				PlayerKeys[] keys = PlayerKeys.values();
				int limit = Math.min(keys.length, receivedData.length);
				boolean masteryOwnsEyes = side == Side.SERVER
						&& player.getEntityData().getBoolean("bvHypnosisEyesOwned");

				for (int i = 0; i < limit; i++) {
					// Updating the IEEP data
					if (side == Side.SERVER && (keys[i] == PlayerKeys.LOCATION || keys[i] == PlayerKeys.SECONDSLOT)) {
						continue; // server owns these; ignore client uploads
					}
					// Mastery channel owns HYPNOTIZING on the server — don't let a stale client upload clear it.
					if (masteryOwnsEyes && keys[i] == PlayerKeys.HYPNOTIZING) {
						continue;
					}
					if (VExtendedPlayer.isBooleanKey(keys[i])) {
						props.setBoolean(keys[i], receivedData[i] != 0);
					} else {
						props.setInteger(keys[i], receivedData[i]);
					}
				}

				if(receivedStrings != null) {
					StringPlayerKeys[] strings = StringPlayerKeys.values();
					int max = Math.min(strings.length, receivedStrings.length);
					for(int i = 0; i < max; i ++) {
						props.setString(strings[i], receivedStrings[i]);
					}
				}

				props.migrateCorruptedBooleanKeys();

				boolean hypno = props.getBoolean(PlayerKeys.HYPNOTIZING);
				if (HypnosisDebug.enabled()) {
					int hypnoIdx = PlayerKeys.HYPNOTIZING.ordinal();
					int raw = (receivedData != null && hypnoIdx < receivedData.length) ? receivedData[hypnoIdx] : -1;
					HypnosisDebug.logSync(player, side, hypno);
					HypnosisDebug.log(player, "SYNC-RAW", "HYPNOTIZING ordinal rawInt=" + raw);
				}
				VisualUtil.changeForm(player, hypno);
			}
		}
	}
}