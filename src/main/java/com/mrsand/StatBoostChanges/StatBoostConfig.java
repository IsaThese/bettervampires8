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
	 * When true, vampire apparel cape uses Legends basic red cape. When false,
	 * uses the default (custom) cape_0 texture.
	 */
	public static boolean useRedVampireCape = true;

	/**
	 * When true, Legends Daywalker is removed for vampire legacy players. Sunlight
	 * weakens them (most Legends powers blocked + sun burn/nausea). Vampirism
	 * lords are exempt.
	 */
	public static boolean removeLegacyDaywalker = true;

	/**
	 * When true, enforce BetterVampires sun tiers: Witchery L5+ debuff-only (no
	 * fire); lower levels burn even with a helmet; lord+full clothes full skip.
	 */
	public static boolean bvAccurateSunDamage = true;

	/** Per-tick WASD push while misted. */
	public static double mistHorizontalSpeed = 0.14D;
	/** Cap on horizontal mist speed (blocks/tick). */
	public static double mistMaxHorizontalSpeed = 0.4D;
	/** Space while misted. */
	public static double mistJumpSpeed = 0.4D;
	/** Sneak while misted (negative = sink). */
	public static double mistSinkSpeed = -0.4D;

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
				"When true, vampires get Legends apparel day/night visuals (chest at night/shade, hidden in sunlight). When false, worn armor (e.g. Witchery clothes) renders normally.");
		useRedVampireCape = config.getBoolean(
				"useRedVampireCape",
				"visual",
				false,
				"When true, vampire apparel uses the basic Legends red cape. When false, uses the default cape texture.");
		enableVampireLordDamageCap = config.getBoolean(
				"enableVampireLordDamageCap",
				"combat",
				true,
				"When true, Vampire Lord wearing a full Witchery vampire clothes set soft-caps damage from living attackers (e.g. OreSpawn bosses). Fire, starve, drown, void, and sun are not capped.");
		removeLegacyDaywalker = config.getBoolean(
				"removeLegacyDaywalker",
				"legacy",
				true,
				"When true, removes Legends Daywalker from vampire legacies. Non-lord vampires are weakened in sunlight (most Legends abilities blocked; sun burn/nausea). Vampirism Vampire Lords are exempt.");
		bvAccurateSunDamage = config.getBoolean(
				"bvAccurateSunDamage",
				"legacy",
				true,
				"When true, use BetterVampires sun rules: Witchery vampire level >= 5 gets debuffs only (no fire); below 5 burns even with a helmet; Vampire Lord + full Witchery clothes skips the sun package. When false, leave Legends 8 helmet soft-fire alone.");
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
}
