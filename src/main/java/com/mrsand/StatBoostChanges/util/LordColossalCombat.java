package com.mrsand.StatBoostChanges.util;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mrsand.StatBoostChanges.StatBoostConfig;
import com.mrsand.StatBoostChanges.abilities.DaywalkerControl;

import cpw.mods.fml.common.Loader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;

/**
 * Vampire Lord colossal combat: %maxHP armor-bypass on fists/abilities,
 * True Form ability/blood scales, incoming burst budget.
 */
public final class LordColossalCombat {

	private static final Logger LOG = LogManager.getLogger("BetterVampires.Colossal");
	private static final String TAG_BONUS_GUARD = "bvColossalBonusGuard";

	private static final ConcurrentHashMap<UUID, BurstState> BURST = new ConcurrentHashMap<UUID, BurstState>();
	/** World-time end of hard death-save invuln (LivingAttack cancel). */
	private static final ConcurrentHashMap<UUID, Long> DEATH_SAVE_INVULN_UNTIL = new ConcurrentHashMap<UUID, Long>();
	/**
	 * Depth of vanilla {@code damageEntity} — allows {@code setHealth} HP drops from
	 * real combat; blocks OreSpawn-style direct {@code setHealth(evh)} anti-heal.
	 */
	private static final ThreadLocal<Integer> ALLOW_DIRECT_HEALTH_DROP = new ThreadLocal<Integer>();
	/** Armed by LivingHurt before damageEntity; consumed by setHealth guard. */
	private static final ConcurrentHashMap<UUID, Boolean> EXPECT_HEALTH_DROP = new ConcurrentHashMap<UUID, Boolean>();
	private static Field vampireBatShooterField;
	private static int nullWarnThrottle;

	private LordColossalCombat() {}

	public static boolean enabled() {
		return StatBoostConfig.enableLordColossalCombat;
	}

	public static boolean isColossal(EntityLivingBase target) {
		return target != null && target.getMaxHealth() >= StatBoostConfig.colossalHpThreshold;
	}

	public static boolean isLordCombatEligible(EntityPlayer player) {
		if (player == null || !DaywalkerControl.isVampireLord(player)) return false;
		if (LegendsUtil.isBat(player)) return false;
		return true;
	}

	public static boolean isTrueFormLord(EntityPlayer player) {
		return isLordCombatEligible(player) && LegendsUtil.isDraculaTrueForm(player);
	}

	/** Vanilla player melee while holding an item — pack weapons stay untouched. */
	public static boolean isHeldWeaponMelee(EntityPlayer player, DamageSource source) {
		if (player == null || player.getHeldItem() == null) return false;
		if (source == null) return false;
		String type = source.damageType;
		return "player".equals(type);
	}

	public static boolean isLegendsAbilityDamage(DamageSource source) {
		if (source == null) return false;
		String type = source.damageType;
		if (type == null) return false;
		if ("bluntForce".equals(type) || "bleeding".equals(type) || "burn".equals(type)
				|| "crush".equals(type) || "bloodForce".equals(type) || "piercing".equals(type)) {
			return true;
		}
		// Legends indirect helpers often use custom type names; class check as backup.
		String cls = source.getClass().getName();
		return cls != null && cls.contains("LegendsDamageSource");
	}

	/**
	 * Resolve the Vampire Lord responsible for this hit.
	 * @param warnIfNull when true and resolution fails, emit a throttled warning
	 */
	public static EntityPlayer resolveLord(DamageSource source, Entity fallbackAttacker, Entity victim, boolean warnIfNull) {
		EntityPlayer lord = resolveLordQuiet(source, fallbackAttacker);
		if (lord == null && warnIfNull && StatBoostConfig.colossalWarnNullSource) {
			warnNullSource(source, fallbackAttacker, victim);
		}
		return lord;
	}

	public static EntityPlayer resolveLordQuiet(DamageSource source, Entity fallbackAttacker) {
		EntityPlayer fromSource = asPlayer(source != null ? source.getEntity() : null);
		if (fromSource != null) return fromSource;

		if (source instanceof EntityDamageSourceIndirect) {
			EntityPlayer indirekt = asPlayer(((EntityDamageSourceIndirect) source).getEntity());
			if (indirekt != null) return indirekt;
		}

		EntityPlayer fromFallback = asPlayer(fallbackAttacker);
		if (fromFallback != null) return fromFallback;

		Entity batOwner = resolveVampireBatShooter(fallbackAttacker);
		if (batOwner == null && source != null) {
			batOwner = resolveVampireBatShooter(source.getEntity());
		}
		EntityPlayer fromBat = asPlayer(batOwner);
		if (fromBat != null) return fromBat;

		if (fallbackAttacker instanceof EntityThrowable) {
			return asPlayer(((EntityThrowable) fallbackAttacker).getThrower());
		}
		return null;
	}

