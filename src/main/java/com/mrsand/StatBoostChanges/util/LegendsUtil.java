package com.mrsand.StatBoostChanges.util;

import com.mrsand.StatBoostChanges.abilities.VampireRestoredAbilities;
import com.mrsand.StatBoostChanges.abilities.VampireSunRules;
import com.mrsand.StatBoostChanges.common.management.PlayerKeys;
import com.mrsand.StatBoostChanges.common.management.VExtendedPlayer;
import com.tihyo.horror.blocks.RegisterBlocksHorror;
import com.tihyo.legends.abilities.LegendsAbilities;
import com.tihyo.legends.abilities.timed.TimedAbility;
import com.tihyo.legends.handlers.PowersHandler;
import com.tihyo.legends.legacy.LegacyHelper;
import com.tihyo.legends.legacy.LegacyRegistry;
import com.tihyo.legends.management.PlayerHelper;
import com.tihyo.legends.management.properties.LegendsProperties;
import com.tihyo.legends.morph.MorphHelper;
import com.tihyo.legends.morph.MorphRegistry;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
			boolean wasBat = MorphHelper.isBat(player);
			if (wasBat) {
				MorphHelper.morph(player, MorphRegistry.bat);
				// Eject deferred to tick — hitbox is still bat-sized this tick.
				markBatFormForEject(player);
			} else if (MorphHelper.isWolf(player)) {
				MorphHelper.morph(player, MorphRegistry.wolf);
			} else if (MorphHelper.isVampireBeast(player)) {
				MorphHelper.morph(player, MorphRegistry.vampireBeast);
			} else if (MorphHelper.isDraculaTrueForm(player)) {
				MorphHelper.morph(player, MorphRegistry.draculaTrueForm);
			}
		}
	}

	public static void markBatFormActive(EntityPlayer player) {
		if (player != null) {
			player.getEntityData().setBoolean(NBT_WAS_BAT_FORM, true);
		}
	}

	/** Keep eject-pending flag after demorph so tick can snap out of solids. */
	public static void markBatFormForEject(EntityPlayer player) {
		markBatFormActive(player);
	}

	public static boolean consumeBatEjectIfSafe(EntityPlayer player) {
		if (player == null || player.worldObj == null || player.worldObj.isRemote) return true;
		if (!player.getEntityData().getBoolean(NBT_WAS_BAT_FORM)) return true;
		if (MorphHelper.isBat(player)) {
			return true; // still bat — wait for demorph
		}
		// Force human BB — Legends often leaves bat size for 1+ ticks after MorphHelper demorph.
		if (!PhysicsUtil.ejectFromSolidsIfNeeded(player, true)) {
			return false; // retry next tick
		}
		player.getEntityData().setBoolean(NBT_WAS_BAT_FORM, false);
		return true;
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
		return (!MorphHelper.isVampireBeast(player) && (MorphHelper.isMorphed(player) || LegendsAbilityBridge.isMist(player)));
	}
	public static final String NBT_MIST_DEMIST_TICKS = "statbooMistDemistTicks";
	/** Delay after exit press before mist actually clears. */
	public static final int MIST_DEMIST_DELAY_TICKS = 2 * 20;
	/**
	 * Set while Legends bat morph is active (any entry path). Cleared after
	 * post-demorph solid eject succeeds — same pattern as mist {@code IsMistFormSmall}.
	 */
	public static final String NBT_WAS_BAT_FORM = "IsBatFormSmall";

	public static boolean isMist(EntityPlayer player) {
		return LegendsAbilityBridge.isMist(player);
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
		if (!VampireSunRules.canStartMist(player)) return;
		PowersHandler.mistForm(player, duration);
	}

	/**
	 * Mist with no practical expiry. Exit via {@link #requestMistDemist} (2s) or {@link #clearMistForm}.
	 */
	public static void activateMistFormIndefinite(EntityPlayer player) {
		if (!Loader.isModLoaded("legends") || player == null) return;
		if (!VampireSunRules.canStartMist(player)) return;
		cancelMistDemist(player);
		LegendsAbilityBridge.setMistFormIndefinite(player);
	}

	public static void clearMistForm(EntityPlayer player) {
		if (!Loader.isModLoaded("legends") || player == null) return;
		cancelMistDemist(player);
		if (isMist(player)) {
			LegendsAbilityBridge.clearMistForm(player);
		}
	}

	public static boolean isMistDemisting(EntityPlayer player) {
		return player != null && player.getEntityData().getInteger(NBT_MIST_DEMIST_TICKS) > 0;
	}

	/** Begin 2s solidify / demist. Second call while demisting cancels and stays mist. */
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
			LegendsAbilityBridge.clearMistForm(player);
			applyMistKeyCooldownAfterDemist(player);
			return;
		}
		player.getEntityData().setInteger(NBT_MIST_DEMIST_TICKS, t);
	}

	private static void applyMistKeyCooldownAfterDemist(EntityPlayer player) {
		// Legends 8 renamed mist power to "morpMist"
		int key = LegacyHelper.findAbility(player, "morpMist");
		if (key <= 0) key = LegacyHelper.findAbility(player, "morphVampireMist");
		if (key <= 0) return;
		if (player.getEntityData().getInteger("isInCombat") == 1) {
			setLegacyCooldown(player, key, 40);
		} else {
			LegacyHelper.setCooldown(player, key, 0);
		}
	}

	public static boolean isBloodLust(EntityPlayer player) {
		return Loader.isModLoaded("legends") && player != null
				&& LegendsAbilityBridge.isBloodLust(player);
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
			LegendsAbilityBridge.clearBloodLust(player);
		}
	}

	public static void displayFangs(EntityPlayer player) {
		if(LegacyHelper.getLegacyClass(player) == LegacyRegistry.vampire && !LegendsAbilityBridge.isVampireFangs(player)) {
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
				BatFormDebug.log(player, "morphBat(true)", "already");
				return true;
			}
			// toggleBatForm enters; do not require isBat() this tick (Legends sync lag).
			return toggleBatForm(player);
		}

		// Disable: always allowed (day/sun must not trap the player as bat).
		if (MorphHelper.isBat(player)) {
			BatFormDebug.log(player, "morphBat(false)", "exit");
			markBatFormForEject(player);
			MorphHelper.morph(player, MorphRegistry.bat);
		}
		return true;
	}


	/** Witchery pie / shared toggle — single enter or exit, never both. */
	public static void morphBat(EntityPlayer player) {
		toggleBatForm(player);
	}

	/**
	 * Toggle Legends bat once. Exit always allowed; enter refused during calendar daytime
	 * (sun-safe biomes count as night). Returns true if now bat or successfully demorphed.
	 */
	public static boolean toggleBatForm(EntityPlayer player) {
		if (player == null || player.worldObj == null) return false;
		if (player.worldObj.isRemote) {
			BatFormDebug.log(player, "skip", "remote");
			return MorphHelper.isBat(player);
		}

		player.worldObj.playSoundAtEntity((Entity) player, "mob.bat.takeoff", 3.0F, 1.0F);

		if (player instanceof EntityPlayerMP && Loader.isModLoaded("Morph")) {
			EntityPlayerMP playerMP = (EntityPlayerMP) player;
			if (MorphUtil.isPlayerMorphed(playerMP) && !MorphHelper.isBat(player)) {
				MorphUtil.saveMorph(playerMP);
				MorphUtil.forceDemorph(playerMP);
			}
		}

		if (MorphHelper.isBat(player)) {
			BatFormDebug.log(player, "exit", "wasBat");
			markBatFormForEject(player);
			ifMorphedTurnHuman(player);
			return true;
		}

		if (VampireSunRules.batFormBlocked(player)) {
			BatFormDebug.log(player, "refuse", "daytime");
			GeneralUtil.sendDebugMessage(player, new ChatComponentText("Can't take bat form during the day")
					.setChatStyle(new ChatStyle().setBold(true).setColor(EnumChatFormatting.RED)));
			return false;
		}

		if (MorphHelper.isMorphed(player)) {
			ifMorphedTurnHuman(player);
		}

		if (Loader.isModLoaded("witchery")) {
			if (!WitcheryUtil.hasBloodPower(player, 0, .05F)) {
				BatFormDebug.log(player, "refuse", "noBlood");
				return false;
			}
		}

		SuspicionUtil.checkForSuspicionWitnesses(player, 5000, 12.0D);
		MorphHelper.morph(player, MorphRegistry.bat);
		markBatFormActive(player);
		BatFormDebug.log(player, "enter", "ok");
		return true;
	}
	public static void morphBeast(EntityPlayer player) {
		if (!Loader.isModLoaded("legends") || player == null) return;
		// Allow demorph while blocked; refuse entering beast
		if (!isVampireBeast(player) && VampireSunRules.generalPowersBlocked(player)) {
			return;
		}
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

		if (!isVampireBeast(player))
			ifMorphedTurnHuman(player);
		if (!MorphHelper.isVampireBeast(player)) {
			MorphHelper.morph(player, MorphRegistry.vampireBeast);
		} else {
			ifMorphedTurnHuman(player);
		}
		player.worldObj.playSoundAtEntity((Entity) player, "mob.bat.takeoff", 3.0F, 1.0F);
	}

	/** Toggle Castlevania Dracula True Form morph. */
	public static void morphTrueForm(EntityPlayer player) {
		if (!Loader.isModLoaded("legends") || player == null) return;
		if (!isDraculaTrueForm(player) && VampireSunRules.generalPowersBlocked(player)) {
			return;
		}
		SuspicionUtil.checkForSuspicionWitnesses(player, 5000, 12.0D);
		if (player instanceof EntityPlayerMP && !player.worldObj.isRemote) {
			EntityPlayerMP playerMP = (EntityPlayerMP) player;
			if (Loader.isModLoaded("Morph") && MorphUtil.isPlayerMorphed(playerMP)) {
				MorphUtil.saveMorph(playerMP);
				MorphUtil.forceDemorph(playerMP);
			}
		}

		boolean entering = !MorphHelper.isDraculaTrueForm(player);
		if (entering) {
			ifMorphedTurnHuman(player);
			MorphHelper.morph(player, MorphRegistry.draculaTrueForm);
			if (!player.worldObj.isRemote) {
				player.worldObj.playSoundAtEntity(player, "horror:dracula_trueform", 1.0F, 1.0F);
				// Vampire-legacy Transvection (levitate); stats() keeps it refreshed at night.
				if (!VampireSunRules.isDaytime(player)) {
					PowersHandler.transvection(player, true);
					player.getEntityData().setBoolean("bvTrueFormTransvection", true);
				}
			}
		} else {
			ifMorphedTurnHuman(player);
			if (!player.worldObj.isRemote && player.getEntityData().getBoolean("bvTrueFormTransvection")) {
				PowersHandler.transvection(player, false);
				player.getEntityData().removeTag("bvTrueFormTransvection");
			}
		}
	}

	public static boolean isDraculaTrueForm(EntityPlayer player) {
		return Loader.isModLoaded("legends") && player != null && MorphHelper.isDraculaTrueForm(player);
	}

	/** True if the named assignable power is on a Legends ability hotbar key. */
	public static boolean isAbilityEquipped(EntityPlayer player, String powerName) {
		return player != null && powerName != null && LegacyHelper.findAbility(player, powerName) > 0;
	}

	public static void clearTrueFormTransvection(EntityPlayer player) {
		if (player == null || player.worldObj == null || player.worldObj.isRemote) return;
		if (player.getEntityData().getBoolean("bvTrueFormTransvection")) {
			PowersHandler.transvection(player, false);
			player.getEntityData().removeTag("bvTrueFormTransvection");
		}
	}

	/** Castlevania Dracula teleport (Horror tick spawns EntityDraculaTeleport). */
	public static void draculaTeleport(EntityPlayer player, int durationSeconds) {
		if (!Loader.isModLoaded("legends") || player == null || player.worldObj.isRemote) return;
		if (VampireSunRules.generalPowersBlocked(player)) return;
		PowersHandler.draculaTeleport(player, Math.max(1, durationSeconds));
	}

	/** Castlevania Dracula Hellfire (Horror tick spawns EntityHellfire volleys). */
	public static void hellfire(EntityPlayer player, int durationSeconds) {
		if (!Loader.isModLoaded("legends") || player == null || player.worldObj.isRemote) return;
		if (VampireSunRules.generalPowersBlocked(player)) return;
		PowersHandler.hellfire(player, Math.max(1, durationSeconds));
	}

	public static void waveOfFireballs(EntityPlayer player, int durationSeconds) {
		if (!Loader.isModLoaded("legends") || player == null || player.worldObj.isRemote) return;
		if (VampireSunRules.generalPowersBlocked(player)) return;
		PowersHandler.waveOfFireballs(player, Math.max(1, durationSeconds));
	}

	public static void vampireBite(EntityPlayer player, int durationSeconds) {
		if (!Loader.isModLoaded("legends") || player == null || player.worldObj.isRemote) return;
		if (VampireSunRules.generalPowersBlocked(player)) return;
		PowersHandler.vampireBite(player, Math.max(1, durationSeconds));
	}

	/** The Count / Legacy Vampire's Wrath (embrace drain channel). */
	public static void vampiresWrath(EntityPlayer player, int durationSeconds) {
		if (!Loader.isModLoaded("legends") || player == null || player.worldObj.isRemote) return;
		if (VampireSunRules.generalPowersBlocked(player)) return;
		PowersHandler.vampiresWrath(player, Math.max(1, durationSeconds));
	}

	public static void furySwipes(EntityPlayer player, int durationSeconds) {
		if (!Loader.isModLoaded("legends") || player == null || player.worldObj.isRemote) return;
		if (VampireSunRules.generalPowersBlocked(player)) return;
		PowersHandler.furySwipes(player, Math.max(1, durationSeconds));
	}

	public static void battlePrecognition(EntityPlayer player, int durationSeconds) {
		if (!Loader.isModLoaded("legends") || player == null || player.worldObj.isRemote) return;
		if (VampireSunRules.generalPowersBlocked(player)) return;
		PowersHandler.battlePrecognition(player, Math.max(1, durationSeconds));
	}

	public static void combat(EntityPlayer player, int durationSeconds) {
		if (!Loader.isModLoaded("legends") || player == null || player.worldObj.isRemote) return;
		if (VampireSunRules.generalPowersBlocked(player)) return;
		PowersHandler.combat(player, Math.max(1, durationSeconds));
	}

	public static void darkInferno(EntityPlayer player, int durationSeconds) {
		if (!Loader.isModLoaded("legends") || player == null || player.worldObj.isRemote) return;
		if (VampireSunRules.generalPowersBlocked(player)) return;
		TimedAbility.darkInferno.start(player, Math.max(1, durationSeconds), false);
	}

	public static void rampage(EntityPlayer player, int durationSeconds) {
		if (!Loader.isModLoaded("legends") || player == null || player.worldObj.isRemote) return;
		if (VampireSunRules.generalPowersBlocked(player)) return;
		TimedAbility.ramming.start(player, Math.max(1, durationSeconds));
	}

	public static void thunderWave(EntityPlayer player, int durationSeconds) {
		if (!Loader.isModLoaded("legends") || player == null || player.worldObj.isRemote) return;
		if (VampireSunRules.generalPowersBlocked(player)) return;
		PowersHandler.thunderWave(player, Math.max(1, durationSeconds));
	}

	public static void soulSteal(EntityPlayer player, int durationSeconds) {
		if (!Loader.isModLoaded("legends") || player == null || player.worldObj.isRemote) return;
		if (VampireSunRules.generalPowersBlocked(player)) return;
		PowersHandler.soulSteal(player, Math.max(1, durationSeconds));
	}

	public static void demonicMegiddo(EntityPlayer player, int durationSeconds) {
		if (!Loader.isModLoaded("legends") || player == null || player.worldObj.isRemote) return;
		if (VampireSunRules.generalPowersBlocked(player)) return;
		PowersHandler.demonicMegiddo(player, Math.max(1, durationSeconds));
	}

	/** Toggle Castlevania cape mode (closed ↔ open). */
	public static void toggleCapeMode(EntityPlayer player) {
		if (!Loader.isModLoaded("legends") || player == null || player.worldObj.isRemote) return;
		Boolean on = (Boolean) LegendsProperties.CAPE_MODE.get(player);
		boolean next = on == null || !on.booleanValue();
		PowersHandler.capeMode(player, next);
	}

	public static boolean fangs(EntityPlayer player) {
		return LegendsAbilityBridge.isVampireFangs(player);
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
		if (VampireSunRules.batFormBlocked(player)) return false;

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
		// Legends 8 removed MorphVampireBeast power; auto-grant restored Demonic Form.
		return VampireRestoredAbilities.hasDemonicForm(player);
	}
	public static boolean hasBeastInSlot(EntityPlayer player){
		return VampireRestoredAbilities.hasDemonicForm(player)
				|| LegacyHelper.findAbility(player, "morphVampireBeast") != 0;
	}

	public static void bloodVision(EntityPlayer player, boolean display) {
		PowersHandler.bloodVision(player, display);
	}
	public static boolean bloodVision(EntityPlayer player) {
		return LegendsAbilityBridge.isBloodVision(player);
	}

	public static boolean isVampire(EntityPlayer player) {
		return LegacyHelper.getLegacyClass(player) == LegacyRegistry.vampire;
	}
	public static boolean mobAttack(EntityPlayer player) {
		if(LegacyHelper.getLegacyClass(player) == LegacyRegistry.vampire) {
			return (LegendsAbilityBridge.isVampireFangs(player) || MorphHelper.isMorphed(player)
					|| LegendsAbilityBridge.isBloodVision(player));
		}
		return false;

	}
	public static void setStepAssist(EntityPlayer player, int howMuchPlus1) {
		LegendsAbilities.stepAssist(player, howMuchPlus1);
	}
	/**
	 * Assign Legends vampire legacy when the player has no legacy yet.
	 * Used when Witchery / Vampirism converts the player.
	 */
	public static void ensureVampireLegacy(EntityPlayer player) {
		if (player == null || player.worldObj == null || player.worldObj.isRemote) return;
		if (!Loader.isModLoaded("legends")) return;
		if (LegacyRegistry.vampire == null) return;
		if (LegacyHelper.getLegacyClass(player) != null) return;
		LegacyHelper.setClass(LegacyRegistry.vampire, player);

		Item altarItemType = Item.getItemFromBlock(RegisterBlocksHorror.altar);
		if (altarItemType != null) {
			ItemStack altarItem = new ItemStack(altarItemType, 1);
			GeneralUtil.givePlayerItem(player, altarItem);
		}
	}

	/** Give Altar if new vampire (delegates to {@link #ensureVampireLegacy}). */
	public static void increaseVampireLevel(EntityPlayer player, int vampireLevel) {
		ensureVampireLegacy(player);
	}

	/** Flat skill-point reward for vampire kill / drink-to-death style events. */
	public static void addVampireKillSkillPoints(EntityPlayer player) {
		if (player == null || player.worldObj == null || player.worldObj.isRemote) return;
		if (!Loader.isModLoaded("legends")) return;
		LegacyHelper.addSkillPoints(player, 60);
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
		int criticalBlood = 300;
		boolean lowBlood = bloodPower > 0 && bloodPower < criticalBlood;
		boolean vulnerable = vPlayer.getBoolean(PlayerKeys.IS_VULNERABLE);

		// Already in True Form under threshold: hotbar gates exit vs stay+warn.
		if (lowBlood && MorphHelper.isDraculaTrueForm(player) && !isInSunlight && !vulnerable) {
			vPlayer.setBoolean(PlayerKeys.IS_VULNERABLE, true);
			vPlayer.syncClient();
			if (isAbilityEquipped(player, "bvTrueForm")) {
				player.addChatComponentMessage(
						new ChatComponentText("Your thirst is critical — leave true form or feed.")
						.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
			} else {
				clearTrueFormTransvection(player);
				ifMorphedTurnHuman(player);
				if (LegacyHelper.findAbility(player, "morpMist") != 0
						|| LegacyHelper.findAbility(player, "morphVampireMist") != 0) {
					PowersHandler.mistForm(player, 12);
				}
				player.addChatComponentMessage(
						new ChatComponentText("You are extremely thirsty.")
						.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
			}
			return false;
		}

		// Already in Demonic Form under threshold: hotbar gates exit vs stay+warn.
		if (lowBlood && MorphHelper.isVampireBeast(player) && !isInSunlight && !vulnerable) {
			vPlayer.setBoolean(PlayerKeys.IS_VULNERABLE, true);
			vPlayer.syncClient();
			if (isAbilityEquipped(player, "bvDemonicForm")) {
				player.addChatComponentMessage(
						new ChatComponentText("Your thirst is critical — leave demonic form or feed.")
						.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
			} else {
				ifMorphedTurnHuman(player);
				if (LegacyHelper.findAbility(player, "morpMist") != 0
						|| LegacyHelper.findAbility(player, "morphVampireMist") != 0) {
					PowersHandler.mistForm(player, 12);
				}
				player.addChatComponentMessage(
						new ChatComponentText("You are extremely thirsty.")
						.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
			}
			return false;
		}

		// First-time low-blood morph — no hotbar required.
		if (lowBlood && !isInSunlight && !vulnerable
				&& !MorphHelper.isVampireBeast(player)
				&& !MorphHelper.isDraculaTrueForm(player)) {

			if (VampireRestoredAbilities.hasTrueForm(player)) {
				if (MorphHelper.isMorphed(player)) {
					ifMorphedTurnHuman(player);
				}
				SuspicionUtil.checkForSuspicionWitnesses(player, 5000, 10.0D);
				MorphHelper.morph(player, MorphRegistry.draculaTrueForm);
				if (!player.worldObj.isRemote && !VampireSunRules.isDaytime(player)) {
					PowersHandler.transvection(player, true);
					player.getEntityData().setBoolean("bvTrueFormTransvection", true);
				}
				player.addChatComponentMessage(
						new ChatComponentText("Enraged, you take your true form.")
						.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
				player.getEntityData().setInteger("isInCombat", 1);
				player.getEntityData().setInteger("isInCombatTimer", 1200);
				return true;
			}
			if (VampireRestoredAbilities.hasDemonicForm(player)) {
				if (MorphHelper.isMorphed(player)) {
					ifMorphedTurnHuman(player);
				}
				SuspicionUtil.checkForSuspicionWitnesses(player, 5000, 10.0D);
				MorphHelper.morph(player, MorphRegistry.vampireBeast);
				player.addChatComponentMessage(
						new ChatComponentText("Enraged, you use your demonic form.")
						.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
				player.getEntityData().setInteger("isInCombat", 1);
				player.getEntityData().setInteger("isInCombatTimer", 1200);
				return true;
			}
			if (LegacyHelper.findAbility(player, "morpMist") != 0
					|| LegacyHelper.findAbility(player, "morphVampireMist") != 0) {
				vPlayer.setBoolean(PlayerKeys.IS_VULNERABLE, true);
				vPlayer.syncClient();
				PowersHandler.mistForm(player, 12);
			}
		}
		return false;
	}

}