package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.abilities.BvSunDamage;
import com.tihyo.horror.handlers.HorrorCommonEventHandler;
import com.tihyo.legends.client.particles.LegendsParticleType;
import com.tihyo.legends.client.particles.LegendsParticles;

import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingEvent;

/**
 * Legends 8 Horror hooks: BV sun fire tiers + thinner mist smoke in first person.
 */
@Mixin(value = HorrorCommonEventHandler.class, remap = false)
public abstract class HorrorCommonEventHandlerMixin {

	@Redirect(method = "onPlayerUpdate", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/entity/player/EntityPlayer;func_70015_d(I)V"))
	private void bvGateSunFire(EntityPlayer player, int ticks) {
		if (!BvSunDamage.isEnabled()) {
			player.setFire(ticks);
			return;
		}
		if (BvSunDamage.shouldSkipSunFire(player)) {
			return;
		}
		player.setFire(ticks);
	}

	/**
	 * Helmet skips the setFire invoke entirely in L8; force burn after sunlight
	 * flag is applied when BV says the player should burn.
	 */
	@Inject(method = "onPlayerUpdate", at = @At(value = "INVOKE",
			target = "Lcom/tihyo/legends/handlers/PowersHandler;sunlight(Lnet/minecraft/entity/player/EntityPlayer;I)V",
			shift = At.Shift.AFTER))
	private void bvForceSunFirePastHelmet(LivingEvent.LivingUpdateEvent event, CallbackInfo ci) {
		if (!(event.entity instanceof EntityPlayer)) return;
		EntityPlayer player = (EntityPlayer) event.entity;
		if (!BvSunDamage.shouldBurnInSun(player)) return;
		if (player.worldObj.isRemote) return;
		player.setFire(1);
	}

	/**
	 * Vanilla Horror mist spawns 3 THICK_SMOKE per tick — too dense in first person.
	 * Keep at most one particle on an interval (stricter in FP).
	 */
	@Redirect(method = "onAbilities", at = @At(value = "INVOKE",
			target = "Lcom/tihyo/legends/client/particles/LegendsParticles;spawnParticle(Lcom/tihyo/legends/client/particles/LegendsParticleType;DDDDDD)V"))
	private void bvThrottleMistSmoke(LegendsParticleType type, double x, double y, double z,
			double mx, double my, double mz) {
		if (type != LegendsParticleType.THICK_SMOKE) {
			LegendsParticles.spawnParticle(type, x, y, z, mx, my, mz);
			return;
		}
		if (!FMLCommonHandler.instance().getSide().isClient()) return;
		try {
			Class<?> throttle = Class.forName("com.mrsand.StatBoostChanges.client.MistSmokeThrottle");
			Object allowed = throttle.getMethod("allow").invoke(null);
			if (!(allowed instanceof Boolean) || !((Boolean) allowed).booleanValue()) return;
		} catch (Throwable t) {
			return;
		}
		LegendsParticles.spawnParticle(type, x, y, z, mx, my, mz);
	}
}
