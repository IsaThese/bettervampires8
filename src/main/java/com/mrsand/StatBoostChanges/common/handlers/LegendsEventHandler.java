package com.mrsand.StatBoostChanges.common.handlers;

import java.util.UUID;

import com.emoniph.witchery.Witchery;
import com.mrsand.StatBoostChanges.abilities.VampireRestoredAbilities;
import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.LegendsAbilityBridge;
import com.mrsand.StatBoostChanges.util.LegendsUtil;
import com.mrsand.StatBoostChanges.util.MistPhaseDebug;
import com.mrsand.StatBoostChanges.util.MorphUtil;
import com.mrsand.StatBoostChanges.util.PhysicsUtil;
import com.mrsand.StatBoostChanges.util.SuspicionUtil;
import com.mrsand.StatBoostChanges.util.VampireBloodChannels;
import com.mrsand.StatBoostChanges.util.VampirismUtil;
import com.mrsand.StatBoostChanges.util.WebPassDebug;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;
import com.tihyo.legends.management.LegendsHelper;
import com.tihyo.legends.morph.MorphHelper;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.relauncher.Side;
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
						LegendsUtil.addVampireKillSkillPoints(player);
					}
				} else {
					WitcheryUtil.increaseBloodPower(player, (int) (WitcheryUtil.getMaxBloodPower(player) * 0.1F * multiplier));
				}
			} else if (isGuard) {
				WitcheryUtil.increaseBloodPower(player, (int) (WitcheryUtil.getMaxBloodPower(player) * 0.1F) * multiplier);
				if (isVampireBeast) LegendsUtil.addVampireKillSkillPoints(player);
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

								LegendsUtil.addVampireKillSkillPoints(player);
							}
							else if(WitcheryUtil.isGuard(entity)) {
								WitcheryUtil.increaseBlood(player, 0, .4F);
								if(WitcheryUtil.getBloodLevel(player) == WitcheryUtil.getMaxBloodPower(player))
									increase = true;
								LegendsUtil.addVampireKillSkillPoints(player);
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
							LegendsUtil.addVampireKillSkillPoints(player);
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
			VExtendedPlayer vPlayer = VExtendedPlayer.get(serverPlayer);
			if (Loader.isModLoaded("Morph") && vPlayer != null) {
				int before = vPlayer.getInteger(PlayerKeys.COUNTER_MORPH);
				int counter = before;
				if (MorphUtil.getMorphTime(serverPlayer) == .0125F) {
					counter = 85;
				}
				if (counter == 1 && MorphUtil.isPlayerMorphed(serverPlayer)) {
					LegendsUtil.ifMorphedTurnHuman(serverPlayer);
				}
				if (counter > 0) {
					counter--;
				}
				// Never syncClient every tick — full IEEP flood thrashes HYPNOTIZING / eyes / ReplayMod.
				if (counter != before) {
					vPlayer.setInteger(PlayerKeys.COUNTER_MORPH, counter);
					vPlayer.syncClient();
				}
			}
			// Catch-up: Witchery / Vampirism vampires with no Legends legacy yet.
			if (player.ticksExisted % 40 == 0 && !LegendsUtil.isVampire(player)) {
				boolean otherModVampire = false;
				if (Loader.isModLoaded("witchery") && WitcheryUtil.isVampire(player)) {
					otherModVampire = true;
				} else if (Loader.isModLoaded("vampirism") && VampirismUtil.isVampire(player)) {
					otherModVampire = true;
				}
				if (otherModVampire) {
					LegendsUtil.ensureVampireLegacy(player);
				}
			}
		}



		if (event.phase == Phase.END) {
			if (Loader.isModLoaded("legends") && !player.worldObj.isRemote) {
				VampireBloodChannels.tick(player);
			}
			if (Loader.isModLoaded("legends") && LegendsAbilityBridge.isMist(player)) {
				MistPhaseDebug.sample(player, "tickEND");
			}
			return;
		}
		if (event.phase != Phase.START) return;

		if (Loader.isModLoaded("legends")) {
			if (!player.worldObj.isRemote) {
				VampireRestoredAbilities.tick(player);
				LegendsUtil.tickMistDemist(player);
			}
			// Web clear + mist noClip must run on client too (movement is client-predicted).
			VampireRestoredAbilities.tickWebPassThrough(player);
			if (LegendsAbilityBridge.isMist(player)) {
				PhysicsUtil.handleMistPhasing(player);
				player.getEntityData().setBoolean("IsMistFormSmall", true);
				MistPhaseDebug.sample(player, "tickSTART");
			}
			if (LegendsUtil.isBat(player)) {
				LegendsUtil.markBatFormActive(player);
			}
		} else if (WebPassDebug.enabled()) {
			VampireRestoredAbilities.tickWebPassThrough(player);
		}

		boolean isMistActive = LegendsAbilityBridge.isMist(player);
		boolean wasMisty = player.getEntityData().getBoolean("IsMistFormSmall");

		// --- CASE: MIST ENDED ---
		if (!isMistActive && wasMisty) {
			// Snap to solid ground if embedded; retry next tick if none found.
			if (!PhysicsUtil.ejectFromSolidsIfNeeded(player)) {
				return;
			}

			player.getEntityData().setBoolean("IsMistFormSmall", false);
			if (!player.capabilities.isCreativeMode) {
				player.noClip = false;
			}
		}

		// --- CASE: BAT ENDED (hitbox expands next ticks — eject must retry like mist) ---
		if (!player.worldObj.isRemote) {
			LegendsUtil.consumeBatEjectIfSafe(player);
		}
	}

	/**
	 * Legends LivingUpdate clears noClip unless isPhasing/isIntangible.
	 * Mist is neither — re-assert after Legends so moveEntity still phases.
	 * Also clear cobweb flag again around LivingUpdate (before moveEntity body).
	 */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onWebLivingUpdateEarly(LivingEvent.LivingUpdateEvent event) {
		if (!(event.entityLiving instanceof EntityPlayer)) return;
		VampireRestoredAbilities.tickWebPassThrough((EntityPlayer) event.entityLiving);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onMistLivingUpdate(LivingEvent.LivingUpdateEvent event) {
		if (!(event.entityLiving instanceof EntityPlayer)) return;
		EntityPlayer player = (EntityPlayer) event.entityLiving;
		VampireRestoredAbilities.tickWebPassThrough(player);
		if (!Loader.isModLoaded("legends")) return;
		// After Horror Transvection flight: sneak+jump holds altitude for horizontal-only move.
		PhysicsUtil.tickTransvectionAltitudeLock(player);
		if (!LegendsAbilityBridge.isMist(player)) return;
		// Re-apply full mist physics after Legends (clears noClip + may add flight motionY).
		PhysicsUtil.handleMistPhasing(player);
		MistPhaseDebug.sample(player, "postLegends");
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