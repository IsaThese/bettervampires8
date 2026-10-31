package com.mrsand.StatBoostChanges.common.handlers;

import java.util.UUID;

import com.emoniph.witchery.Witchery;
import com.mrsand.StatBoostChanges.abilities.VampireRestoredAbilities;
import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.LegendsAbilityBridge;
import com.mrsand.StatBoostChanges.util.LegendsUtil;
import com.mrsand.StatBoostChanges.util.MorphUtil;
import com.mrsand.StatBoostChanges.util.PhysicsUtil;
import com.mrsand.StatBoostChanges.util.SuspicionUtil;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;
import com.tihyo.legends.management.LegendsHelper;
import com.tihyo.legends.morph.MorphHelper;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

public class LegendsEventHandler {


	@SubscribeEvent
	public void onDeath(LivingDeathEvent event) {
		if (event.source.getEntity() instanceof EntityPlayer) {
			Entity entity = event.entity;
			EntityPlayer player = (EntityPlayer) event.source.getEntity();

			if (player == null || !Loader.isModLoaded("witchery")) {
				return;
			}
			boolean isVampireBeast = MorphHelper.isVampireBeast(player);
			boolean isFuryActive = VampireRestoredAbilities.isDemonicFuryActive(player);
			boolean isBiteActive = LegendsAbilityBridge.isVampireBite(player);

			boolean holdingCane = player.getHeldItem() != null && player.getHeldItem().getItem() == Witchery.Items.CANE_SWORD;

			if (holdingCane && !isVampireBeast && !isFuryActive && !isBiteActive) {
				return;
			}

			boolean isVillager = entity instanceof EntityVillager;
			boolean isGuard = WitcheryUtil.isGuard(entity);
			int multiplier = isVampireBeast ? 3 : 1;

			if (isVillager || isGuard) {
				if (SuspicionUtil.checkForSuspicionWitnesses(player, 2000, 128)) {
					WitcheryUtil.spawnHunter(player, true);
				} else {
					SuspicionUtil.increaseSuspicion(player, 1700);
				}
				if (isVampireBeast) {
					WitcheryUtil.increaseLegendsVampire(player, 3);
				} else if (isFuryActive) {
					WitcheryUtil.increaseLegendsVampire(player, 2);
				} else if (isBiteActive) {
					WitcheryUtil.increaseLegendsVampire(player, 1);
				}
			}

			// Blood Power Logic
			if (isVillager) {
				EntityVillager villager = (EntityVillager) entity;
				if (WitcheryUtil.getBlood(villager) >= 100) {
					WitcheryUtil.increaseBloodPower(player, (int) (WitcheryUtil.getMaxBloodPower(player)));
					WitcheryUtil.villagerDied(player, false);
					if (isVampireBeast && GeneralUtil.getClosestVillage(entity, 128) != null) {
						player.addExperience(1069);
					}
				} else {
					WitcheryUtil.increaseBloodPower(player, (int) (WitcheryUtil.getMaxBloodPower(player) * 0.1F * multiplier));
				}
			} else if (isGuard) {
				WitcheryUtil.increaseBloodPower(player, (int) (WitcheryUtil.getMaxBloodPower(player) * 0.1F) * multiplier);
				if (isVampireBeast) player.addExperience(1069);
			}
		}
	}