	private static EntityPlayer asPlayer(Entity entity) {
		return entity instanceof EntityPlayer ? (EntityPlayer) entity : null;
	}

	private static Entity resolveVampireBatShooter(Entity entity) {
		if (entity == null || !Loader.isModLoaded("legends")) return null;
		try {
			if (!(entity instanceof com.tihyo.legends.entities.mobs.EntityVampireBat)) return null;
			if (vampireBatShooterField == null) {
				vampireBatShooterField = com.tihyo.legends.entities.mobs.EntityVampireBat.class
						.getDeclaredField("shootingEntity");
				vampireBatShooterField.setAccessible(true);
			}
			Object shooter = vampireBatShooterField.get(entity);
			return shooter instanceof Entity ? (Entity) shooter : null;
		} catch (Throwable t) {
			return null;
		}
	}

	private static void warnNullSource(DamageSource source, Entity fallback, Entity victim) {
		if ((++nullWarnThrottle) % 40 != 1) return;
		String type = source != null ? source.damageType : "null";
		String srcCls = source != null ? source.getClass().getName() : "null";
		String fb = fallback != null ? fallback.getClass().getName() : "null";
		String vic = victim != null ? victim.getCommandSenderName() : "?";
		LOG.warn("[Colossal] unresolved lord attacker damageType={} sourceClass={} fallback={} victim={}",
				type, srcCls, fb, vic);
	}

	/** Scale damage for DamageHelper when attacker is TF lord and hit is ability-like. */
	public static float scaleAbilityDamageForTrueForm(Entity victim, Entity attacker, DamageSource source, float amount) {
		if (!enabled() || amount <= 0.0F) return amount;
		EntityPlayer lord = resolveLordQuiet(source, attacker);
		if (lord == null) return amount;
		if (isHeldWeaponMelee(lord, source)) return amount;
		if (!isTrueFormLord(lord)) return amount;
		// Skip pure vanilla fist; Legends ability types (and DamageHelper customs) scale
		if ("player".equals(source != null ? source.damageType : null) && !isLegendsAbilityDamage(source)) {
			return amount;
		}
		float mult = StatBoostConfig.trueFormAbilityDamageMult;
		if (mult <= 1.0F) return amount;
		return amount * mult;
	}

	public static float computeColossalBonus(EntityPlayer lord, EntityLivingBase target) {
		if (!enabled() || lord == null || target == null) return 0.0F;
		if (!isLordCombatEligible(lord)) return 0.0F;
		if (!isColossal(target)) return 0.0F;
		boolean tf = LegendsUtil.isDraculaTrueForm(lord);
		float pct = tf ? StatBoostConfig.colossalPctTrueForm : StatBoostConfig.colossalPctCombat;
		float raw = target.getMaxHealth() * pct;
		float cap = tf ? trueFormCap(target.getMaxHealth()) : StatBoostConfig.colossalCapCombat;
		return Math.min(raw, cap);
	}

	private static float trueFormCap(float maxHp) {
		float div = StatBoostConfig.colossalTrueFormCapLogDivisor;
		if (div < 1.0F) div = 1.0F;
		double ratio = Math.max(1.0D, (double) maxHp / (double) div);
		return StatBoostConfig.colossalTrueFormCapBase
				+ StatBoostConfig.colossalTrueFormCapLogScale * (float) (Math.log10(ratio));
	}

	public static boolean shouldApplyColossalBonus(EntityPlayer lord, EntityLivingBase target, DamageSource source) {
		if (!enabled() || lord == null || target == null) return false;
		if (!isLordCombatEligible(lord)) return false;
		if (!isColossal(target)) return false;
		if (isHeldWeaponMelee(lord, source)) return false;
		boolean emptyHand = lord.getHeldItem() == null;
		boolean ability = isLegendsAbilityDamage(source);
		return emptyHand || ability;
	}

