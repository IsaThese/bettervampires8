package com.mrsand.StatBoostChanges;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

@LateMixin
public class LateMixinsLoader implements ILateMixinLoader {

	public String getMixinConfig() {
		return "latemixin.statboo.json";
	}
	public List<String> getMixins(Set<String> loadedMods) {
		List<String> mixins = new ArrayList<String>();

		// --- WITCHERY & LEGENDS BOTH REQUIRED ---
		if (loadedMods.contains("witchery") && loadedMods.contains("legends")) {
			mixins.add("EntityWitchHunterMixin");
			mixins.add("ItemVampireClothesMixin");
			mixins.add("legends.LegacyPowerMorphBatMixin");
			mixins.add("legends.LegacyPowerMorphMistMixin");
			mixins.add("legends.LegacyPowerMorphWolfMixin");
			mixins.add("legends.LegacyVampireMixin");
			mixins.add("legends.LegendsCommonEventHandlerMixin");
			mixins.add("legends.MorphWolfMixin");
			mixins.add("legends.PowersHandlerMixin");
			mixins.add("ShapeshiftMixin");
		}

		// --- LEGENDS ONLY REQUIRED ---
		if (loadedMods.contains("legends")) {
			mixins.add("legends.BlockAltarMixin");
			mixins.add("legends.EntityVampireMixin");
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
			mixins.add("VampirePlayerMixin");
		}

		return mixins;
	}
}
