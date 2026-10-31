package com.mrsand.StatBoostChanges.util;

import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.tihyo.horror.blocks.RegisterBlocksHorror;
import com.tihyo.legends.abilities.Ability;
import com.tihyo.legends.abilities.AbilityHelper;
import com.tihyo.legends.abilities.LegendsAbilities;
import com.tihyo.legends.handlers.PowersHandler;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.LegacyManager;
import com.tihyo.legends.legacy.LegacyRegistry;
import com.tihyo.legends.management.PlayerHelper;
import com.tihyo.legends.morph.MorphHelper;
import com.tihyo.legends.morph.MorphRegistry;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

public class LegendsUtil {

	//Boolean means it goes back to the original code
	//Wrap code into this Legends class

	public static void setGrounded(EntityPlayer player, int duration) {
		PlayerHelper.setGrounded(player, duration);
	}
	public static void ifMorphedTurnHuman(EntityPlayer player) {
		if (MorphHelper.isMorphed(player)) {
			if (MorphHelper.isBat(player)) {
				MorphHelper.morph(player, MorphRegistry.bat);
			} else if (MorphHelper.isWolf(player)) {
				MorphHelper.morph(player, MorphRegistry.wolf);
			} else if (MorphHelper.isVampireBeast(player)) {
				MorphHelper.morph(player, MorphRegistry.vampireBeast);

			}
		}

	}

	// In LegendsUtil.java
	public static void setLegacyCooldown(EntityPlayer player, int key, int ticks) {
		// We pass the player so the code knows WHOSE cooldown to set
		LegacyHelper.setCooldown(player, key, ticks);
	}

	public static boolean isMorphed(EntityPlayer player) {
		return MorphHelper.isMorphed(player);
	}

	public static boolean isHidingMorph(EntityPlayer player) {
		return (!MorphHelper.isVampireBeast(player) && (MorphHelper.isMorphed(player) || AbilityHelper.isAbilityActive(player, Ability.mistForm)));
	}
	public static final String NBT_MIST_DEMIST_TICKS = "statbooMistDemistTicks";
	/** Delay after exit press before mist actually clears. */
	public static final int MIST_DEMIST_DELAY_TICKS = 5 * 20;

	public static boolean isMist(EntityPlayer player) {
		return AbilityHelper.isAbilityActive(player, Ability.mistForm);
	}

	/** Mist/bat/wolf cannot use items, right-click blocks, or entity-interact. */
	public static boolean blocksItemUse(EntityPlayer player) {
		return player != null && (isMist(player) || isBat(player) || isWolf(player));
	}

	/** Mist/bat cannot dig or break blocks. Wolf may break. */
	public static boolean blocksBlockBreak(EntityPlayer player) {
		return player != null && (isMist(player) || isBat(player));
	}

	/** Mist/bat cannot melee. Wolf may attack. */
	public static boolean blocksEntityAttack(EntityPlayer player) {
		return player != null && (isMist(player) || isBat(player));
	}

	public static void activateMistForm(EntityPlayer player, int duration) {
		if (!Loader.isModLoaded("legends") || player == null) return;
		PowersHandler.mistForm(player, duration);
	}

	/**
	 * Mist with no practical expiry. Exit via {@link #requestMistDemist} (5s) or {@link #clearMistForm}.
	 */
	public static void activateMistFormIndefinite(EntityPlayer player) {
		if (!Loader.isModLoaded("legends") || player == null) return;
		cancelMistDemist(player);
		AbilityHelper.addEffectAndSync(player, Ability.mistForm, Integer.MAX_VALUE, 0);
	}

	public static void clearMistForm(EntityPlayer player) {
		if (!Loader.isModLoaded("legends") || player == null) return;
		cancelMistDemist(player);
		if (isMist(player)) {
			AbilityHelper.removeEffectAndSync(player, Ability.mistForm);
		}
	}

	public static boolean isMistDemisting(EntityPlayer player) {
		return player != null && player.getEntityData().getInteger(NBT_MIST_DEMIST_TICKS) > 0;
	}

	/** Begin 5s solidify / demist. Second call while demisting cancels and stays mist. */
	public static void requestMistDemist(EntityPlayer player) {
		if (!Loader.isModLoaded("legends") || player == null || player.worldObj.isRemote) return;
		if (!isMist(player)) return;
		if (isMistDemisting(player)) {
			cancelMistDemist(player);
			return;
		}
		player.getEntityData().setInteger(NBT_MIST_DEMIST_TICKS, MIST_DEMIST_DELAY_TICKS);
	}

