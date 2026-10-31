package com.mrsand.StatBoostChanges.mixins.late.legends;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.util.PhysicsUtil;
import com.mrsand.StatBoostChanges.util.SuspicionUtil;
import com.mrsand.StatBoostChanges.util.VampirismUtil;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;
import com.tihyo.legends.abilities.Ability;
import com.tihyo.legends.abilities.AbilityHelper;
import com.tihyo.legends.abilities.LegendsAbilities;
import com.tihyo.legends.client.particles.LegendsParticleType;
import com.tihyo.legends.client.particles.LegendsParticles;
import com.tihyo.legends.effects.LegendsEffects;
import com.tihyo.legends.handlers.LegendsCommonEventHandler;
import com.tihyo.legends.handlers.PowersHandler;
import com.tihyo.legends.management.LegendsDamageSource;
import com.tihyo.legends.management.LegendsHelper;
import com.tihyo.legends.management.PlayerHelper;
import com.tihyo.legends.morph.MorphHelper;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;

@Mixin(value = LegendsCommonEventHandler.class, remap = false)
public abstract class LegendsCommonEventHandlerMixin {



	//Changes how sunlight works for Legends Vampires, no longer get burned, only Power Disable
	@Redirect(method = "onPlayerUpdate", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/entity/player/EntityPlayer;func_70015_d(I)V"))
	private void noFireVampires(EntityPlayer player, int ticks) {
		if(Loader.isModLoaded("witchery")) {
			if(WitcheryUtil.hasFullSunImmunity(player) || WitcheryUtil.getVampireLevel(player) >= 5) {
				return;
			}
		} else {
			player.setFire(1);
		}
	}


	@Redirect(method = "onPlayerUpdate", at = @At(value = "INVOKE",
			target = "Lcom/tihyo/legends/handlers/PowersHandler;sunlight(Lnet/minecraft/entity/player/EntityPlayer;I)V"))
	private void noSunlightInOtherDimensions(EntityPlayer player, int ticks) {
		//GeneralUtil.sendDebugMessage(player, "Intercepted Sunlight Weakness");
		if (WitcheryUtil.hasFullSunImmunity(player)) {
			return;
		}
		if(player.worldObj.provider.dimensionId == 0) {
			if(Loader.isModLoaded("vampirism")) {
				if(!VampirismUtil.isInVampireBiome(player) && player.worldObj.isDaytime()) {
					PowersHandler.sunlight(player, ticks);
				}
			}
			else {
				PowersHandler.sunlight(player, ticks);
			}
		}
	}

	//Changes MistForm
	@Redirect(
			method = "onAbilities", 
			at = @At(value = "INVOKE", target = "Lcom/tihyo/legends/abilities/AbilityHelper;isAbilityActive(Lnet/minecraft/entity/EntityLivingBase;Lcom/tihyo/legends/abilities/Ability;)Z")
			)
	private boolean redirectMistForm(EntityLivingBase entity, Ability ability) {
		if (ability == Ability.mistForm && entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) entity;
			if(player == null) return false;

			// Check if the ability is actually active
			if (AbilityHelper.isAbilityActive(player, Ability.mistForm)) {
				PhysicsUtil.handlePhasingPhysics(player);

				// --- 2. CLIENT SIDE: Particles (Reduced by half) ---
				if (player.worldObj.isRemote) {
					// Alternates between 1 and 2 particles per tick (Average 1.5)
					int count = (player.ticksExisted % 2 == 0) ? 1 : 2;

					for (int k = 0; k < count; k++) {
						LegendsParticles.spawnParticle(
								LegendsParticleType.THICK_SMOKE, 
								player.posX + (player.worldObj.rand.nextDouble() - 0.5D) * player.width, 
								player.posY + (player.worldObj.rand.nextDouble() * player.height) - 0.25D, 
								player.posZ + (player.worldObj.rand.nextDouble() - 0.5D) * player.width, 
								player.worldObj.rand.nextDouble() - 0.5D, 
								-player.worldObj.rand.nextDouble(), 
								player.worldObj.rand.nextDouble() - 0.5D
								);
					}
				}

				// --- 3. SERVER SIDE: AOE Smoke Screen ---
				if (!player.worldObj.isRemote) {
					AxisAlignedBB area = player.boundingBox.expand(10.0D, 10.0D, 10.0D);
					List<Entity> targets = player.worldObj.getEntitiesWithinAABBExcludingEntity(player, area);

					for (Entity target : targets) {
						if (target instanceof EntityLivingBase) {
							EntityLivingBase livingTarget = (EntityLivingBase) target;
							if (!(livingTarget instanceof com.tihyo.legends.entities.mobs.EntityStand) && 
									LegendsHelper.canAOEHit(player, livingTarget)) {

								LegendsEffects.addSmokeScreen(livingTarget, 50, 10);
							}
						}
					}
				}

				// RETURN FALSE: This skips the original code's if-block for Mist Form
				// but does NOT stop the rest of the onAbilities method from running.
				return false;
			}
		}
		if(ability == Ability.vampireBite && entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer)entity;
			if(player == null) return false;
			NBTTagCompound playerNBT = player.getEntityData();
			if (AbilityHelper.isAbilityActive((EntityLivingBase)player, Ability.vampireBite) 
					&& !AbilityHelper.isAbilityActive((EntityLivingBase)player, Ability.hypnosisControl)) {
				//				cir.cancel();

				EntityLivingBase targetEntity = LegendsHelper.getTarget(player, 2.0D); 


				if (targetEntity != null) {
					if (LegendsHelper.isBloodTarget(targetEntity)) {
						String currentVictimUUID = targetEntity.getUniqueID().toString();
						String lastVictimUUID = playerNBT.getString("CurrentBiteTargetUUID");
						if (!currentVictimUUID.equals(lastVictimUUID)) {
							SuspicionUtil.checkForSuspicionWitnesses(player, 250, 128, "", false);
							playerNBT.setString("CurrentBiteTargetUUID", currentVictimUUID);
						}
						if(LegendsHelper.getTicks(player) % 10 == 0) {
							if (!(targetEntity instanceof EntityPlayer) && !(targetEntity instanceof com.tihyo.horror.entities.mobs.EntityVampire)) {
								EntityCreature mob = (EntityCreature)targetEntity;

								if (!MorphHelper.isVampireBeast(player) && ((mob.getEntityToAttack() == player && !LegendsEffects.hasSlowness(targetEntity)) || (Loader.isModLoaded("witchery") && WitcheryUtil.isGuard(targetEntity)))) {
									//									cir.cancel();
								} else {
									//									ExtendedPlayer playerEx = ExtendedPlayer.get(player);
									boolean beast = MorphHelper.isVampireBeast(player);
									boolean checkWitnesses = false;
									int multiplier = beast ? 3 : 1;

									// 1. CROWD CONTROL: Run every 10 ticks (0.5s) to guarantee they cannot escape
									((Entity)targetEntity).motionX = 0.0D; 
									((Entity)targetEntity).motionZ = 0.0D; 
									LegendsEffects.addSlowness((EntityLivingBase)player, 20, 50);
									LegendsEffects.addSlowness(targetEntity, 20, 50);
									PlayerHelper.setStunned(targetEntity, 2);
									AbilityHelper.addEffectAndSync(player, Ability.invincible, 20, 50);

									// 2. DAMAGE & BLOOD DRAIN: Run only every 20 ticks (1.0s)
									if (LegendsHelper.getTicks(player) % 20 == 0) {
										float biteDamage = 7.0F;
										// Ensure they are actually alive before doing anything
										if (!targetEntity.isDead && ((EntityLivingBase) targetEntity).getHealth() > 0) {
											int armorValue = ((EntityLivingBase) targetEntity).getTotalArmorValue();

											// Minecraft armor goes up to 20 (Full Diamond). 
											// Reduce damage by 0.4 for every point of armor.
											// Full diamond (20) would reduce the 10 damage down to 2 damage per second!
											if (armorValue > 0) {
												biteDamage = Math.max(1.0F, biteDamage - (armorValue * 0.4F)); 
											}
											if(Loader.isModLoaded("witchery")) {
												WitcheryUtil.increaseBloodPower(player, (int)Math.ceil(WitcheryUtil.getMaxBloodPower(player)*.03F));
												WitcheryUtil.playDrinkSound(player, targetEntity);
											}

											// Deal the bite damage
											targetEntity.attackEntityFrom(LegendsDamageSource.causeBleedingDamage((Entity)targetEntity, (EntityLivingBase)player, 2, false), biteDamage); 

											// Check if they SURVIVED the bite this second (Continuous Drain)
											// Check if the bite just KILLED them (Death Burst / Bonus)
										}
										else {
											NBTTagCompound nbt = targetEntity.getEntityData();
											//											if (!nbt.getBoolean("gotBloodBonus")) {
											//
											//												if(Loader.isModLoaded("witchery")) {
											//
											//													if (WitcheryUtil.isGuard(targetEntity)) {
											//														checkWitnesses = true;
											//														WitcheryUtil.increaseLegendsVampire(player);
											//														WitcheryUtil.increaseBloodPower(player, (int)(WitcheryUtil.getMaxBloodPower(player) * 0.1F) * multiplier); 
											//														if(beast) player.addExperience(1069);
											//													}
											//													else if(targetEntity instanceof EntityVillager) {
											//														checkWitnesses = true;
											//														EntityVillager target = (EntityVillager) targetEntity;
											//														//															ExtendedVillager villagerEx = ExtendedVillager.get(target);    
											//														WitcheryUtil.increaseLegendsVampire(player); //For recovering/saving a villager?
											//
											//														if(WitcheryUtil.getBlood(target) >= 400) {
											//
											//															WitcheryUtil.increaseBloodPower(player, (int)(WitcheryUtil.getMaxBloodPower(player)));
											//															GeneralUtil.villagerDied(player);
											//															if(beast && GeneralUtil.getClosestVillage(targetEntity, 128) != null) {
											//																player.addExperience(1069);
											//
											//																WitcheryUtil.increaseLegendsVampire(player);
											//															}
											//														} else{
											//															WitcheryUtil.increaseBloodPower(player, (int)(WitcheryUtil.getMaxBloodPower(player) * 0.1F * multiplier)); 
											//														}
											//													}
											//													//														else {
											//													//															WitcheryUtil.increaseBloodPower(player, (int)(WitcheryUtil.getMaxBloodPower(player) * 0.02857142857F * multiplier)); 
											//													//														}
											//													WitcheryUtil.drinkParticles(player, targetEntity);
											//												}
											//												else if(!Loader.isModLoaded("witchery") && targetEntity instanceof EntityVillager) {
											//													player.addExperience(4069);
											//													checkWitnesses = true;
											//												}
											//												nbt.setBoolean("gotBloodBonus", true);
											//											}

											// Suspicion checks if a human died
											//											if(checkWitnesses) {
											//												if (Loader.isModLoaded("witchery") && GeneralUtil.checkForSuspicionWitnesses(player, 0, 128)) {
											//													WitcheryUtil.spawnHunter(player);
											//												} else {
											//													GeneralUtil.increaseSuspicion(player, 1250);
											//												}
											//											}
										}

									}
								}
							}
						} 
					}
				}
			}
		}
		return AbilityHelper.isAbilityActive(entity, ability);
	}

	//Bug fix, bites must work on unprovoked enemies, if witchery is loaded give blood
	@Inject(method = "onAbilities", at = @At("HEAD"), cancellable = true)
	private void abilityFix(LivingEvent.LivingUpdateEvent event, CallbackInfo cir) {
		if (event.entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer)event.entity;
			if (AbilityHelper.isAbilityActive((EntityLivingBase)player, Ability.fortitude)) {
				int amplifier = (AbilityHelper.getEffect((EntityLivingBase)player, Ability.fortitude.id)).amplifier + 1;
				// Prevent division by zero in modulo calculation
				if (amplifier == 0) {
					cir.cancel();
					return;
				}

			}
			NBTTagCompound playerNBT = player.getEntityData();
			//			if (AbilityHelper.isAbilityActive((EntityLivingBase)player, Ability.vampireBite) 
			//					&& !AbilityHelper.isAbilityActive((EntityLivingBase)player, Ability.hypnosisControl)) {
			//				cir.cancel();
			//
			//				EntityLivingBase targetEntity = LegendsHelper.getTarget(player, 2.0D); 
			//
			//
			//				if (targetEntity != null) {
			//					if (LegendsHelper.isBloodTarget(targetEntity)) {
			//						String currentVictimUUID = targetEntity.getUniqueID().toString();
			//						String lastVictimUUID = playerNBT.getString("CurrentBiteTargetUUID");
			//						if (!currentVictimUUID.equals(lastVictimUUID)) {
			//							GeneralUtil.checkForSuspicionWitnesses(player, 250, 128, "", false);
			//							playerNBT.setString("CurrentBiteTargetUUID", currentVictimUUID);
			//						}
			//						if(LegendsHelper.getTicks(player) % 10 == 0) {
			//							if (!(targetEntity instanceof EntityPlayer) && !(targetEntity instanceof com.tihyo.horror.entities.mobs.EntityVampire)) {
			//								EntityCreature mob = (EntityCreature)targetEntity;
			//
			//								if (!MorphHelper.isVampireBeast(player) && ((mob.getEntityToAttack() == player && !LegendsEffects.hasSlowness(targetEntity)) || (Loader.isModLoaded("witchery") && WitcheryUtil.isGuard(targetEntity)))) {
			//									cir.cancel();
			//								} else {
			//									//									ExtendedPlayer playerEx = ExtendedPlayer.get(player);
			//									boolean beast = MorphHelper.isVampireBeast(player);
			//									boolean checkWitnesses = false;
			//
			//									// 1. CROWD CONTROL: Run every 10 ticks (0.5s) to guarantee they cannot escape
			//									((Entity)targetEntity).motionX = 0.0D; 
			//									((Entity)targetEntity).motionZ = 0.0D; 
			//									LegendsEffects.addSlowness((EntityLivingBase)player, 20, 50);
			//									LegendsEffects.addSlowness(targetEntity, 20, 50);
			//									PlayerHelper.setStunned(targetEntity, 2);
			//									AbilityHelper.addEffectAndSync(player, Ability.invincible, 20, 50);
			//
			//									// 2. DAMAGE & BLOOD DRAIN: Run only every 20 ticks (1.0s)
			//									if (LegendsHelper.getTicks(player) % 20 == 0) {
			//										float biteDamage = 7.0F;
			//										// Ensure they are actually alive before doing anything
			//										if (!targetEntity.isDead && ((EntityLivingBase) targetEntity).getHealth() > 0) {
			//											int armorValue = ((EntityLivingBase) targetEntity).getTotalArmorValue();
			//
			//											// Minecraft armor goes up to 20 (Full Diamond). 
			//											// Reduce damage by 0.4 for every point of armor.
			//											// Full diamond (20) would reduce the 10 damage down to 2 damage per second!
			//											if (armorValue > 0) {
			//												biteDamage = Math.max(1.0F, biteDamage - (armorValue * 0.4F)); 
			//											}
			//
			//											// Deal the bite damage
			//											targetEntity.attackEntityFrom(LegendsDamageSource.causeBleedingDamage((Entity)targetEntity, (EntityLivingBase)player, 2, false), biteDamage); 
			//											int multiplier = beast ? 3 : 1;
			//											// Check if they SURVIVED the bite this second (Continuous Drain)
			//											if (!targetEntity.isDead && ((EntityLivingBase) targetEntity).getHealth() > 0) {
			//
			//												player.setHealth(player.getHealth() + 4);
			//
			//												// Give per-second blood based on the target type
			//												if(Loader.isModLoaded("witchery")) {
			//
			//													float multi = .87F - WitcheryUtil.getVampireLevel(player)*.04F; //was .9 so now .47 is max at level 10
			//													WitcheryUtil.increaseBloodPower(player, (int)Math.ceil(WitcheryUtil.getMaxBloodPower(player)*.03F), (int)Math.ceil((WitcheryUtil.getMaxBloodPower(player) * multi)));		
			//													WitcheryUtil.playDrinkSound(player, targetEntity);
			//												}
			//											} 
			//											// Check if the bite just KILLED them (Death Burst / Bonus)
			//											else {
			//												NBTTagCompound nbt = targetEntity.getEntityData();
			//												if (!nbt.getBoolean("gotBloodBonus")) {
			//
			//													if(Loader.isModLoaded("witchery")) {
			//
			//														if (WitcheryUtil.isGuard(targetEntity)) {
			//															checkWitnesses = true;
			//															WitcheryUtil.increaseLegendsVampire(player);
			//															WitcheryUtil.increaseBloodPower(player, (int)(WitcheryUtil.getMaxBloodPower(player) * 0.1F) * multiplier); 
			//															if(beast) player.addExperience(1069);
			//														}
			//														else if(targetEntity instanceof EntityVillager) {
			//															checkWitnesses = true;
			//															EntityVillager target = (EntityVillager) targetEntity;
			//															//															ExtendedVillager villagerEx = ExtendedVillager.get(target);    
			//															WitcheryUtil.increaseLegendsVampire(player);
			//
			//															if(WitcheryUtil.getBlood(target) >= 400) {
			//
			//																WitcheryUtil.increaseBloodPower(player, (int)(WitcheryUtil.getMaxBloodPower(player)));
			//																GeneralUtil.villagerDied(player);
			//																if(beast && GeneralUtil.getClosestVillage(targetEntity, 128) != null) {
			//																	player.addExperience(1069);
			//
			//																	WitcheryUtil.increaseLegendsVampire(player);
			//																}
			//															} else{
			//																WitcheryUtil.increaseBloodPower(player, (int)(WitcheryUtil.getMaxBloodPower(player) * 0.1F * multiplier)); 
			//															}
			//														}
			//														//														else {
			//														//															WitcheryUtil.increaseBloodPower(player, (int)(WitcheryUtil.getMaxBloodPower(player) * 0.02857142857F * multiplier)); 
			//														//														}
			//														WitcheryUtil.drinkParticles(player, targetEntity);
			//													}
			//													if(!Loader.isModLoaded("witchery") && targetEntity instanceof EntityVillager) {
			//														player.addExperience(4069);
			//														checkWitnesses = true;
			//													}
			//													nbt.setBoolean("gotBloodBonus", true);
			//												}
			//
			//												// Suspicion checks if a human died
			//												if(checkWitnesses) {
			//													if (Loader.isModLoaded("witchery") && GeneralUtil.checkForSuspicionWitnesses(player, 0, 128)) {
			//														WitcheryUtil.spawnHunter(player);
			//													} else {
			//														GeneralUtil.increaseSuspicion(player, 1250);
			//													}
			//												}
			//											}
			//										}
			//										cir.cancel();
			//									}
			//								}
			//							}
			//						} 
			//					}
			//				}
			//			}
			//			//If bite is currently not running
			//			else { 
			//				if (playerNBT.hasKey("CurrentBiteTargetUUID")) {
			//					String victimUUID = playerNBT.getString("CurrentBiteTargetUUID");
			//					Entity victim = GeneralUtil.getEntityByUUID(player.worldObj, victimUUID);
			//
			//					if (victim instanceof EntityVillager && victim.isEntityAlive() && !GeneralUtil.checkForSuspicionWitnesses(player, 0, 128)) {
			//						// Tag the villager with a timer and the Player's UUID
			//						NBTTagCompound victimNBT = victim.getEntityData();
			//						EntityVillager villager = (EntityVillager)victim;
			//						if(villager.villageObj != null) {
			//							victimNBT.setInteger("ReportingTimer", 600); // 30 seconds to kill them
			//							victimNBT.setString("AggressorUUID", player.getUniqueID().toString());
			//						}
			//
			//						// Make the villager actually run away (Pathfinding)
			//						//			            if (victim instanceof EntityCreature) {
			//						//			                EntityCreature creature = (EntityCreature) victim;
			//						//			                Vec3 fleePos = RandomPositionGenerator.findRandomTargetBlockAwayFrom(creature, 16, 7, Vec3.createVectorHelper(player.posX, player.posY, player.posZ));
			//						//			                if (fleePos != null) {
			//						//			                    creature.getNavigator().tryMoveToXYZ(fleePos.xCoord, fleePos.yCoord, fleePos.zCoord, 1.5D); // Sprint speed
			//						//			                }
			//						//			            }
			//					}
			//					playerNBT.removeTag("CurrentBiteTargetUUID");
			//				}
			//			}
			if (AbilityHelper.isAbilityActive((EntityLivingBase)player, Ability.hypnosisControl)) {
				cir.cancel();
				// Gaze channel uses DIY float/look — skip Legends yank while HYPNOTIZING
				VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
				if (vPlayer != null && vPlayer.getBoolean(PlayerKeys.HYPNOTIZING)) {
					return;
				}
				// Finds the target entity the player is looking at, up to 25 blocks away
				EntityLivingBase target = LegendsHelper.getTarget(player, 25.0D);
				Boolean paralysed = false;

				if(Loader.isModLoaded("witchery") && WitcheryUtil.canHypnotizeVictim(target)) {
					paralysed = WitcheryUtil.isParalyzedActive(target);
				}

				if (PlayerHelper.canBeTouched(player, target) && LegendsHelper.canLiftTarget(target))
					if (target instanceof EntityPlayer) {
						// Check if the target is NOT in Creative mode (cannot be damaged/affected)
						if (!((EntityPlayer)target).capabilities.disableDamage) { // field_71075_bZ.field_75098_d

							// Check for 'isFlying' status (or a custom 'isUsingPower' flag)
							if (player.isSprinting() == true) { // player.field_82175_bq is usually 'isSprinting'
								PowersHandler.hypnosis(player, 10); // A simpler self-application or related effect
							} else {
								LegendsAbilities.hypnosis(target);

								// --- Calculate velocity to pull the target towards the player ---

								// Get the player's look vector (normalized direction)
								Vec3 lookVector = player.getLookVec(); // func_70040_Z()

								// Calculate the displacement vector: (PlayerPos + LookVector * Distance) - TargetPos
								double targetX = player.posX + lookVector.xCoord * 3.0D - target.posX; // field_70165_t, field_72450_a
								double targetY = player.posY + lookVector.yCoord * 5.0D - target.posY; // field_70163_u, field_72448_b
								double targetZ = player.posZ + lookVector.zCoord * 3.0D - target.posZ; // field_70161_v, field_72449_c

								double distance = Math.sqrt(targetX * targetX + targetY * targetY + targetZ * targetZ);

								// Set the target's motion (pull velocity)
								target.motionX = targetX / distance * 0.1D; // field_70159_w

								// Only apply vertical motion if the target is below the player
								if (target.posY < player.posY) // field_70163_u
									target.motionY = targetY / distance * 0.1D; // field_70181_x

								target.motionZ = targetZ / distance * 0.1D; // field_70179_y

								target.fallDistance = 0.0F; // field_70143_R (Reset fall distance while being pulled)
							}
						}
					} else if (player.isSprinting() == true) { // Non-Player Entity logic (same as player 'isSprinting' branch)
						PowersHandler.hypnosis(player, 10);
					} else if (!paralysed){

						// Non-Player Entity logic (same as player normal branch)
						LegendsAbilities.hypnosis(target);
						// ... (Velocity calculation and application code is identical)
						Vec3 lookVector = player.getLookVec(); // func_70040_Z()

						// Calculate the displacement vector: (PlayerPos + LookVector * Distance) - TargetPos
						double targetX = player.posX + lookVector.xCoord * 3.0D - target.posX; // field_70165_t, field_72450_a
						double targetY = player.posY + lookVector.yCoord * 5.0D - target.posY; // field_70163_u, field_72448_b
						double targetZ = player.posZ + lookVector.zCoord * 3.0D - target.posZ; // field_70161_v, field_72449_c

						double distance = Math.sqrt(targetX * targetX + targetY * targetY + targetZ * targetZ);

						// Set the target's motion (pull velocity)
						target.motionX = targetX / distance * 0.1D; // field_70159_w

						// Only apply vertical motion if the target is below the player
						if (target.posY < player.posY) // field_70163_u
							target.motionY = targetY / distance * 0.1D; // field_70181_x

						target.motionZ = targetZ / distance * 0.1D; // field_70179_y

						target.fallDistance = 0.0F; // field_70143_R (Reset fall distance while being pulled)
					}
			}

		}
	}
}