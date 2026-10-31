package com.mrsand.StatBoostChanges.abilities.powers;

import com.tihyo.legends.legacy.LegacyPower;
import com.tihyo.legends.legacy.LegacyRegistry;

/**
 * Singletons for BetterVampires-restored vampire powers registered into Legends.
 */
public final class BvVampirePowers {

	public static final LegacyPowerBvHypnosisMastery hypnosisMastery = new LegacyPowerBvHypnosisMastery();
	public static final LegacyPowerBvDemonicForm demonicForm = new LegacyPowerBvDemonicForm();
	public static final LegacyPowerBvDemonicFury demonicFury = new LegacyPowerBvDemonicFury();

	private static boolean registered;

	private BvVampirePowers() {}

	/**
	 * Must go through LegacyManager so getPowerFromName / NBT / keybinds resolve the power.
	 * manager.addPower also calls addToLegacy().
	 */
	public static void register() {
		if (registered) return;
		if (LegacyRegistry.manager == null) return;
		LegacyRegistry.manager.addPower(hypnosisMastery);
		LegacyRegistry.manager.addPower(demonicForm);
		LegacyRegistry.manager.addPower(demonicFury);
		registered = true;
	}

	public static LegacyPower[] all() {
		return new LegacyPower[] { hypnosisMastery, demonicForm, demonicFury };
	}
}
