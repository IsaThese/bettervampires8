package com.mrsand.StatBoostChanges.common.management;

import com.mrsand.StatBoostChanges.StatBoostChanges;
import com.mrsand.StatBoostChanges.common.packets.VExtendedPlayerPacket;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

public class VExtendedPlayer implements IExtendedEntityProperties {
	public static final String EXT_PROP_NAME = "BetterVampires_ExtendedPlayer";
	private final EntityPlayer player;
	private NBTTagCompound data = new NBTTagCompound();

	@SideOnly(Side.CLIENT)
	private ResourceLocation originalSkin;
	public static final String NBT_PREFIX = "BetterVampires_";

	@SideOnly(Side.CLIENT)
	public ResourceLocation getOriginalSkin() {
		return originalSkin;
	}

	@SideOnly(Side.CLIENT)
	public void setOriginalSkin(ResourceLocation skin) {
		this.originalSkin = skin;
	}

	public VExtendedPlayer(EntityPlayer player) {
		this.player = player;
	}

	public static void register(EntityPlayer player) {
		player.registerExtendedProperties(EXT_PROP_NAME, new VExtendedPlayer(player));
	}

	public static VExtendedPlayer get(EntityPlayer player) {
		return (VExtendedPlayer) player.getExtendedProperties(EXT_PROP_NAME);
	}

	@Override
	public void saveNBTData(NBTTagCompound compound) {
		migrateCorruptedBooleanKeys();
		compound.setTag(EXT_PROP_NAME, data.copy());
	}

	@Override
	public void loadNBTData(NBTTagCompound compound) {
		if (compound.hasKey(EXT_PROP_NAME)) {
			this.data = (NBTTagCompound) compound.getTag(EXT_PROP_NAME);
		}
		migrateCorruptedBooleanKeys();
	}

	@Override
	public void init(Entity entity, World world) {}

	public NBTTagCompound getData() {
		return data;
	}

	private static final String LEAVE_STAND_TAG = NBT_PREFIX + "LeaveStand";

	/** Keys that must stay NBT booleans (old packet sync rewrote them as ints). */
	public static boolean isBooleanKey(PlayerKeys key) {
		switch (key) {
		case IS_STARVING:
		case HAS_STARVED:
		case IS_VULNERABLE:
		case HYPNOTIZING:
		case WALL_CLIMB:
		case OTHERFORM:
		case UNLOCKED_RESTORED_POWERS:
		case LEAVE_STANDING:
			return true;
		default:
			return false;
		}
	}

	/** Rewrite int-typed boolean keys back to NBTTagByte (old-world corruption). */
	public void migrateCorruptedBooleanKeys() {
		if (data == null) return;
		for (PlayerKeys key : PlayerKeys.values()) {
			if (!isBooleanKey(key)) continue;
			String name = NBT_PREFIX + key.name();
			if (!data.hasKey(name)) continue;
			NBTBase tag = data.getTag(name);
			if (tag instanceof NBTTagByte) continue;
			boolean value = false;
			if (tag instanceof NBTTagInt) {
				value = data.getInteger(name) != 0;
			} else {
				value = data.getInteger(name) != 0;
			}
			data.setBoolean(name, value);
		}
	}

	/**
	 * A packet from the client sending to the server, sends all arrays and keys of extended vampire properties
	 */
	public void sync() {
		if(this.player.worldObj.isRemote)
			StatBoostChanges.network.sendToServer(new VExtendedPlayerPacket(this.player, get(this.player)));
	}
	/**
	 * A packet from the server sending to the client, sends all arrays and keys of extended vampire properties
	 */
	 
	public void syncClient() {
		if(!this.player.worldObj.isRemote)
			StatBoostChanges.network.sendToAll(new VExtendedPlayerPacket(this.player, get(this.player)));
	}


	public int getInteger(com.mrsand.StatBoostChanges.common.management.PlayerKeys key) {
		return (data != null) ? data.getInteger(NBT_PREFIX + key.name()) : 0;
	}

	public void setInteger(com.mrsand.StatBoostChanges.common.management.PlayerKeys key, int value) {
		if(data != null)
			data.setInteger(NBT_PREFIX + key.name(), value);
	}
	public String getString(StringPlayerKeys key) {
	    return (data != null) ? data.getString(NBT_PREFIX + key.name()) : "";
	}

	public void setString(StringPlayerKeys key, String value) {
	    if (data != null) {
	        data.setString(NBT_PREFIX + key.name(), value);
	    }
	}

