package com.mrsand.StatBoostChanges;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class StatBoostConfig {

	public static Configuration config;

	/**
	 * When true, vampire players get Legends apparel visuals: chest/cape when bat
	 * form is allowed (OW night or sun-safe biome; other dims), hidden on OW day.
	 * When false, worn armor renders normally.
	 */
	public static boolean enableDayNightVampireApparel = true;

	/**
	 * When true, Vampire Lord + full Witchery vampire clothes soft-caps huge
	 * living-attacker hits (OreSpawn-scale).
	 */
	public static boolean enableVampireLordDamageCap = true;

	/**
	 * Witchery vampire clothes cape only (not Legends default apparel).
	 * {@code 0} = cape_red (black + red lining), {@code 1} = cape_gray,
	 * {@code 2} = stock Legends Dracula cape (no pack remap).
	 */
	public static int witcheryVampireCapeStyle = 0;

	/**
	 * When true, Legends Daywalker is removed for vampire legacy players. Sunlight
	 * weakens non-lords (powers blocked + sun burn/nausea). Lords skip sun burn but
	 * still cannot use powers in daytime except rain/mist under a roof.
	 */
	public static boolean removeLegacyDaywalker = true;

	/**
	 * When true, enforce BetterVampires sun tiers: Witchery L5+ debuff-only (no
	 * fire); lower levels burn even with a helmet; lord+full clothes full skip.
	 */
	public static boolean bvAccurateSunDamage = true;

	/**
	 * When true, Vampirism lord Teleport pie skill uses Castlevania Dracula
	 * teleport ({@code PowersHandler.draculaTeleport}) instead of the raycast blink.
	 */
	public static boolean useCastlevaniaDraculaTeleport = true;

	/**
	 * When true, cycling Vampiric Eyes prints the new mode in chat.
	 */
	public static boolean showVampireEyesModeChat = true;

	public static boolean daylightOnlyAffectsOverworld = true;

	/**
	 * Legends fullbright multiplier for vampire eye overlays.
	 * {@code 1.0} = stock {@code setLighting(61680)}; {@code 0.0} = ambient lighting only.
	 */
	public static float vampireEyesGlow = 0.8F;

	/** Per-tick WASD push while misted. */
	public static double mistHorizontalSpeed = 0.14D;
	/** Cap on horizontal mist speed (blocks/tick). */
	public static double mistMaxHorizontalSpeed = 0.4D;
	/** Space while misted. */
	public static double mistJumpSpeed = 0.4D;
	/** Sneak while misted (negative = sink). */
	public static double mistSinkSpeed = -0.4D;

	/**
	 * Volume for {@code mob.wolf.howl} on wolf morph / immortal vigor.
	 * Default {@code 0.5} is half of the old hard-coded {@code 1.0}.
	 */
	public static float wolfMorphVolume = 0.5F;

	/**
	 * Volume for {@code mob.bat.takeoff} on bat morph.
	 * Default {@code 2.4} is about 2 dB below the old hard-coded {@code 3.0}.
	 */
	public static float batMorphVolume = 2.4F;

	/**
	 * When false (default), Vampire Lord + full Witchery set hides the coat in
	 * true sunlight (regular skin; shade keeps Dracula coat). When true, basic
	 * Apparel V1 stays visible in sunlight instead of the Dracula coat.
	 */
	public static boolean lordDaylightShowVampireApparel = false;

	/**
	 * When true, double-tapping Space toggles Legends transvection (like creative fly).
	 */
	public static boolean enableDoubleJumpTransvection = true;

	/** Lord bare-hand / ability %maxHP vs high-HP targets + TF scales + incoming burst. */
	public static boolean enableLordColossalCombat = true;
	/** Min maxHP for colossal % bonus. Wither=300 (excluded at 500); Queen~3000. */
	public static float colossalHpThreshold = 500.0F;
	public static float colossalPctCombat = 0.018F;
	public static float colossalPctTrueForm = 0.028F;
	public static float colossalCapCombat = 55.0F;
	public static float colossalTrueFormCapBase = 48.0F;
	public static float colossalTrueFormCapLogScale = 24.0F;
	public static float colossalTrueFormCapLogDivisor = 250.0F;
	public static int colossalBurstWindowTicks = 40;
	/** Higher budget = more compressed damage lands per window (less tanky). */
	public static float colossalBurstBudget = 48.0F;
	public static float colossalBurstBudgetTrueForm = 72.0F;
	public static float colossalBurstFloor = 1.0F;
	public static float trueFormAbilityDamageMult = 1.5F;
	/**
	 * True Form multiplies Legends blood-meter pulses and bite latch Witchery
	 * ({@code 1/7 * 7/6 = 1/6}).
	 */
	public static float trueFormBloodGainMult = 7.0F / 6.0F;
	public static boolean colossalWarnNullSource = true;
	/** Master toggle for hard death-save invulnerability window. */
	public static boolean enableVampireDeathSaveInvuln = true;
	/** Hard death-save invuln ticks after vampire blood/level save (20 ticks = 1s). */
	public static int vampireDeathSaveInvulnTicks = 40;
	/** Longer death-save invuln when the killing blow was from a colossal (e.g. Queen). */
	public static int colossalDeathSaveInvulnTicks = 60;
	/**
	 * After a Vampire Lord damages a mob, suppress that mob's HP increases
	 * ({@code heal}/{@code setHealth} up) for this many seconds.
	 */
	public static boolean enableLordEngagementAntiHeal = true;
	/** Seconds of heal-suppress after a lord hit (clamped 5–10). */
	public static int lordEngagementAntiHealSeconds = 8;

	/** Vampiric Drain look reach (stock Legends = 10). */
	public static float vampiricDrainRange = 28.0F;
	/** Extra AABB/search expand around the look ray (stock ≈ 1). */
	public static float vampiricDrainWideness = 3.0F;
	/** Vampiric Bite acquire range (stock Horror = 2). Clamped 3–5. */
	public static float vampiricBiteRange = 4.0F;
	public static float vampiricBiteWideness = 1.0F;
	/** Once per bite latch: fraction of max Witchery blood (× trueFormBloodGainMult in TF). */
	public static float biteLatchBloodFraction = 1.0F / 7.0F;
	/** Once per drain latch: fraction of max Witchery blood. */
	public static float drainLatchBloodFraction = 0.20F;
	/** Once per lord Wrath latch: fraction of max Witchery blood. */
	public static float wrathLatchBloodFraction = 1.0F / 3.0F;
	/** True/Demonic form punch heal as fraction of max HP (replaces 2% blood). */
	public static float formHitHealPercent = 0.02F;

	public static void load(File configFile) {
		config = new Configuration(configFile);
		sync();
	}

	public static void sync() {
		if (config == null) {
			return;
		}
		enableDayNightVampireApparel = config.getBoolean(
				"enableDayNightVampireApparel",
				"visual",
				true,
				"When true, vampires get day/night apparel (Witchery cape/coat + Legends V1): shown when bat form is allowed (OW night or vampire/ominous woods; other dims), hidden on OW day. When false, no day/night hide — worn armor always renders.");
		witcheryVampireCapeStyle = config.getInt(
				"witcheryVampireCapeStyle",
				"visual",
				0,
				0,
				2,
				"Witchery vampire clothes cape only. 0=cape_red (black+red lining), 1=cape_gray, 2=stock Legends cape (no remap).");
		enableVampireLordDamageCap = config.getBoolean(
				"enableVampireLordDamageCap",
				"combat",
				true,
				"When true, Vampire Lord wearing a full Witchery vampire clothes set soft-caps damage from living attackers (e.g. OreSpawn bosses). Fire, starve, drown, void, and sun are not capped.");
		daylightOnlyAffectsOverworld = config.getBoolean(
				"daylightOnlyAffectsOverworld",
				"gameplay",
				true,
				"Sunlight weaknesses only apply during the overworld");
		removeLegacyDaywalker = config.getBoolean(
				"removeLegacyDaywalker",
				"legacy",
				true,
				"When true, removes Legends Daywalker from vampire legacies. Non-lords: sunlight blocks powers + burn/nausea. Lords: no sun burn; daytime blocks powers except mist + weather under a roof or while raining, plus fangs/bite/blood vision.");
		bvAccurateSunDamage = config.getBoolean(
				"bvAccurateSunDamage",
				"legacy",
				true,
				"When true, use BetterVampires sun rules: Witchery vampire level >= 5 gets debuffs only (no fire); below 5 burns even with a helmet; Vampire Lord + full Witchery clothes skips the sun package. When false, leave Legends 8 helmet soft-fire alone.");
		useCastlevaniaDraculaTeleport = config.getBoolean(
				"useCastlevaniaDraculaTeleport",
				"abilities",
				true,
				"When true, Vampirism Vampire Lord Teleport uses Castlevania Dracula teleport instead of the short raycast blink.");
		showVampireEyesModeChat = config.getBoolean(
				"showVampireEyesModeChat",
				"visual",
				true,
				"When true, cycling the Vampiric Eyes ability prints powers-only / always / disabled in chat.");
		vampireEyesGlow = (float) config.get(
				"visual",
				"vampireEyesGlow",
				0.8D,
				"Vampire eyes fullbright strength. 1.0 = stock Legends glow, 0.0 = no glow (ambient lighting).")
				.getDouble(0.8D);
		if (vampireEyesGlow < 0.0F) {
			vampireEyesGlow = 0.0F;
		} else if (vampireEyesGlow > 1.0F) {
			vampireEyesGlow = 1.0F;
		}
		lordDaylightShowVampireApparel = config.getBoolean(
				"lordDaylightShowVampireApparel",
				"visual",
				false,
				"When false, Vampire Lord with full Witchery vampire clothes shows regular skin in true sunlight (coat hidden; shade keeps Dracula coat). When true, basic Apparel V1 stays visible in sunlight instead of the Dracula coat.");
		enableDoubleJumpTransvection = config.getBoolean(
				"enableDoubleJumpTransvection",
				"gameplay",
				true,
				"When true, double-tapping Space toggles Legends transvection (must have learned the ability; hotbar not required).");
		wolfMorphVolume = (float) config.get(
				"sound",
				"wolfMorphVolume",
				0.5D,
				"Volume for wolf morph / immortal vigor howl (mob.wolf.howl). Default 0.5 is half of the old 1.0.")
				.getDouble(0.5D);
		batMorphVolume = (float) config.get(
				"sound",
				"batMorphVolume",
				2.4D,
				"Volume for bat morph takeoff (mob.bat.takeoff). Default 2.4 is about 2 dB below the old 3.0.")
				.getDouble(2.4D);
		wolfMorphVolume = clampVolume(wolfMorphVolume);
		batMorphVolume = clampVolume(batMorphVolume);

		enableLordColossalCombat = config.getBoolean(
				"enableLordColossalCombat",
				"colossal",
				true,
				"Vampire Lord: %maxHP armor-bypass on fists/abilities vs high-HP mobs; True Form ability/blood mult; incoming burst budget with clothes soft-cap.");
		colossalHpThreshold = (float) config.get("colossal", "colossalHpThreshold", 500.0D,
				"Min target max HP for colossal % bonus (Queen ~3000; typical TF/OreSpawn trash ~300).").getDouble(500.0D);
		colossalPctCombat = (float) config.get("colossal", "colossalPctCombat", 0.018D,
				"Fraction of target max HP added per eligible hit (non-True Form).").getDouble(0.018D);
		colossalPctTrueForm = (float) config.get("colossal", "colossalPctTrueForm", 0.028D,
				"Fraction of target max HP added per eligible hit in True Form.").getDouble(0.028D);
		colossalCapCombat = (float) config.get("colossal", "colossalCapCombat", 55.0D,
				"Per-hit cap on colossal bonus for non-True Form.").getDouble(55.0D);
		colossalTrueFormCapBase = (float) config.get("colossal", "colossalTrueFormCapBase", 48.0D,
				"True Form colossal cap base before log term.").getDouble(48.0D);
		colossalTrueFormCapLogScale = (float) config.get("colossal", "colossalTrueFormCapLogScale", 24.0D,
				"True Form colossal cap += scale * log10(maxHp / divisor).").getDouble(24.0D);
		colossalTrueFormCapLogDivisor = (float) config.get("colossal", "colossalTrueFormCapLogDivisor", 250.0D,
				"Divisor inside True Form colossal log10 term.").getDouble(250.0D);
		colossalBurstWindowTicks = config.getInt("colossalBurstWindowTicks", "colossal", 40, 1, 200,
				"Incoming soft-cap burst budget window in ticks.");
		colossalBurstBudget = (float) config.get("colossal", "colossalBurstBudget", 48.0D,
				"Max compressed incoming damage per window (lord clothes). Higher = take more damage.").getDouble(48.0D);
		colossalBurstBudgetTrueForm = (float) config.get("colossal", "colossalBurstBudgetTrueForm", 72.0D,
				"Max compressed incoming damage per window in True Form. Higher = take more damage.").getDouble(72.0D);
		colossalBurstFloor = (float) config.get("colossal", "colossalBurstFloor", 1.0D,
				"Minimum damage when burst budget is exhausted.").getDouble(1.0D);
		trueFormAbilityDamageMult = (float) config.get("colossal", "trueFormAbilityDamageMult", 1.5D,
				"True Form multiplies Legends ability damage (Bat Attack, bite, etc.).").getDouble(1.5D);
		trueFormBloodGainMult = (float) config.get("colossal", "trueFormBloodGainMult", 7.0D / 6.0D,
				"True Form multiplies Legends blood meter gains and bite latch Witchery (1/7 * 7/6 = 1/6).")
				.getDouble(7.0D / 6.0D);
		colossalWarnNullSource = config.getBoolean("colossalWarnNullSource", "colossal", true,
				"Log a warning when a colossal-relevant hit cannot resolve a lord attacker.");
		enableVampireDeathSaveInvuln = config.getBoolean("enableVampireDeathSaveInvuln", "colossal", true,
				"When true, after a vampire death-save grant a hard invuln window (cancel LivingAttack/Hurt + clamp HP). Set false to disable.");
		vampireDeathSaveInvulnTicks = config.getInt("vampireDeathSaveInvulnTicks", "colossal", 40, 0, 400,
				"Death-save invuln length in ticks (20 = 1s). Default 40 = 2s.");
		colossalDeathSaveInvulnTicks = config.getInt("colossalDeathSaveInvulnTicks", "colossal", 60, 0, 400,
				"Death-save invuln ticks vs colossal killers / Queen-style explosions (lords use this). Default 60 = 3s.");
		enableLordEngagementAntiHeal = config.getBoolean(
				"enableLordEngagementAntiHeal",
				"colossal",
				true,
				"When a Vampire Lord damages a mob, block that mob from healing / raising HP for a short window (counters OreSpawn-style fast regen).");
		lordEngagementAntiHealSeconds = config.getInt(
				"lordEngagementAntiHealSeconds",
				"colossal",
				8,
				5,
				10,
				"Heal-suppress duration in seconds after a lord hit (5–10). Default 8.");

		vampiricDrainRange = (float) config.get("drain", "vampiricDrainRange", 28.0D,
				"Vampiric Drain look/reach distance in blocks (stock Legends = 10). Keep lock if AABB or center is within range.").getDouble(28.0D);
		vampiricDrainWideness = (float) config.get("drain", "vampiricDrainWideness", 3.0D,
				"Extra blocks of look-ray / AABB expand for Drain targeting (stock ≈ 1). Helps large bosses.").getDouble(3.0D);
		if (vampiricDrainRange < 1.0F) vampiricDrainRange = 1.0F;
		if (vampiricDrainRange > 48.0F) vampiricDrainRange = 48.0F;
		if (vampiricDrainWideness < 0.0F) vampiricDrainWideness = 0.0F;
		if (vampiricDrainWideness > 16.0F) vampiricDrainWideness = 16.0F;

		vampiricBiteRange = (float) config.get("drain", "vampiricBiteRange", 4.0D,
				"Vampiric Bite acquire range in blocks (stock Horror = 2). Clamped 3–5; latch snaps victim to ~2.").getDouble(4.0D);
		vampiricBiteWideness = (float) config.get("drain", "vampiricBiteWideness", 1.0D,
				"Extra AABB expand for Bite targeting.").getDouble(1.0D);
		if (vampiricBiteRange < 3.0F) vampiricBiteRange = 3.0F;
		if (vampiricBiteRange > 5.0F) vampiricBiteRange = 5.0F;
		if (vampiricBiteWideness < 0.0F) vampiricBiteWideness = 0.0F;
		if (vampiricBiteWideness > 8.0F) vampiricBiteWideness = 8.0F;

		biteLatchBloodFraction = (float) config.get("drain", "biteLatchBloodFraction", 1.0D / 7.0D,
				"Once per bite latch: fraction of max Witchery blood (True Form also × trueFormBloodGainMult).")
				.getDouble(1.0D / 7.0D);
		drainLatchBloodFraction = (float) config.get("drain", "drainLatchBloodFraction", 0.20D,
				"Once per drain latch: fraction of max Witchery blood granted.").getDouble(0.20D);
		wrathLatchBloodFraction = (float) config.get("drain", "wrathLatchBloodFraction", 1.0D / 3.0D,
				"Once per lord Vampire's Wrath latch: fraction of max Witchery blood.").getDouble(1.0D / 3.0D);
		formHitHealPercent = (float) config.get("drain", "formHitHealPercent", 0.02D,
				"True/Demonic form melee: heal this fraction of max HP instead of Witchery blood.").getDouble(0.02D);
		if (biteLatchBloodFraction < 0.0F) biteLatchBloodFraction = 0.0F;
		if (drainLatchBloodFraction < 0.0F) drainLatchBloodFraction = 0.0F;
		if (wrathLatchBloodFraction < 0.0F) wrathLatchBloodFraction = 0.0F;
		if (formHitHealPercent < 0.0F) formHitHealPercent = 0.0F;

		// Mist speeds: tune field defaults above for now (skip .cfg override).
//		mistHorizontalSpeed = config.get(
//				"mist",
//				"mistHorizontalSpeed",
//				mistHorizontalSpeed,
//				"Per-tick WASD acceleration while in mist form. Raise for snappier mist travel.").getDouble(mistHorizontalSpeed);
//		mistMaxHorizontalSpeed = config.get(
//				"mist",
//				"mistMaxHorizontalSpeed",
//				mistMaxHorizontalSpeed,
//				"Max horizontal speed (blocks/tick) while misted.").getDouble(mistMaxHorizontalSpeed);
//		mistJumpSpeed = config.get(
//				"mist",
//				"mistJumpSpeed",
//				mistJumpSpeed,
//				"Upward speed while holding jump in mist form.").getDouble(mistJumpSpeed);
//		mistSinkSpeed = config.get(
//				"mist",
//				"mistSinkSpeed",
//				mistSinkSpeed,
//				"Vertical speed while sneaking in mist (negative sinks through floors).").getDouble(mistSinkSpeed);
		if (config.hasChanged()) {
			config.save();
		}
	}

	private static float clampVolume(float v) {
		if (v < 0.0F) return 0.0F;
		if (v > 4.0F) return 4.0F;
		return v;
	}
}
