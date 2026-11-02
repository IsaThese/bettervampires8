package com.mrsand.StatBoostChanges.util;

import java.util.List;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.brewing.potions.PotionEnslaved;
import com.emoniph.witchery.common.ExtendedPlayer;
import com.emoniph.witchery.common.ExtendedPlayer.VampireUltimate;
import com.emoniph.witchery.common.ExtendedVillager;
import com.emoniph.witchery.entity.EntityVillageGuard;
import com.emoniph.witchery.entity.EntityVillagerWere;
import com.emoniph.witchery.entity.EntityWitchHunter;
import com.emoniph.witchery.infusion.InfusedBrewEffect;
import com.emoniph.witchery.infusion.Infusion;
import com.emoniph.witchery.infusion.infusions.InfusionOtherwhere;
import com.emoniph.witchery.item.ItemGeneral;
import com.emoniph.witchery.item.ItemVampireClothes;
import com.emoniph.witchery.network.PacketParticles;
import com.emoniph.witchery.network.PacketSound;
import com.emoniph.witchery.util.BlockSide;
import com.emoniph.witchery.util.BlockUtil;
import com.emoniph.witchery.util.ChatUtil;
import com.emoniph.witchery.util.Config;
import com.emoniph.witchery.util.CreatureUtil;
import com.emoniph.witchery.util.EntityUtil;
import com.emoniph.witchery.util.ParticleEffect;
import com.emoniph.witchery.util.SoundEffect;
import com.emoniph.witchery.util.TargetPointUtil;
import com.emoniph.witchery.util.TimeUtil;
import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.tihyo.legends.legacy.LegacyHelper;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.village.Village;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

public class WitcheryUtil {

	public static ItemStack goblet = new ItemStack(Witchery.Items.BLOOD_GOBLET);
	private static final long HUNTER_DELAY = TimeUtil.minsToTicks(2);

	public static boolean isParalyzedActive(EntityLivingBase entity) {
		return entity.isPotionActive(Witchery.Potions.PARALYSED);
	}
	public static boolean isVampire(EntityPlayer player) {
		return ExtendedPlayer.get(player).isVampire();
	}

	public static int getBlood(EntityVillager villager) {
		return ExtendedVillager.get(villager).getBlood();
	}

	public static boolean canHypnotizeVictim(Entity target) {
		return (target instanceof EntityVillageGuard || (target instanceof EntityVillager && !(target instanceof EntityVillagerWere)));
	}
	public static void theyComeNoise(EntityPlayer player) {
		Witchery.packetPipeline.sendTo(
				(IMessage) new PacketSound(SoundEffect.WITCHERY_RANDOM_THEYCOME, (Entity) player, 1.0F, 1.0F),
				player);
	}

	public static void decreaseVampireLevel(EntityPlayer player) {
		ExtendedPlayer self = ExtendedPlayer.get(player);
		if (self.getVampireLevel() > 1) {
			self.setVampireLevel(self.getVampireLevel() - 1);
			if (!player.worldObj.isRemote) {
				ChatUtil.sendTranslated(EnumChatFormatting.GOLD, (ICommandSender) player, "You become weaker",
						new Object[0]);
				SoundEffect.MOB_ENDERDRAGON_HIT.playOnlyTo(player);
			}
		}
	}
	
	
	public static boolean isInSunlight(EntityLivingBase entity) {
		return CreatureUtil.isInSunlight(entity);
	}

