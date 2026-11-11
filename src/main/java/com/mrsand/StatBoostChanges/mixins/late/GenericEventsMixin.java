package com.mrsand.StatBoostChanges.mixins.late;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.brewing.potions.PotionEnslaved;
import com.emoniph.witchery.common.ExtendedPlayer;
import com.emoniph.witchery.common.ExtendedVillager;
import com.emoniph.witchery.common.GenericEvents;
import com.emoniph.witchery.entity.EntityVillageGuard;
import com.emoniph.witchery.item.ItemVampireClothes;
import com.emoniph.witchery.network.PacketParticles;
import com.emoniph.witchery.util.ChatUtil;
import com.emoniph.witchery.util.Config;
import com.emoniph.witchery.util.CreatureUtil;
import com.emoniph.witchery.util.EntityUtil;
import com.emoniph.witchery.util.ParticleEffect;
import com.emoniph.witchery.util.SoundEffect;
import com.emoniph.witchery.util.TargetPointUtil;
import com.emoniph.witchery.util.TimeUtil;
import com.emoniph.witchery.util.TransformCreature;
import com.mrsand.StatBoostChanges.abilities.VampireRestoredAbilities;
import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.LegendsUtil;
import com.mrsand.StatBoostChanges.util.SuspicionUtil;
import com.mrsand.StatBoostChanges.util.VampireClothesRepair;
import com.mrsand.StatBoostChanges.util.VisualUtil;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;
import com.tihyo.legends.morph.MorphHelper;
import com.tihyo.legends.morph.MorphRegistry;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.village.Village;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;

@Mixin(value = GenericEvents.class, remap = false)
public abstract class GenericEventsMixin {

	@Inject(method = "onLivingUpdate", at = @At("HEAD"), cancellable = true)
	private void sunlightLevel10(LivingEvent.LivingUpdateEvent event, CallbackInfo ci) {
		// Changes how sunlight works for Level 10 Witchery Vampires,
		// only gives them disable ability, weakness, and miner debuff

		if (!event.entity.worldObj.isRemote && event.entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) event.entity;
			int tick = player.ticksExisted;
			
			
			
			ExtendedPlayer playerEx = ExtendedPlayer.get(player);
			VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
			if(vPlayer.getBoolean(PlayerKeys.IS_VULNERABLE)) {
				if(playerEx.getBloodPower() > (int)playerEx.getMaxBloodPower()*.8) {
					//Forces player to eat a villager?
					vPlayer.setBoolean(PlayerKeys.IS_VULNERABLE, false);
					//Unsure why this was canceled?
					if(LegendsUtil.isVampireBeast(player))
						LegendsUtil.ifMorphedTurnHuman(player);
					vPlayer.syncClient();
					player.addChatComponentMessage(new ChatComponentText("Your immortal vigor returns.").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
					player.worldObj.playSoundAtEntity(player, "mob.wolf.howl", 1.0F, 1.0F);
				}
			}
			if (playerEx.isVampire()) {	
				int decreaseBlood;
				int ifBeast = 1;
				int vLevel = playerEx.getVampireLevel();
				
				int suspicion = SuspicionUtil.getSuspicion(player);
				if(suspicion >= 6000) {
					WitcheryUtil.spawnHunter(player);
					SuspicionUtil.decreaseSuspicion(player, 100);

				}
				if (tick % 40 == 0) {
				    Village village = GeneralUtil.getClosestVillage(player, 90); //Closer to 8 Chunk radius
				    boolean isDay = player.worldObj.isDaytime();

				    if (isDay) {
				        int reputation = (village == null) ? 0 : village.getReputationForPlayer(player.getCommandSenderName());
				        if (village == null || reputation > 0) {
				            SuspicionUtil.decreaseSuspicion(player, 7 + reputation);
				        }
				    } 
				    else {
				        if (village != null) {
				            if (!MorphHelper.isMorphed(player) || MorphHelper.isVampireBeast(player)) {
				                SuspicionUtil.increaseSuspicion(player, 10); 
				            } else if (MorphHelper.isMorphed(player)) {
				                SuspicionUtil.decreaseSuspicion(player, 6);
				            }
				        } 
				        else { 
				            SuspicionUtil.decreaseSuspicion(player, 6);
				        }
				    }
				}
//				if(tick % 400 == 0) {
//					player.addChatComponentMessage(new ChatComponentText("Your suspicions is: " + GeneralUtil.getSuspicion(player)).setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
//				}
				if (player.isInWater()) {
					player.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 40, 3));
					player.setAir(300);
				}
				
				if(Loader.isModLoaded("legends")) {
					if(MorphHelper.isVampireBeast(player))
						ifBeast = 3;
				}
				if(tick % 60 == 0) {
					if(LegendsUtil.isVampire(player)) {
						//Originally was going to do if it wasn't in their hot bar? But was too difficult to implement once sated
						boolean vulnerableStarving = vPlayer.getBoolean(PlayerKeys.IS_VULNERABLE)
								&& vPlayer.getBoolean(PlayerKeys.IS_STARVING)
								&& !GeneralUtil.isInLight(player)
								&& !LegendsUtil.isMist(player);
						if (vulnerableStarving && VampireRestoredAbilities.hasTrueForm(player)
								&& !LegendsUtil.isDraculaTrueForm(player)
								&& !LegendsUtil.isVampireBeast(player)) {
							if (MorphHelper.isMorphed(player)) {
								LegendsUtil.ifMorphedTurnHuman(player);
							}
							MorphHelper.morph(player, MorphRegistry.draculaTrueForm);
							VisualUtil.changeForm(player, true);
						} else if (vulnerableStarving && LegendsUtil.hasLearnedBeastForm(player)
								&& !LegendsUtil.isVampireBeast(player)
								&& !LegendsUtil.isDraculaTrueForm(player)) {
							LegendsUtil.morphBeast(player);
							VisualUtil.changeForm(player, true);
						}

					}

				}

				int maxBlood = playerEx.getMaxBloodPower();

				// 1. Calculate target survival time in ticks
				// Range: 14,400 (12m) to 18,600 (15.5m)
				double targetTicks = 14400 + (vLevel - 1) * 466.67;

				// 2. Calculate the "Heartbeat" Interval
				// How many ticks pass between each 1-unit blood loss
				int interval = (int) Math.round(targetTicks / (maxBlood * ifBeast));

				// 3. Trigger drain
				if (tick % Math.max(1, interval) == 0) {
					playerEx.decreaseBloodPower(1, true);
				}

				// Blood-fed mend for worn vampire clothes
				if (tick % VampireClothesRepair.PASSIVE_INTERVAL_TICKS == 0) {
					VampireClothesRepair.tryPassiveBloodMend(player);
				}

//				if(player.getFoodStats().getFoodLevel() < 18 && tick % 80 == 0) {
//					player.getFoodStats().setFoodLevel(20);
//					player.getFoodStats().setFoodSaturationLevel(5.0F);
//				}
			}
			
