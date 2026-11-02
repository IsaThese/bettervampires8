package com.mrsand.StatBoostChanges.common.handlers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.common.ExtendedPlayer;
import com.emoniph.witchery.common.ExtendedPlayer.VampirePower;
import com.emoniph.witchery.entity.EntityBolt;
import com.emoniph.witchery.entity.EntityVillageGuard;
import com.emoniph.witchery.entity.EntityWitchHunter;
import com.emoniph.witchery.item.ItemCaneSword;
import com.emoniph.witchery.network.PacketSound;
import com.emoniph.witchery.util.SoundEffect;
import com.mrsand.StatBoostChanges.ModCompat;
import com.mrsand.StatBoostChanges.StatBoostChanges;
import com.mrsand.StatBoostChanges.StatBoostConfig;
import com.mrsand.StatBoostChanges.abilities.VampireRestoredAbilities;
import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.common.management.VExtendedVillager;
import com.mrsand.StatBoostChanges.common.packets.PacketSyncTarget;
import com.mrsand.StatBoostChanges.common.packets.PacketVignette;
import com.mrsand.StatBoostChanges.common.packets.VExtendedPlayerPacket;
import com.mrsand.StatBoostChanges.util.DamageCalculator;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.HypnosisDebug;
import com.mrsand.StatBoostChanges.util.LegendsUtil;
import com.mrsand.StatBoostChanges.util.MorphFormDebug;
import com.mrsand.StatBoostChanges.util.PhysicsUtil;
import com.mrsand.StatBoostChanges.util.PlayerDataUtil;
import com.mrsand.StatBoostChanges.util.SuspicionUtil;
import com.mrsand.StatBoostChanges.util.VampireHostility;
import com.mrsand.StatBoostChanges.util.VisualUtil;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.LegacyManager;
import com.tihyo.legends.legacy.LegacyRegistry;
import com.tihyo.legends.morph.MorphHelper;

import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.village.Village;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;

public class GeneralEventHandler {

	private int CombatTimer = 15 * 20; 
	private static final Set<UUID> processingBat = new HashSet<>();

