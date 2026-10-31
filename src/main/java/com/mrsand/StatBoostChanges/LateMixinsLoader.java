package com.mrsand.StatBoostChanges;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

@LateMixin
public class LateMixinsLoader implements ILateMixinLoader {

	//Sets up LateMixins for all Mixins needed in order to change class files
	// that are not my mods, also allows safe mod integration or powerful class changes

	public String getMixinConfig() {
		return "latemixin.statboo.json";
	}
	public List<String> getMixins(Set<String> loadedMods) {
		List<String> mixins = new ArrayList<String>();

		// --- WITCHERY & LEGENDS BOTH REQUIRED ---
		if (loadedMods.contains("witchery") && loadedMods.contains("legends")) {
			mixins.add("EntityWitchHunterMixin");
			mixins.add("ItemVampireClothesMixin");
			mixins.add("legends.HypnosisMasteryMixin");
			mixins.add("legends.LegacyPowerMorphBatMixin");
			mixins.add("legends.LegacyPowerMorphBeastMixin");
			mixins.add("legends.LegacyPowerMorphMistMixin");
			mixins.add("legends.LegacyPowerMorphWolfMixin");
			mixins.add("legends.LegacyPowerTeleportationMixin");
			mixins.add("legends.LegacyVampireMixin");
			mixins.add("legends.LegendsCommonEventHandlerMixin");
			mixins.add("legends.MorphWolfMixin");
			mixins.add("legends.PowersHandlerMixin");
			mixins.add("ShapeshiftMixin");
		}

		// --- LEGENDS ONLY REQUIRED ---
		if (loadedMods.contains("legends")) {
			mixins.add("legends.BlockAlterMixin");
			mixins.add("legends.EntityVampireMixin");
			mixins.add("legends.LegacyPowerDemonicFuryMixin");
			mixins.add("legends.LegacyPowerMorphVampireBeastMixin");
			mixins.add("legends.LegacyPowerWallClimbingMixin");
			mixins.add("legends.ModelVampireApparelV1Mixin");
			mixins.add("legends.ModelVampireTeethMixin");
			mixins.add("legends.MorphBatMixin");
			mixins.add("legends.MorphHelperMixin");
			mixins.add("legends.PlayerHelperMixin");
			mixins.add("legends.RegisterBlocksMixin");
			mixins.add("legends.RegisterHorrorBlocksMixin");

		}

		if (loadedMods.contains("witchery")) {
			mixins.add("BlockCoffinMixin");
			mixins.add("CreatureUtilMixin");
			mixins.add("ExtendedPlayerMixin");
			mixins.add("ExtendedVillagerMixin");
			mixins.add("GenericEventsMixin");
			mixins.add("ItemGeneralMixin");
			mixins.add("ItemGlassGobletMixin");
		}

		if (loadedMods.contains("vampirism")) {
			mixins.add("BlockCoffinVampirismMixin");
//			mixins.add("RenderHandlerMixin");
			mixins.add("VampirePlayerMixin");
		}

		if (loadedMods.contains("Minecraft")) {
			// mixins.add("EntitySpiderMixin");
		}

		return mixins;
	}
}