	/**
	 * Fold %maxHP into the same DamageHelper hit (avoids OreSpawn {@code hurt_timer}
	 * eating a second attackEntityFrom).
	 */
	public static float addColossalBonusToAbilityDamage(Entity victim, Entity attacker, DamageSource source,
			float amount) {
		if (!enabled() || amount <= 0.0F) return amount;
		if (!(victim instanceof EntityLivingBase)) return amount;
		EntityLivingBase living = (EntityLivingBase) victim;
		EntityPlayer lord = resolveLordQuiet(source, attacker);
		if (!shouldApplyColossalBonus(lord, living, source)) return amount;
		float bonus = computeColossalBonus(lord, living);
		if (bonus <= 0.0F) return amount;
		return amount + bonus;
	}

	public static void tryApplyColossalBonus(EntityPlayer lord, EntityLivingBase target, DamageSource source) {
		if (!shouldApplyColossalBonus(lord, target, source)) return;
		if (target.worldObj == null || target.worldObj.isRemote) return;
		if (lord.getEntityData().getBoolean(TAG_BONUS_GUARD)) return;

		float bonus = computeColossalBonus(lord, target);
		if (bonus <= 0.0F) return;

		lord.getEntityData().setBoolean(TAG_BONUS_GUARD, true);
		try {
			clearBossIframes(target);
			DamageSource bypass = DamageSource.causePlayerDamage(lord).setDamageBypassesArmor().setMagicDamage();
			target.attackEntityFrom(bypass, bonus);
		} finally {
			lord.getEntityData().removeTag(TAG_BONUS_GUARD);
		}
	}

	/**
	 * After a Witchery death-save: grant hard invuln that cancels LivingAttack/Hurt
	 * and clamps HP each tick. Vanilla {@code hurtResistantTime} alone does not stop
	 * Queen same-tick multi-hits; OreSpawn can also drop HP without LivingHurt
	 * (logs: hp 36.7→5.5 with only cancelAttack, no cancelHurt).
	 */
	public static void grantDeathSaveInvuln(EntityPlayer player, DamageSource source) {
		if (player == null || player.worldObj == null || player.worldObj.isRemote) return;
		if (!StatBoostConfig.enableVampireDeathSaveInvuln) {
			player.isDead = false;
			player.deathTime = 0;
			player.hurtTime = 0;
			player.maxHurtTime = 0;
			player.extinguish();
			player.setHealth(player.getMaxHealth());
			return;
		}
		int ticks = Math.max(0, StatBoostConfig.vampireDeathSaveInvulnTicks);
		boolean colossalBlow = false;
		if (source != null) {
			Entity e = source.getEntity();
			if (e == null) e = source.getSourceOfDamage();
			if (e instanceof EntityLivingBase && isColossal((EntityLivingBase) e)) {
				colossalBlow = true;
			}
			// OreSpawn Queen jump blasts: explosion, entity often null
			if (source.isExplosion() && (e == null || !(e instanceof EntityPlayer))) {
				colossalBlow = true;
			}
		}
		// Lords always get the longer boss window (thrown/projectiles were only 60 ticks).
		if (DaywalkerControl.isVampireLord(player)) {
			colossalBlow = true;
		}
		if (colossalBlow) {
			ticks = Math.max(ticks, StatBoostConfig.colossalDeathSaveInvulnTicks);
		}
		long until = player.worldObj.getTotalWorldTime() + ticks;
		UUID id = player.getUniqueID();
		Long prev = DEATH_SAVE_INVULN_UNTIL.get(id);
		if (prev == null || until > prev.longValue()) {
			DEATH_SAVE_INVULN_UNTIL.put(id, Long.valueOf(until));
		}
		player.isDead = false;
		player.deathTime = 0;
		// Do not inflate hurtResistantTime for the full window — that keeps the red overlay.
		// LivingAttack cancel is the real invuln; keep a short vanilla resist only.
		player.hurtResistantTime = Math.max(player.hurtResistantTime, player.maxHurtResistantTime);
		player.hurtTime = 0;
		player.maxHurtTime = 0;
		player.extinguish();
		player.setHealth(player.getMaxHealth());
	}

