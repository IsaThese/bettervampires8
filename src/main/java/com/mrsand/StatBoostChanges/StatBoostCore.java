package com.mrsand.StatBoostChanges;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;

import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.launchwrapper.Launch;

@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions({"optifine"})
@IFMLLoadingPlugin.SortingIndex(1501)
//@IFMLLoadingPlugin.DependsOn("cofh.asm.LoadingPlugin")


public class StatBoostCore implements IEarlyMixinLoader, IFMLLoadingPlugin {

	public StatBoostCore() {
        try {
            Field transformerExceptionsField = Launch.classLoader.getClass().getDeclaredField("transformerExceptions");
            transformerExceptionsField.setAccessible(true);
            Set<String> transformerExceptions = (Set<String>) transformerExceptionsField.get(Launch.classLoader);
            
            if (transformerExceptions.remove("de.teamlapen.vampirism")) {
                System.out.println("[StatBoostCore] Successfully bypassed Vampirism's transformer exclusion wall!");
            }
        } catch (Exception e) {
            System.err.println("[StatBoostCore] Failed to clear Vampirism's exclusions: " + e.getMessage());
            e.printStackTrace();
        }
    }
	//IDK grabs .json. Stolen template from WitcheryNature

	@Override
	public String getMixinConfig() {
		return "mixin.statboo.early.json";
	}

	@Override
	public List<String> getMixins(Set<String> loadedMods) {
		List<String> mixins = new ArrayList<String>();
		//	      if(!loadedCoreMods.contains("unimixins"))
		mixins.add("EntitySpiderMixin");
		mixins.add("EntityGhastMixin");
		mixins.add("EntityBlazeMixin");
		mixins.add("MixinAbstractClientPlayer");
		mixins.add("EntityPlayerMPMixin");
		mixins.add("EntityPlayerMixin");
		mixins.add("BlockIsBedCoffinMixin");
		mixins.add("FoodStatsMixin");
		mixins.add("ItemArmorVampireClothesRepairMixin");
		// IEarlyMixinLoader receives coremod class names, not FML modids.
		boolean legendsCore = loadedMods.contains("com.tihyo.legends.management.asm.LegendsLoadingPlugin");
		if (legendsCore) {
			// Must be early: LegendsRenderHelper loads before late mixins prepare
			mixins.add("LegendsRenderHelperMixin");
			System.out.println("[StatBoostCore] LegendsLoadingPlugin detected — loading LegendsRenderHelperMixin (Witchery FP hand)");
		}
		if (loadedMods.contains("vampirism")
				|| loadedMods.contains("de.teamlapen.vampirism.coremod.VampirismCore")) {
			mixins.add("BatSkillMixin");
		}
		// GTNH IEarlyMixinLoader only applies mixins returned here — JSON "client" arrays are unused.
		if (FMLLaunchHandler.side() == Side.CLIENT) {
			// Fallback if a plain RenderPlayer is used for FP arm.
			mixins.add("RenderPlayerMorphHandMixin");
			// RenderPlayerHand morph FP arm is a late mixin (Legends class; early hook never applied).
			// Witchery vampire clothes: keep FP arm visible if armor getArmorModel left showModel false
			mixins.add("RenderPlayerWitcheryHandFixMixin");
			if (Boolean.parseBoolean(System.getProperty("statboo.debugHandRender", "false"))) {
				System.out.println("[StatBoostCore] statboo.debugHandRender=true — loading morph FP hand debug mixins");
				mixins.add("EventBusHandDebugMixin");
				mixins.add("ItemRendererHandDebugMixin");
				mixins.add("RenderPlayerArmDebugMixin");
			}
			if (Boolean.parseBoolean(System.getProperty("statboo.debugWitcheryHand", "false"))) {
				System.out.println("[StatBoostCore] statboo.debugWitcheryHand=true — loading Witchery clothes FP hand debug");
				mixins.add("RenderPlayerWitcheryHandDebugMixin");
				mixins.add("ItemRendererWitcheryHandDebugMixin");
				if (legendsCore) {
					mixins.add("RenderPlayerHandWitcheryDebugMixin");
				}
			}
		}
		return mixins;
	}

	public String[] getASMTransformerClass() {
		return null;
	}

	public String getModContainerClass() {
		return null;
	}

	public String getSetupClass() {
		return null;
	}

	public void injectData(Map<String, Object> data) {}

	public String getAccessTransformerClass() {
		return null;
	}
}