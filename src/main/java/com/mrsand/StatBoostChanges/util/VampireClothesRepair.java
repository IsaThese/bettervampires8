package com.mrsand.StatBoostChanges.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.emoniph.witchery.Witchery;
import com.emoniph.witchery.common.ExtendedPlayer;
import com.emoniph.witchery.item.ItemVampireClothes;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * Blood-fed and drink-burst repair for Witchery vampire clothes.
 */
public final class VampireClothesRepair {

	public static final int PASSIVE_INTERVAL_TICKS = 40;
	public static final float PASSIVE_BLOOD_FLOOR = 0.40F;
	public static final int PASSIVE_MEND_AMOUNT = 1;
	public static final int PASSIVE_BLOOD_COST = 1;
	public static final int DRINK_MEND_AMOUNT = 12;

	private VampireClothesRepair() {}

	/**
	 * Restores up to {@code amount} durability across worn vampire clothes,
	 * worst-damaged pieces first.
	 */
	public static int mendWornVampireClothes(EntityPlayer player, int amount) {
		if (player == null || amount <= 0 || player.worldObj.isRemote) {
			return 0;
		}
		ItemStack[] armor = player.inventory.armorInventory;
		List<ItemStack> damaged = new ArrayList<ItemStack>(4);
		for (int i = 0; i < 4; i++) {
			ItemStack stack = armor[i];
			if (stack != null && stack.getItem() instanceof ItemVampireClothes && stack.isItemDamaged()) {
				damaged.add(stack);
			}
		}
		if (damaged.isEmpty()) {
			return 0;
		}
		Collections.sort(damaged, new Comparator<ItemStack>() {
			@Override
			public int compare(ItemStack a, ItemStack b) {
				return Integer.compare(b.getItemDamage(), a.getItemDamage());
			}
		});
		int remaining = amount;
		int mended = 0;
		for (ItemStack stack : damaged) {
			if (remaining <= 0) break;
			int damage = stack.getItemDamage();
			int heal = Math.min(damage, remaining);
			stack.setItemDamage(damage - heal);
			remaining -= heal;
			mended += heal;
		}
		return mended;
	}

	/**
	 * If the vampire is above the blood floor and wears damaged clothes,
	 * spend a little blood to mend one durability.
	 */
	public static boolean tryPassiveBloodMend(EntityPlayer player) {
		if (player == null || player.worldObj.isRemote) {
			return false;
		}
		ExtendedPlayer playerEx = ExtendedPlayer.get(player);
		if (playerEx == null || !playerEx.isVampire()) {
			return false;
		}
		int maxBlood = playerEx.getMaxBloodPower();
		if (maxBlood <= 0 || playerEx.getBloodPower() < (int) (maxBlood * PASSIVE_BLOOD_FLOOR)) {
			return false;
		}
		if (!hasDamagedVampireClothes(player)) {
			return false;
		}
		if (!playerEx.decreaseBloodPower(PASSIVE_BLOOD_COST, true)) {
			return false;
		}
		return mendWornVampireClothes(player, PASSIVE_MEND_AMOUNT) > 0;
	}

	public static boolean hasDamagedVampireClothes(EntityPlayer player) {
		ItemStack[] armor = player.inventory.armorInventory;
		for (int i = 0; i < 4; i++) {
			ItemStack stack = armor[i];
			if (stack != null && stack.getItem() instanceof ItemVampireClothes && stack.isItemDamaged()) {
				return true;
			}
		}
		return false;
	}

	/** Anvil material check: Woven Cruor only. */
	public static boolean isWovenCruor(ItemStack material) {
		return material != null && Witchery.Items.GENERIC.itemDarkCloth.isMatch(material);
	}
}
