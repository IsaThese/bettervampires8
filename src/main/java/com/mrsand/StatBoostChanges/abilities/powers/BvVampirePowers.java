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
	public static final LegacyPowerBvTrueForm trueForm = new LegacyPowerBvTrueForm();
	public static final LegacyPowerBvDraculaTeleport draculaTeleport = new LegacyPowerBvDraculaTeleport();
	/** Lord-only passive; registered for UI but not mass-auto-granted via {@link #all()}. */
	public static final LegacyPowerBvIntangibility intangibility = new LegacyPowerBvIntangibility();

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
		LegacyRegistry.manager.addPower(trueForm);
		LegacyRegistry.manager.addPower(draculaTeleport);
		LegacyRegistry.manager.addPower(intangibility);
		registered = true;
	}

	/** Auto-granted restored powers only (excludes lord-only intangibility). */
	public static LegacyPower[] all() {
		return new LegacyPower[] { hypnosisMastery, demonicForm, demonicFury, trueForm, draculaTeleport };
	}
}
