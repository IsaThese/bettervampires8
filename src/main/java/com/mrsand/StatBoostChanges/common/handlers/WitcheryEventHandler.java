package com.mrsand.StatBoostChanges.common.handlers;

import java.util.List;

import com.emoniph.witchery.common.ExtendedPlayer;
import com.emoniph.witchery.common.ExtendedVillager;
import com.emoniph.witchery.util.CreatureUtil;
import com.mrsand.StatBoostChanges.ModCompat;
import com.mrsand.StatBoostChanges.common.management.VExtendedVillager;
import com.mrsand.StatBoostChanges.common.management.VExtendedVillager.VillagerKeys;
import com.mrsand.StatBoostChanges.util.SuspicionUtil;
import com.mrsand.StatBoostChanges.util.VisualUtil;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;
import de.teamlapen.vampirism.entity.player.VampirePlayer;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;

public class WitcheryEventHandler {

	/** Set when a vampire death is saved so PlayerDrops/Gravestone cannot strip inventory. */
	public static final String NBT_VAMPIRE_DEATH_SAVED = "statbooVampireDeathSaved";

	/**
	 * Run before Gravestone and other death listeners. Restores HP via checkForVampireDeath
	 * and cancels so item drops / graves never start.
	 */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onVampireDeathSaveEarly(LivingDeathEvent event) {
		if (event.entity == null || event.entity.worldObj == null || event.entity.worldObj.isRemote) return;
		if (!(event.entityLiving instanceof EntityPlayer)) return;
		EntityPlayer player = (EntityPlayer) event.entityLiving;
		ExtendedPlayer playerEx = ExtendedPlayer.get(player);
		if (playerEx == null || !playerEx.isVampire()) return;

		// Mirror Witchery: if health already restored, just cancel
		if (player.getHealth() > 0.0F) {
			event.setCanceled(true);
			player.isDead = false;
			return;
		}

		// false = survived (mixin restored HP / beast / mist)
		if (!CreatureUtil.checkForVampireDeath(player, event.source)) {
			event.setCanceled(true);
			player.isDead = false;
			player.deathTime = 0;
			player.getEntityData().setBoolean(NBT_VAMPIRE_DEATH_SAVED, true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onVampirePlayerDropsCancel(PlayerDropsEvent event) {
		if (event.entityPlayer == null || event.entityPlayer.worldObj == null || event.entityPlayer.worldObj.isRemote) return;
		if (event.entityPlayer.getEntityData().getBoolean(NBT_VAMPIRE_DEATH_SAVED)) {
			event.setCanceled(true);
			event.entityPlayer.getEntityData().removeTag(NBT_VAMPIRE_DEATH_SAVED);
		}
	}

	//	@SubscribeEvent
	//	public static void onEntityLivingDeath(LivingDeathEvent event) {
	//				Entity entity = event.entity;
	//				if(entity instanceof EntityVillager) {
	//					if(event.source.getEntity() instanceof EntityPlayer) {
	//						EntityPlayer player = (EntityPlayer)event.source.getEntity();
	//						ExtendedPlayer ePlayer = ExtendedPlayer.get(player);
	//						if(ePlayer.getVampireLevel() > 0 && !player.worldObj.isRemote) {
	//							if(GeneralUtil.checkForSuspicionWitnesses(player, 0)) {
	//								Genera
	//							}
	//		
	//						}
	//					}
	//				}
	//	}

	@SubscribeEvent
	public void onEntityPlayerUpdate(PlayerTickEvent event) {
		EntityPlayer player = event.player;
		if(player == null) return;
		// Drop cancel may never run if LivingDeath was canceled early — clear stale flag at END
		if (event.phase == Phase.END && !player.worldObj.isRemote
				&& player.getEntityData().getBoolean(NBT_VAMPIRE_DEATH_SAVED)) {
			player.getEntityData().removeTag(NBT_VAMPIRE_DEATH_SAVED);
		}
		ExtendedPlayer playerEx = ExtendedPlayer.get(player);
		if (playerEx == null || playerEx.getVampireLevel() != 10) {
			return;
		}
		// Max vampires stay in otherform; Vampire Lords default human (starve/hypno/blood vision still force it in VisualUtil)
		boolean lord = false;
		if (ModCompat.VAMPIRISM) {
			VampirePlayer vp = VampirePlayer.get(player);
			lord = vp != null && vp.isVampireLord();
		}
		VisualUtil.changeForm(player, !lord);
	}
//	@SubscribeEvent
//	public void onPlayerSleepInBed(PlayerSleepInBedEvent event) {
//		  World world = event.entityPlayer.worldObj;
//		  EntityPlayer player = event.entityPlayer;
//		  
//		  if (CreatureUtil.isWerewolf((Entity)event.entityPlayer)) {
//		    if (!world.isClient) {
//		      ChatUtil.sendTranslated(EnumChatFormatting.RED, (ICommandSender)event.entityPlayer, "witchery.nosleep.wolf", new Object[0]);
//		      event.result = EntityPlayer.EnumStatus.OTHER_PROBLEM;
//		    } 
//		  } else if (event.entityPlayer.isPotionActive(Witchery.Potions.RESIZING)) {
//		    if (!world.isClient) {
//		      ChatUtil.sendTranslated(EnumChatFormatting.RED, (ICommandSender)event.entityPlayer, "witchery.nosleep.resized", new Object[0]);
//		      event.result = EntityPlayer.EnumStatus.OTHER_PROBLEM;
//		    } 
//		  } else if (ExtendedPlayer.get(event.entityPlayer).isVampire() && world.getBlock(event.x, event.y, event.z) == Witchery.Blocks.COFFIN) {
//		    if (!event.entityPlayer.worldObj.isDaytime()) {
//		      if (!world.isClient) {
//		        ChatUtil.sendTranslated(EnumChatFormatting.RED, (ICommandSender)event.entityPlayer, "witchery.nosleep.dayonly", new Object[0]);
//		        event.result = EntityPlayer.EnumStatus.OTHER_PROBLEM;
//		      } 
//		    } else {
//		      if (!world.isClient) {
//		        if (player.isPlayerSleeping() || !player.isEntityAlive())
//		          return; 
//		        if (!world.provider.isSurfaceWorld())
//		          return; 
//		        if (!world.isDaytime()) {
//		          event.result = EntityPlayer.EnumStatus.OTHER_PROBLEM;
//		          return;
//		        } 
//		        if (Math.abs(player.posX - event.x) > 3.0D || Math.abs(player.posY - event.y) > 2.0D || Math.abs(player.posZ - event.z) > 3.0D) {
//		          event.result = EntityPlayer.EnumStatus.TOO_FAR_AWAY;
//		          return;
//		        } 
//		        double d0 = 8.0D;
//		        double d1 = 5.0D;
//		        List list = world.getEntitiesWithinAABB(EntityMob.class, AxisAlignedBB.getBoundingBox(event.x - d0, event.y - d1, event.z - d0, event.x + d0, event.y + d1, event.z + d0));
//		        if (!list.isEmpty()) {
//		          event.result = EntityPlayer.EnumStatus.NOT_SAFE;
//		          return;
//		        } 
//		      } 
//		      if (player.isRiding())
//		        player.mountEntity((Entity)null); 
//		      PotionResizing.setEntitySize((Entity)player, 0.2F, 0.2F);
//		      player.yOffset = 0.2F;
//		      if (world.blockExists(event.x, event.y, event.z)) {
//		        int l = world.getBlock(event.x, event.y, event.z).getBedDirection((IBlockAccess)world, event.x, event.y, event.z);
//		        float f1 = 0.5F;
//		        float f = 0.5F;
//		        switch (l) {
//		          case 0:
//		            f = 0.9F;
//		            break;
//		          case 1:
//		            f1 = 0.1F;
//		            break;
//		          case 2:
//		            f = 0.1F;
//		            break;
//		          case 3:
//		            f1 = 0.9F;
//		            break;
//		        } 
//		        player.field_71079_bU = 0.0F;
//		        player.field_71089_bV = 0.0F;
//		        switch (l) {
//		          case 0:
//		            player.field_71089_bV = -1.8F;
//		            break;
//		          case 1:
//		            player.field_71079_bU = 1.8F;
//		            break;
//		          case 2:
//		            player.field_71089_bV = 1.8F;
//		            break;
//		          case 3:
//		            player.field_71079_bU = -1.8F;
//		            break;
//		        } 
//		        player.setPosition((event.x + f1), (event.y + 0.9375F), (event.z + f));
//		      } else {
//		        player.setPosition((event.x + 0.5F), (event.y + 0.9375F), (event.z + 0.5F));
//		      } 
//		      player.sleeping = true;
//		      player.sleepTimer = 0;
//		      player.playerLocation = new ChunkCoordinates(event.x, event.y, event.z);
//		      player.motionX = player.motionZ = player.motionY = 0.0D;
//		      if (!world.isClient)
//		        world.updateAllPlayersSleepingFlag(); 
//		      event.result = EntityPlayer.EnumStatus.OK;
//		      return;
//		    } 
//		  } 
//		}
	
	@SubscribeEvent
	public void onPlayerDeath(LivingDeathEvent event) {
		if (event.isCanceled()) return;
		if(event.entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer)event.entity;
			if(player == null) return;
			ExtendedPlayer playerEx = ExtendedPlayer.get(player);
			if(playerEx.isVampire()) {
				float multiplier = .87F - WitcheryUtil.getVampireLevel(player)*.04F; //was .9 so now .47 is max at level 10
				WitcheryUtil.increaseBloodPower(player, (int)Math.ceil(WitcheryUtil.getMaxBloodPower(player)), (int)Math.ceil((WitcheryUtil.getMaxBloodPower(player) * multiplier)));	
			}
		}
	}

	// Might generalize all villager deaths, Oh for vampire beast?
//	@SubscribeEvent
//	public void onLivingDeath(LivingDeathEvent event) {
//		if(event.entity instanceof EntityVillager && event.source.getEntity() instanceof EntityPlayer) {
//			EntityVillager villager = (EntityVillager) event.entity;
//			EntityPlayer player = (EntityPlayer)event.source.getEntity();
//			if(player != null && villager != null && GeneralUtil.isVampire(player)) {
//				GeneralUtil.villagerDied(player);
//				if(Loader.isModLoaded("legends")){
//
//				}
//
//			}
//		}
//	}

	@SubscribeEvent
	public void onLivingUpdate(LivingUpdateEvent event) { 
		if(event.entityLiving instanceof EntityVillager && !event.entityLiving.worldObj.isRemote) {
			EntityVillager villager = (EntityVillager)event.entityLiving;
			VExtendedVillager vVillager = VExtendedVillager.get(villager);
			ExtendedVillager villagerEx = ExtendedVillager.get(villager);
			EntityPlayer closestPlayer = villager.worldObj.getClosestPlayerToEntity(villager, 16.0D);
			int seconds = 2;
			if(closestPlayer == null) return;

			if (vVillager.getBoolean(VillagerKeys.IS_CAGED)) {
				double centerX = vVillager.getInteger(VillagerKeys.CAGE_X) + 0.5D;
				double centerZ = vVillager.getInteger(VillagerKeys.CAGE_Z) + 0.5D;

				// Kill horizontal momentum so they can't "rub" against the iron bars
				villager.motionX = 0.0D;
				villager.motionZ = 0.0D;

				// Snapping the horizontal position to keep them away from walls
				if (villager.getDistanceSq(centerX, villager.posY, centerZ) > 0.001D) {
					villager.setPosition(centerX, villager.posY, centerZ);
				}
			}

			// Polling Strategy: Check every 2 seconds
			if (villager.ticksExisted % 40 == 0) {

				if (vVillager != null) {
					vVillager.checkCageStatus();
				}


				World world = villager.worldObj;
				//				int counter  = vVillager.getInteger(VillagerKeys.TIMER_REPLINISH);

				// 1. Only check during the day (Time 0 - 12000)
				if (vVillager.getBoolean(VillagerKeys.IS_CAGED)) {
					EntityPlayer captor = vVillager.getCaptor();
					if (captor == null) return;

					int replenish = 12; // originall 3. 6 was perfect, but 12 because GTNH having issues atm
					// 1. Get current world age (TotalTime)
					int currentTotalTime = (int)villager.worldObj.getTotalWorldTime();
					int lastProcessedTime = vVillager.getInteger(VillagerKeys.LAST_UPDATE_TIME);

					// 2. Get Sleep Tracking Data
//					int lastSleepEventTime = (int)GeneralEventHandler.lastSleepSkipTotalTime;
//					int lastVillagerSleepCheck = vVillager.getInteger(VillagerKeys.LAST_SLEEP_CHECK);
//					int sleepAmount = (int)GeneralEventHandler.lastSleepSkipAmount;
					int lastSleepDay = (int)GeneralEventHandler.lastSleepSkipDay;
					int lastVillagerSleepDay = vVillager.getInteger(VillagerKeys.LAST_SLEEP_CHECK);
					int sleepAmount = (int)GeneralEventHandler.lastSleepSkipAmount;

					int totalCycles = 0;

					if (lastProcessedTime > 0 && currentTotalTime > lastProcessedTime + 45) {
//						GeneralUtil.sendDebugMessage(captor, "processing lapsed time");
						int elapsed = currentTotalTime - lastProcessedTime;
						totalCycles += (elapsed / 16*20); // Add cycles for real-time passage
					}


					if (lastSleepDay > lastVillagerSleepDay) {
						totalCycles += (lastSleepDay - lastVillagerSleepDay)* 37; //37 cycles in a day
//						GeneralUtil.sendDebugMessage(captor, "processing sleep time");
						vVillager.setInteger(VillagerKeys.LAST_SLEEP_CHECK, lastSleepDay);
					}

					if (totalCycles > 0) {
						villagerEx.giveBlood(replenish * totalCycles);
						villagerEx.sync();
						vVillager.setInteger(VillagerKeys.LAST_UPDATE_TIME, currentTotalTime);
					}

					if (villager.ticksExisted % 40 == 0) {
						vVillager.setInteger(VillagerKeys.LAST_UPDATE_TIME, currentTotalTime);

						if (villagerEx.getBlood() < 500) {
							int counter = vVillager.getInteger(VillagerKeys.TIMER_REPLINISH);
							counter++;

							if (counter >= 8) {
								counter = 0;
								villagerEx.giveBlood(replenish);
								villagerEx.sync();
								villager.worldObj.setEntityState(villager, (byte)19);
							}
							vVillager.setInteger(VillagerKeys.TIMER_REPLINISH, counter);
						}
					}


					if(world.isDaytime() ) {


						int cX = vVillager.getInteger(VillagerKeys.CAGE_X);
						int cY = vVillager.getInteger(VillagerKeys.CAGE_Y);
						int cZ = vVillager.getInteger(VillagerKeys.CAGE_Z);

						int[] standSpot = WitcheryUtil.findNavigableSpotNearCage(world, cX, cY, cZ);
						boolean attemptToFree = false;

						if(standSpot != null) {
							AxisAlignedBB searchArea = villager.boundingBox.expand(16.0D, 4.0D, 16.0D);
							List<EntityVillager> enforcement = world.getEntitiesWithinAABB(EntityLiving.class, searchArea);

							for (EntityLiving guard : enforcement) {
								if (guard instanceof EntityVillager) {
									if(!VExtendedVillager.get((EntityVillager)guard).getBoolean(VillagerKeys.IS_CAGED)) {
										attemptToFree = true;
										break;
									}
									else if (guard instanceof EntityGolem) {
										attemptToFree = true;
										break;
									}
									else if(WitcheryUtil.isGuard(guard)) {
										attemptToFree = true;
										break;
									}
								}
								if(attemptToFree) {
									PathEntity path = guard.getNavigator().getPathToXYZ(standSpot[0], standSpot[1], standSpot[2]);
									if (path != null) {
										guard.getNavigator().setPath(path, 1.0D); 

										if (captor != null) {
											SuspicionUtil.increaseSuspicion(captor, 1000);
											captor.addChatMessage(new ChatComponentText("Your prisoner was set free.")
													.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
										}
										vVillager.liberateVillager();
										break; 
									}
								}
							}


						}
					}
					vVillager.syncClient();

				}
			}
		}
	}
}
