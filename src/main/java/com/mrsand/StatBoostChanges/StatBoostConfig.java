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
	public static boolean useRedVampireCape = false;

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
		if (config.hasChanged()) {
			config.save();
		}
	}
}