	@SubscribeEvent
	public void onAttack(AttackEntityEvent event) {
		if (event.entity instanceof EntityPlayer && !event.entityPlayer.worldObj.isRemote) {
			EntityPlayer player = (EntityPlayer)event.entity;
			if (VampireRestoredAbilities.isDemonicFuryActive(player)) {
				if (event.target instanceof EntityLivingBase) {
					EntityLivingBase entity = (EntityLivingBase)event.target;
					if(entity.getHealth() > 0 && !entity.isDead) {
						boolean increase = false;
						if(Loader.isModLoaded("witchery")) {
							if(entity instanceof EntityVillager) {
								WitcheryUtil.increaseBlood(player, 0, .3F);
								//								if(WitcheryUtil.getBloodLevel(player) == WitcheryUtil.getMaxBloodPower(player))
								//									increase = true;

								player.addExperience(1000);
							}
							else if(WitcheryUtil.isGuard(entity)) {
								WitcheryUtil.increaseBlood(player, 0, .4F);
								if(WitcheryUtil.getBloodLevel(player) == WitcheryUtil.getMaxBloodPower(player))
									increase = true;
								player.addExperience(1000);
							}
							else if (entity.getHealth() != 0) {
								//							target.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer)event.entityPlayer), 1000.0F);
								WitcheryUtil.increaseBloodPower(player, (int)Math.ceil(WitcheryUtil.getMaxBloodPower(player)*.02F));
								WitcheryUtil.playDrinkSound(event.entityPlayer, entity);
							}
							//							Increase changed to onDeath
							//							if(increase) {
							//								WitcheryUtil.increaseLegendsVampire(player);
							//							}
						}
						else if(entity instanceof EntityVillager) {
							player.addExperience(1000);
						}
					}
				}
			}
			else if (MorphHelper.isVampireBeast(player)) {
				if (event.target instanceof EntityLivingBase) {
					EntityLivingBase target = (EntityLivingBase)event.target;
					if(LegendsHelper.isBloodTarget(target)) {
						if(Loader.isModLoaded("witchery"))
							if(target.getHealth() != 0) {
								//									target.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer)event.entityPlayer), 1000.0F);
								WitcheryUtil.increaseBloodPower(player, (int)Math.ceil(WitcheryUtil.getMaxBloodPower(player)*.02F));
								WitcheryUtil.playDrinkSound(event.entityPlayer, target);
							}
					}
				}
			}
		} 

	}

	@SubscribeEvent
	public void onServerTick(ServerTickEvent event) {
		if (event.side == Side.CLIENT) return;
		if (event.phase != TickEvent.Phase.START) return;

		MinecraftServer server = MinecraftServer.getServer();

		// Ensure the server instance and its configuration manager are fully loaded
//		if (server != null && server.getConfigurationManager() != null) {
//
//			// Iterate through FML/Minecraft's master online player pool
//			for (Object obj : server.getConfigurationManager().playerEntityList) {
//				if (obj instanceof EntityPlayerMP) {
//					EntityPlayerMP serverPlayer = (EntityPlayerMP) obj;
//					VExtendedPlayer vPlayer =  VExtendedPlayer.get(serverPlayer);
//					int counter = vPlayer.getInteger(PlayerKeys.COUNTER_MORPH);
//					if(Loader.isModLoaded("Morph")) { 
//						if(MorphUtil.getMorphTime(serverPlayer) == .0125F){
//							counter = 85;
//						}
//						if(counter == 0 && MorphUtil.isPlayerMorphed(serverPlayer)) {
//							LegendsUtil.ifMorphedTurnHuman(serverPlayer);
//						}
//						
//						if(counter > 0) {
//							counter--;
//						}
//						GeneralUtil.sendDebugMessage(serverPlayer, "counter: " + counter);
//						vPlayer.setInteger(PlayerKeys.COUNTER_MORPH, counter);
//						vPlayer.syncClient();
//					}
//				}
//			}
//		}
	}

