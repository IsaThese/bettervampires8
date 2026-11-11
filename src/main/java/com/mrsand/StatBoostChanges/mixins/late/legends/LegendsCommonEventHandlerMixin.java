package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import com.mrsand.StatBoostChanges.util.CoffinSleepUtil;
import com.tihyo.legends.handlers.LegendsCommonEventHandler;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Coffin nest: skip Legends yaw lock (free left/right look). Shift/sneak exit is left
 * to stock Legends {@code setInCoffin(false)} → BV {@code onNestFlagCleared}.
 *
 * Bytecode anchors in {@code onPlayerUpdate} (Legends 8.6.2):
 * isInCoffin @1245 → putfield yaw @1265 → putfield yawHead @1282 →
 * setSprinting @1287 → setInCoffin(false) @1318.
 * Slice isInCoffin → setInCoffin (not setSprinting — earlier setSprinting makes a negative slice).
 * isLying motion÷100 @1322 → isCrawling @1357.
 */
@Mixin(value = LegendsCommonEventHandler.class, remap = false)
public abstract class LegendsCommonEventHandlerMixin {

	/**
	 * Drop {@code rotationYaw = FORCED_ROTATION} in the coffin block only.
	 * Slice isInCoffin → setInCoffin (sneak exit); avoids earlier yaw PUTFIELDs.
	 */
	@Redirect(
			method = "onPlayerUpdate",
			slice = @Slice(
					from = @At(
							value = "INVOKE",
							target = "Lcom/tihyo/legends/management/PlayerHelper;isInCoffin(Lnet/minecraft/entity/player/EntityPlayer;)Z"
					),
					to = @At(
							value = "INVOKE",
							target = "Lcom/tihyo/legends/management/PlayerHelper;setInCoffin(Lnet/minecraft/entity/player/EntityPlayer;ZF)V"
					)
			),
			at = @At(
					value = "FIELD",
					opcode = Opcodes.PUTFIELD,
					target = "Lnet/minecraft/entity/player/EntityPlayer;field_70177_z:F"
			)
	)
	private void bvSkipCoffinForcedYaw(EntityPlayer player, float yaw) {
		// free look — do not apply FORCED_ROTATION
	}

	/**
	 * Drop {@code rotationYawHead = FORCED_ROTATION}. Unique PUTFIELD of
	 * {@code field_70759_as} in {@code onPlayerUpdate}.
	 */
	@Redirect(
			method = "onPlayerUpdate",
			at = @At(
					value = "FIELD",
					opcode = Opcodes.PUTFIELD,
					target = "Lnet/minecraft/entity/player/EntityPlayer;field_70759_as:F"
			)
	)
	private void bvSkipCoffinForcedYawHead(EntityPlayer player, float yaw) {
		// free look — do not apply FORCED_ROTATION to head
	}

	/**
	 * Skip stock {@code isLying → motion/100} for coffin nest (BV owns immobility).
	 * Slice: after sneak {@code setInCoffin(false)} through {@code isCrawling}.
	 */
	@Redirect(
			method = "onPlayerUpdate",
			slice = @Slice(
					from = @At(
							value = "INVOKE",
							target = "Lcom/tihyo/legends/management/PlayerHelper;setInCoffin(Lnet/minecraft/entity/player/EntityPlayer;ZF)V"
					),
					to = @At(
							value = "INVOKE",
							target = "Lcom/tihyo/legends/management/PlayerHelper;isCrawling(Lnet/minecraft/entity/player/EntityPlayer;)Z"
					)
			),
			at = @At(
					value = "INVOKE",
					target = "Lcom/tihyo/legends/management/PlayerHelper;isLying(Lnet/minecraft/entity/player/EntityPlayer;)Z"
			)
	)
	private boolean bvIsLyingIgnoreCoffinForCrawl(EntityPlayer player) {
		return CoffinSleepUtil.isLyingIgnoringCoffin(player);
	}
}
