package com.mrsand.StatBoostChanges.common.management;

import java.util.List;
import java.util.UUID;

import com.emoniph.witchery.util.BlockUtil;
import com.mrsand.StatBoostChanges.StatBoostChanges;
import com.mrsand.StatBoostChanges.common.packets.VExtendedVillagerPacket;
import com.mrsand.StatBoostChanges.util.GeneralUtil;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

public class VExtendedVillager implements IExtendedEntityProperties {

	private final EntityVillager villager;
	private NBTTagCompound data = new NBTTagCompound();
	public static final String EXT_PROP_NAME = "BetterVampires_ExtendedVillager";
	public static final String NBT_PREFIX = "BetterVampires_";

	public VExtendedVillager(EntityVillager villager){
		this.villager = villager;

	}
	public static void register(EntityVillager villager) {
		villager.registerExtendedProperties(EXT_PROP_NAME, new VExtendedVillager(villager));
	}
	@Override
	public void saveNBTData(NBTTagCompound compound) {
		compound.setTag(EXT_PROP_NAME, data.copy());
	}

	@Override
	public void loadNBTData(NBTTagCompound compound) {
		if (compound.hasKey(EXT_PROP_NAME)) {
			this.data = (NBTTagCompound) compound.getTag(EXT_PROP_NAME);
		} 
	}

	@Override
	public void init(Entity entity, World world) {	
	}

	public static NBTTagCompound getVData(EntityVillager villager) {
		VExtendedVillager props = get(villager);
		if (props != null) {
			return props.getData();
		}
		return null;
	}

	public static VExtendedVillager get(EntityVillager villager) {
		return (VExtendedVillager) villager.getExtendedProperties(EXT_PROP_NAME);
	}

	public NBTTagCompound getData() {
		return data;
	}
	public void sync() {
		if(this.villager.worldObj.isRemote)
			StatBoostChanges.network.sendToServer(new VExtendedVillagerPacket(this.villager, get(this.villager)));
	}
	//From Server to Client
	public void syncClient() {
		if(!this.villager.worldObj.isRemote) {
			StatBoostChanges.network.sendToAll(new VExtendedVillagerPacket(this.villager, get(this.villager)));
			//    		StatBoostChanges.network.sendTo(new VExtendedVillagerPacket(this.villager, get(this.villager)), (EntityPlayerMP)this.villager);
		}
	}


	public int getInteger(VillagerKeys key) {
		return (data != null) ? data.getInteger(NBT_PREFIX + key.name()) : 0;
	}

	public void setInteger(VillagerKeys key, int value) {
		if(data != null)
			data.setInteger(NBT_PREFIX + key.name(), value);
	}

	public boolean getBoolean(VillagerKeys key) {
		if(data != null){
			return data.getBoolean(NBT_PREFIX + key.name());
		}
		return false;
	}

	public void setBoolean(VillagerKeys key, boolean value) {		
		if(data != null)
			data.setBoolean(NBT_PREFIX + key.name(), value);  
	}

	public void setCaptor(UUID uuid) {
		if (uuid == null) {
			setInteger(VillagerKeys.CAPTOR_MSB_HI, 0);
			setInteger(VillagerKeys.CAPTOR_MSB_LO, 0);
			setInteger(VillagerKeys.CAPTOR_LSB_HI, 0);
			setInteger(VillagerKeys.CAPTOR_LSB_LO, 0);
			return;
		}

		long msb = uuid.getMostSignificantBits();
		long lsb = uuid.getLeastSignificantBits();

		// Slicing the longs into integers
		setInteger(VillagerKeys.CAPTOR_MSB_HI, (int) (msb >> 32));
		setInteger(VillagerKeys.CAPTOR_MSB_LO, (int) (msb & 0xFFFFFFFFL));
		setInteger(VillagerKeys.CAPTOR_LSB_HI, (int) (lsb >> 32));
		setInteger(VillagerKeys.CAPTOR_LSB_LO, (int) (lsb & 0xFFFFFFFFL));
	}

	public EntityPlayer getCaptor() {
		long msb = ((long) getInteger(VillagerKeys.CAPTOR_MSB_HI) << 32) | (getInteger(VillagerKeys.CAPTOR_MSB_LO) & 0xFFFFFFFFL);
		long lsb = ((long) getInteger(VillagerKeys.CAPTOR_LSB_HI) << 32) | (getInteger(VillagerKeys.CAPTOR_LSB_LO) & 0xFFFFFFFFL);

		if (msb == 0 && lsb == 0) return null;

		UUID uuid = new UUID(msb, lsb);

//		if (uuid == null) return null;

		// In 1.7.10, playerEntities is a List of all players in this specific world/dimension
		for (Object obj : villager.worldObj.playerEntities) {
			EntityPlayer player = (EntityPlayer) obj;
			if (uuid.equals(player.getUniqueID())) {
				return player;
			}
		}
		return null;
	}

