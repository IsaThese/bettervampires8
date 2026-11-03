package com.mrsand.StatBoostChanges.mixins.late.legends;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mrsand.StatBoostChanges.abilities.BvSunDamage;
import com.tihyo.horror.handlers.HorrorCommonEventHandler;
import com.tihyo.legends.client.particles.LegendsParticleType;
import com.tihyo.legends.client.particles.LegendsParticles;

import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingEvent;

/**
 * Legends 8 Horror hooks: BV sun fire tiers + thinner mist smoke in first person
 * + vampire bite / bat ability damage retunes.
 */
@Mixin(value = HorrorCommonEventHandler.class, remap = false)
public abstract class HorrorCommonEventHandlerMixin {

	/** Per-tick mob bite damage (L8 stock 1.0F; was BV-buffed to 5). */
	private static final float BITE_MOB_TICK_DAMAGE = 2.0F;

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

	/**
	 * Vampiric Bite mob tick damage → 2.0F (6th DamageHelper.attack in the bite block).
	 * Player bleeding hit (ordinal 2) stays stock.
	 */
	@ModifyArg(
			method = "onAbilities",
			slice = @Slice(
					from = @At(
							value = "FIELD",
							target = "Lcom/tihyo/legends/management/properties/LegendsProperties;VAMPIRE_BITE:Lcom/tihyo/legends/management/properties/LegendsProperties;"
					),
					to = @At(
							value = "FIELD",
							target = "Lcom/tihyo/legends/management/properties/LegendsProperties;VAMPIRIZATION:Lcom/tihyo/legends/management/properties/LegendsProperties;"
					)
			),
			at = @At(
					value = "INVOKE",
					target = "Lcom/tihyo/legends/management/DamageHelper;attack(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/DamageSource;F)V",
					ordinal = 5
			),
			index = 3
	)
	private float bvBiteMobTickDamage(float damage) {
		return BITE_MOB_TICK_DAMAGE;
	}

	/** Bat Attack projectile damage: L8 stock 4 → 14. */
	@ModifyArg(
			method = "onAbilities",
			at = @At(
					value = "INVOKE",
					target = "Lcom/tihyo/legends/entities/projectiles/EntityBatAttack;<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;I)V"
			),
			index = 2
	)
	private int bvBuffBatAttackDamage(int damage) {
		return 14;
	}

	/** Blinding Bat projectile damage: L8 stock 5 → 15. */
	@ModifyArg(
			method = "onAbilities",
			at = @At(
					value = "INVOKE",
					target = "Lcom/tihyo/legends/entities/projectiles/EntityBlindingBat;<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;I)V"
			),
			index = 2
	)
	private int bvBuffBlindingBatDamage(int damage) {
		return 15;
	}

	/** Bat Slam projectile damage: L8 stock 4 → 6. */
	@ModifyArg(
			method = "onAbilities",
			at = @At(
					value = "INVOKE",
					target = "Lcom/tihyo/legends/entities/projectiles/EntityBatSlam;<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;I)V"
			),
			index = 2
	)
	private int bvBuffBatSlamDamage(int damage) {
		return 6;
	}
}