	@SubscribeEvent
	public void onLivingUpdate(PlayerTickEvent event) {
		EntityPlayer player = event.player;
		if (player == null) return;

		long tick = player.ticksExisted;

		if (player.isDead || player.getHealth() < 1) {
			SuspicionUtil.setSuspicion(player, 0);
		}

		if (event.phase == Phase.END && !player.worldObj.isRemote && ModCompat.LEGENDS) {
			LegendsUtil.tickMistDemist(player);
		}

		if (event.phase == Phase.END) {
			PotionEffect speed = player.getActivePotionEffect(Potion.moveSpeed);
			boolean hasVampireSpeed = (speed != null && speed.getAmplifier() >= 2);

			if (hasVampireSpeed) {
				if (ModCompat.LEGENDS) {
					LegendsUtil.setStepAssist(player, 1);
				} else {
					player.stepHeight = 2.0F;
				}
			} else if (!ModCompat.LEGENDS && player.stepHeight == 2.0F) {
				player.stepHeight = 0.5F;
			}
			if (GeneralUtil.isVampire(player)) {
				if (ModCompat.WITCHERY && WitcheryUtil.getBloodLevel(player) < 1) {
					player.getFoodStats().foodLevel = 0;
					player.getFoodStats().foodSaturationLevel = 0.0F;
				} else if (player.getFoodStats().getFoodLevel() < 18) {
					player.getFoodStats().foodLevel = 20;
					player.getFoodStats().foodSaturationLevel = 5.0F;
				}			
			}
		}

		// Performance Optimization: Throttle heavy ray-casting down to once every 4 frames
		if (event.side == Side.CLIENT && tick % 4 == 0 && LegacyHelper.getLegacyClass(player) != null) {
			EntityLivingBase target = PhysicsUtil.getTarget(player, 25.0D);

			if (target != null 
					&& (target instanceof EntityMob || target instanceof EntityAgeable) 
					&& !(target instanceof EntityWitchHunter) 
					&& !LegacyHelper.getLegacyClass(player).getWeaknesses(player)
					&& !MorphHelper.isMorphed(player)) 
			{
				if (PhysicsUtil.isMobLookingAtPlayer(target, player, .75) && (VampireRestoredAbilities.hasHypnosisMastery(player) || LegacyHelper.hasLearnedPower(player, LegacyManager.getPowerFromName("hypnosis")) || ExtendedPlayer.get(player).getVampireLevel() >= 8)
						&& VExtendedPlayer.get(player).getBoolean(PlayerKeys.HYPNOTIZING))
					StatBoostChanges.network.sendToServer(new PacketSyncTarget(target.getEntityId()));
			}
		}

		// Timers
		if (event.side == Side.SERVER && event.phase == Phase.START) {
			NBTTagCompound nbt = player.getEntityData();

			if (nbt.getBoolean("hunterCooldown")) {
				int timer = nbt.hasKey("hunterTimer") ? nbt.getInteger("hunterTimer") : 1200;
				timer--;

				if (timer <= 0) {
					nbt.removeTag("hunterCooldown");
					nbt.removeTag("hunterTimer");
				} else {
					nbt.setInteger("hunterTimer", timer);
				}
			}
			if (nbt.getInteger("isInCombat") == 1) {
				int timer = nbt.hasKey("isInCombatTimer") ? nbt.getInteger("isInCombatTimer") : CombatTimer;
				timer -= 1;
				nbt.setInteger("isInCombatTimer", timer);

				if (timer <= 0) {
					nbt.removeTag("isInCombat");
					nbt.removeTag("isInCombatTimer");
				}
			}
		}

		if (event.side == Side.SERVER && event.phase == TickEvent.Phase.START) {
			VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
			if (vPlayer == null) return;

			int counter = vPlayer.getInteger(PlayerKeys.COUNTER_STARVATION);
			if (counter > 0) {
				counter--;
				vPlayer.setInteger(PlayerKeys.COUNTER_STARVATION, counter);
				// Syncing every tick flooded hypnosis/debug chat and network; only push when done
				if (counter == 0) {
					vPlayer.syncClient();
				}
			}

			if (GeneralUtil.isInLight(player) && vPlayer.getBoolean(PlayerKeys.HYPNOTIZING)) {
				WitcheryUtil.endHypnosisChannel(player);
				player.getEntityData().removeTag("bvHypnosisEyesOwned");
			}

			// Mist/bat/wolf: stop bows, food, and other charged item use
			if (LegendsUtil.blocksItemUse(player) && player.isUsingItem()) {
				player.clearItemInUse();
			}

			if (vPlayer.getBoolean(PlayerKeys.OTHERFORM) && !GeneralUtil.isVampire(player)) {
				VisualUtil.changeForm(player, false);
			}

			if (vPlayer.getBoolean(PlayerKeys.IS_STARVING) || LegendsUtil.bloodVision(player)) {
				if (tick % 40 == 0) {
					Village village = GeneralUtil.findNearestVillage(player);
					if (village != null && village.getCenter() != null) {
						double dx = player.posX - village.getCenter().posX;
						double dz = player.posZ - village.getCenter().posZ;
						int distance = (int) Math.sqrt(dx * dx + dz * dz);
						VisualUtil.updateVillageSidebar(player, distance);
					}
				}
			} else {
				Scoreboard sb = player.getWorldScoreboard();
				if (sb.getObjective("VillageTracker") != null) {
					sb.func_96519_k(sb.getObjective("VillageTracker")); 
				}
			}

			if (GeneralUtil.isVampire(player)) {
				int suspicion = vPlayer.getInteger(PlayerKeys.SUSPICION);
				if (suspicion >= 5300) {
					float intensity = Math.min(0.35F, (suspicion - 5300) / (float)(5700 - 5300));
					StatBoostChanges.network.sendTo(new PacketVignette(intensity), (EntityPlayerMP) player);
				} else {
					StatBoostChanges.network.sendTo(new PacketVignette(0.0F), (EntityPlayerMP) player);
				}

				if (tick % 40 == 0) {
					if (vPlayer.getBoolean(PlayerKeys.OTHERFORM) && !LegendsUtil.isHidingMorph(player) && !LegendsUtil.isVampireBeast(player)) {
						SuspicionUtil.checkForSuspicionWitnesses(player, 6, 32, "", false);
					}

					if (suspicion >= 6000) {
						WitcheryUtil.spawnHunter(player);
						SuspicionUtil.decreaseSuspicion(player, 100);
					}

					Village village = GeneralUtil.getClosestVillage(player, 90); 
					boolean isDay = player.worldObj.isDaytime();

					if (isDay) {
						int reputation = (village == null) ? 0 : village.getReputationForPlayer(player.getCommandSenderName());
						if (village == null || reputation > 0) {
							SuspicionUtil.decreaseSuspicion(player, 7 + reputation);
						}
					} else {
						if (village != null) {
							if (!MorphHelper.isMorphed(player) || MorphHelper.isVampireBeast(player)) {
								SuspicionUtil.increaseSuspicion(player, 10); 
							} else if (MorphHelper.isMorphed(player)) {
								SuspicionUtil.decreaseSuspicion(player, 6);
							}
						} else { 
							SuspicionUtil.decreaseSuspicion(player, 6);
						}
					}

					if (player.isInWater()) {
						player.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 40, 3));
						player.setAir(300);
					}
				}
			}

