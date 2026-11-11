package com.mrsand.StatBoostChanges.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.tihyo.horror.blocks.RegisterBlocksHorror;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;

/** Merge Witchery/Vampirism coffin crafts into Legends Horror coffin. */
public final class CoffinRecipeMerge {

	private CoffinRecipeMerge() {}

	public static void register() {
		if (!Loader.isModLoaded("legends")) return;

		Item legendsCoffin = Item.getItemFromBlock(RegisterBlocksHorror.coffin);
		if (legendsCoffin == null) return;
		ItemStack legendsOut = new ItemStack(legendsCoffin, 1);

		if (Loader.isModLoaded("witchery")) {
			Item witchItem = com.emoniph.witchery.Witchery.Items.COFFIN;
			if (witchItem != null) {
				GameRegistry.addShapelessRecipe(legendsOut.copy(), new ItemStack(witchItem, 1));
			}
			Item witchFromBlock = Item.getItemFromBlock(com.emoniph.witchery.Witchery.Blocks.COFFIN);
			if (witchFromBlock != null && witchFromBlock != witchItem) {
				GameRegistry.addShapelessRecipe(legendsOut.copy(), new ItemStack(witchFromBlock, 1));
			}
		}

		if (Loader.isModLoaded("vampirism")) {
			Item vampCoffin = de.teamlapen.vampirism.ModItems.coffin;
			if (vampCoffin != null) {
				GameRegistry.addShapelessRecipe(legendsOut.copy(), new ItemStack(vampCoffin, 1));
				rewriteVampirismCoffinCraft(legendsOut.copy(), vampCoffin);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void rewriteVampirismCoffinCraft(ItemStack legendsOut, Item vampCoffinItem) {
		List<IRecipe> list = CraftingManager.getInstance().getRecipeList();
		List<IRecipe> replacements = new ArrayList<IRecipe>();
		Iterator<IRecipe> it = list.iterator();
		while (it.hasNext()) {
			IRecipe recipe = it.next();
			ItemStack out = recipe.getRecipeOutput();
			if (out == null || out.getItem() != vampCoffinItem) continue;

			if (recipe instanceof ShapedRecipes) {
				ShapedRecipes shaped = (ShapedRecipes) recipe;
				replacements.add(new ShapedRecipes(shaped.recipeWidth, shaped.recipeHeight, shaped.recipeItems, legendsOut.copy()));
				it.remove();
			} else if (recipe instanceof ShapelessRecipes) {
				ShapelessRecipes shapeless = (ShapelessRecipes) recipe;
				replacements.add(new ShapelessRecipes(legendsOut.copy(), shapeless.recipeItems));
				it.remove();
			}
		}
		list.addAll(replacements);
	}
}
