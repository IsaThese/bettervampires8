package com.mrsand.StatBoostChanges.util;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveThroughVillage;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.village.Village;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;

public class GeneralUtil {

	public static final String BAT_BLIND_STEALTH = "batBlindStealth";
	public static final String BAT_BLIND_FORGET = "batBlindForget";

	public static boolean hasBatBlindStealth(EntityPlayer player) {
		return player != null && player.getEntityData().getInteger(BAT_BLIND_STEALTH) > 0;
	}


	/**
	 * Checks if the blocks between the player and the target (x, z) are passable.
	 */

	/**
	 * Calendar day for ability gates. Null-safe.
	 * Sun-safe biomes (vampire forest / ominous, via {@link VampirismUtil#isInSunSafeBiome})
	 * always count as night so lords can use powers there by day.
	 */
	public static boolean isDay(EntityPlayer player) {
		if (player == null || player.worldObj == null) return false;
		if (Loader.isModLoaded("vampirism") && VampirismUtil.isInSunSafeBiome(player))
			return false;
		long worldTime = player.worldObj.getWorldTime() % 24000L;
		return (worldTime < 12500 || worldTime > 23500);
	}

	//Modified IsInSunlight Ceck by using Brightness than isDayTime
	public static boolean isInLight(EntityLivingBase entity) {
		if (entity == null || entity.worldObj == null) return false;
		World world = entity.worldObj;

		if (Loader.isModLoaded("vampirism") && VampirismUtil.isInSunSafeBiome(entity))
			return false;


		// Dimension & Basic Checks (Make sure these IDs are synced!)
		int dimId = world.provider.dimensionId;
		if (world.provider.hasNoSky || !world.provider.isSurfaceWorld()) return false;

		// Use Sun Brightness instead of isDaytime()
		// 1.0F is full noon, < 0.5F is getting dark/dusk.
		// getSunBrightness(1.0f) handles the sun angle and time of day perfectly.
		if (world.getSunBrightness(1.0F) < 0.5F) return false;

		int x = MathHelper.floor_double(entity.posX);
		int y = MathHelper.floor_double(entity.posY);
		int z = MathHelper.floor_double(entity.posZ);

		// Weather Check: If it's raining, the sun is "blocked"
		if (world.isRaining()) {
			BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
			if (biome.canSpawnLightningBolt()) return false; 
		}

		// Sky Visibility Check
		// We add the entity height to check from the eyes/head
		return world.canBlockSeeTheSky(x, MathHelper.floor_double(entity.posY + entity.getEyeHeight()), z);
	}

	public static void sendDebugMessage(EntityPlayer player, String text) {
		if(player != null)
			player.addChatComponentMessage(new ChatComponentText(text));
	}
	public static void sendDebugMessage(EntityPlayer player, IChatComponent text) {
		if(player != null)
			player.addChatComponentMessage(text);
	}

	public static boolean hasItemInFullInventory(EntityPlayer player, Item item) {
		for (int i = 0; i < 36; i++) {
			ItemStack stack = player.inventory.getStackInSlot(i);
			if (stack != null && stack.getItem() == item) {
				return true;
			}
		}
		return false;

	}

	public static void givePlayerItem(EntityPlayer player, ItemStack item) {
		if (player != null && item != null && item.getItem() != null) {
			if (!player.inventory.addItemStackToInventory(item)) {
				player.dropPlayerItemWithRandomChoice(item, false);
			}
			if (!player.worldObj.isRemote) {
				player.inventoryContainer.detectAndSendChanges();
			}
		}
	}

	public static void villagerDied(EntityPlayer player) {
		WitcheryUtil.villagerDied(player, true);
	}

