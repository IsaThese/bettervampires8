package com.mrsand.StatBoostChanges.mixins.late;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.common.ExtendedPlayer;
import com.emoniph.witchery.common.ExtendedPlayer.VampirePower;
import com.emoniph.witchery.common.ExtendedPlayer.VampireUltimate;
import com.emoniph.witchery.item.ItemGeneral;
import com.emoniph.witchery.util.Config;
import com.emoniph.witchery.util.CreatureUtil;
import com.emoniph.witchery.util.SoundEffect;
import com.emoniph.witchery.util.TransformCreature;
import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.mrsand.StatBoostChanges.util.GeneralUtil;
import com.mrsand.StatBoostChanges.util.LegendsUtil;
import com.mrsand.StatBoostChanges.util.SuspicionUtil;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.village.Village;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

@Mixin(value = ExtendedPlayer.class, remap = false)
public abstract class ExtendedPlayerMixin {

	// Makes new StatBoost instance if a Legends Vampire

	@Shadow
	private EntityPlayer player;

	@Shadow
	public abstract VampirePower getSelectedVampirePower();

	@Shadow
	private int vampireCooldown;
	@Shadow
	private int bloodReserve;
	@Shadow
	private int bloodPower;
	@Shadow
	private int vampireLevel;

	@Shadow
	public void setBloodPower(int bloodLevel) {
	}

	@Shadow public abstract void increaseBloodPower(int quantity);
	@Shadow public abstract int getMaxBloodPower();
	@Shadow public abstract void increaseVampireLevel();
	@Shadow public abstract void setVampireUltimate(VampireUltimate skill, int charges);
	@Shadow public abstract VampireUltimate getVampireUltimate();

	@Inject(method = "useBloodReserve", at = @At("HEAD"), cancellable = true)
	public void useBloodReserve(CallbackInfo ci) {

		ItemStack heldItem = player.getHeldItem();
		if (heldItem != null && heldItem.getItem() == Witchery.Items.CANE_SWORD) {
			ci.cancel();
			int temp = this.bloodReserve;
			if (bloodPower < getMaxBloodPower()) {
				bloodReserve = 0;
				increaseBloodPower( (getMaxBloodPower()*temp) / 250 );
				GeneralUtil.villagerDied(player);
				if(vampireLevel == 9) {
					increaseVampireLevel();
					if (GeneralUtil.getClosestVillage(player, 96) != null)
						player.addExperience(4788);
					else
						player.addExperience(1069);
					player.addPotionEffect(new PotionEffect(Potion.regeneration.id, 10000, 1));
					player.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, 10000, 2));
					player.addPotionEffect(new PotionEffect(Potion.damageBoost.id, 10000, 2));
					if (getVampireUltimate() == VampireUltimate.NONE) {
						setVampireUltimate(VampireUltimate.FARM, 5);
					} else {
						setVampireUltimate(getVampireUltimate(), Math.min(this.vampireUltimateCharges + 5, 10));
					}

				}
			} 
		}
	}
	//Doesn't work since there is two seperate methods target setBloodPower instead