	public boolean getBoolean(com.mrsand.StatBoostChanges.common.management.PlayerKeys key) {
		if (data == null) return false;
		String name = NBT_PREFIX + key.name();
		if (!data.hasKey(name)) return false;
		NBTBase tag = data.getTag(name);
		// Corrupted saves / old sync stored these as ints — getBoolean alone always returns false
		if (tag instanceof NBTTagInt) {
			return data.getInteger(name) != 0;
		}
		return data.getBoolean(name);
	}

	public void setBoolean(PlayerKeys key, boolean value) {		
		if(data != null)
			data.setBoolean(NBT_PREFIX + key.name(), value);  
	}

	public static NBTTagCompound getVData(EntityPlayer player) {
		VExtendedPlayer props = get(player);
		if (props != null) {
			return props.getData();
		}
		return null;
	}

	/** Server leave-stand block (feet air). Cleared when settle ends / walk-away. */
	public void setLeaveStand(int x, int y, int z) {
		if (this.data == null) return;
		NBTTagCompound stand = new NBTTagCompound();
		stand.setInteger("x", x);
		stand.setInteger("y", y);
		stand.setInteger("z", z);
		this.data.setTag(LEAVE_STAND_TAG, stand);
		setBoolean(PlayerKeys.LEAVE_STANDING, true);
	}

	public ChunkCoordinates getLeaveStand() {
		if (this.data == null || !this.data.hasKey(LEAVE_STAND_TAG)) return null;
		NBTTagCompound stand = this.data.getCompoundTag(LEAVE_STAND_TAG);
		return new ChunkCoordinates(stand.getInteger("x"), stand.getInteger("y"), stand.getInteger("z"));
	}

	public void clearLeaveStand() {
		if (this.data == null) return;
		this.data.removeTag(LEAVE_STAND_TAG);
		setBoolean(PlayerKeys.LEAVE_STANDING, false);
	}

	public void setTeleportSpot(int slot, int x, int y, int z, int dimension) {
		NBTTagCompound spotTag = new NBTTagCompound();
		spotTag.setInteger("x", x);
		spotTag.setInteger("y", y);
		spotTag.setInteger("z", z);
		spotTag.setInteger("dim", dimension);

		this.data.setTag(NBT_PREFIX + "Teleport_" + slot, spotTag);
	}

	public ChunkCoordinates getTeleportSpot(int slot) {
		String tagName = NBT_PREFIX + "Teleport_" + slot;
		if (this.data.hasKey(tagName)) {
			NBTTagCompound spotTag = this.data.getCompoundTag(tagName);
			// This is how you create a new instance of ChunkCoordinates
			return new ChunkCoordinates(
					spotTag.getInteger("x"),
					spotTag.getInteger("y"),
					spotTag.getInteger("z")
					);
		}
		return null;
	}
	public int getTeleportDimension(int slot) {
		String tagName = NBT_PREFIX + "Teleport_" + slot;
		if (this.data.hasKey(tagName)) {
			NBTTagCompound spotTag = this.data.getCompoundTag(tagName);
			// This is how you create a new instance of ChunkCoordinates
			return spotTag.getInteger("dim");
		}
		return 0;
	}

	public void teleportToSpot(int slot, EntityPlayerMP player) {
		NBTTagCompound spotTag = this.data.getCompoundTag(NBT_PREFIX + "Teleport_" + slot);
		if (spotTag == null) return;
		NBTTagCompound nbt = player.getEntityData();
        if(nbt.getInteger("isInCombat") == 1) {
			nbt.getInteger("isInCombatTimer");
			GeneralUtil.sendDebugMessage(player, new ChatComponentText("Can't teleport in combat, timer: " + Math.round(nbt.getInteger("isInCombatTimer")/20) + "s").setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
			return;
		}

		int x = spotTag.getInteger("x");
		int y = spotTag.getInteger("y");
		int z = spotTag.getInteger("z");
		int targetDim = spotTag.getInteger("dim");
		if(!WitcheryUtil.teleportToLocationSafely(player.worldObj, x + 0.5, y, z + 0.5, targetDim, player, true)) {

			if (player.dimension != targetDim) {
				player.mcServer.getConfigurationManager().transferPlayerToDimension(player, targetDim);
			}
			player.setPositionAndUpdate(x + 0.5, y, z + 0.5);
		}
	}

}