	@SubscribeEvent
	public void onPlayerTick(PlayerTickEvent event) {
		EntityPlayer player = event.player;
		if(player == null) return;
		
		
		if (event.side == Side.SERVER && event.phase == Phase.START && !player.worldObj.isRemote) {
			EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
			VExtendedPlayer vPlayer =  VExtendedPlayer.get(serverPlayer);
			int counter = vPlayer.getInteger(PlayerKeys.COUNTER_MORPH);
			if(Loader.isModLoaded("Morph")) { 
				if(MorphUtil.getMorphTime(serverPlayer) == .0125F){
					counter = 85;
				}
				if(counter == 1 && MorphUtil.isPlayerMorphed(serverPlayer)) {
					LegendsUtil.ifMorphedTurnHuman(serverPlayer);
				}
				
				if(counter > 0) {
					counter--;
				}
//				GeneralUtil.sendDebugMessage(serverPlayer, "counter: " + counter);
				vPlayer.setInteger(PlayerKeys.COUNTER_MORPH, counter);
				vPlayer.syncClient();
			}
		}



		if (event.phase != Phase.START) return;

		if (!player.worldObj.isRemote && Loader.isModLoaded("legends")) {
			VampireRestoredAbilities.tick(player);
			LegendsUtil.tickMistDemist(player);
			if (LegendsAbilityBridge.isMist(player)) {
				PhysicsUtil.handlePhasingPhysics(player);
				player.getEntityData().setBoolean("IsMistFormSmall", true);
			}
		}

		boolean isMistActive = LegendsAbilityBridge.isMist(player);
		boolean wasMisty = player.getEntityData().getBoolean("IsMistFormSmall");

		// --- CASE: ABILITY ENDED ---
		if (!isMistActive && wasMisty) {
			World world = player.worldObj;

			// If the player is inside a block, we find the nearest exit
			if (player.isEntityInsideOpaqueBlock()) {
				int px = MathHelper.floor_double(player.posX);
				int py = MathHelper.floor_double(player.posY);
				int pz = MathHelper.floor_double(player.posZ);

				// Priority 1: Grounded (Closest 2x Air + Solid Floor)
				double bestGroundedDistSq = -1.0D;
				double gX = 0, gY = 0, gZ = 0;
				boolean foundGrounded = false;

				// Priority 2: Any Air (Closest 2x Air anywhere - Fallback)
				double bestAirDistSq = -1.0D;
				double aX = 0, aY = 0, aZ = 0;
				boolean foundAnyAir = false;

				// Radial Search: 3 block radius
				for (int x = px - 3; x <= px + 3; x++) {
					for (int z = pz - 3; z <= pz + 3; z++) {
						for (int y = py - 1; y <= py + 2; y++) {

							// Check for 2-block high air gap
							if (world.isAirBlock(x, y, z) && world.isAirBlock(x, y + 1, z)) {
								double dx = (x + 0.5D) - player.posX;
								double dy = y - player.posY;
								double dz = (z + 0.5D) - player.posZ;
								double distSq = dx*dx + dy*dy + dz*dz;

								// Check if there is a solid block to stand on
								Block floor = world.getBlock(x, y - 1, z);
								boolean hasGround = floor.getMaterial().isSolid() || floor.isOpaqueCube();

								// Track Closest Grounded Spot
								if (hasGround) {
									if (!foundGrounded || distSq < bestGroundedDistSq) {
										bestGroundedDistSq = distSq;
										gX = x + 0.5D; gY = y; gZ = z + 0.5D;
										foundGrounded = true;
									}
								}

								// Track Closest Any Air Spot (The fallback)
								if (!foundAnyAir || distSq < bestAirDistSq) {
									bestAirDistSq = distSq;
									aX = x + 0.5D; aY = y; aZ = z + 0.5D;
									foundAnyAir = true;
								}
							}
						}
					}
				}

				// --- EXECUTE SNAP ---
				if (foundGrounded) {
					player.setPositionAndUpdate(gX, gY, gZ);
				} 
				else if (foundAnyAir) {
					player.setPositionAndUpdate(aX, aY, aZ);
				} 
				else {
					// If absolutely no air was found in a 7x7x4 area, keep noClip 
					// on so the player can continue drifting out manually.
					player.noClip = true;
					return; // Wait for next tick to try search again or wait for player to move
				}
			}

			// --- FINAL CLEANUP (Ran if safe, or after a successful snap) ---
			player.getEntityData().setBoolean("IsMistFormSmall", false);
			if (!player.capabilities.isCreativeMode) {
				player.noClip = false;
			}
		}
	}

	@SubscribeEvent
	public void onVillagerTick(LivingEvent.LivingUpdateEvent event) {
		if (event.entityLiving instanceof EntityVillager && !event.entityLiving.worldObj.isRemote) {
			EntityVillager villager = (EntityVillager) event.entityLiving;
			NBTTagCompound nbt = villager.getEntityData();

			if (nbt.hasKey("ReportingTimer")) {
				int timer = nbt.getInteger("ReportingTimer");

				if (villager.isEntityAlive() && (!WitcheryUtil.isKnockedOut(villager) || WitcheryUtil.isSleeping(villager))) {
					if (timer > 1) {
						// Count down
						nbt.setInteger("ReportingTimer", timer - 1);
					} else {
						// TIMER HIT ZERO: The villager successfully reported you!
						EntityPlayer player = villager.worldObj.func_152378_a(UUID.fromString(nbt.getString("AggressorUUID")));
						if (player != null) {
							SuspicionUtil.increaseSuspicion(player, 2000);
							GeneralUtil.sendDebugMessage(player, new ChatComponentText("Your meal has gotten away").setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
						}
						nbt.removeTag("ReportingTimer");
						nbt.removeTag("AggressorUUID");
					}
				} else {
					// Villager died or was knocked out before the timer finished!
					// The report is cancelled.
					nbt.removeTag("ReportingTimer");
					nbt.removeTag("AggressorUUID");
				}
			}
		}
	}
}