	/** True if all four armor slots are Witchery {@link ItemVampireClothes}. */
	public static boolean isWearingFullVampireClothes(EntityLivingBase entity) {
		if (entity == null) return false;
		ItemStack[] armor = entity instanceof EntityPlayer
				? ((EntityPlayer) entity).inventory.armorInventory
				: null;
		if (armor == null) {
			for (int slot = 1; slot <= 4; slot++) {
				ItemStack stack = entity.getEquipmentInSlot(slot);
				if (stack == null || !(stack.getItem() instanceof ItemVampireClothes)) {
					return false;
				}
			}
			return true;
		}
		for (int i = 0; i < 4; i++) {
			ItemStack stack = armor[i];
			if (stack == null || !(stack.getItem() instanceof ItemVampireClothes)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Full sun immunity: Vampire Lord and a complete Witchery vampire clothes set.
	 */
	public static boolean hasFullSunImmunity(EntityPlayer player) {
		if (player == null || !Loader.isModLoaded("vampirism")) return false;
		if (!de.teamlapen.vampirism.entity.player.VampirePlayer.get(player).isVampireLord()) {
			return false;
		}
		return isWearingFullVampireClothes(player);
	}

	/**
	 * Soft incoming-damage cap eligibility: Vampire Lord + full Witchery vampire clothes.
	 * Form-agnostic (human and Vampire Beast use the same curve).
	 */
	public static boolean hasVampireLordClothesCap(EntityPlayer player) {
		return hasFullSunImmunity(player);
	}

	public static boolean teleportToLocationSafely(World world, double x, double y, double z, int targetDim, Entity entity, boolean presetPosition) {
		return ItemGeneral.teleportToLocationSafely(world, x + 0.5, y, z + 0.5, targetDim, entity, presetPosition);
	}
	public static boolean isGuard(Entity entity) {
		return (entity instanceof EntityVillageGuard);
	}

	public static int[] findNavigableSpotNearCage(World world, int cX, int cY, int cZ) {
		// We check a 3x3 area around the cage (radius of 2)
		// We also check 1 block up/down in case the cage is on a slope
		for (int x = cX - 2; x <= cX + 2; x++) {
			for (int z = cZ - 2; z <= cZ + 2; z++) {
				for (int y = cY - 1; y <= cY + 1; y++) {

					// 1. Is this the center? Skip it (that's the barred area)
					if (x == cX && z == cZ) continue;

					// 2. Structural Check: Solid floor + 2 blocks of air (clearance)
					if (!BlockUtil.isReplaceableBlock(world, x, y, z)      // Solid Floor
							&& BlockUtil.isReplaceableBlock(world, x, y + 1, z) // Headspace (Feet)
							&& BlockUtil.isReplaceableBlock(world, x, y + 2, z)) // Headspace (Head)
					{
						// Return this as the "Inspection Point"
						return new int[] { x, y + 1, z };
					}
				}
			}
		}
		return null; // No suitable ground found
	}

	//Should work for all mods
	public static void increaseLegendsVampire(EntityPlayer player, int amount) {
		ExtendedPlayer ePlayer = ExtendedPlayer.get(player);
		ItemStack vampireBook =  new ItemStack(Witchery.Items.VAMPIRE_BOOK);
		ItemStack cane =  new ItemStack(Witchery.Items.CANE_SWORD);
		int level = ePlayer.getVampireLevel();
		if(level == 0) {
			if(!GeneralUtil.hasItemInFullInventory(player, vampireBook.getItem()))
				GeneralUtil.givePlayerItem(player, vampireBook);
			if(!GeneralUtil.hasItemInFullInventory(player, Witchery.Items.COFFIN))
				GeneralUtil.givePlayerItem(player,  new ItemStack(Witchery.Items.COFFIN));
		}
		if (level < 10) {
			ePlayer.setVampireLevel(Math.min(level + amount, 10));
			if (!player.worldObj.isRemote) {
				ChatUtil.sendTranslated(EnumChatFormatting.GOLD, (ICommandSender)player, "Your thirst grows stronger!", new Object[0]);
				SoundEffect.RANDOM_LEVELUP.playOnlyTo(player);
			} 
		}
		int newLevel = ePlayer.getVampireLevel();
		//Give cane at level 10
		if(newLevel == 10 && level != 10) {
			if(!GeneralUtil.hasItemInFullInventory(player, cane.getItem()))
				GeneralUtil.givePlayerItem(player, cane);
		}
		//Happens at level 9
		if (newLevel > 8 && level < 9 && !WitcheryUtil.hasWitcheryGobletInFullInventory(player)) {
			if(WitcheryUtil.goblet != null)
				GeneralUtil.givePlayerItem(player, WitcheryUtil.goblet);
		}
		LegendsUtil.increaseVampireLevel(player, newLevel);
		VampirismUtil.increaseVampireLevel(player, newLevel);
	}

	public static void increaseLegendsVampire(EntityPlayer player) {
		increaseLegendsVampire(player, 1);
	}

	public static boolean decreaseBlood(EntityPlayer player, int cost, float percentage, boolean specific) {
		ExtendedPlayer ePlayer = ExtendedPlayer.get(player);
		if (ePlayer == null) return false;
		int costFromPercentage = (int) (ePlayer.getMaxBloodPower() * percentage);
		int finalCost = Math.max(cost, costFromPercentage);

		return ePlayer.decreaseBloodPower(finalCost, specific);
	}

	/**
	 * Start Mesmerize gaze channel (no blood cost). Safe on client or server:
	 * client syncs to server; server syncs back to tracking clients.
	 */
	public static void beginHypnosisChannel(EntityPlayer player) {
		if (player == null || player.worldObj == null) return;
		VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
		if (vPlayer == null) return;
		boolean was = vPlayer.getBoolean(PlayerKeys.HYPNOTIZING);
		if (!was) {
			vPlayer.setBoolean(PlayerKeys.HYPNOTIZING, true);
			// changeForm may syncClient when OTHERFORM flips; always one authoritative bag sync after.
			VisualUtil.changeForm(player, true);
			if (player.worldObj.isRemote) {
				vPlayer.sync();
			} else {
				vPlayer.syncClient();
			}
		}
		if (HypnosisDebug.enabled()) {
			HypnosisDebug.logChannel(player, "begin", was, vPlayer.getBoolean(PlayerKeys.HYPNOTIZING));
		}
	}

	/** End Mesmerize gaze channel (no blood cost). */
	public static void endHypnosisChannel(EntityPlayer player) {
		if (player == null || player.worldObj == null) return;
		VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
		if (vPlayer == null) return;
		boolean was = vPlayer.getBoolean(PlayerKeys.HYPNOTIZING);
		if (was) {
			vPlayer.setBoolean(PlayerKeys.HYPNOTIZING, false);
			if (!vPlayer.getBoolean(PlayerKeys.IS_STARVING)) {
				VisualUtil.changeForm(player, false);
			}
			if (player.worldObj.isRemote) {
				vPlayer.sync();
			} else {
				vPlayer.syncClient();
			}
		}
		if (HypnosisDebug.enabled()) {
			HypnosisDebug.logChannel(player, "end", was, vPlayer.getBoolean(PlayerKeys.HYPNOTIZING));
		}
	}

	/**
	 * Blood cost for a successful stare only. Call at attempt threshold, never on channel start.
	 * @return true if blood was paid
	 */
	public static boolean tryHypnosisBloodCost(EntityPlayer player) {
		if (player == null) return false;
		if (decreaseBlood(player, 0, 0.10F, true)) {
			return true;
		}
		if (!player.worldObj.isRemote) {
			player.addChatMessage(new ChatComponentText("Not enough blood to bind their will")
					.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED).setBold(true)));
		}
		return false;
	}

	public static void resetHypnosisAttempts(EntityLivingBase target) {
		if (target != null) {
			target.getEntityData().removeTag("VampireHypnosisAttempts");
		}
	}
	public static void decreaseBloodToCap(EntityPlayer player, int cost, float percentage, float cap) {
		ExtendedPlayer ePlayer = ExtendedPlayer.get(player);
		if (ePlayer == null) return;
		int maxBlood = ePlayer.getMaxBloodPower();
		int costFromPercentage = (int) (maxBlood * percentage);
		int finalCost = Math.max(cost, costFromPercentage);
		
		ePlayer.setBloodPower(Math.max(ePlayer.getBloodPower() - finalCost, (int)(maxBlood *cap)) );
		
//		return ePlayer.decreaseBloodPower(finalCost, specific);
	}
	public static void increaseBlood(EntityPlayer player, int cost, float percentage) {
		ExtendedPlayer ePlayer = ExtendedPlayer.get(player);
		if (ePlayer == null) return;
		int costFromPercentage = (int) (ePlayer.getMaxBloodPower() * percentage);
		int finalCost = Math.max(cost, costFromPercentage);

		ePlayer.increaseBloodPower(finalCost); //This was set to 0 lol. because of setBlood changes
	}
	public static void drinkParticles(EntityLivingBase entity, EntityLivingBase target ) {
		ParticleEffect.REDDUST.send(SoundEffect.WITCHERY_RANDOM_DRINK, entity.worldObj, target.posX, target.posY + target.height * 0.8D, target.posZ, 0.5D, 0.2D, 16);
	}


	public static boolean isEnslavedBy(EntityPlayer player, EntityLiving living) {
		return PotionEnslaved.isMobEnslavedBy(living, player);
	}

	public static boolean setEnslaverForMob(EntityLiving entity, EntityPlayer player) {
		return setEnslaverForMob(entity, player, 2147483647);
	}

	public static boolean setEnslaverForMob(EntityLiving entity, EntityPlayer player, int duration) {
		if (entity == null || player == null)
			return false;
		if(player.worldObj.isRemote)
			return false;

		String enslaverName = entity.getEntityData().getString("WITCEnslaverName");
		boolean isEnslaved = (enslaverName != null && !enslaverName.isEmpty());

		if (!isEnslaved || !player.getCommandSenderName().equals(enslaverName)) {
			NBTTagCompound data = entity.getEntityData();
			data.setString("WITCEnslaverName", player.getCommandSenderName());
			data.setBoolean("tranced", true);
			data.setInteger("trancedTimer", duration/5);
			data.removeTag("angry");	
			data.setString("vampirePlayerUUID", player.getUniqueID().toString());
			if(!player.worldObj.isRemote)
				entity.addPotionEffect(new PotionEffect(Witchery.Potions.ENSLAVED.id, duration, 0));
			SoundEffect.WITCHERY_RANDOM_HYPNOSIS.playAtPlayer(player.worldObj, player, 0.5F, 1.0F);
			EntityUtil.dropAttackTarget(entity);
			return true;
		}
		return false;
	}

	/** NBT key for DIY thrall controller (Mind Control–style, no Wizardry/ENSLAVED dependency). */
	public static final String THRALL_CONTROLLER_UUID = "bvControllerUUID";

	/** Soft upward float while hypnotizing — does not wipe horizontal motion. */
	public static void applyChannelFloat(EntityLivingBase target) {
		if (target == null) return;
		target.fallDistance = 0.0F;
		if (target.motionY < 0.15D) {
			target.motionY += 0.08D;
		}
	}

	/**
	 * DIY thrall: loyalty via bvControllerUUID + tranced timer. Combat AI does not rely on PotionEnslaved.
	 */
	public static boolean startThrall(EntityLiving entity, EntityPlayer player, int durationTicks) {
		if (entity == null || player == null || player.worldObj.isRemote) return false;

		NBTTagCompound data = entity.getEntityData();
		data.setString(THRALL_CONTROLLER_UUID, player.getUniqueID().toString());
		data.setString("vampirePlayerUUID", player.getUniqueID().toString());
		data.setBoolean("tranced", true);
		data.setInteger("trancedTimer", Math.max(1, durationTicks / 5));
		data.removeTag("angry");

		entity.setAttackTarget(null);
		entity.setRevengeTarget(null);
		if (entity instanceof EntityCreature) {
			((EntityCreature) entity).setTarget(null);
		}
		if (entity.getNavigator() != null) {
			entity.getNavigator().clearPathEntity();
		}

		// Do not call setControlling here — cane sword owns that combat/follow flag and its chat.
		SoundEffect.WITCHERY_RANDOM_HYPNOSIS.playAtPlayer(player.worldObj, player, 0.5F, 1.0F);
		return true;
	}

	public static void clearThrall(EntityLiving entity) {
		if (entity == null) return;
		NBTTagCompound nbt = entity.getEntityData();
		nbt.removeTag(THRALL_CONTROLLER_UUID);
		nbt.removeTag("tranced");
		nbt.removeTag("trancedTimer");
		nbt.removeTag("angry");
		nbt.removeTag("vampirePlayerUUID");
		nbt.removeTag("WITCEnslaverName");
		if (entity.isPotionActive(Witchery.Potions.ENSLAVED)) {
			entity.removePotionEffect(Witchery.Potions.ENSLAVED.id);
		}
	}

	public static boolean isThralled(EntityLiving entity) {
		if (entity == null) return false;
		NBTTagCompound nbt = entity.getEntityData();
		return nbt.getBoolean("tranced") && nbt.hasKey(THRALL_CONTROLLER_UUID)
				&& !nbt.getString(THRALL_CONTROLLER_UUID).isEmpty();
	}

	public static boolean isThralledBy(EntityLiving entity, EntityPlayer player) {
		if (!isThralled(entity) || player == null) return false;
		return player.getUniqueID().toString().equals(entity.getEntityData().getString(THRALL_CONTROLLER_UUID));
	}

	public static EntityPlayer getThrallController(EntityLiving entity) {
		if (!isThralled(entity) || entity.worldObj == null) return null;
		String uuid = entity.getEntityData().getString(THRALL_CONTROLLER_UUID);
		if (uuid == null || uuid.isEmpty()) return null;
		for (Object o : entity.worldObj.playerEntities) {
			EntityPlayer p = (EntityPlayer) o;
			if (p.getUniqueID().toString().equals(uuid)) {
				return p;
			}
		}
		return null;
	}

	/** Retarget thrall onto controller's enemies; never keep controller as target. */
	public static void tickThrallAI(EntityLiving mob) {
		if (mob == null || mob.worldObj.isRemote || !isThralled(mob)) return;

		EntityPlayer controller = getThrallController(mob);
		if (controller == null || controller.isDead) {
			clearThrall(mob);
			return;
		}

		EntityLivingBase current = mob.getAttackTarget();
		if (current == controller || mob.getAITarget() == controller) {
			mob.setAttackTarget(null);
			mob.setRevengeTarget(null);
			if (mob instanceof EntityCreature) {
				((EntityCreature) mob).setTarget(null);
			}
			current = null;
		}

		if (current == null || current.isDead || current == controller) {
			EntityLivingBase best = null;
			double bestDist = 256.0D;
			AxisAlignedBB box = mob.boundingBox.expand(16.0D, 8.0D, 16.0D);
			for (Object o : mob.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
				EntityLivingBase e = (EntityLivingBase) o;
				if (e == mob || e == controller || e.isDead) continue;
				if (e instanceof EntityLiving && isThralledBy((EntityLiving) e, controller)) continue;

				boolean attackingController = (e instanceof EntityLiving)
						&& (((EntityLiving) e).getAttackTarget() == controller
								|| ((EntityLiving) e).getAITarget() == controller);
				boolean hostileMob = e instanceof net.minecraft.entity.monster.EntityMob;
				if (!attackingController && !hostileMob) continue;

				double d = e.getDistanceSqToEntity(mob);
				if (d < bestDist) {
					bestDist = d;
					best = e;
				}
			}
			if (best != null) {
				mob.setAttackTarget(best);
				if (mob instanceof EntityCreature) {
					((EntityCreature) mob).setTarget(best);
				}
			}
		}
	}

	public static int getVampireLevel(EntityPlayer player) {
		return ExtendedPlayer.get(player).getVampireLevel();
	}
	public static int getBloodLevel(EntityPlayer player) {
		return ExtendedPlayer.get(player).getBloodPower();
	}
	public static int getMaxBloodPower(EntityPlayer player) {
		return ExtendedPlayer.get(player).getMaxBloodPower();
	}
	public static boolean hasBloodPower(EntityPlayer player, int amount, float percentage) {
	    ExtendedPlayer playerEx = ExtendedPlayer.get(player);
	    if(percentage > 0) {
	        amount = (int)(playerEx.getMaxBloodPower() * percentage);
	    }
	    return playerEx.getBloodPower() >= amount;
	}

	public static void increaseBloodPower(EntityPlayer player, int quantity, int maxIncrease) {
		ExtendedPlayer.get(player).increaseBloodPower(quantity, maxIncrease);
	}
	public static void increaseBloodPower(EntityPlayer player, int quantity) {
		ExtendedPlayer.get(player).increaseBloodPower(quantity);
	}
	public static void playHypnoSound(World world, EntityPlayer player, float volume, float pitch) {
		SoundEffect.WITCHERY_RANDOM_HYPNOSIS.playAtPlayer(world, player, volume, pitch);
	}
	public static void playDrinkSound(Entity player, Entity target) {
		ParticleEffect.REDDUST.send(SoundEffect.WITCHERY_RANDOM_DRINK, player.worldObj, target.posX, target.posY + target.height * 0.8D, target.posZ, 0.5D, 0.2D, 16);
	}

	public static boolean useNecroStone2(World world, EntityPlayer player, ItemStack itemstack) {
		// Return early on the client side

		//		 if (world.isRemote)
		//		      return false; 
		double MAX_TARGET_RANGE = 50.0D;
		MovingObjectPosition mop = InfusionOtherwhere.doCustomRayTrace(world, player, true, 50.0D);
		if (mop != null)
			switch (mop.typeOfHit) {
			case ENTITY:
				if (mop.entityHit instanceof EntityLivingBase) {
					if (!player.isSneaking()) {
						EntityLivingBase targetEntityLivingBase = (EntityLivingBase) mop.entityHit;
						int r = 50;
						int minionCount = 0;
						AxisAlignedBB bounds = AxisAlignedBB.getBoundingBox(player.posX - 50.0D, player.posY - 15.0D,
								player.posZ - 50.0D, player.posX + 50.0D, player.posY + 15.0D, player.posZ + 50.0D);
						for (Object obj : world.getEntitiesWithinAABB(EntityLiving.class, bounds)) {
							EntityLiving nearbyLivingEntity = (EntityLiving) obj;
							if (// nearbyLivingEntity.getCreatureAttribute() == EnumCreatureAttribute.UNDEAD &&
									PotionEnslaved.isMobEnslavedBy(nearbyLivingEntity, player)) {
								minionCount++;
								EntityUtil.setTarget(nearbyLivingEntity, targetEntityLivingBase);
								GeneralUtil.sendDebugMessage(player, 
										new ChatComponentText("They attack to your will").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
							}
						}


						if (minionCount > 0) {
							Witchery.packetPipeline.sendToAllAround(
									(IMessage) new PacketParticles(ParticleEffect.CRIT, SoundEffect.MOB_ZOMBIE_DEATH,
											(Entity) targetEntityLivingBase, 0.5D, 2.0D),
									TargetPointUtil.from((Entity) targetEntityLivingBase, 16.0D));
							return true;
						}


						break;
					}
					else if(player.isSneaking()) {
						if (PlayerDataUtil.isControlling(player)) {
							PlayerDataUtil.setControlling(player, false);
						} else {
							PlayerDataUtil.setControlling(player, true);

							if (InfusedBrewEffect.Grave.isActive(player) && InfusedBrewEffect.Grave.tryUseEffect(player, mop)) {
								Witchery.packetPipeline
								.sendToAllAround(
										(IMessage) new PacketParticles(ParticleEffect.MOB_SPELL,
												SoundEffect.MOB_ZOMBIE_INFECT, mop.entityHit, 1.0D, 1.0D),
										TargetPointUtil.from(mop.entityHit, 16.0D));
								return true;
							}
							Witchery.packetPipeline.sendToAllAround((IMessage) new PacketParticles(ParticleEffect.SMOKE,
									SoundEffect.NOTE_SNARE, mop.entityHit, 1.0D, 1.0D),
									TargetPointUtil.from(mop.entityHit, 16.0D));

						}
						break;
					}
				}
			case BLOCK:

				if (world.getBlock(mop.blockX, mop.blockY, mop.blockZ) == Witchery.Blocks.ALLURING_SKULL)
					return false;
				if (BlockSide.TOP.isEqual(mop.sideHit)) {
					int minionCount = 0;
					int r = 50;
					AxisAlignedBB bounds = AxisAlignedBB.getBoundingBox(player.posX - 50.0D, player.posY - 15.0D,
							player.posZ - 50.0D, player.posX + 50.0D, player.posY + 15.0D, player.posZ + 50.0D);
					for (Object obj : world.getEntitiesWithinAABB(EntityLiving.class, bounds)) {
						EntityLiving creature = (EntityLiving) obj;
						EntityCreature creature2 = (creature instanceof EntityCreature) ? (EntityCreature) creature
								: null;
						if (// creature.getCreatureAttribute() == EnumCreatureAttribute.UNDEAD &&
								PotionEnslaved.isMobEnslavedBy(creature, player)) {
							minionCount++;
							creature.setAttackTarget(null);
							creature.setRevengeTarget(null);
							if (creature2 != null)
								creature2.setTarget(null);
							if ((creature instanceof net.minecraft.entity.monster.EntitySpider || !creature
									.getNavigator().tryMoveToXYZ(mop.blockX, (mop.blockY + 1), mop.blockZ, 1.0D))
									&& creature2 != null)
								creature2.setPathToEntity(world.getEntityPathToXYZ((Entity) creature, mop.blockX,
										mop.blockY + 1, mop.blockZ, 10.0F, true, false, false, true));
						}
					}
					if (minionCount > 0) {
						ParticleEffect.INSTANT_SPELL.send(SoundEffect.RANDOM_POP, world, 0.5D + mop.blockX,
								(mop.blockY + 1), 0.5D + mop.blockZ, 1.0D, 1.0D, 16);
						return true;
					}
				}
				break;
			case MISS:
				if (PlayerDataUtil.isControlling(player)) {
					PlayerDataUtil.setControlling(player, false);
				} else {
					PlayerDataUtil.setControlling(player, true);
				}

				break;
			}

		// SoundEffect.NOTE_SNARE.playAtPlayer(world, player);
		return false;
	}
	public static void starveVampire(EntityPlayer player, Integer bloodPower, Integer maxBloodPower) {
		VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
		if(bloodPower < (int)(maxBloodPower*.08F)) {
			vPlayer.setBoolean(PlayerKeys.IS_STARVING, true);
			LegendsUtil.bloodVision(player, true);
			if(bloodPower == 0) {
				int counter = vPlayer.getInteger(PlayerKeys.COUNTER_STARVATION);
				if (counter <= 0) {
					decreaseVampireLevel(player);
					decreaseVampireLevel(player);
					decreaseVampireLevel(player);
					vPlayer.setInteger(PlayerKeys.COUNTER_STARVATION, 2*60*15); //Lose 9 levels in 8 minutes, originally 3200
				}
			}
			vPlayer.syncClient();
		}


	}

	/**
	 * Pie-menu mist: pay combat blood cost without touching Legends key cooldowns.
	 * @return false if in combat and the player cannot afford 50% max blood
	 */
	public static boolean tryPayMistCombatBlood(EntityPlayer player) {
		if (!Loader.isModLoaded("witchery")) return true;
		if (player.getEntityData().getInteger("isInCombat") != 1) return true;

		ExtendedPlayer playerEx = ExtendedPlayer.get(player);
		int cost = (int) (playerEx.getMaxBloodPower() * 0.5);

		if (playerEx.getBloodPower() >= cost) {
			playerEx.decreaseBloodPower(cost, true);
			GeneralUtil.sendDebugMessage(player, new ChatComponentText("The combat strain drains your essence...")
					.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.DARK_RED).setItalic(true)));
			return true;
		}

		GeneralUtil.sendDebugMessage(player, new ChatComponentText("You lack the blood to mist while in combat!")
				.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED).setBold(true)));
		return false;
	}

	// In LegendsUtil.java
	public static boolean mistFormWhileInCombatPenalty(EntityPlayer player, int key) {
		// 1. Dependency Check
		if (!Loader.isModLoaded("witchery")) return true;

		ExtendedPlayer playerEx = ExtendedPlayer.get(player);

		// 2. Calculate 50% of their current max blood
		int cost = (int)(playerEx.getMaxBloodPower() * 0.5);

		if (playerEx.getBloodPower() >= cost) {
			// --- SUCCESS CASE ---
			// Drain the blood (true = can go into negative/starvation if needed)
			playerEx.decreaseBloodPower(cost, true);

			LegendsUtil.setLegacyCooldown(player, key, 40); //Large

			GeneralUtil.sendDebugMessage(player, new ChatComponentText("The combat strain drains your essence...")
					.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.DARK_RED).setItalic(true)));

			return true; 
		} else {
			// --- FAILURE CASE ---
			GeneralUtil.sendDebugMessage(player, new ChatComponentText("You lack the blood to mist while in combat!")
					.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED).setBold(true)));

			// Stop the power from activating
			LegacyHelper.setDuration(player, key, 0);

			return false; 
		}
	}
	public static boolean isKnockedOut(Entity entity) {
		if (!(entity instanceof EntityLiving)) return false;
		EntityLiving living = (EntityLiving) entity;

		PotionEffect para = living.getActivePotionEffect(Witchery.Potions.PARALYSED);

		// Combine all "Knockout" triggers into one clean check
		if ((para != null && para.getAmplifier() >= 4) || 
				PotionEnslaved.isMobEnslaved(living) || 
				living.getEntityData().getBoolean("tranced")) {
			return true;
		}

		// Specific check for Villagers sleeping
		return (living instanceof EntityVillager) && ExtendedVillager.get((EntityVillager) living).isSleeping();
	}
	public static boolean isSleeping(EntityVillager villager) {
		if (!(villager instanceof EntityVillager)) return false;

		return ExtendedVillager.get(villager).isSleeping();
	}

	public static void spawnHunter(EntityPlayer player) {
		spawnHunter(player, false);
	}

	public static boolean isVampireActive2(EntityPlayer player) {
		if ((Config.instance()).vampireHunterSpawnChance <= 0.0D || player.capabilities.isCreativeMode)
			return false;
		ExtendedPlayer playerEx = ExtendedPlayer.get(player);
		Village village = player.worldObj.villageCollectionObj.findNearestVillage(MathHelper.floor_double(player.posX),
				MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ), 128);
		List hunters = player.worldObj.getEntitiesWithinAABB(EntityWitchHunter.class,
				player.boundingBox.expand(64.0D, 16.0D, 64.0D));
		return (hunters == null || hunters.isEmpty());
	}
	public static void spawnHunter(EntityPlayer player, boolean spawnRegardlessOfNearbyHunters) {

		// sendDebugMessage(player, "Spawn Hunter");
		if (!player.capabilities.isCreativeMode
				&& player.worldObj.difficultySetting != EnumDifficulty.PEACEFUL) {
			if(isVampireActive2(player) || spawnRegardlessOfNearbyHunters) {
				//			long daysPassed = player.worldObj.getTotalWorldTime() / 24000L;
				NBTTagCompound nbtPlayer = Infusion.getNBT((Entity) player);
				ExtendedPlayer playerEx = ExtendedPlayer.get(player);
				nbtPlayer.removeTag("WITCHunterTrigger");
				int level = playerEx.getVampireLevel();
				//	        int increaseSpawn = (int)daysPassed/6; 
				//	        int MAX_SPAWNS = Math.max(10, 2 + increaseSpawn);
				//	        int tries = Math.max(15, 2 + increaseSpawn);
				int tries = 3;
				int MAX_SPAWNS = 5;
				int spawned = 0;
				for (int i = 0; i < tries && spawned < MAX_SPAWNS; i++) {
					EntityWitchHunter creature = (EntityWitchHunter) Infusion.spawnCreature(player.worldObj,
							EntityWitchHunter.class, MathHelper.floor_double(player.posX),
							MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ),
							(EntityLivingBase) player, 3, 8, ParticleEffect.SMOKE, null);
					if (creature != null) {
						spawned++;
						// creature.targetPlayerName = player.getCommandSenderName();
						creature.onSpawnWithEgg((IEntityLivingData) null);
						EntityUtil.setTarget((EntityLiving) creature, (EntityLivingBase) player);
						//	            creature.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(30.0D + playerEx.getVampireLevel()*10*0);
						creature.setHealth((int) ((float) playerEx.getVampireLevel() * 2.8F + 12.0F)); // Current health =
						// max health
						//                player.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, TimeUtil.secsToTicks(240), 3, false));
						//                PlayerHelper.setGrounded(player, 240);
					} else {
						// GeneralUtil.sendDebugMessage(player, "Hunter Spawning Failed");
					}
				}
				if (spawned > 0)
					nbtPlayer.setBoolean("hunterCooldown", true);
				nbtPlayer.setInteger("hunterTimer", 300); //15s of pure frenzy for Vampire Beast form
				Witchery.packetPipeline.sendTo(
						(IMessage) new PacketSound(SoundEffect.WITCHERY_RANDOM_THEYCOME, (Entity) player, 1.0F, 1.0F),
						player);

			}
		}

	}

	private static final String GOBLET_USE_TICK = "bettervampires_gobletUseTick";
	/** Ignore duplicate drink/fill/interact within this many ticks (server). */
	private static final int GOBLET_USE_DEBOUNCE_TICKS = 5;

	/**
	 * Server-only guard against double onEaten / interact (client+server or duplicate packets).
	 * @return false if this use should be ignored
	 */
	public static boolean tryConsumeGobletAction(EntityPlayer player) {
		if (player == null || player.worldObj == null || player.worldObj.isRemote) {
			return false;
		}
		NBTTagCompound nbt = player.getEntityData();
		int now = player.ticksExisted;
		int last = nbt.getInteger(GOBLET_USE_TICK);
		if (last > 0 && now >= last && now - last < GOBLET_USE_DEBOUNCE_TICKS) {
			return false;
		}
		nbt.setInteger(GOBLET_USE_TICK, now);
		return true;
	}

	public static boolean isGobletEmpty(ItemStack stack) {
		if (stack == null || stack.getItem() != Witchery.Items.BLOOD_GOBLET) {
			return false;
		}
		if (!stack.hasTagCompound()) {
			return true;
		}
		String bloodUUID = stack.getTagCompound().getString("WITCBloodUUID");
		return bloodUUID == null || bloodUUID.isEmpty();
	}

	/** Clear blood NBT so emptied goblets can merge with fresh stacks. */
	public static void clearGobletBlood(ItemStack stack) {
		if (stack == null) {
			return;
		}
		stack.setItemDamage(0);
		stack.setTagCompound(null);
	}

	/**
	 * If stackSize &gt; 1, splits off one goblet for mutation; remainder stays on {@code stack}.
	 * Returns the single stack to mutate (same reference when size was 1).
	 */
	public static ItemStack splitOneGobletForUse(ItemStack stack) {
		if (stack == null || stack.stackSize <= 1) {
			return stack;
		}
		return stack.splitStack(1);
	}

	/** Give a split-off goblet back to the player after mutation. No-op if not split. */
	public static void returnSplitGoblet(EntityPlayer player, ItemStack original, ItemStack used) {
		if (player == null || used == null || used == original) {
			return;
		}
		GeneralUtil.givePlayerItem(player, used);
	}

	/** Hotbar empty goblet, or null. */
	public static ItemStack getWitcheryGoblet(EntityPlayer player) {
		for (int i = 0; i < 9; i++) {
			ItemStack stack = player.inventory.getStackInSlot(i);
			if (isGobletEmpty(stack)) {
				return stack;
			}
		}
		return null;
	}

	public static boolean hasWitcheryGobletInFullInventory(EntityPlayer player) {
		for (int i = 0; i < 36; i++) {
			ItemStack stack = player.inventory.getStackInSlot(i);
			if (stack != null && stack.getItem() == Witchery.Items.BLOOD_GOBLET) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasEmptyWitcheryGoblet(EntityPlayer player) {
		return getWitcheryGoblet(player) != null;
	}

	public static boolean hasBloodType(String blood, ItemStack stack) {
		if (stack == null || !stack.hasTagCompound()) {
			return false;
		}
		NBTTagCompound nbtRoot = stack.getTagCompound();
		String bloodUUID = nbtRoot.getString("WITCBloodUUID");
		return bloodUUID != null && bloodUUID.equals(blood);
	}

	/**
	 * Fill one goblet from a (possibly stacked) empty stack. Splits first when needed.
	 */
	public static void setWitcheryGoblet(ItemStack stack, EntityPlayer player, String blood) {
		if (stack == null) {
			return;
		}
		ItemStack used = splitOneGobletForUse(stack);

		if (blood != null) {
			NBTTagCompound nbtRoot = used.hasTagCompound() ? used.getTagCompound() : new NBTTagCompound();
			nbtRoot.setString("WITCBloodUUID", "__" + blood);
			nbtRoot.setString("WITCBloodName", blood + " (3/3 Uses)");
			nbtRoot.setInteger("BloodUseCounter", 3);
			used.setTagCompound(nbtRoot);
			used.setItemDamage(1);
		} else if (player != null) {
			NBTTagCompound nbtRoot = used.hasTagCompound() ? used.getTagCompound() : new NBTTagCompound();
			nbtRoot.setString("WITCBloodUUID", player.getGameProfile().getId().toString());
			nbtRoot.setString("WITCBloodName", player.getGameProfile().getName() + " (3/3 Uses)");
			nbtRoot.setInteger("BloodUseCounter", 3);
			used.setTagCompound(nbtRoot);
			used.setItemDamage(1);
		}

		returnSplitGoblet(player, stack, used);
	}

	public static void enslaveMob(EntityLiving targetLiving, EntityPlayer player) {
		if (PotionEnslaved.canCreatureBeEnslaved(targetLiving)) {
	
			{
				if (!PotionEnslaved.isMobEnslavedBy(targetLiving, player)) {
					boolean enslaved = setEnslaverForMob(targetLiving, player);
					// You may want to send feedback or play sound here
					// SoundEffect.WITCHERY_RANDOM_HYPNOSIS.playOnlyTo(player);
					//            		Witchery.packetPipeline.sendToAllAround((IMessage)new PacketParticles(ParticleEffect.MOB_SPELL, SoundEffect.MOB_ZOMBIE_INFECT, targetLiving.entityHit, 1.0D, 1.0D), TargetPointUtil.from(targetLiving.entityHit, 16.0D));
					Witchery.packetPipeline
					.sendToAllAround(
							(IMessage) new PacketParticles(ParticleEffect.MOB_SPELL,
									SoundEffect.MOB_ZOMBIE_INFECT, targetLiving, 0.5D, 1.0D),
							(TargetPointUtil.from(targetLiving, 16.0D)));
					SoundEffect.WITCHERY_RANDOM_HYPNOSIS.playAtPlayer(targetLiving.worldObj, player, 0.5F, 1.0F);
					if (!player.worldObj.isRemote)
						player.addChatMessage(
								new ChatComponentText("You can now control mob mob with a necromantic stone"));
	
				}
	
			}
		}
	}

	public static void blackMagic2(EntityPlayer player) {
		if (player != null && player.worldObj != null && !player.worldObj.isRemote) {
			NBTTagCompound nbtPlayer = Infusion.getNBT((Entity) player);
			if (nbtPlayer != null && nbtPlayer.getLong("WITCHunterTrigger") <= 0L) {
				long totalWorldTicks = TimeUtil.getServerTimeInTicks();
				nbtPlayer.setLong("WITCHunterTrigger", totalWorldTicks);
			} else if (nbtPlayer.getLong("WITCHunterTrigger") > 0L) {
				spawnHunter(player);
				// Witchery.packetPipeline.sendTo((IMessage)new
				// PacketSound(SoundEffect.WITCHERY_RANDOM_THEYCOME, (Entity)player, 1.0F,
				// 1.0F), player);
			}
		}
	}

	public static boolean isVampireActive(EntityPlayer player) {
		if ((Config.instance()).vampireHunterSpawnChance <= 0.0D || player.capabilities.isCreativeMode)
			return false;
		ExtendedPlayer playerEx = ExtendedPlayer.get(player);
		if (playerEx.getVampireLevel() < 10)
			return false;
		if (player.worldObj.rand.nextDouble() < (Config.instance()).vampireHunterSpawnChance) {
			Village village = player.worldObj.villageCollectionObj.findNearestVillage(
					MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY),
					MathHelper.floor_double(player.posZ), 128);
			if (village != null && village.getReputationForPlayer(player.getCommandSenderName()) < -1) {
				List hunters = player.worldObj.getEntitiesWithinAABB(EntityWitchHunter.class,
						player.boundingBox.expand(64.0D, 16.0D, 64.0D));
				return (hunters == null || hunters.isEmpty());
			}
		}
		return false;
	}

	public static boolean isOwner(ItemStack stack, EntityPlayer player) {
		String name = player.getGameProfile().getName();
		if (stack == null) {
			return false;
		}
		NBTTagCompound nbtRoot = stack.getTagCompound();
		String bloodUUID = nbtRoot.getString("WITCPlayerName");
		if (bloodUUID.equals(name)) {
			// sendDebugMessage(player, "Empty goblet found 2");
			return true;
		}
	
		return false;
	}

	//Bonus not really used atm, was for vampire form
	public static void villagerDied(EntityPlayer player, Boolean levelUp) {
		ExtendedPlayer playerEx = ExtendedPlayer.get(player);
		int more = 0;
	
		if (!player.worldObj.isRemote) {
			playerEx.increaseBloodPower(50);
			player.addChatComponentMessage(new ChatComponentText("The taste lingers")
					.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
			if (playerEx.getVampireLevel() < 9) {
				if(levelUp)
					increaseLegendsVampire(player);
				playerEx.resetVampireQuestCounter();
				playerEx.sync();
				LegendsUtil.addVampireKillSkillPoints(player);
			}
			if (playerEx.getVampireLevel() == 10) {
				LegendsUtil.addVampireKillSkillPoints(player);
				player.addPotionEffect(new PotionEffect(Potion.regeneration.id, 20*30, 1));
				player.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, 20*60*7, 2));
				player.addPotionEffect(new PotionEffect(Potion.damageBoost.id, 20*60, 2));
				if (playerEx.getVampireUltimate() == VampireUltimate.NONE) {
					playerEx.setVampireUltimate(VampireUltimate.FARM, Math.min(playerEx.getVampireUltimateCharges() + 1, 2));
				} else {
					playerEx.setVampireUltimate(playerEx.getVampireUltimate(), Math.min(playerEx.getVampireUltimateCharges() + 1, 2));
				}
			}
	
		}
	}

	public static void removeEnslaver(EntityLiving entity) {
		clearThrall(entity);
		if (entity != null && entity.isPotionActive(Witchery.Potions.PARALYSED)) {
			entity.removePotionEffect(Witchery.Potions.PARALYSED.id);
		}
	}


	// Copied code just added true to remove particles


}