	public static void cancelMistDemist(EntityPlayer player) {
		if (player == null) return;
		player.getEntityData().removeTag(NBT_MIST_DEMIST_TICKS);
	}

	/** Server tick: count down demist delay, then clear mist. */
	public static void tickMistDemist(EntityPlayer player) {
		if (!Loader.isModLoaded("legends") || player == null || player.worldObj.isRemote) return;
		int t = player.getEntityData().getInteger(NBT_MIST_DEMIST_TICKS);
		if (t <= 0) return;
		if (!isMist(player)) {
			cancelMistDemist(player);
			return;
		}
		t--;
		if (t <= 0) {
			cancelMistDemist(player);
			AbilityHelper.removeEffectAndSync(player, Ability.mistForm);
			applyMistKeyCooldownAfterDemist(player);
			return;
		}
		player.getEntityData().setInteger(NBT_MIST_DEMIST_TICKS, t);
	}

	private static void applyMistKeyCooldownAfterDemist(EntityPlayer player) {
		int key = LegacyHelper.findAbility(player, "morphVampireMist");
		if (key <= 0) return;
		if (player.getEntityData().getInteger("isInCombat") == 1) {
			setLegacyCooldown(player, key, 40);
		} else {
			LegacyHelper.setCooldown(player, key, 0);
		}
	}

	public static boolean isBloodLust(EntityPlayer player) {
		return Loader.isModLoaded("legends") && player != null
				&& AbilityHelper.isAbilityActive(player, Ability.bloodLust);
	}

	/**
	 * Legends Blood Lust (vampire combat burst). Enables fangs (hiss) then applies bloodLust.
	 * @param durationSeconds passed to PowersHandler.bloodLust (internally * 20 ticks)
	 */
	public static void activateBloodLust(EntityPlayer player, int durationSeconds) {
		if (!Loader.isModLoaded("legends") || player == null) return;
		PowersHandler.vampireFangs(player, true);
		PowersHandler.bloodLust(player, durationSeconds);
	}

	public static void clearBloodLust(EntityPlayer player) {
		if (!Loader.isModLoaded("legends") || player == null) return;
		if (isBloodLust(player)) {
			AbilityHelper.removeEffectAndSync(player, Ability.bloodLust);
		}
	}

	public static void displayFangs(EntityPlayer player) {
		if(LegacyHelper.getLegacyClass(player) == LegacyRegistry.vampire && !AbilityHelper.isAbilityActive(player, Ability.vampireFangs)) {
			PowersHandler.vampireFangs(player, true);
		}
	}
	/**
	 * Apply or remove Legends bat morph. Idempotent: enable when already bat is a no-op;
	 * disable when not bat is a no-op. Does not toggle-off on enable (that caused needing
	 * two presses to return to human).
	 * @return false only if enable was refused (e.g. not enough blood); true after morph call
	 *         even if {@link MorphHelper#isBat} is not synced yet this tick
	 */
	public static boolean morphBat(EntityPlayer player, boolean transform) {
		if (player == null || player.worldObj == null) return false;
		player.worldObj.playSoundAtEntity((Entity) player, "mob.bat.takeoff", 3.0F, 1.0F);
		if (player.worldObj.isRemote) {
			return true;
		}

		if (player instanceof EntityPlayerMP && Loader.isModLoaded("Morph")) {
			EntityPlayerMP playerMP = (EntityPlayerMP) player;
			if (MorphUtil.isPlayerMorphed(playerMP)) {
				MorphUtil.saveMorph(playerMP);
				MorphUtil.forceDemorph(playerMP);
			}
		}

		if (transform) {
			if (MorphHelper.isBat(player)) {
				return true;
			}
			if (MorphHelper.isMorphed(player)) {
				ifMorphedTurnHuman(player);
			}
			if (Loader.isModLoaded("witchery") && !WitcheryUtil.hasBloodPower(player, 0, .05F)) {
				return false;
			}
			SuspicionUtil.checkForSuspicionWitnesses(player, 5000, 12.0D);
			MorphHelper.morph(player, MorphRegistry.bat);
			// Do not require isBat() this tick — Legends may sync morph on the next tick
			return true;
		}

		// Disable: only demorph if currently bat (MorphHelper.morph toggles)
		if (MorphHelper.isBat(player)) {
			MorphHelper.morph(player, MorphRegistry.bat);
		}
		return true;
	}


