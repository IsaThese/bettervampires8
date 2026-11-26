package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mrsand.StatBoostChanges.util.CoffinSleepUtil;
import com.mrsand.StatBoostChanges.util.LegendsAbilityBridge;
import com.mrsand.StatBoostChanges.util.VampireBloodChannels;
import com.tihyo.legends.management.PlayerHelper;
import com.tihyo.legends.management.properties.LegendsProperties;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

@Mixin(value = PlayerHelper.class, remap = false)
public abstract class PlayerHelperMixin {

	@Inject(method = "setInCoffin", at = @At("RETURN"))
	private static void bvAfterSetInCoffin(EntityPlayer player, boolean inCoffin, float yaw, CallbackInfo ci) {
		if (player == null || player.worldObj == null) return;
		if (inCoffin) {
			CoffinSleepUtil.afterSetInCoffinTrue(player);
		} else if (!CoffinSleepUtil.isExitAllowed()) {
			// Legends cleared the flag without BV exitCoffin — full leave (size/eye/rot/teleport).
			CoffinSleepUtil.onNestFlagCleared(player);
		}
	}

	/**
	 * Coffin nest must not trigger Legends {@code cantMove} (head lock + slowness).
	 * Keep real lying and bath.
	 */
	@Redirect(
			method = "cantMove",
			at = @At(
					value = "INVOKE",
					target = "Lcom/tihyo/legends/management/PlayerHelper;isLying(Lnet/minecraft/entity/player/EntityPlayer;)Z"
			)
	)
	private static boolean bvCantMoveIsLyingIgnoreCoffin(EntityPlayer player) {
		return CoffinSleepUtil.isLyingIgnoringCoffin(player);
	}

	/**
	 * Stock Vampiric Drain grants +20 on the same look-at hit as damage.
	 * Retune that amount to slightly under Vampiric Bite while drain is active.
	 */
	@ModifyVariable(method = "increaseBloodMeter", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private static int bvRetuneDrainBloodGrant(int amount, EntityPlayer player) {
		int retuned = VampireBloodChannels.retuneDrainBloodAmount(player, amount);
		return VampireBloodChannels.scaleTrueFormBloodGain(player, retuned);
	}

	/**
	 * Never replace stock {@code isInvincible}. A prior HEAD-cancel wiped Legends states
	 * (full {@code DRACULA_TELEPORT}, {@code BAT_SWARMING}/{@code BAT_HORDE}, dash, etc.).
	 * Only OR BV extras stock does not list. Note: stock does not treat
	 * {@code VAMPIRE_BITE} as invincible (timer + anim only) — do not re-add it.
	 */
	@Inject(method = "isInvincible", at = @At("RETURN"), cancellable = true)
	private static void isInvincibleBvExtras(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue() != null && cir.getReturnValue().booleanValue()) return;
		// Mist is BV phasing invuln (stock lists mist under absolute-invisible, not isInvincible).
		// Stat.invulnerability covers BV setInvincibleTicks paths beyond the INVINCIBLE prop.
		if (LegendsAbilityBridge.isMist(player) || LegendsAbilityBridge.isInvincible(player)) {
			cir.setReturnValue(Boolean.TRUE);
		}
	}

	/** Allow other ability keys while Transvection hover is active. */
	@Redirect(
			method = "canPerformAbility",
			at = @At(
					value = "INVOKE",
					target = "Lcom/tihyo/legends/management/properties/LegendsProperties;get(Lnet/minecraft/entity/Entity;)Ljava/lang/Object;"
			)
	)
	private static Object bvIgnoreTransvectionBusy(LegendsProperties props, Entity entity) {
		if (props == LegendsProperties.TRANSVECTION) {
			return Boolean.FALSE;
		}
		return props.get(entity);
	}

	/**
	 * Stock {@code overrideFlyingAnimation} treats Bat Slam / Dracula teleport as
	 * overriding levitation pose. Keep Transvection pose during those casts.
	 */
	@Redirect(
			method = "overrideFlyingAnimation",
			at = @At(
					value = "INVOKE",
					target = "Lcom/tihyo/legends/management/properties/LegendsProperties;get(Lnet/minecraft/entity/Entity;)Ljava/lang/Object;"
			)
	)
	private static Object bvKeepTvPoseDuringAbilityOverride(LegendsProperties props, Entity entity) {
		Object value = props.get(entity);
		if (!(entity instanceof EntityPlayer)) return value;
		if (props != LegendsProperties.BAT_SLAM && props != LegendsProperties.DRACULA_TELEPORT) {
			return value;
		}
		Object tv = LegendsProperties.TRANSVECTION.get(entity);
		if (tv instanceof Boolean && ((Boolean) tv).booleanValue()) {
			return Integer.valueOf(0);
		}
		return value;
	}

	/**
	 * {@code BAT_STORM} forces {@code cantMove}, which skips Horror Transvection
	 * hover physics. Treat storm as non-locking while hovering.
	 */
	@Redirect(
			method = "cantMove",
			at = @At(
					value = "INVOKE",
					target = "Lcom/tihyo/legends/management/properties/LegendsProperties;get(Lnet/minecraft/entity/Entity;)Ljava/lang/Object;"
			)
	)
	private static Object bvKeepTvMoveDuringBatStorm(LegendsProperties props, Entity entity) {
		Object value = props.get(entity);
		if (props == LegendsProperties.BAT_STORM && entity instanceof EntityPlayer) {
			Object tv = LegendsProperties.TRANSVECTION.get(entity);
			if (tv instanceof Boolean && ((Boolean) tv).booleanValue()) {
				return Integer.valueOf(0);
			}
		}
		return value;
	}
}