	// Default radius was 128 blocks
	public static Village getClosestVillage(EntityPlayer player, int radius) {
		Village village = player.worldObj.villageCollectionObj.findNearestVillage(MathHelper.floor_double(player.posX),
				MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ), radius);
		return village;
	}
	public static Village getClosestVillage(Entity entity, int radius) {
		Village village = entity.worldObj.villageCollectionObj.findNearestVillage(MathHelper.floor_double(entity.posX),
				MathHelper.floor_double(entity.posY), MathHelper.floor_double(entity.posZ), radius);
		return village;
	}

	public static Village findNearestVillage(Entity entity) {
		int targetDimension = entity.dimension;
		WorldServer worldServer = MinecraftServer.getServer().worldServerForDimension(targetDimension);
		Village village = worldServer.villageCollectionObj.findNearestVillage(
				MathHelper.floor_double(entity.posX), 
				MathHelper.floor_double(entity.posY), 
				MathHelper.floor_double(entity.posZ), 16*150);

		return village;
	}

	public static Entity getEntityByUUID(World world, String uuid) {
		for (Object entity : world.loadedEntityList) {
			if (entity instanceof Entity && ((Entity) entity).getUniqueID().toString().equals(uuid)) {
				return (Entity) entity;
			}
		}
		return null;
	}

	public static boolean isVampire(EntityPlayer player) {
	    if (Loader.isModLoaded("legends") && LegendsUtil.isVampire(player)) return true;
	    if (Loader.isModLoaded("witchery") && WitcheryUtil.isVampire(player)) return true;
	    if (Loader.isModLoaded("vampirism") && VampirismUtil.isVampire(player)) return true;
	    return false; // Default if no vampire mods match
	}

	public static void removeAvoidanceTasks(EntityLiving creature) {
		// 1. Get the list of all AI tasks the mob currently has
		java.util.Iterator iterator = creature.tasks.taskEntries.iterator();

		while (iterator.hasNext()) {
			Object obj = iterator.next();
			// 2. In 1.7.10, the set contains "EntityAITaskEntry" objects
			net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry entry = (net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry) obj;

			// 3. Check if the ACTUAL AI code is the Avoidance task
			if (entry.action instanceof net.minecraft.entity.ai.EntityAIAvoidEntity || 
					entry.action instanceof net.minecraft.entity.ai.EntityAIPanic) {
				// 4. Delete it from the mob's brain
				iterator.remove();
			}
		}
	}

	public static void restoreZombieTask(EntityZombie mob) {
		mob.getNavigator().setBreakDoors(true);
		mob.tasks.addTask(0, new EntityAISwimming(mob));
		mob.tasks.addTask(2, new EntityAIAttackOnCollide(mob, EntityPlayer.class, 1.0D, false));
		mob.tasks.addTask(4, new EntityAIAttackOnCollide(mob, EntityVillager.class, 1.0D, true));
		mob.tasks.addTask(5, new EntityAIMoveTowardsRestriction(mob, 1.0D));
		mob.tasks.addTask(6, new EntityAIMoveThroughVillage(mob, 1.0D, false));
		mob.tasks.addTask(7, new EntityAIWander(mob, 1.0D));
		mob.tasks.addTask(8, new EntityAIWatchClosest(mob, EntityPlayer.class, 8.0F));
		mob.tasks.addTask(8, new EntityAILookIdle(mob));
		mob.targetTasks.addTask(1, new EntityAIHurtByTarget(mob, true));
		mob.targetTasks.addTask(2, new EntityAINearestAttackableTarget(mob, EntityPlayer.class, 0, true));
		mob.targetTasks.addTask(2, new EntityAINearestAttackableTarget(mob, EntityVillager.class, 0, false));
		// mob.setSize(0.6F, 1.8F);

	}

	public static void restoreSkeletonTask(EntitySkeleton mob) {
		//		mob.tasks.taskEntries.clear();
		//		mob.targetTasks.taskEntries.clear();
		//		mob.tasks.addTask(1, new EntityAISwimming(mob));
		//		mob.tasks.addTask(2, new EntityAIRestrictSun(mob));
		//		mob.tasks.addTask(3, new EntityAIFleeSun(mob, 1.0D));
		//		mob.tasks.addTask(5, new EntityAIWander(mob, 1.0D));
		//		mob.tasks.addTask(6, new EntityAIWatchClosest(mob, EntityPlayer.class, 8.0F));
		//		mob.tasks.addTask(6, new EntityAILookIdle(mob));
		//		mob.targetTasks.addTask(1, new EntityAIHurtByTarget(mob, false));
		//		mob.targetTasks.addTask(2, new EntityAINearestAttackableTarget(mob, EntityPlayer.class, 0, true));
		//		mob.setCombatTask();
		mob.targetTasks.taskEntries.clear();
		mob.targetTasks.addTask(1, new EntityAIHurtByTarget(mob, false));
		mob.targetTasks.addTask(2, new EntityAINearestAttackableTarget(mob, EntityPlayer.class, 0, true));

	}

	public static void restoreGeneralTargetTasks(EntityMob mob) {
		mob.targetTasks.taskEntries.clear();
		if (mob instanceof EntitySkeleton) {
			restoreSkeletonTask((EntitySkeleton) mob);
		} else if (mob instanceof EntityZombie) {
			restoreZombieTask((EntityZombie) mob);
		} else {
			mob.targetTasks.addTask(1, new EntityAIHurtByTarget(mob, false));
			mob.targetTasks.addTask(2, new EntityAINearestAttackableTarget(mob, EntityPlayer.class, 0, true));
		}
	}

	public static void removePlayerAttack(EntityCreature mob) {
		List<EntityAITasks.EntityAITaskEntry> tasks = new ArrayList(mob.targetTasks.taskEntries);
		for (EntityAITasks.EntityAITaskEntry task : tasks) {
			EntityAIBase ai = task.action;
			if (ai instanceof EntityAINearestAttackableTarget || ai instanceof EntityAIHurtByTarget) {
				EntityAINearestAttackableTarget nat = (EntityAINearestAttackableTarget) ai;
				mob.targetTasks.removeTask(nat);
			}
		}
	}

	public static List<Entity> getEntityListFromEntity(Entity entity, Integer radius) {
		List<Entity> entities = entity.worldObj.getEntitiesWithinAABB(EntityLivingBase.class,
				AxisAlignedBB.getBoundingBox(entity.posX - radius, entity.posY - radius, entity.posZ - radius,
						entity.posX + radius, entity.posY + radius, entity.posZ + radius));
		return entities;

	}

	/**
	 * Clears nearby mobs targeting the player and sets a short forget/stealth window
	 * so they do not immediately re-acquire (Summon Bat disengage).
	 */
	public static void disengageFromMobs(EntityPlayer player, int radius, int forgetTicks) {
		if (player == null || player.worldObj == null || player.worldObj.isRemote) return;

		player.getEntityData().setInteger(BAT_BLIND_STEALTH, forgetTicks);

		List<Entity> entities = getEntityListFromEntity(player, radius);
		for (Entity entity : entities) {
			if (!(entity instanceof EntityLiving) || entity instanceof EntityPlayer) continue;
			EntityLiving living = (EntityLiving) entity;
			if (living.getAttackTarget() == player || living.getAITarget() == player
					|| (living instanceof EntityCreature && ((EntityCreature) living).getEntityToAttack() == player)) {
				clearMobAggroOnPlayer(living, player);
				living.getEntityData().setInteger(BAT_BLIND_FORGET, forgetTicks);
			}
		}
	}

	public static void clearMobAggroOnPlayer(EntityLiving living, EntityPlayer player) {
		if (living == null) return;
		boolean targeting = living.getAttackTarget() == player || living.getAITarget() == player;
		if (living instanceof EntityCreature && ((EntityCreature) living).getEntityToAttack() == player) {
			targeting = true;
		}
		if (!targeting) return;

		living.setAttackTarget(null);
		living.setRevengeTarget(null);
		// setTarget / getEntityToAttack live on EntityCreature in 1.7.10, not EntityLiving
		if (living instanceof EntityCreature) {
			((EntityCreature) living).setTarget(null);
		}
		if (living.getNavigator() != null) {
			living.getNavigator().clearPathEntity();
		}
		if (living instanceof EntityCreeper) {
			((EntityCreeper) living).setCreeperState(-1);
		}
	}

	/**
	 * While batBlindStealth is active, keep clearing retargets and tick forget timers down.
	 * @param step ticks to subtract (matches the player tick scan interval)
	 */
	public static void tickBatBlindDisengage(EntityPlayer player, List<Entity> entities, int step) {
		if (player == null) return;
		NBTTagCompound playerNbt = player.getEntityData();
		int stealth = playerNbt.getInteger(BAT_BLIND_STEALTH);
		if (stealth <= 0) return;

		stealth -= step;
		if (stealth <= 0) {
			playerNbt.removeTag(BAT_BLIND_STEALTH);
		} else {
			playerNbt.setInteger(BAT_BLIND_STEALTH, stealth);
		}

		for (Entity entity : entities) {
			if (!(entity instanceof EntityLiving) || entity instanceof EntityPlayer) continue;
			EntityLiving living = (EntityLiving) entity;
			NBTTagCompound nbt = living.getEntityData();
			int forget = nbt.getInteger(BAT_BLIND_FORGET);
			if (forget > 0) {
				forget -= step;
				if (forget <= 0) {
					nbt.removeTag(BAT_BLIND_FORGET);
				} else {
					nbt.setInteger(BAT_BLIND_FORGET, forget);
				}
				clearMobAggroOnPlayer(living, player);
			} else if (stealth > 0) {
				clearMobAggroOnPlayer(living, player);
			}
		}
	}

	/** Force mob/player yaw+pitch to face target (hypnosis channel). */
	public static void forceLookAtEntity(EntityLivingBase mob, EntityLivingBase target) {
		// 1. Calculate the difference in positions

		double diffX = target.posX - mob.posX;
		double diffZ = target.posZ - mob.posZ;
		double diffY;

		// Adjust for eye heights
		if (target instanceof EntityLivingBase) {
			EntityLivingBase entitylivingbase = (EntityLivingBase) target;
			diffY = entitylivingbase.posY + (double) entitylivingbase.getEyeHeight()
			- (mob.posY + (double) mob.getEyeHeight());
		} else {
			diffY = (target.boundingBox.minY + target.boundingBox.maxY) / 2.0D
					- (mob.posY + (double) mob.getEyeHeight());
		}

		// 2. Calculate the Angle (Trigonometry)
		double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
		float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0D / Math.PI) - 90.0F;
		float pitch = (float) (-(Math.atan2(diffY, dist) * 180.0D / Math.PI));

		// 3. Force the rotations
		// We set EVERYTHING to prevent the AI from fighting back
		mob.rotationYaw = yaw;
		mob.rotationYawHead = yaw;
		mob.renderYawOffset = yaw; // Turns the body too
		mob.rotationPitch = pitch;
	}
}
