package com.mrsand.StatBoostChanges.common.packets;

import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.StringPlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.util.CoffinSleepUtil;
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
import net.minecraft.util.ChunkCoordinates;

public class VExtendedPlayerPacket implements IMessage {
	private int entityID;
	private int[] dataValues;
	private String[] stringValues;
	private boolean hasLeaveStand;
	private int leaveStandX;
	private int leaveStandY;
	private int leaveStandZ;

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

		ChunkCoordinates stand = vExtended.getLeaveStand();
		if (stand != null && vExtended.getBoolean(PlayerKeys.LEAVE_STANDING)) {
			this.hasLeaveStand = true;
			this.leaveStandX = stand.posX;
			this.leaveStandY = stand.posY;
			this.leaveStandZ = stand.posZ;
		} else {
			this.hasLeaveStand = false;
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
		buffer.writeBoolean(this.hasLeaveStand);
		if (this.hasLeaveStand) {
			buffer.writeInt(this.leaveStandX);
			buffer.writeInt(this.leaveStandY);
			buffer.writeInt(this.leaveStandZ);
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

		this.hasLeaveStand = buffer.readBoolean();
		if (this.hasLeaveStand) {
			this.leaveStandX = buffer.readInt();
			this.leaveStandY = buffer.readInt();
			this.leaveStandZ = buffer.readInt();
		}
	}
	public int getPlayerID() { return entityID; }
	public int[] getDataValues() { return dataValues; }
	public String[] getStringValues() { return stringValues; }
	public boolean hasLeaveStand() { return hasLeaveStand; }
	public int getLeaveStandX() { return leaveStandX; }
	public int getLeaveStandY() { return leaveStandY; }
	public int getLeaveStandZ() { return leaveStandZ; }


	public static class SyncVExtendedPlayerHandler implements IMessageHandler<VExtendedPlayerPacket, IMessage> {


		@Override
		public IMessage onMessage(VExtendedPlayerPacket message, MessageContext ctx) {

			if (ctx.side == Side.SERVER) {
				EntityPlayerMP serverPlayer = ctx.getServerHandler().playerEntity;
				if (serverPlayer != null) {
					VExtendedPlayer props = VExtendedPlayer.get(serverPlayer);
					boolean hypnoBefore = props != null && props.getBoolean(PlayerKeys.HYPNOTIZING);
					handleSync(serverPlayer, props, message, Side.SERVER);
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
				boolean leaveBefore = props.getBoolean(PlayerKeys.LEAVE_STANDING);
				handleSync(targetPlayer, props, message, Side.CLIENT);
				boolean hypnoAfter = props.getBoolean(PlayerKeys.HYPNOTIZING);
				boolean otherAfter = props.getBoolean(PlayerKeys.OTHERFORM);
				if (hypnoBefore != hypnoAfter || otherBefore != otherAfter) {
					VampireEyesDebug.logState(targetPlayer, "packet-apply-client");
				}
				applyLeaveStandClient(targetPlayer, props, leaveBefore);
			}

			return null;
		}

		private void applyLeaveStandClient(EntityPlayer player, VExtendedPlayer props, boolean leaveBefore) {
			boolean leaveAfter = props.getBoolean(PlayerKeys.LEAVE_STANDING);
			ChunkCoordinates stand = props.getLeaveStand();
			if (leaveAfter && stand != null) {
				CoffinSleepUtil.applyLeaveStandFromServer(player, stand);
			} else if (leaveBefore && !leaveAfter) {
				CoffinSleepUtil.clearClientLeaveLock(player);
				CoffinSleepUtil.clearLeaveStand(player);
			}
		}

		private void handleSync(EntityPlayer player, VExtendedPlayer props, VExtendedPlayerPacket message, Side side) {
			int[] receivedData = message.getDataValues();
			String[] receivedStrings = message.getStringValues();
			if (props != null && receivedData != null) {
				PlayerKeys[] keys = PlayerKeys.values();
				int limit = Math.min(keys.length, receivedData.length);
				boolean masteryOwnsEyes = side == Side.SERVER
						&& player.getEntityData().getBoolean("bvHypnosisEyesOwned");

				for (int i = 0; i < limit; i++) {
					// Updating the IEEP data
					if (side == Side.SERVER && (keys[i] == PlayerKeys.LOCATION || keys[i] == PlayerKeys.SECONDSLOT
							|| keys[i] == PlayerKeys.LEAVE_STANDING)) {
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

				// Leave stand coords: server owns; client applies from packet payload.
				if (side == Side.CLIENT) {
					if (message.hasLeaveStand()) {
						props.setLeaveStand(message.getLeaveStandX(), message.getLeaveStandY(), message.getLeaveStandZ());
					} else {
						props.clearLeaveStand();
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