	public static boolean isDeathSaveInvulnerable(EntityPlayer player) {
		if (!StatBoostConfig.enableVampireDeathSaveInvuln) return false;
		if (player == null || player.worldObj == null) return false;
		Long until = DEATH_SAVE_INVULN_UNTIL.get(player.getUniqueID());
		if (until == null) return false;
		long now = player.worldObj.getTotalWorldTime();
		if (now >= until.longValue()) {
			DEATH_SAVE_INVULN_UNTIL.remove(player.getUniqueID());
			return false;
		}
		return true;
	}

	public static void pushAllowDirectHealthDrop() {
		Integer d = ALLOW_DIRECT_HEALTH_DROP.get();
		ALLOW_DIRECT_HEALTH_DROP.set(Integer.valueOf(d == null ? 1 : d.intValue() + 1));
	}

	public static void popAllowDirectHealthDrop() {
		Integer d = ALLOW_DIRECT_HEALTH_DROP.get();
		if (d == null || d.intValue() <= 1) {
			ALLOW_DIRECT_HEALTH_DROP.remove();
		} else {
			ALLOW_DIRECT_HEALTH_DROP.set(Integer.valueOf(d.intValue() - 1));
		}
	}

	public static boolean isAllowDirectHealthDrop() {
		Integer d = ALLOW_DIRECT_HEALTH_DROP.get();
		return d != null && d.intValue() > 0;
	}

	/** Call from LivingHurt when this player is about to lose HP via the combat path. */
	public static void markExpectedHealthDrop(EntityPlayer player) {
		if (player == null) return;
		EXPECT_HEALTH_DROP.put(player.getUniqueID(), Boolean.TRUE);
	}

	public static boolean consumeExpectedHealthDrop(EntityPlayer player) {
		if (player == null) return false;
		return EXPECT_HEALTH_DROP.remove(player.getUniqueID()) != null;
	}

	/**
	 * Clear vanilla hurt-resist and common mod boss i-frame fields (e.g. OreSpawn
	 * {@code hurt_timer}) so a follow-up armor-bypass hit is not eaten.
	 */
	public static void clearBossIframes(EntityLivingBase target) {
		if (target == null) return;
		target.hurtResistantTime = 0;
		for (Class<?> c = target.getClass(); c != null && c != EntityLivingBase.class; c = c.getSuperclass()) {
			try {
				Field f = c.getDeclaredField("hurt_timer");
				f.setAccessible(true);
				if (f.getType() == int.class) {
					f.setInt(target, 0);
					return;
				}
			} catch (Throwable ignored) {
			}
		}
	}

	/**
	 * OreSpawn can lower HP without LivingHurt; re-clamp every tick while invuln lasts.
	 */
	public static void tickDeathSaveInvuln(EntityPlayer player) {
		if (player == null || player.worldObj == null || player.worldObj.isRemote) return;
		if (!isDeathSaveInvulnerable(player)) return;
		player.isDead = false;
		player.deathTime = 0;
		player.hurtTime = 0;
		player.maxHurtTime = 0;
		player.extinguish();
		float max = player.getMaxHealth();
		float hp = player.getHealth();
		if (hp < max) {
			player.setHealth(max);
		}
	}

	/** After clothes soft-cap: enforce per-window incoming budget. */
	public static float applyIncomingBurst(EntityPlayer player, float cappedAmount) {
		if (!enabled() || player == null || cappedAmount <= 0.0F) return cappedAmount;
		UUID id = player.getUniqueID();
		long now = player.worldObj != null ? player.worldObj.getTotalWorldTime() : 0L;
		BurstState state = BURST.get(id);
		int window = Math.max(1, StatBoostConfig.colossalBurstWindowTicks);
		if (state == null || now - state.windowStart >= window) {
			state = new BurstState(now, 0.0F);
			BURST.put(id, state);
		}
		float budget = LegendsUtil.isDraculaTrueForm(player)
				? StatBoostConfig.colossalBurstBudgetTrueForm
				: StatBoostConfig.colossalBurstBudget;
		float allowed = Math.max(0.0F, budget - state.spent);
		float floor = Math.max(0.0F, StatBoostConfig.colossalBurstFloor);
		float out;
		if (allowed <= 0.0F) {
			out = Math.min(cappedAmount, floor);
		} else {
			out = Math.min(cappedAmount, allowed);
		}
		state.spent += out;
		return out;
	}

	private static final class BurstState {
		long windowStart;
		float spent;

		BurstState(long windowStart, float spent) {
			this.windowStart = windowStart;
			this.spent = spent;
		}
	}
}