	public void checkCageStatus() {
		VExtendedVillager props = get(villager);
		World world = villager.worldObj;

		int[] anchor = VExtendedVillager.getCageAnchor(villager);
//		boolean currentlyCaged = WitcheryUtil.villagerIsInCage(villager);
		boolean currentlyCaged = (anchor != null);

		if (currentlyCaged && !getBoolean(VillagerKeys.IS_CAGED)) {
			setBoolean(VillagerKeys.IS_CAGED, true);

			setBoolean(VillagerKeys.IS_CAGED, true);
			setInteger(VillagerKeys.CAGE_X, anchor[0]);
			setInteger(VillagerKeys.CAGE_Y, anchor[1]);
			setInteger(VillagerKeys.CAGE_Z, anchor[2]);
			setInteger(VillagerKeys.CAGE_DIM, world.provider.dimensionId);
			villager.setPositionAndUpdate(anchor[0] + 0.5D, anchor[1], anchor[2] + 0.5D);

			EntityPlayer captor = world.getClosestPlayerToEntity(villager, 8.0D);
			if (captor != null) {
				setCaptor(captor.getUniqueID());
				if(GeneralUtil.getClosestVillage(villager, 128) != null) {
					GeneralUtil.sendDebugMessage(captor, new  ChatComponentText("A risky spot for a prisoner.").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
				}
				else {
					GeneralUtil.sendDebugMessage(captor, new  ChatComponentText("A new subject has been secured in your estate.").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
				}
			}
			

			
			syncClient();
		} 

		else if (!currentlyCaged && getBoolean(VillagerKeys.IS_CAGED)) {
			setBoolean(VillagerKeys.IS_CAGED, false);
			setInteger(VillagerKeys.LAST_UPDATE_TIME, 0);

			// Clearing the bits (Pass null to reset all 4 integer slots to 0)
			setCaptor(null);

			syncClient();
		}
		if (getBoolean(VillagerKeys.IS_CAGED)) {
		    double centerX = getInteger(VillagerKeys.CAGE_X) + 0.5D;
		    double centerZ = getInteger(VillagerKeys.CAGE_Z) + 0.5D;

		    if (villager.getDistanceSq(centerX, villager.posY, centerZ) > 0.01D) {
		        villager.setPosition(centerX, villager.posY, centerZ);
		    }
		}
	}
	public static boolean isAnchorOccupied(World world, int x, int y, int z, EntityVillager exclusion) {
	    // Expand a bounding box slightly around the block to catch any villager standing in it
	    AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 2, z + 1);
	    List targets = world.getEntitiesWithinAABB(EntityVillager.class, box);

	    for (Object obj : targets) {
	        EntityVillager other = (EntityVillager) obj;
	        
	        // Skip checking the villager who is currently looking for a cage
	        if (other == exclusion) continue;

	        VExtendedVillager props = get(other);
	        if (props != null && props.getBoolean(VillagerKeys.IS_CAGED)) {
	            // Check if their saved cage coordinates match this exact spot
	            if (props.getInteger(VillagerKeys.CAGE_X) == x &&
	                props.getInteger(VillagerKeys.CAGE_Y) == y &&
	                props.getInteger(VillagerKeys.CAGE_Z) == z) {
	                return true; // Someone is already officially locked in here!
	            }
	        }
	    }
	    return false;
	}
	
	
	public void liberateVillager() {

		if (getBoolean(VillagerKeys.IS_CAGED)) {
			int x = getInteger(VillagerKeys.CAGE_X);
			int y = getInteger(VillagerKeys.CAGE_Y);
			int z = getInteger(VillagerKeys.CAGE_Z);
			int dim = getInteger(VillagerKeys.CAGE_DIM);

			// Ensure we are in the right dimension before destroying blocks
			if (villager.worldObj.provider.dimensionId == dim) {
				VExtendedVillager.destroyCageBlocks(villager.worldObj, x, y, z);

				// Log the 'Civil Breach'
				setBoolean(VillagerKeys.IS_CAGED, false);
//				GeneralUtil.sendDebugMessage(getCaptor(), "Your prisoner was set free");
				setCaptor(null);
				syncClient();
			}
		}
	}
	public boolean isCaged(EntityVillager villager) {
		return getBoolean(VillagerKeys.IS_CAGED);
	}


	public static boolean villagerIsInCage(EntityVillager target) {
		return isCaged(target.worldObj, 
				MathHelper.floor_double(target.posX), 
				MathHelper.floor_double(target.posY), 
				MathHelper.floor_double(target.posZ));
	}
	
	//Has witchery code need to change
	public static boolean isCaged(World world, int x, int y, int z) {
		int count = 0;
		Block bars = Blocks.iron_bars;
		count += (world.getBlock(x + 1, y, z) == bars) ? 1 : 0;
		count += (world.getBlock(x, y, z + 1) == bars) ? 1 : 0;
		count += (world.getBlock(x - 1, y, z) == bars) ? 1 : 0;
		count += (world.getBlock(x, y, z - 1) == bars) ? 1 : 0;
		count += (world.getBlock(x + 1, y, z + 1) == bars) ? 1 : 0;
		count += (world.getBlock(x - 1, y, z + 1) == bars) ? 1 : 0;
		count += (world.getBlock(x + 1, y, z - 1) == bars) ? 1 : 0;
		count += (world.getBlock(x - 1, y, z - 1) == bars) ? 1 : 0;
		y++;
		count += (world.getBlock(x + 1, y, z) == bars) ? 1 : 0;
		count += (world.getBlock(x, y, z + 1) == bars) ? 1 : 0;
		count += (world.getBlock(x - 1, y, z) == bars) ? 1 : 0;
		count += (world.getBlock(x, y, z - 1) == bars) ? 1 : 0;
		count += (world.getBlock(x + 1, y, z + 1) == bars) ? 1 : 0;
		count += (world.getBlock(x - 1, y, z + 1) == bars) ? 1 : 0;
		count += (world.getBlock(x + 1, y, z - 1) == bars) ? 1 : 0;
		count += (world.getBlock(x - 1, y, z - 1) == bars) ? 1 : 0;
		if (count < 15)
			return false;
		count = 0;
		y++;
		count += !BlockUtil.isReplaceableBlock(world, x + 1, y, z) ? 1 : 0;
		count += !BlockUtil.isReplaceableBlock(world, x, y, z + 1) ? 1 : 0;
		count += !BlockUtil.isReplaceableBlock(world, x - 1, y, z) ? 1 : 0;
		count += !BlockUtil.isReplaceableBlock(world, x, y, z - 1) ? 1 : 0;
		count += !BlockUtil.isReplaceableBlock(world, x + 1, y, z + 1) ? 1 : 0;
		count += !BlockUtil.isReplaceableBlock(world, x - 1, y, z + 1) ? 1 : 0;
		count += !BlockUtil.isReplaceableBlock(world, x + 1, y, z - 1) ? 1 : 0;
		count += !BlockUtil.isReplaceableBlock(world, x - 1, y, z - 1) ? 1 : 0;
		count += !BlockUtil.isReplaceableBlock(world, x, y, z) ? 1 : 0;
		if (count < 9)
			return false;
		return true;
	}
	public static int[] getCageAnchor(EntityVillager target) {
	    World world = target.worldObj;
	    double vx = target.posX;
	    double vy = target.posY;
	    double vz = target.posZ;
	
	    int ogX = MathHelper.floor_double(vx);
	    int ogY = MathHelper.floor_double(vy);
	    int ogZ = MathHelper.floor_double(vz);
	
	    int[] bestAnchor = null;
	    double closestDistSq = Double.MAX_VALUE;
	
	    // Search the 3x3 area around the villager's feet
	    for (int x = ogX - 1; x <= ogX + 1; x++) {
	        for (int z = ogZ - 1; z <= ogZ + 1; z++) {
	            if (isCaged(world, x, ogY, z)) {
	                // Calculate distance to the center of this potential cage
	            	if (isAnchorOccupied(world, x, ogY, z, target)) {
                        continue; // Skip it, this cage is full!
                    }
	            	
	                double centerX = x + 0.5D;
	                double centerZ = z + 0.5D;
	                double dx = vx - centerX;
	                double dz = vz - centerZ;
	                double distSq = dx * dx + dz * dz;
	
	                // No strict threshold—just find the closest valid cage in the 3x3 grid
	                if (distSq < closestDistSq) {
	                    closestDistSq = distSq;
	                    bestAnchor = new int[] { x, ogY, z };
	                }
	            }
	        }
	    }
	    return bestAnchor;
	}
	public static void destroyCageBlocks(World world, int x, int y, int z) {
		// 1. Remove the Iron Bars (Layers Y and Y+1) with drops
		for (int layer = 0; layer <= 1; layer++) {
			int currentY = y + layer;
	
			// Loop through the 3x3 footprint
			for (int offX = -1; offX <= 1; offX++) {
				for (int offZ = -1; offZ <= 1; offZ++) {
					// Skip the center where the villager is standing
					if (offX == 0 && offZ == 0) continue;
	
					// Parameters: (x, y, z, dropItems)
					// Setting the last parameter to true forces the block to drop its items
					world.func_147480_a(x + offX, currentY, z + offZ, true); //func_147480_a destroyBlock
				}
			}
		}
	
		// 2. Remove the Roof (Layer Y+2) with drops
		int roofY = y + 2;
		for (int offX = -1; offX <= 1; offX++) {
			for (int offZ = -1; offZ <= 1; offZ++) {
				// This will drop whatever the roof was made of (Stone, Wood, etc.)
				world.func_147480_a(x + offX, roofY, z + offZ, true);
			}
		}
	
		// You can still keep the sound effect for extra impact
		world.playAuxSFX(2001, x, y, z, Block.getIdFromBlock(Blocks.iron_bars));
	}


	public enum VillagerKeys{
		CAPTOR, IS_CAGED, 
		CAPTOR_MSB_HI, // Part 1
		CAPTOR_MSB_LO, // Part 2
		CAPTOR_LSB_HI, // Part 3
		CAPTOR_LSB_LO  // Part 4
		, CAGE_X, CAGE_Y, CAGE_Z, CAGE_DIM,
		TIMER_REPLINISH, LAST_UPDATE_TIME, LAST_SLEEP_CHECK
		

	}



}
