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
	public static final LegacyPowerBvHellfire hellfire = new LegacyPowerBvHellfire();
	public static final LegacyPowerBvVampireBite vampireBite = new LegacyPowerBvVampireBite();
	public static final LegacyPowerBvBattlePrecognition battlePrecognition = new LegacyPowerBvBattlePrecognition();
	public static final LegacyPowerBvDarkInferno darkInferno = new LegacyPowerBvDarkInferno();
	public static final LegacyPowerBvSoulSteal soulSteal = new LegacyPowerBvSoulSteal();
	public static final LegacyPowerBvDemonicMegiddo demonicMegiddo = new LegacyPowerBvDemonicMegiddo();
	public static final LegacyPowerBvVampiresWrath vampiresWrath = new LegacyPowerBvVampiresWrath();
	public static final LegacyPowerBvCape cape = new LegacyPowerBvCape();
	/** Lord-only passive; shown on the tree, never purchasable. */
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
		LegacyRegistry.manager.addPower(hellfire);
		LegacyRegistry.manager.addPower(vampireBite);
		LegacyRegistry.manager.addPower(battlePrecognition);
		LegacyRegistry.manager.addPower(darkInferno);
		LegacyRegistry.manager.addPower(soulSteal);
		LegacyRegistry.manager.addPower(demonicMegiddo);
		LegacyRegistry.manager.addPower(vampiresWrath);
		LegacyRegistry.manager.addPower(cape);
		LegacyRegistry.manager.addPower(intangibility);
		registered = true;
	}

	/** All BetterVampires vampire powers — Vampirism lord-only. */
	public static LegacyPower[] all() {
		return new LegacyPower[] {
				hypnosisMastery, demonicForm, demonicFury, trueForm, draculaTeleport, hellfire,
				vampireBite, battlePrecognition, darkInferno, soulSteal, demonicMegiddo, vampiresWrath,
				cape, intangibility
		};
	}

	public static boolean isLordOnlyPower(LegacyPower power) {
		if (power == null) return false;
		for (LegacyPower p : all()) {
			if (p == power) return true;
		}
		return false;
	}
}