//	@Inject(method = "increaseBloodPower", at = @At("HEAD"), cancellable = true)
//	private void injectStarvationCheck(int quantity, CallbackInfo ci) {
//		VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
////		ExtendedPlayer self = (ExtendedPlayer)(Object)this;
//		GeneralUtil.sendDebugMessage(player, "bloodpower: " + this.bloodPower  +" or " + bloodPower);
//		if (vPlayer.getBoolean(PlayerKeys.IS_STARVING) && (this.bloodPower + quantity > 230)) {
//			vPlayer.setBoolean(PlayerKeys.HAS_STARVED, false);
//			vPlayer.setBoolean(PlayerKeys.IS_STARVING, false);
//			vPlayer.setInteger(PlayerKeys.COUNTER_STARVATION, 0);
//			LegendsUtil.bloodVision(player, false);
//			vPlayer.syncClient();
//
//
//		}
//	}
	@Inject(method = "setBloodPower", at = @At("HEAD"), cancellable = true)
	private void unStarve(int bloodLevel, CallbackInfo ci) {
		VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
		int prevLevel = bloodPower;
		if (vPlayer.getBoolean(PlayerKeys.IS_STARVING) &&  bloodLevel > 230) {
			vPlayer.setBoolean(PlayerKeys.HAS_STARVED, false);
			vPlayer.setBoolean(PlayerKeys.IS_STARVING, false);
			vPlayer.setInteger(PlayerKeys.COUNTER_STARVATION, 0);
			if(Loader.isModLoaded("legends")) {
				LegendsUtil.bloodVision(player, false);
				if(LegendsUtil.isVampireBeast(player) && !LegendsUtil.hasBeastInSlot(player)) {
					LegendsUtil.morphBeast(player); //Return Human
				}
			}
			vPlayer.syncClient();
		}
		//PrevLevel not needed since this is IF doesn't have it in slot
		if(Loader.isModLoaded("legends") && bloodLevel > (int)(getMaxBloodPower()*.35F)) {
			if(LegendsUtil.isVampireBeast(player) && !LegendsUtil.hasBeastInSlot(player)) {
				LegendsUtil.morphBeast(player); //Return Human
			}
		}

	}
	

	@Inject(method = "decreaseBloodPower", at = @At("HEAD"), cancellable = true)
	private void decreaseBloodPower(int quantity, boolean exact, CallbackInfoReturnable<Boolean> cir) {
		cir.cancel();
				ExtendedPlayer self = (ExtendedPlayer) (Object) this;
		WitcheryUtil.starveVampire(player, self.getBloodPower(), getMaxBloodPower());

		if (player.capabilities.isCreativeMode || player.worldObj.difficultySetting == EnumDifficulty.PEACEFUL) {
			cir.setReturnValue(true);
			return;
		}
		if (bloodPower >= (exact ? quantity : 1)) {
			setBloodPower(bloodPower - quantity);
			cir.setReturnValue(true);		
			return;
		} 

		cir.setReturnValue(false);
	}


	@Inject(method = "fillBloodReserve", at = @At("HEAD"), cancellable = true)
	private void fillBloodReserve(int quantity, CallbackInfo ci) {
		ItemStack heldItem = player.getHeldItem();
		if (heldItem != null && heldItem.getItem() == Witchery.Items.CANE_SWORD) {

			if (SuspicionUtil.checkForSuspicionWitnesses(player, 0, 128)) {
				WitcheryUtil.spawnHunter(player);
			} else {
				SuspicionUtil.increaseSuspicion(player, 1500);
			}
		}
	}

	@Shadow
	public abstract void setVampireLevel(int level); //Some reason this code doesn't work nvm, token doesn't work?
	@Inject(method = "increaseVampireLevel", at = @At("HEAD"), cancellable = true)
	private void increaseLegendsVampire(CallbackInfo ci) {
		ci.cancel();
		WitcheryUtil.increaseLegendsVampire(player);
	}

	@Shadow private int vampireUltimateCharges;
	@Shadow
	public abstract TransformCreature getCreatureType();
	@Shadow 
	public abstract void sync();

	@Inject(method = "triggerSelectedVampirePower", at = @At("HEAD"), cancellable = true)
	private void triggerSelectedVampirePower(CallbackInfo ci) {
		VampirePower power = getSelectedVampirePower();
		ExtendedPlayer playerEx = ExtendedPlayer.get(player);

		if (power == VampirePower.BAT && Loader.isModLoaded("legends")) {
			ci.cancel();
			if(!CreatureUtil.isInSunlight(player))
				LegendsUtil.morphBat(player);
		}
		if(power == VampirePower.SPEED) {
			ci.cancel();
			if (getCreatureType() == TransformCreature.NONE) { // Must be in human form
				PotionEffect speedEffect = this.player.getActivePotionEffect(Potion.moveSpeed);

				// Calculates the current "Step" of speed using Logarithms
				int currentLevel = (speedEffect == null) ? 0 : (int)Math.ceil(Math.log((speedEffect.getAmplifier() + 1)) / Math.log(2.0D));

				// Requirement: Vampire Level 4+ and haven't exceeded the speed cap for your level
				if (this.vampireLevel >= 4 && currentLevel <= Math.ceil(((this.vampireLevel - 3) / 2.0F))) {
					if (WitcheryUtil.decreaseBlood(player, power.INITIAL_COST, 0.0F, true)) {
						SoundEffect.RANDOM_FIZZ.playOnlyTo(this.player);

						int newLevel = (speedEffect == null) ? 2 : ((speedEffect.getAmplifier() + 1) * 2);

						// 2. Setup Duration Constraints
						int maxBurst = 400; // 20 seconds (The absolute cap for the vampire burst)
						int minBurst = 200; // 10 seconds (The starting point)
						int currentDuration = (speedEffect == null) ? 0 : speedEffect.getDuration();

						/* * 3. Duration Logic:
						 * - If no effect: Give them minBurst (10s).
						 * - If effect exists: Add 3s (60 ticks), but cap it at maxBurst (20s).
						 * - Math.max(..., currentDuration): If the player has an 8-minute potion (9600 ticks),
						 * this ensures we don't accidentally reduce their time to 20 seconds.
						 */
						int nextDuration = (speedEffect == null) ? minBurst : (currentDuration + 60);
						int clampedDuration = Math.min(nextDuration, maxBurst);
						int finalDuration = Math.max(clampedDuration, currentDuration);

						// 4. Apply Speed
						this.player.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, finalDuration, newLevel - 1, true));

						// 5. Apply Jump Boost (Matches the speed duration)
						this.player.addPotionEffect(new PotionEffect(Potion.jump.id, finalDuration, currentLevel + 1, true));
						return;
					} 
					SoundEffect.NOTE_SNARE.playOnlyTo(this.player);
					return;
				} 
				SoundEffect.NOTE_SNARE.playOnlyTo(this.player);
				return;
			} 
			SoundEffect.NOTE_SNARE.playOnlyTo(this.player);
			return;
		}

		if (power == VampirePower.ULTIMATE && vampireLevel >= 10 && vampireUltimateCharges > 0 && getCreatureType() == TransformCreature.NONE) {
			if (getVampireUltimate() == VampireUltimate.FARM) {
				if (this.player.worldObj.isRemote) return;
				ci.cancel();
				if (this.player.dimension == Config.instance().dimensionDreamID) return;

				VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
				int location = vPlayer.getInteger(PlayerKeys.LOCATION);
				boolean done = false;
				if (player.isSneaking()) {
					int now = (int) player.worldObj.getTotalWorldTime();
					int lastChange = vPlayer.getInteger(PlayerKeys.LOCATION_LAST_CHANGE_TICK);
					if (now - lastChange < 10) {
						return;
					}

					location = (location + 1) % 4;
					vPlayer.setInteger(PlayerKeys.LOCATION, location);
					vPlayer.setInteger(PlayerKeys.LOCATION_LAST_CHANGE_TICK, now);
					vPlayer.syncClient();

					switch(location) {
					case 0:
						GeneralUtil.sendDebugMessage(player, new ChatComponentText("You recall a village").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
						break;
					case 1:
						GeneralUtil.sendDebugMessage(player, new ChatComponentText("You recall the first bind").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
						break;
					case 2:
						GeneralUtil.sendDebugMessage(player, new ChatComponentText("You recall the second bind").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
						break;
					case 3:
						GeneralUtil.sendDebugMessage(player, new ChatComponentText("You recall home").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
						break;
					}
					return; 
				}
				World world = this.player.worldObj;
				int targetDimension = player.dimension;
				ChunkCoordinates targetCoords = null;
				boolean success = false;
				ChunkCoordinates townPos;
				double dist = 8.0D; 	
				double HOME_DIST_SQ = dist*dist;



				if(location == 1 || location == 2) {
					targetCoords = vPlayer.getTeleportSpot(location);
					if(targetCoords != null)
						if(player.getDistanceSq(targetCoords.posX, player.posY, targetCoords.posZ) > HOME_DIST_SQ) {
							success = true; 
							done = WitcheryUtil.teleportToLocationSafely(player.worldObj, targetCoords.posX + 0.5, targetCoords.posY, targetCoords.posZ + 0.5, vPlayer.getTeleportDimension(location), player, true);
//							vPlayer.teleportToSpot(location, (EntityPlayerMP)player);
//							done = true;
						}
				}
				else if (location == 3) {
					success = false; //success if Home logic
				}
				else if (location == 0) {
					success = true;
					WorldServer worldServer = MinecraftServer.getServer().worldServerForDimension(targetDimension);
					Village village = worldServer.villageCollectionObj.findNearestVillage(
							MathHelper.floor_double(player.posX), 
							MathHelper.floor_double(player.posY), 
							MathHelper.floor_double(player.posZ), 16*150);

					if (village != null) {
						townPos = village.getCenter();
						if(player.getDistanceSq(townPos.posX, player.posY, townPos.posZ) > HOME_DIST_SQ) {
							done = ItemGeneral.teleportToLocationSafely(world, townPos.posX + 0.5D, townPos.posY + 1, townPos.posZ + 0.5D, targetDimension, player, true);
						}
						else {
							success = false;
						}
					}

				}
				if(!success) {
					targetCoords = player.getBedLocation(player.dimension);
					if (targetCoords == null) {
						targetCoords = player.getBedLocation(0);
						targetDimension = 0;
					}


					if (targetCoords != null) {
						WorldServer homeWorld = MinecraftServer.getServer().worldServerForDimension(targetDimension);
						targetCoords = Blocks.bed.getBedSpawnPosition(homeWorld, targetCoords.posX, targetCoords.posY, targetCoords.posZ, null);
						if (targetCoords != null && player.getDistanceSq(targetCoords.posX, player.posY, targetCoords.posZ) > HOME_DIST_SQ) {
							done = ItemGeneral.teleportToLocationSafely(world, targetCoords.posX + 0.5, targetCoords.posY + 1, targetCoords.posZ + 0.5, targetDimension, player, true);
							success = true;
						}
					}

				}
				if (!done) {
					SoundEffect.NOTE_SNARE.playOnlyTo(player);
				} else if (!player.capabilities.isCreativeMode) {
					vampireUltimateCharges--;
					sync();
				}
			}
		}
	}
}