	public static void morphBat(EntityPlayer player) {
		player.worldObj.playSoundAtEntity((Entity)player, "mob.bat.takeoff", 3.0F, 1.0F);
		//		if(!MorphHelper.isBat(player))
		//			LegendsUtil.ifMorphedTurnHuman(player);
		if(player instanceof EntityPlayerMP && !player.worldObj.isRemote) {
			EntityPlayerMP playerMP = (EntityPlayerMP) player;
			if (Loader.isModLoaded("Morph")) {
				if (MorphUtil.isPlayerMorphed(playerMP)) {
					MorphUtil.saveMorph(playerMP);
					MorphUtil.forceDemorph(playerMP);
				} else if (isBat(playerMP)) {
					MorphUtil.reMorphIntoLast(playerMP);
				}
			}
		}

		if(Loader.isModLoaded("witchery")) {
			if(!MorphHelper.isBat(player)) {
				if(WitcheryUtil.hasBloodPower(player, 0, .05F)) {
					SuspicionUtil.checkForSuspicionWitnesses(player, 5000, 12.0D);
					MorphHelper.morph(player, MorphRegistry.bat);
					//					LegacyPower
					//					LegacyRegistry.morphBat.execute(player);
				}
			} else {
				LegendsUtil.ifMorphedTurnHuman(player);
			}


		}
		else {
			MorphHelper.morph(player, MorphRegistry.bat);
		}

	}
	public static void morphBeast(EntityPlayer player) {
		SuspicionUtil.checkForSuspicionWitnesses(player, 5000, 12.0D);
		if(player instanceof EntityPlayerMP && !player.worldObj.isRemote) {
			EntityPlayerMP playerMP = (EntityPlayerMP) player;
			if (Loader.isModLoaded("Morph") && playerMP != null) {
				if (MorphUtil.isPlayerMorphed(playerMP)) {
					MorphUtil.saveMorph(playerMP);
					MorphUtil.forceDemorph(playerMP);
				}
			}
		}

		if(!isVampireBeast(player))
			ifMorphedTurnHuman(player);
		if(Loader.isModLoaded("witchery")) {
			if(!MorphHelper.isVampireBeast(player)) {
				MorphHelper.morph(player, MorphRegistry.vampireBeast);

			} else {
				ifMorphedTurnHuman(player);
			}
			player.worldObj.playSoundAtEntity((Entity)player, "mob.bat.takeoff", 3.0F, 1.0F);

		}
		//		MorphHelper.morph(player, MorphRegistry.vampireBeast);
	}
	public static boolean fangs(EntityPlayer player) {
		return AbilityHelper.isAbilityActive(player, Ability.vampireFangs);
	}

	public static boolean isVampireBeast(EntityPlayer player) {
		return MorphHelper.isVampireBeast(player);
	}
	public static boolean isBat(EntityPlayer player) {
		return MorphHelper.isBat(player);
	}

	public static boolean isWolf(EntityPlayer player) {
		return MorphHelper.isWolf(player);
	}

