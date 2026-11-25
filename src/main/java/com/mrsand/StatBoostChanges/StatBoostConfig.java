package com.mrsand.StatBoostChanges;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class StatBoostConfig {

	public static Configuration config;

	/**
	 * When true, vampire players get Legends apparel visuals: chest when not in
	 * sunlight, hidden armor in sunlight. When false, worn armor renders normally.
	 */
	public static boolean enableDayNightVampireApparel = false;

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
				false,
				"When true, vampires get Legends apparel day/night visuals (chest at night/shade, hidden in true sunlight via VampireSunRules.isInSunlight). When false, worn armor (e.g. Witchery clothes) renders normally.");
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
