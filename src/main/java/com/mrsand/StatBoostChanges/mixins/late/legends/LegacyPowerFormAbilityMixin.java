package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.tihyo.legends.legacy.powers.LegacyPowerAmbush;
import com.tihyo.legends.legacy.powers.LegacyPowerBatAttack;
import com.tihyo.legends.legacy.powers.LegacyPowerBatSlam;
import com.tihyo.legends.legacy.powers.LegacyPowerBatStorm;
import com.tihyo.legends.legacy.powers.LegacyPowerBattleCry;
import com.tihyo.legends.legacy.powers.LegacyPowerBlindingBat;
import com.tihyo.legends.legacy.powers.LegacyPowerBloodlust;
import com.tihyo.legends.legacy.powers.LegacyPowerBrutality;
import com.tihyo.legends.legacy.powers.LegacyPowerCallOfTheNight;
import com.tihyo.legends.legacy.powers.LegacyPowerCallOfTheWild;
import com.tihyo.legends.legacy.powers.LegacyPowerCanineBite;
import com.tihyo.legends.legacy.powers.LegacyPowerConcussiveRoar;
import com.tihyo.legends.legacy.powers.LegacyPowerDrakyulaCape;
import com.tihyo.legends.legacy.powers.LegacyPowerFangs;
import com.tihyo.legends.legacy.powers.LegacyPowerFurySwipes;
import com.tihyo.legends.legacy.powers.LegacyPowerHeadbutt;
import com.tihyo.legends.legacy.powers.LegacyPowerHowlOfTerror;
import com.tihyo.legends.legacy.powers.LegacyPowerHypnosis;
import com.tihyo.legends.legacy.powers.LegacyPowerImpaler;
import com.tihyo.legends.legacy.powers.LegacyPowerLeap;
import com.tihyo.legends.legacy.powers.LegacyPowerMoonlightMadness;
import com.tihyo.legends.legacy.powers.LegacyPowerRampage;
import com.tihyo.legends.legacy.powers.LegacyPowerRoadkill;
import com.tihyo.legends.legacy.powers.LegacyPowerRoar;
import com.tihyo.legends.legacy.powers.LegacyPowerRubble;
import com.tihyo.legends.legacy.powers.LegacyPowerScattershot;
import com.tihyo.legends.legacy.powers.LegacyPowerSlash;
import com.tihyo.legends.legacy.powers.LegacyPowerSpreadTheBeastblood;
import com.tihyo.legends.legacy.powers.LegacyPowerTransvection;
import com.tihyo.legends.legacy.powers.LegacyPowerVampireBite;
import com.tihyo.legends.legacy.powers.LegacyPowerVampiresWrath;
import com.tihyo.legends.legacy.powers.LegacyPowerVampirization;
import com.tihyo.legends.legacy.powers.LegacyPowerVrykirBite;
import com.tihyo.legends.morph.MorphHelper;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Stock {@code keyPressed} often returns early when {@link MorphHelper#isMorphed}.
 * Demonic form and true form must still cast (Bat Attack, bite, etc.).
 * Bat / wolf / mist morph keys are not included — those stay form-gated.
 */
@Mixin(
		value = {
				LegacyPowerAmbush.class,
				LegacyPowerBatAttack.class,
				LegacyPowerBatSlam.class,
				LegacyPowerBatStorm.class,
				LegacyPowerBattleCry.class,
				LegacyPowerBlindingBat.class,
				LegacyPowerBloodlust.class,
				LegacyPowerBrutality.class,
				LegacyPowerCallOfTheNight.class,
				LegacyPowerCallOfTheWild.class,
				LegacyPowerCanineBite.class,
				LegacyPowerConcussiveRoar.class,
				LegacyPowerDrakyulaCape.class,
				LegacyPowerFangs.class,
				LegacyPowerFurySwipes.class,
				LegacyPowerHeadbutt.class,
				LegacyPowerHowlOfTerror.class,
				LegacyPowerHypnosis.class,
				LegacyPowerImpaler.class,
				LegacyPowerLeap.class,
				LegacyPowerMoonlightMadness.class,
				LegacyPowerRampage.class,
				LegacyPowerRoadkill.class,
				LegacyPowerRoar.class,
				LegacyPowerRubble.class,
				LegacyPowerScattershot.class,
				LegacyPowerSlash.class,
				LegacyPowerSpreadTheBeastblood.class,
				LegacyPowerTransvection.class,
				LegacyPowerVampireBite.class,
				LegacyPowerVampiresWrath.class,
				LegacyPowerVampirization.class,
				LegacyPowerVrykirBite.class
		},
		remap = false
)
public abstract class LegacyPowerFormAbilityMixin {

	@Redirect(
			method = "keyPressed",
			at = @At(
					value = "INVOKE",
					target = "Lcom/tihyo/legends/morph/MorphHelper;isMorphed(Lnet/minecraft/entity/EntityLivingBase;)Z"
			)
	)
	private boolean bvAllowDemonicAndTrueForm(EntityLivingBase entity) {
		if (entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) entity;
			if (MorphHelper.isVampireBeast(player) || MorphHelper.isDraculaTrueForm(player)) {
				return false;
			}
		}
		return MorphHelper.isMorphed(entity);
	}
}