	/**
	 * Force Legends bat morph (does not toggle off if already bat).
	 * @return true if player is bat afterward
	 */
	public static boolean tryForceBatMorph(EntityPlayer player) {
		if (!Loader.isModLoaded("legends") || player == null || player.worldObj.isRemote) return false;
		if (player.isInWater()) return false;
		if (MorphHelper.isBat(player)) return true;

		if (player.getEntityData().getInteger("isInCombat") == 1 && MorphHelper.isVampireBeast(player)) {
			GeneralUtil.sendDebugMessage(player, new ChatComponentText("Can't change in combat while in this form")
					.setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
			return false;
		}

		if (MorphHelper.isMorphed(player)) {
			ifMorphedTurnHuman(player);
		}

		if (Loader.isModLoaded("witchery")) {
			if (!WitcheryUtil.decreaseBlood(player, 36, 0F, true)) {
				GeneralUtil.sendDebugMessage(player, new ChatComponentText("Not enough blood to take bat form")
						.setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
				return false;
			}
		}

		MorphHelper.morph(player, MorphRegistry.bat);
		player.worldObj.playSoundAtEntity(player, "mob.bat.takeoff", 3.0F, 1.0F);
		// isBat() can lag a tick after morph; treat successful morph call as success
		return true;
	}

	public static boolean hasLearnedBeastForm(EntityPlayer player) {
		return LegacyHelper.hasLearnedPower(player, LegacyManager.getPowerFromName("morphVampireBeast"));
	}
	public static boolean hasBeastInSlot(EntityPlayer player){
		return LegacyHelper.findAbility(player, "morphVampireBeast") != 0;
	}

	public static void bloodVision(EntityPlayer player, boolean display) {
		PowersHandler.bloodVision(player, display);
	}
	public static boolean bloodVision(EntityPlayer player) {
		return AbilityHelper.isAbilityActive(player, Ability.bloodVision);
	}

	public static boolean isVampire(EntityPlayer player) {
		return LegacyHelper.getLegacyClass(player) == LegacyRegistry.vampire;
	}
	public static boolean mobAttack(EntityPlayer player) {
		if(LegacyHelper.getLegacyClass(player) == LegacyRegistry.vampire) {
			return (AbilityHelper.isAbilityActive(player, Ability.vampireFangs) || MorphHelper.isMorphed(player)
					||AbilityHelper.isAbilityActive(player, Ability.bloodVision));
		}
		return false;

	}
	public static void setStepAssist(EntityPlayer player, int howMuchPlus1) {
		LegendsAbilities.stepAssist(player, howMuchPlus1);
	}
	//Give Altar if new vampire
	public static void increaseVampireLevel(EntityPlayer player, int vampireLevel) {
		if (LegacyHelper.getLegacyClass(player) == null) {
			LegacyHelper.setClass(LegacyRegistry.vampire, player);

			if (!player.worldObj.isRemote) {
				Item altarItemType = Item.getItemFromBlock(RegisterBlocksHorror.altar);

				if (altarItemType != null) {
					ItemStack altarItem = new ItemStack(altarItemType, 1);
					GeneralUtil.givePlayerItem(player, altarItem);
				}
			}
		}
	}
	public static boolean getWeakness(EntityPlayer player) {
		if(player == null) return true;
		if(LegacyHelper.getLegacyClass(player) == null) return false;
		return LegacyHelper.getLegacyClass(player).getWeaknesses(player);
	}
	//	public static void decreaseBloodPower(int quantity, boolean exact, int bloodpower){
	//		if(bloodpower < 230) {
	//
	//		}
	//
	//	}


	public static boolean checkForVampireDeath(EntityPlayer player, Integer bloodPower, Integer maxBloodPower, boolean isInSunlight) {
		VExtendedPlayer vPlayer = VExtendedPlayer.get(player);
		//Decrease needs to be same as critical blood I believe an error occured
		int criticalBlood = Math.round(maxBloodPower*.17333F); //260 at max Blood
		criticalBlood = 300;
		//If already transformed to beast
		if ((bloodPower > 0 && bloodPower < criticalBlood)
				&& MorphHelper.isVampireBeast(player)
				&& !isInSunlight
				&& !vPlayer.getBoolean(PlayerKeys.IS_VULNERABLE)) {
			vPlayer.setBoolean(PlayerKeys.IS_VULNERABLE, true);
			vPlayer.syncClient();
			MorphHelper.morph(player, MorphRegistry.vampireBeast); //Turns back to human
			if (LegacyHelper.hasLearnedPower(player, LegacyManager.getPowerFromName("morphVampireMist")))
				PowersHandler.mistForm(player, 12); // make mist form forever? Could
			// probably change time to 8000
			// or large
			player.addPotionEffect(new PotionEffect(Potion.regeneration.id, 20 * 3, 3, false));
			player.addChatComponentMessage(
					new ChatComponentText("You are extremely thirsty.")
					.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
		} 
		//First time and transform to beast (learned power is enough — do not require hotbar slot)
		else if ((bloodPower > 0 && bloodPower < criticalBlood)
				&& !isInSunlight
				&& !vPlayer.getBoolean(PlayerKeys.IS_VULNERABLE) 
				&& !MorphHelper.isVampireBeast(player)) {

			if (LegacyHelper.hasLearnedPower(player, LegacyManager.getPowerFromName("morphVampireBeast"))) {
				if(MorphHelper.isMorphed(player))
					LegendsUtil.ifMorphedTurnHuman(player);
				MorphHelper.morph(player, MorphRegistry.vampireBeast);
				player.addChatComponentMessage(
						new ChatComponentText("Enraged, you use your demonic form.")
						.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));

				player.getEntityData().setInteger("isInCombat", 1);
				player.getEntityData().setInteger("isInCombatTimer", 1200);
				player.addPotionEffect(new PotionEffect(Potion.regeneration.id, 20 * 10, 3, false));
				return true;
			}
			else if (LegacyHelper.hasLearnedPower(player, LegacyManager.getPowerFromName("morphVampireMist"))) {
				vPlayer.setBoolean(PlayerKeys.IS_VULNERABLE, true);
				vPlayer.syncClient();
				PowersHandler.mistForm(player, 12);
			}
		}
		return false;
	}

}