			//Should Cancel abilities?
			if (playerEx.isVampire() && CreatureUtil.isInSunlight(player)) {
				ci.cancel();
				// BetterVampires: lord + full Witchery clothes skip; L5+ debuff only; else fire+slow
				if (WitcheryUtil.hasFullSunImmunity(player)) {
					return;
				}
				if(player.worldObj.provider.dimensionId == 0) {
					if (playerEx.getVampireLevel() >= 5) {
						player.addPotionEffect(new PotionEffect(Potion.weakness.id, TimeUtil.secsToTicks(10), 4, false));
						player.addPotionEffect(new PotionEffect(Potion.digSlowdown.id, TimeUtil.secsToTicks(10), 0, true));
					}
					else {
						playerEx.decreaseBloodPower(2, false);
						player.addPotionEffect(new PotionEffect(Potion.weakness.id, TimeUtil.secsToTicks(10), 8, true));
						player.addPotionEffect(new PotionEffect(Potion.digSlowdown.id, TimeUtil.secsToTicks(2), 0, true));
						player.setFire(1);
						player.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, TimeUtil.secsToTicks(2), 2, true));
					}
				}
			}
		}
	}
	@Shadow protected abstract void checkForBloodDrinkingWitnesses(EntityPlayer player, EntityLivingBase victim);

	@Shadow protected abstract boolean villagerIsInCage(EntityVillager target);
	@Shadow protected abstract void playTameEffect(EntityTameable entity, boolean tamed);

	//Entire Method
	@Inject (method = "onEntityInteract", at = @At("HEAD"), cancellable = true)
	private void onEntityInteract(EntityInteractEvent event, CallbackInfo cir) {
		cir.cancel();
		PotionEffect effect = event.entityPlayer.getActivePotionEffect(Witchery.Potions.PARALYSED);
		//Wait why is this here this looks like a bug, oh if the player is paralysed they can't do anything
		if (effect != null && effect.getAmplifier() >= 4) {
			event.setCanceled(true);
			cir.cancel();
			return;
		}
		ExtendedPlayer playerEx = ExtendedPlayer.get(event.entityPlayer);
		ExtendedPlayer.VampirePower power = playerEx.getSelectedVampirePower();
		if (power != ExtendedPlayer.VampirePower.NONE)
			if (power == ExtendedPlayer.VampirePower.DRINK && event.target instanceof EntityLivingBase) {
				if (!event.entityPlayer.worldObj.isRemote) {
					float RANGE = ((EntityLivingBase)event.target).isPotionActive(Witchery.Potions.PARALYSED) ? 2.1F : 1.3F;
					if (event.target.getDistanceSq(event.entityPlayer.posX, event.target.posY, event.entityPlayer.posZ) <= (RANGE * RANGE)) {
						int drinkAmount = ItemVampireClothes.isDrinkBoostActive((EntityLivingBase)event.entityPlayer) ? 15 : 10;
						//New logic
						LegendsUtil.displayFangs(event.entityPlayer);
						if (CreatureUtil.isWerewolf(event.target, true)) {
							event.entityPlayer.attackEntityFrom((DamageSource)new EntityDamageSource(DamageSource.magic.getDamageType(), (Entity)event.entityPlayer), 4.0F);
							ParticleEffect.FLAME.send(SoundEffect.WITCHERY_RANDOM_DRINK, event.entityPlayer.worldObj, event.target.posX, event.target.posY + event.target.height * 0.8D, event.target.posZ, 0.5D, 0.2D, 16);
						} else if (event.target instanceof EntityVillageGuard) {
							EntityVillageGuard target = (EntityVillageGuard)event.target;
							playerEx.increaseBloodPower(target.takeBlood((playerEx.getCreatureType() == TransformCreature.NONE) ? drinkAmount : 2, (EntityLivingBase)event.entityPlayer));
							VampireClothesRepair.mendWornVampireClothes(event.entityPlayer, VampireClothesRepair.DRINK_MEND_AMOUNT);
							ParticleEffect.REDDUST.send(SoundEffect.WITCHERY_RANDOM_DRINK, event.entityPlayer.worldObj, target.posX, target.posY + target.height * 0.8D, target.posZ, 0.5D, 0.2D, 16);
							checkForBloodDrinkingWitnesses(event.entityPlayer, (EntityLivingBase)target);
						} else if (event.target instanceof EntityVillager) {
							EntityVillager target = (EntityVillager)event.target;
							ExtendedVillager villagerEx = ExtendedVillager.get(target);
							playerEx.increaseBloodPower(villagerEx.takeBlood((playerEx.getCreatureType() == TransformCreature.NONE) ? drinkAmount : 2, (EntityLivingBase)event.entityPlayer));
							VampireClothesRepair.mendWornVampireClothes(event.entityPlayer, VampireClothesRepair.DRINK_MEND_AMOUNT);
							ParticleEffect.REDDUST.send(SoundEffect.WITCHERY_RANDOM_DRINK, event.entityPlayer.worldObj, target.posX, target.posY + target.height * 0.8D, target.posZ, 0.5D, 0.2D, 16);
							checkForBloodDrinkingWitnesses(event.entityPlayer, (EntityLivingBase)target);
							//Changed rules for upgrading, No windows between 250-280
							if (playerEx.getVampireLevel() == 2) {
								if ((Config.instance()).allowVampireQuests && villagerEx.getBlood() >= 250 && villagerEx.getBlood() <= 280) {
									if (playerEx.getVampireQuestCounter() >= 5) {
										playerEx.increaseVampireLevel();
									} else {
										SoundEffect.NOTE_PLING.playOnlyTo(event.entityPlayer, 1.0F, 1.0F);
										playerEx.increaseVampireQuestCounter();
									}
								} else if (villagerEx.getBlood() < 250) {
//									playerEx.resetVampireQuestCounter();
								}
							} else if (playerEx.getVampireLevel() == 8 && playerEx.canIncreaseVampireLevel() &&
									villagerIsInCage(target)) {
								if (villagerEx.getBlood() >= 250) {
									if (playerEx.getVampireQuestCounter() >= 5) {
										playerEx.increaseVampireLevel();
									} else {
										SoundEffect.NOTE_PLING.playOnlyTo(event.entityPlayer, 1.0F, 1.0F);
										playerEx.increaseVampireQuestCounter();
									}
								} else if (villagerEx.getBlood() < 250) {
									playerEx.resetVampireQuestCounter();
								}
							}
						} else if (event.target instanceof EntityPlayer) {
							EntityPlayer target = (EntityPlayer)event.target;
							playerEx.increaseBloodPower(ExtendedPlayer.get(target).takeHumanBlood((playerEx.getCreatureType() == TransformCreature.NONE) ? drinkAmount : 2, (EntityLivingBase)event.entityPlayer));
							VampireClothesRepair.mendWornVampireClothes(event.entityPlayer, VampireClothesRepair.DRINK_MEND_AMOUNT);
							ParticleEffect.REDDUST.send(SoundEffect.WITCHERY_RANDOM_DRINK, event.entityPlayer.worldObj, target.posX, target.posY + target.height * 0.8D, target.posZ, 0.5D, 0.2D, 16);
						} else if (event.target instanceof EntityAnimal) {
							//Working for random mobs as well?
							EntityLivingBase target = (EntityLivingBase)event.target;
							if(target.getHealth() != 0) {
								float multiplier = .9F - playerEx.getVampireLevel()*.04F;
								target.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer)event.entityPlayer), 1000.0F);
								playerEx.increaseBloodPower((int)Math.ceil(playerEx.getMaxBloodPower()*.07F), (int)Math.ceil((playerEx.getMaxBloodPower() * multiplier)));
								VampireClothesRepair.mendWornVampireClothes(event.entityPlayer, VampireClothesRepair.DRINK_MEND_AMOUNT);
								ParticleEffect.REDDUST.send(SoundEffect.WITCHERY_RANDOM_DRINK, event.entityPlayer.worldObj, target.posX, target.posY + target.height * 0.8D, target.posZ, 0.5D, 0.2D, 16);
							}
						}
					}
					event.setCanceled(true);
				}
			} else if (power == ExtendedPlayer.VampirePower.MESMERIZE && !GeneralUtil.isInLight(event.entityPlayer)) {
				if (!event.entityPlayer.worldObj.isRemote) {
					if (event.entityPlayer.isSneaking() && playerEx.getVampireLevel() >= 2) {
						playerEx.toggleVampireVision();
					} else if (playerEx.getCreatureType() == TransformCreature.NONE && playerEx.getVampireLevel() >= 2) {
						if ((event.target instanceof EntityVillager && !(event.target instanceof com.emoniph.witchery.entity.EntityVillagerWere)) || event.target instanceof EntityPlayer || event.target instanceof EntityVillageGuard) {
							EntityLivingBase victim = (EntityLivingBase)event.target;
							if (!victim.isPotionActive(Witchery.Potions.PARALYSED)) {
								if (playerEx.decreaseBloodPower(ExtendedPlayer.VampirePower.MESMERIZE.INITIAL_COST, true)) {
									victim.addPotionEffect(new PotionEffect(Witchery.Potions.PARALYSED.id, TimeUtil.secsToTicks(5 + playerEx.getVampireLevel() / 2 + Math.max(0, (playerEx.getVampireLevel() - 4) / 2) + (ItemVampireClothes.isMezmeriseBoostActive((EntityLivingBase)event.entityPlayer) ? 3 : 0)), (playerEx.getVampireLevel() >= 8) ? 5 : 4));
									SoundEffect.WITCHERY_RANDOM_HYPNOSIS.playAtPlayer(event.entity.worldObj, event.entityPlayer, 0.5F, 1.0F);
								} else {
									SoundEffect.NOTE_SNARE.playOnlyTo(event.entityPlayer, 1.0F, 0.5F);
								}
							} else {
								SoundEffect.NOTE_SNARE.playOnlyTo(event.entityPlayer, 1.0F, 0.5F);
							}
						} else {
							SoundEffect.NOTE_SNARE.playOnlyTo(event.entityPlayer, 1.0F, 0.5F);
						}
					} else {
						SoundEffect.NOTE_SNARE.playOnlyTo(event.entityPlayer, 1.0F, 0.5F);
					}
					event.setCanceled(true);
				}
			} else {
				event.setCanceled(true);
			}
		if (event.isCanceled())
			return;
		if (event.target != null && !event.target.worldObj.isRemote && event.target instanceof EntityLiving &&
				PotionEnslaved.isMobEnslavedBy((EntityLiving)event.target, event.entityPlayer)) {
			EntityPlayer player = event.entityPlayer;
			EntityLiving creature = (EntityLiving)event.target;
			ItemStack heldObject = player.getHeldItem();
			if (Witchery.Items.GENERIC.itemGraveyardDust.isMatch(heldObject) && creature instanceof com.emoniph.witchery.entity.EntitySummonedUndead) {
				float maxHealth = creature.getMaxHealth() + 2.0F;
				if (maxHealth <= 50.0F) {
					IAttributeInstance attribute = creature.getEntityAttribute(SharedMonsterAttributes.maxHealth);
					if (attribute != null) {
						attribute.setBaseValue(maxHealth);
						creature.setHealth(maxHealth);
						creature.func_110163_bv();  // (no mapping found)
						Witchery.packetPipeline.sendToAllAround((IMessage)new PacketParticles(ParticleEffect.INSTANT_SPELL, SoundEffect.MOB_SILVERFISH_KILL, (Entity)creature, 0.5D, 1.0D), TargetPointUtil.from((Entity)creature, 16.0D));
						if (!player.capabilities.isCreativeMode) {
							heldObject.stackSize--;
							if (player instanceof EntityPlayerMP)
								((EntityPlayerMP)player).sendContainerToPlayer(player.inventoryContainer);
						}
					}
				}
				//You can give commanded weapons? crazy
			} else if (heldObject != null && (creature instanceof EntityZombie || creature instanceof net.minecraft.entity.monster.EntityPigZombie || creature instanceof EntitySkeleton)) {
				if (heldObject.getItem() instanceof ItemArmor) {
					ItemArmor armor = (ItemArmor)heldObject.getItem();
					if (creature.getEquipmentInSlot(4 - armor.armorType) == null) {
						creature.setCurrentItemOrArmor(4 - armor.armorType, heldObject.splitStack(1));
						creature.func_110163_bv();  // (no mapping found)
						if (player instanceof EntityPlayerMP)
							((EntityPlayerMP)player).sendContainerToPlayer(player.inventoryContainer);
					}
				} else if (heldObject.getItem() instanceof net.minecraft.item.ItemSword &&
						creature.getEquipmentInSlot(0) == null) {
					creature.setCurrentItemOrArmor(0, heldObject.splitStack(1));
					creature.func_110163_bv();  // (no mapping found)
					if (player instanceof EntityPlayerMP)
						((EntityPlayerMP)player).sendContainerToPlayer(player.inventoryContainer);
				}
			}
		}
		if (event.target != null && !event.target.worldObj.isRemote && event.target instanceof EntityVillager) {
			EntityVillager villager = (EntityVillager)event.target;
			ExtendedVillager villagerEx = ExtendedVillager.get(villager);
			ItemStack heldObject = event.entityPlayer.getHeldItem();
			if (!villager.isChild() && heldObject != null && heldObject.getItem() == Items.leather_chestplate && event.entityPlayer.isSneaking()) {
				villager.isWithinHomeDistanceCurrentPosition();
				Village village = villager.villageObj;

				if (village != null) {
					int rep = village.getReputationForPlayer(event.entityPlayer.getCommandSenderName());
					if (rep >= 10) {
						if (village.getNumVillagers() > 8) {
							List list = event.entity.worldObj.getEntitiesWithinAABB(EntityVillageGuard.class, AxisAlignedBB.getBoundingBox(
									(village.getCenter().posX - village.getVillageRadius()),
									(village.getCenter().posY - 4),
									(village.getCenter().posZ - village.getVillageRadius()),
									(village.getCenter().posX + village.getVillageRadius()),
									(village.getCenter().posY + 4),
									(village.getCenter().posZ + village.getVillageRadius())));
							int numGuards = list.size();
							if (numGuards < MathHelper.floor_double(village.getNumVillagers() * 0.25D)) {
								int villagerNumTrades = (villager.buyingList == null) ? 1 : villager.buyingList.size();
								if (!CreatureUtil.isWerewolf(event.target, true) && event.target.worldObj.rand.nextInt(villagerNumTrades * 2 + 1) == 0) {
									villager.playSound("mob.villager.yew", 1.0F, (villager.worldObj.rand.nextFloat() - villager.worldObj.rand.nextFloat()) * 0.2F + 1.0F);
									ChatUtil.sendTranslated(EnumChatFormatting.GREEN, (ICommandSender)event.entityPlayer, "witchery.village.villageracceptsguardduty", new Object[0]);
									EntityVillageGuard.createFrom(villager);
								} else {
									ChatUtil.sendTranslated(EnumChatFormatting.RED, (ICommandSender)event.entityPlayer, "witchery.village.villagerrefusesguardduty", new Object[0]);
									villager.playSound("mob.villager.no", 1.0F, (villager.worldObj.rand.nextFloat() - villager.worldObj.rand.nextFloat()) * 0.2F + 1.0F);
								}
							} else {
								ChatUtil.sendTranslated(EnumChatFormatting.BLUE, (ICommandSender)event.entityPlayer, "witchery.village.toomanyguards", new Object[0]);
								SoundEffect.NOTE_SNARE.playOnlyTo(event.entityPlayer);
							}
						} else {
							ChatUtil.sendTranslated(EnumChatFormatting.BLUE, (ICommandSender)event.entityPlayer, "witchery.village.villagetoosmall", new Object[0]);
							SoundEffect.NOTE_SNARE.playOnlyTo(event.entityPlayer);
						}
					} else {
						ChatUtil.sendTranslated(EnumChatFormatting.BLUE, (ICommandSender)event.entityPlayer, "witchery.village.reptoolow", new Object[0]);
						SoundEffect.NOTE_SNARE.playOnlyTo(event.entityPlayer);
					}
				}
			}
		}
		if (!event.entity.worldObj.isRemote && event.target != null && event.target instanceof EntityWolf) {
			EntityWolf wolf = (EntityWolf)event.target;
			if (playerEx.getWerewolfLevel() == 7 && playerEx.getWolfmanQuestState() == ExtendedPlayer.QuestState.STARTED && playerEx.getCreatureType() == TransformCreature.WOLF && !wolf.isTamed() && !wolf.isAngry())
				if (wolf.worldObj.rand.nextInt(3) == 0) {
					wolf.setTamed(true);
					wolf.setPathToEntity((PathEntity)null);
					wolf.setAttackTarget((EntityLivingBase)null);
					wolf.func_70907_r().setSitting(true);  // getAISit().setSitting()
					wolf.setHealth(20.0F);
					wolf.func_152115_b(event.entityPlayer.getUniqueID().toString());
					playTameEffect((EntityTameable)wolf, true);
					wolf.worldObj.setEntityState((Entity)wolf, (byte)7);
					playerEx.increaseWolfmanQuestCounter();
				} else {
					playTameEffect((EntityTameable)wolf, false);
					wolf.worldObj.setEntityState((Entity)wolf, (byte)6);
					if (wolf.worldObj.rand.nextInt(10) == 0) {
						wolf.setAngry(true);
						wolf.setAttackTarget((EntityLivingBase)event.entityPlayer);
					}
				}
		}
		ItemStack heldStack = event.entityPlayer.getHeldItem();
		if (heldStack != null) {
			if (heldStack.getItem() == Witchery.Items.TAGLOCK_KIT) {
				Witchery.Items.TAGLOCK_KIT.onEntityInteract(event.entityPlayer.worldObj, event.entityPlayer, heldStack, event);
				if (event.isCanceled())
					return;
			}
			if (heldStack.getItem() == Witchery.Items.BLOOD_GOBLET) {
				Witchery.Items.BLOOD_GOBLET.onEntityInteract(event.entityPlayer.worldObj, event.entityPlayer, heldStack, event);
				if (event.isCanceled())
					return;
			}
			if (Witchery.Items.GENERIC.itemWoodenStake.isMatch(heldStack) && (Config.instance()).allowStakingVampires)
				if (event.target instanceof EntityPlayer) {
					EntityPlayer victim = (EntityPlayer)event.target;
					if (ExtendedPlayer.get(victim).isVampire() && victim.sleeping) {
						ParticleEffect.REDDUST.send(SoundEffect.WITCHERY_RANDOM_DRINK, victim.worldObj, event.target.posX, event.target.posY, event.target.posZ, event.target.width, event.target.height, 16);
						EntityUtil.instantDeath((EntityLivingBase)victim, (EntityLivingBase)event.entityPlayer);
						if (!event.entityPlayer.capabilities.isCreativeMode)
							heldStack.stackSize--;
						event.setCanceled(true);
						return;
					}
				}
		}
	}
	/** BV owns day→night via Legends coffin Space-skip; suppress Witchery coffin time jump. */
	@Inject(method = "onPlayerWakeUpEvent", at = @At("HEAD"), cancellable = true)
	private void bvCancelWitcheryCoffinTimeSkip(net.minecraftforge.event.entity.player.PlayerWakeUpEvent event,
			CallbackInfo ci) {
		if (event.entityPlayer != null && GeneralUtil.isVampire(event.entityPlayer)) {
			ci.cancel();
		}
	}

}