			// Performance Optimization: Removed passives fear checking loops entirely from here. 
			// Only checking hostile mob target clearing and tranced logic here once every 5 ticks.
			if (tick % 5 == 0) {
				List<Entity> entities = GeneralUtil.getEntityListFromEntity(player, 40);
				double posX = player.posX;
				double posY = player.posY;
				double posZ = player.posZ;
				int x = MathHelper.floor_double(posX);
				int y = MathHelper.floor_double(posY);
				int z = MathHelper.floor_double(posZ);

				boolean batBlindStealth = GeneralUtil.hasBatBlindStealth(player);

				if (batBlindStealth) {
					GeneralUtil.tickBatBlindDisengage(player, entities, 5);
				}

				for (Entity entity : entities) {
					if (entity instanceof EntityPlayer) continue;

					if (entity.getEntityData().getBoolean("tranced") || WitcheryUtil.isThralled((EntityLiving) entity)) {
						EntityLiving mob = (EntityLiving) entity;
						NBTTagCompound nbtMob = mob.getEntityData();
						int timer = nbtMob.getInteger("trancedTimer");
						timer -= 5; 
						nbtMob.setInteger("trancedTimer", timer);

						if (timer < 1) {
							nbtMob.removeTag("forgetVampiretimer");
							nbtMob.removeTag("vampirePlayer");
							WitcheryUtil.clearThrall(mob);
							if (mob instanceof EntityMob) {
								GeneralUtil.restoreGeneralTargetTasks((EntityMob) mob);
							}
						} else {
							// DIY thrall: retarget controller's enemies, never the controller
							WitcheryUtil.tickThrallAI(mob);

							if (WitcheryUtil.isThralledBy(mob, player)
									&& mob.getAITarget() == null && mob.getAttackTarget() == null
									&& PlayerDataUtil.isControlling(player)) {
								if (mob.getDistanceSqToEntity(player) > 25) {
									if (!mob.getNavigator().tryMoveToXYZ(posX, posY, posZ, 1.8D) && mob instanceof EntityCreature) {
										EntityCreature creature = (EntityCreature) entity;
										PathEntity path = player.worldObj.getEntityPathToXYZ(entity, x, y, z, 15.0F, true, false, false, true);
										if (path != null) {
											creature.setPathToEntity(path); 
										}
									} 
								}
							}
						}
					}
				}
			}
		}
	}

	// Performance Optimization: Passive Vampire Fear logic has been relocated here to run on individual mob schedules
	@SubscribeEvent
	public void onLivingUpdate(LivingUpdateEvent event) { 
		if (event.entityLiving.worldObj.isRemote || !(event.entityLiving instanceof EntityLiving)) {
			return;
		}

		EntityLiving creature = (EntityLiving) event.entityLiving;
		NBTTagCompound nbt = creature.getEntityData();
		long tick = creature.ticksExisted;

		// Run checks on entities every 10 ticks (0.5 seconds) to save massive processing power
		if (tick % 10 == 0) {

			if (nbt.getBoolean("vampireFear")) {
				String playerStr = nbt.getString("vampirePlayer");
				EntityPlayer targetVampire = creature.worldObj.getPlayerEntityByName(playerStr);
				int fearTimer = nbt.getInteger("fearTimer");

				if (targetVampire == null || targetVampire.isDead) {
					cleanFearNBT(nbt, creature);
					return;
				}
				if (creature.getDistanceToEntity(targetVampire) > 16) {
					fearTimer -= 10;
					nbt.setInteger("fearTimer", fearTimer);
				}

				if (creature instanceof EntityVillager && WitcheryUtil.isKnockedOut((EntityVillager) creature)) {
					fearTimer = 0;
				}
				if (fearTimer <= 0 || WitcheryUtil.isEnslavedBy(targetVampire, creature)) {
					cleanFearNBT(nbt, creature);
				}
			} 
			// If not currently afraid, check if a terrifying vampire player is within its direct proximity
			else if (creature instanceof EntityAnimal || creature instanceof EntityVillager || creature instanceof EntityVillageGuard) {
				EntityPlayer closestPlayer = creature.worldObj.getClosestPlayerToEntity(creature, 16.0D);

				if (closestPlayer != null && !closestPlayer.isDead) {
					if (GeneralUtil.isVampire(closestPlayer) && (LegendsUtil.isVampireBeast(closestPlayer) || LegendsUtil.fangs(closestPlayer))) {

						if (creature instanceof EntityVillageGuard) {
							EntityVillageGuard guard = (EntityVillageGuard) creature;
							if (guard.canEntityBeSeen(closestPlayer) && !WitcheryUtil.isEnslavedBy(closestPlayer, guard)) {
								guard.setAttackTarget(closestPlayer);
								guard.setTarget(closestPlayer);
							}
						} else {
							if (creature instanceof EntityAnimal && creature instanceof EntityWolf) return;
							if (creature instanceof EntityVillager && WitcheryUtil.isKnockedOut((EntityVillager) creature)) return;

							if (!WitcheryUtil.isEnslavedBy(closestPlayer, creature)) {
								nbt.setBoolean("vampireFear", true);
								nbt.setString("vampirePlayer", closestPlayer.getCommandSenderName());
								nbt.setInteger("fearTimer", 100);

								if (!nbt.getBoolean("hasAvoidanceAI") && creature instanceof EntityCreature) {
									float speedOnPan = (float) ((creature instanceof EntityAnimal) ? 1.6D : 1.0D);
									float speedOnSprint = (float) ((creature instanceof EntityAnimal) ? 2.0D : 1.33D);
									creature.tasks.addTask(2, new EntityAIAvoidEntity((EntityCreature) creature, EntityPlayer.class, 16.0F, speedOnPan, speedOnSprint));
									nbt.setBoolean("hasAvoidanceAI", true);
								}
							}
						}
					}
				}

				// Staggered trigger to spawn hunter when player travels into a village while morphed as a beast
				if (creature instanceof EntityVillager && LegendsUtil.isVampireBeast(closestPlayer)) {
					if (tick % 20 == 0 && closestPlayer != null && !closestPlayer.getEntityData().getBoolean("hunterCooldown")
							&& SuspicionUtil.checkForSuspicionWitnesses(closestPlayer, 0) 
							&& GeneralUtil.getClosestVillage(closestPlayer, 128) != null) {
						WitcheryUtil.spawnHunter(closestPlayer, true);
						closestPlayer.getEntityData().setInteger("hunterTimer", 1200);
						closestPlayer.getEntityData().setBoolean("hunterCooldown", true);
					}
				}
			}
		}
	}

	private void cleanFearNBT(NBTTagCompound nbt, EntityLiving creature) {
		nbt.removeTag("vampireFear");
		nbt.removeTag("vampirePlayer");
		nbt.removeTag("fearTimer");
		nbt.removeTag("hasAvoidanceAI"); 
		GeneralUtil.removeAvoidanceTasks(creature);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
	public void onRightClickEmpty(PlayerInteractEvent event) {
		EntityPlayer player = event.entityPlayer;
		if (player == null) return;

		World world = player.worldObj;
		ItemStack held = player.getHeldItem();

		// Form matrix: mist/bat block dig; mist/bat/wolf block right-click use.
		// Exception: Witchery BAT must right-click to toggle exit (PacketSelectPlayerAbility).
		boolean denyDig = event.action == Action.LEFT_CLICK_BLOCK && LegendsUtil.blocksBlockBreak(player);
		boolean denyUse = (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK)
				&& LegendsUtil.blocksItemUse(player);
		if (denyUse && LegendsUtil.isBat(player)) {
			ExtendedPlayer batEx = ExtendedPlayer.get(player);
			if (batEx != null && batEx.getSelectedVampirePower() == VampirePower.BAT) {
				denyUse = false;
			}
		}
		if (denyDig || denyUse) {
			if (MorphFormDebug.enabled()) {
				MorphFormDebug.onFormGate(player, denyDig ? "denyDig" : "denyUse");
			}
			if (LegendsUtil.isMist(player)) {
				player.addChatComponentMessage(new ChatComponentText("You're in mist form").setChatStyle(new ChatStyle().setBold(true).setItalic(true)));
			}
			event.setCanceled(true);
			event.setResult(Result.DENY);
			return;
		}

		if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
			ExtendedPlayer playerEx = ExtendedPlayer.get(player);
			if (playerEx.isVampire() && !GeneralUtil.isInLight(player)) {
				VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
				boolean isMesmerizeSelected = (playerEx.getSelectedVampirePower() == VampirePower.MESMERIZE);
				boolean canUseMesmerize = (VampireRestoredAbilities.hasHypnosisMastery(player) || LegacyHelper.hasLearnedPower(player, LegacyManager.getPowerFromName("hypnosis")) || playerEx.getVampireLevel() >= 8);

				if (playerEx.getSelectedVampirePower() == VampirePower.NONE && held != null && held.getItem() == Witchery.Items.CANE_SWORD) {
					ItemCaneSword cane = (ItemCaneSword) held.getItem();
					if (!cane.isDrawn(player) && !world.isRemote) {
						WitcheryUtil.useNecroStone2(world, player, held);
					}
				}

				if (player.worldObj.isRemote) {
					if (playerEx.getSelectedVampirePower() == VampirePower.DRINK && player.isSneaking() && (VampireRestoredAbilities.hasHypnosisMastery(player) || LegacyHelper.hasLearnedPower(player, LegacyManager.getPowerFromName("hypnosis")))) {
						// Holding an item fires RIGHT_CLICK_BLOCK then RIGHT_CLICK_AIR in one click — only
						// process AIR when holding something; empty hand may only get BLOCK.
						if (held != null && event.action != Action.RIGHT_CLICK_AIR) {
							return;
						}
						NBTTagCompound nbt = player.getEntityData();
						int now = player.ticksExisted;
						if (now == nbt.getInteger("bvWallClimbToggleTick")) {
							return;
						}
						nbt.setInteger("bvWallClimbToggleTick", now);

						boolean climb = vPlayer.getBoolean(PlayerKeys.WALL_CLIMB);
						boolean newClimb = !climb;
						String message = newClimb ? "on" : "off";
						player.addChatComponentMessage(new ChatComponentText("Wall climbing turned " + message)
								.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
						vPlayer.setBoolean(PlayerKeys.WALL_CLIMB, newClimb);
						vPlayer.sync();
					} else if (!player.isSneaking() && isMesmerizeSelected && canUseMesmerize) {
						if (player.isUsingItem() && event.action != Action.LEFT_CLICK_BLOCK) {
							return; 
						}
						// Empty-hand RIGHT_CLICK_BLOCK repeats every tick while the mouse is held —
						// one physical press must be one toggle (same idea as wall-climb debounce).
						if (held != null && event.action != Action.RIGHT_CLICK_AIR) {
							return;
						}
						NBTTagCompound hypnoNbt = player.getEntityData();
						int hypnoNow = player.ticksExisted;
						int lastHypno = hypnoNbt.getInteger("bvHypnoToggleTick");
						if (lastHypno != 0 && hypnoNow - lastHypno >= 0 && hypnoNow - lastHypno < 10) {
							return;
						}
						hypnoNbt.setInteger("bvHypnoToggleTick", hypnoNow == 0 ? 1 : hypnoNow);

						boolean currentHypno = vPlayer.getBoolean(PlayerKeys.HYPNOTIZING);
						boolean newHypnoState = !currentHypno;
						if (HypnosisDebug.enabled()) {
							HypnosisDebug.logToggle(player,
									"interact action=" + event.action + " held=" + (held != null),
									currentHypno, newHypnoState);
						}

						String message = newHypnoState ? "Your gaze deepens..." : "Your focus loosens...";
						player.addChatComponentMessage(new ChatComponentText(message)
								.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));

						if (newHypnoState) {
							WitcheryUtil.beginHypnosisChannel(player);
						} else {
							WitcheryUtil.endHypnosisChannel(player);
						}
					}
				}
			}
		}

		// Bind teleport spot: empty-hand punch same block 3 times (level 10).
		// LEFT_CLICK_BLOCK repeats every tick while held — only count a new punch when the
		// player releases (gap > 2 ticks) or switches blocks, i.e. a real dig-start.
		if (event.action == Action.LEFT_CLICK_BLOCK && !player.worldObj.isRemote) {
			NBTTagCompound nbt = player.getEntityData();
			if (player.getCurrentEquippedItem() == null && WitcheryUtil.getVampireLevel(player) == 10) {
				int now = player.ticksExisted;
				if (now < nbt.getInteger("bvBindCooldownUntil")) {
					return;
				}

				int bx = event.x;
				int by = event.y;
				int bz = event.z;
				int lastClick = nbt.getInteger("bvLastClickTick");
				boolean sameDigBlock = nbt.getBoolean("bvDigActive")
						&& nbt.getInteger("bvDigX") == bx
						&& nbt.getInteger("bvDigY") == by
						&& nbt.getInteger("bvDigZ") == bz;
				// Still holding the same dig — event spam, not a new punch
				boolean continuingHold = sameDigBlock && (now - lastClick) <= 2;
				nbt.setInteger("bvLastClickTick", now);
				if (continuingHold) {
					return;
				}

				// New punch (released then clicked again, or started on a different block)
				nbt.setBoolean("bvDigActive", true);
				nbt.setInteger("bvDigX", bx);
				nbt.setInteger("bvDigY", by);
				nbt.setInteger("bvDigZ", bz);

				int count;
				if (nbt.getBoolean("bvBindHas")
						&& nbt.getInteger("bvBindX") == bx
						&& nbt.getInteger("bvBindY") == by
						&& nbt.getInteger("bvBindZ") == bz) {
					count = nbt.getInteger("bvBindCount") + 1;
				} else {
					count = 1;
				}
				nbt.setBoolean("bvBindHas", true);
				nbt.setInteger("bvBindX", bx);
				nbt.setInteger("bvBindY", by);
				nbt.setInteger("bvBindZ", bz);
				nbt.setInteger("bvBindCount", count);

				if (count >= 3) {
					nbt.setBoolean("bvBindHas", false);
					nbt.setInteger("bvBindCount", 0);
					// Keep dig active so continued hold does not start a new 3-count
					nbt.setInteger("bvBindCooldownUntil", now + 20);
					VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
					// Next slot lives in entityData (server-only) — VExtendedPlayerPacket sync was wiping SECONDSLOT
					int slot = nbt.getInteger("bvNextTeleportSlot");
					if (slot != 2) {
						slot = 1;
					}
					vPlayer.setTeleportSpot(slot, MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY), MathHelper.floor_double(player.posZ), player.dimension);
					nbt.setInteger("bvNextTeleportSlot", slot == 1 ? 2 : 1);
					if (slot == 2) {
						GeneralUtil.sendDebugMessage(player, new ChatComponentText("You bind the second point").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
					} else {
						GeneralUtil.sendDebugMessage(player, new ChatComponentText("You bind the first point").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
					}
					vPlayer.syncClient();
				}
			} else {
				nbt.setBoolean("bvBindHas", false);
				nbt.setInteger("bvBindCount", 0);
				nbt.setBoolean("bvDigActive", false);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onEntityInteractForm(EntityInteractEvent event) {
		EntityPlayer player = event.entityPlayer;
		if (player == null) return;
		if (LegendsUtil.blocksItemUse(player)) {
			if (LegendsUtil.isMist(player)) {
				player.addChatComponentMessage(new ChatComponentText("You're in mist form").setChatStyle(new ChatStyle().setBold(true).setItalic(true)));
			}
			event.setCanceled(true);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onAttackEntityForm(AttackEntityEvent event) {
		EntityPlayer player = event.entityPlayer;
		if (player == null) return;
		if (LegendsUtil.blocksEntityAttack(player)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void livingHurtVampireLordClothesCap(LivingHurtEvent event) {
		if (event.entity.worldObj.isRemote) {
			return;
		}
		if (!StatBoostConfig.enableVampireLordDamageCap) {
			return;
		}
		if (!(event.entity instanceof EntityPlayer)) {
			return;
		}
		EntityPlayer player = (EntityPlayer) event.entity;
		if (!WitcheryUtil.hasVampireLordClothesCap(player)) {
			return;
		}
		if (!DamageCalculator.shouldCapVampireLordIncoming(event.source)) {
			return;
		}
		event.ammount = DamageCalculator.capDamage(event.ammount);
	}

	@SubscribeEvent
	public void livingAttackEvent(LivingAttackEvent event) {
		if (!event.entity.worldObj.isRemote) {
			if (event.entity instanceof EntityPlayer && event.source.getEntity() != null) {
				EntityPlayer player = (EntityPlayer) event.entity;
				NBTTagCompound nbt = player.getEntityData();
				nbt.setInteger("isInCombat", 1);
				nbt.setInteger("isInCombatTimer", CombatTimer);

				// Interrupt hypnosis channel if hit while staring
				VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
				if (vPlayer != null && vPlayer.getBoolean(PlayerKeys.HYPNOTIZING)) {
					WitcheryUtil.endHypnosisChannel(player);
					Entity attacker = event.source.getEntity();
					if (attacker instanceof EntityCreature) {
						WitcheryUtil.resetHypnosisAttempts((EntityCreature) attacker);
					}
				}

				if (event.source.getSourceOfDamage() instanceof EntityBolt && event.source.getEntity() instanceof EntityWitchHunter) {
					if (!event.source.isDamageAbsolute()) {
						ExtendedPlayer playerEx = ExtendedPlayer.get(player);
						if (playerEx != null && playerEx.getVampireLevel() > 0) {
							float base = 15.0F;
							if (LegendsUtil.isVampireBeast(player)) base = 13.0F;
							float exactDamage = base + player.worldObj.rand.nextFloat() * 3.0F;

							player.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 40, 1));
							if (event.source.getSourceOfDamage().isBurning()) {
								player.setFire(5); 
							}

							DamageSource trueDamage = new net.minecraft.util.EntityDamageSourceIndirect("arrow", event.source.getSourceOfDamage(), event.source.getEntity())
									.setDamageBypassesArmor()
									.setDamageIsAbsolute();

							event.setCanceled(true);
							player.attackEntityFrom(trueDamage, exactDamage);
							return; 
						}
					}
				}
			} else if (event.source.getEntity() instanceof EntityPlayer && event.entity != null) {
				EntityPlayer player = (EntityPlayer) event.source.getEntity();
				NBTTagCompound nbt = player.getEntityData();
				nbt.setInteger("isInCombat", 1);
				nbt.setInteger("isInCombatTimer", CombatTimer);

				if (LegacyHelper.getLegacyClass(player) == LegacyRegistry.vampire) {
					if (event.entity instanceof EntityCreature) {
						EntityCreature mob = (EntityCreature) event.entity;
						WitcheryUtil.removeEnslaver(mob);
						if (!MorphHelper.isVampireBeast(player)) {
							NBTTagCompound nbtMob = mob.getEntityData();
							if (!nbtMob.getBoolean("angry")) {
								if (mob instanceof EntityMob) GeneralUtil.restoreGeneralTargetTasks((EntityMob) mob);
								if (mob.getAttackTarget() != player) {
									mob.setRevengeTarget(player);
									mob.setTarget(player);
									mob.setAttackTarget(mob);
								}
								nbtMob.setBoolean("angry", true);
							}
							nbtMob.setInteger("forgetVampireTimer", 141);
							nbtMob.setString("vampirePlayer", player.getCommandSenderName());
						} else if (MorphHelper.isVampireBeast(player) && !(mob instanceof EntityWitchHunter)) {
							mob.getEntityData().setBoolean("vampireFear", true);
							mob.getEntityData().setString("vampirePlayer", player.getCommandSenderName());
							mob.getEntityData().setInteger("fearTimer", 100);
						}
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void vampireSleep(PlayerSleepInBedEvent event) {
		if (!event.entityPlayer.worldObj.isRemote) {
			ExtendedPlayer playerEx = ExtendedPlayer.get(event.entityPlayer);
			VExtendedPlayer vPlayer = VExtendedPlayer.get(event.entityPlayer);
			vPlayer.setInteger(PlayerKeys.SLEEP_START, (int) event.entityPlayer.worldObj.getWorldTime());
			vPlayer.syncClient();

			if (playerEx != null && playerEx.isVampire()) {
				int suspicion = SuspicionUtil.getSuspicion(event.entityPlayer);
				SuspicionUtil.checkForSuspicionWitnesses(event.entityPlayer, 1500, 128);

				if (suspicion >= 5100) {
					event.result = EntityPlayer.EnumStatus.OTHER_PROBLEM; 
					WitcheryUtil.spawnHunter(event.entityPlayer, true);
					event.entityPlayer.addChatComponentMessage(new ChatComponentText("Hunters found you while sleeping").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED).setBold(true).setItalic(true)));
				}
			}
		}
	}

	public static int lastSleepSkipDay = 0;
	public static int lastSleepSkipAmount = 0; 

	public static void setSleepSkipAmount(Integer amount) {
		lastSleepSkipAmount = amount;
	}

	@SubscribeEvent
	public void wakeUpPlayer(PlayerWakeUpEvent event) {
		if (!event.entity.worldObj.isRemote && event.setSpawn) {
			EntityPlayer player = event.entityPlayer;
			VExtendedPlayer vPlayer = VExtendedPlayer.get(player);

			long timeBeforeSleep = vPlayer.getInteger(PlayerKeys.SLEEP_START);
			long jump = 0;
			long sleepTime = timeBeforeSleep % 24000L;

			if (sleepTime >= 0 && sleepTime < 13000) {
				jump = 13000 - sleepTime;
			} else if (sleepTime >= 13000 && sleepTime < 24000) {
				jump = 24000 - sleepTime;
			}

			GeneralUtil.sendDebugMessage(player, "while sleeping, you skipped ticks: " + jump);

			if (!event.updateWorld) {
				lastSleepSkipDay = (int)(event.entity.worldObj.getTotalWorldTime() / 24000L);
				lastSleepSkipAmount = (int) jump;

				ExtendedPlayer playerEx = ExtendedPlayer.get(player);
				if (playerEx.isVampire()) {
					WitcheryUtil.decreaseBloodToCap(player, 0, .3F, .3F);
					if (player.worldObj.isDaytime()) {
						Witchery.packetPipeline.sendTo((IMessage) new PacketSound(SoundEffect.WITCHERY_MOB_WOLFMAN_HOWL, (Entity) player, .5F, .3F), player);
						int suspicion = SuspicionUtil.getSuspicion(player);
						SuspicionUtil.checkForSuspicionWitnesses(player, 1500, 128);
						SuspicionUtil.increaseSuspicion(player, 300);
						player.addPotionEffect(new PotionEffect(Potion.regeneration.id, 600, 1));

						if (suspicion < 5100 && suspicion >= 4000) {
							player.addChatComponentMessage(new ChatComponentText("The mortals suspect a vampire walks among them").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED).setItalic(true).setBold(true)));
						} else if (suspicion < 4000 && suspicion >= 3000) {
							player.addChatComponentMessage(new ChatComponentText("You can smell their fear").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED).setItalic(true).setBold(true)));
						} else if (suspicion < 3000 && suspicion >= 1500) {
							player.addChatComponentMessage(new ChatComponentText("There have been rumors about blood-draining...").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED).setItalic(true).setBold(true)));
						} else if (suspicion < 1500 && suspicion >= 0) {
							player.addChatComponentMessage(new ChatComponentText("The nearby mortals find no trace of your true nature").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED).setItalic(true).setBold(true)));
						}
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onPlayerCloneEvent(PlayerEvent.Clone event) {
		NBTTagCompound oldPlayerNBT = new NBTTagCompound();
		VExtendedPlayer oldPlayerEx = VExtendedPlayer.get(event.original);
		boolean oldHypno = oldPlayerEx != null && oldPlayerEx.getBoolean(PlayerKeys.HYPNOTIZING);
		if (oldPlayerEx != null) {
			oldPlayerEx.saveNBTData(oldPlayerNBT);
			VExtendedPlayer newPlayerEx = VExtendedPlayer.get(event.entityPlayer);
			if (newPlayerEx != null) {
				newPlayerEx.loadNBTData(oldPlayerNBT);
			}
		}
		if (HypnosisDebug.enabled()) {
			HypnosisDebug.log(event.entityPlayer, "CLONE",
					"wasDeath=" + event.wasDeath + " oldHypno=" + oldHypno
							+ " newHypno=" + (VExtendedPlayer.get(event.entityPlayer) != null
							&& VExtendedPlayer.get(event.entityPlayer).getBoolean(PlayerKeys.HYPNOTIZING)));
		}
	}

	@SubscribeEvent
	public void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
		if (!HypnosisDebug.enabled() || event.player == null) return;
		HypnosisDebug.logDimension(event.player, "changed", event.fromDim, event.toDim);
		VExtendedPlayer props = VExtendedPlayer.get(event.player);
		if (props != null) {
			HypnosisDebug.log(event.player, "DIM-STATE", "afterChange hypno=" + props.getBoolean(PlayerKeys.HYPNOTIZING));
		}
	}

	@SubscribeEvent
	public void onEntityConstructing(EntityEvent.EntityConstructing event) {
		if (event.entity instanceof EntityPlayer && VExtendedPlayer.get((EntityPlayer) event.entity) == null) {
			VExtendedPlayer.register((EntityPlayer) event.entity);
		} else if (event.entity instanceof EntityVillager && VExtendedVillager.get((EntityVillager) event.entity) == null) {
			VExtendedVillager.register((EntityVillager) event.entity);
		} 
	}

	@SubscribeEvent
	public void onEntityJoinWorld(EntityJoinWorldEvent event) {
		if (!event.entity.worldObj.isRemote && event.entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) event.entity;
			VExtendedPlayer props = VExtendedPlayer.get(player);

			if (props != null) {
				if (HypnosisDebug.enabled()) {
					HypnosisDebug.log(player, "JOIN", "syncClient outgoing hypno=" + props.getBoolean(PlayerKeys.HYPNOTIZING));
				}
				props.syncClient();
				for (Object obj : event.world.playerEntities) {
					EntityPlayer otherPlayer = (EntityPlayer) obj;

					if (otherPlayer != player) {
						StatBoostChanges.network.sendTo(new VExtendedPlayerPacket(player, props), (EntityPlayerMP) otherPlayer);
						VExtendedPlayer otherProps = VExtendedPlayer.get(otherPlayer);
						if (otherProps != null) {
							StatBoostChanges.network.sendTo(new VExtendedPlayerPacket(otherPlayer, otherProps), (EntityPlayerMP) player);
						}
					}
				}
			}
		}
		if (!event.world.isRemote && event.entity instanceof EntityVillager) { 
			EntityVillager villager = (EntityVillager) event.entity;
			VExtendedVillager props = VExtendedVillager.get(villager);
			if (props != null) {
				props.syncClient(); 
			}
		}
	}

	@SubscribeEvent
	public void thrallBlockControllerTarget(LivingSetAttackTargetEvent event) {
		if (event.entity.worldObj.isRemote || !(event.entity instanceof EntityLiving)) return;
		EntityLiving mob = (EntityLiving) event.entity;
		if (!WitcheryUtil.isThralled(mob)) return;

		EntityPlayer controller = WitcheryUtil.getThrallController(mob);
		if (controller == null) return;

		if (event.target == controller
				|| (event.target instanceof EntityLiving && WitcheryUtil.isThralledBy((EntityLiving) event.target, controller))) {
			mob.setAttackTarget(null);
			mob.setRevengeTarget(null);
			if (mob instanceof EntityCreature) {
				((EntityCreature) mob).setTarget(null);
			}
			WitcheryUtil.tickThrallAI(mob);
		}
	}

	@SubscribeEvent
	public void hunterChangeTarget(LivingSetAttackTargetEvent event) {
		if (!event.entity.worldObj.isRemote && (event.entity instanceof EntityWitchHunter || event.entity instanceof EntityVillageGuard)) {
			if (event.target instanceof EntityPlayer) {
				if (GeneralUtil.isVampire((EntityPlayer) event.target)) {
					EntityLiving entity = (EntityLiving) event.entity;
					EntityPlayer player = (EntityPlayer) event.target;
					NBTTagCompound nbt = entity.getEntityData();
					int timer = nbt.getInteger("forgetVampireTimer");
					if ((MorphHelper.isMorphed(player) && !MorphHelper.isVampireBeast(player)) && timer == 0) {
						nbt.removeTag("forgetVampireTimer");
						entity.setAttackTarget(null);
					} else {
						nbt.setInteger("forgetVampireTimer", 141);
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void batBlindForgetTarget(LivingSetAttackTargetEvent event) {
		if (event.entity.worldObj.isRemote || !(event.entity instanceof EntityLiving)) return;
		if (!(event.target instanceof EntityPlayer)) return;

		EntityPlayer player = (EntityPlayer) event.target;
		EntityLiving living = (EntityLiving) event.entity;
		NBTTagCompound nbt = living.getEntityData();
		if (nbt.getInteger(GeneralUtil.BAT_BLIND_FORGET) > 0 || GeneralUtil.hasBatBlindStealth(player)) {
			living.setAttackTarget(null);
			living.setRevengeTarget(null);
			if (living instanceof EntityCreature) {
				((EntityCreature) living).setTarget(null);
			}
			if (living.getNavigator() != null) {
				living.getNavigator().clearPathEntity();
			}
		}
	}

	/** Clear aggro when vampire ignore policy applies (normal mobs / lord vs bosses). */
	@SubscribeEvent
	public void vampireHostilityClearTarget(LivingSetAttackTargetEvent event) {
		if (event.entity.worldObj.isRemote || !(event.entity instanceof EntityLiving)) return;
		if (!(event.target instanceof EntityPlayer)) return;

		EntityLiving living = (EntityLiving) event.entity;
		EntityPlayer player = (EntityPlayer) event.target;
		if (!VampireHostility.shouldIgnoreAttacker(living, player)) return;

		living.setAttackTarget(null);
		living.setRevengeTarget(null);
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

	@SubscribeEvent
	public void onLivingDeath(LivingDeathEvent event) {
		EntityLivingBase living = event.entityLiving;
		Entity attacker = event.source.getEntity();
		if (attacker == null) return;
		if (attacker instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) attacker;
//			GeneralUtil.sendDebugMessage(player, "entity living base was killed");
//			GeneralUtil.sendDebugMessage(player, "NBTOptional?: " + living.writeToNBTOptional(new NBTTagCompound()));
		}
	}
}