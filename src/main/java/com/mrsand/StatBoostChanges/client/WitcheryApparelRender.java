package com.mrsand.StatBoostChanges.client;

import com.emoniph.witchery.item.ItemVampireClothes;
import com.mrsand.StatBoostChanges.ModCompat;
import com.mrsand.StatBoostChanges.abilities.VampireSunRules;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;
import com.tihyo.horror.client.models.characters.ModelDracula;
import com.tihyo.legends.client.models.ModelPlayer;
import com.tihyo.legends.proxies.ClientProxy;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

/**
 * Maps Witchery {@code ItemVampireClothes} render to Legends {@link ModelDracula}:
 * chest/coat + cape only (head hidden). Hat/legs/boots stay equipped for set bonuses
 * but use a dedicated hidden ModelBiped. Textures come from resource-pack
 * {@code vampire_apparel_v1}. Lord + full set: coat hides during calendar daytime
 * (sun-safe / vampire biomes count as night — same as {@link VampireSunRules#isDaytime}).
 */
@SideOnly(Side.CLIENT)
public final class WitcheryApparelRender {

	/** ClientProxy id for {@link ModelDracula}. */
	private static final int LEGENDS_DRACULA_MODEL_ID = 154;
	private static final String CHEST_TEXTURE =
			"horror:textures/models/armors/legacy/vampire_apparel_v1/vampire_apparel_v1_1_0.png";

	/** Separate from the shared Dracula instance — all parts off for non-chest slots. */
	private static final ModelBiped HIDDEN = createHiddenModel();

	private WitcheryApparelRender() {}

	private static ModelBiped createHiddenModel() {
		ModelBiped hidden = new ModelBiped(1.0F);
		setShow(hidden.bipedHead, false);
		setShow(hidden.bipedHeadwear, false);
		setShow(hidden.bipedBody, false);
		setShow(hidden.bipedRightArm, false);
		setShow(hidden.bipedLeftArm, false);
		setShow(hidden.bipedRightLeg, false);
		setShow(hidden.bipedLeftLeg, false);
		return hidden;
	}

	/**
	 * Vampire Lord + full Witchery set: hide coat during calendar daytime.
	 * Uses {@link VampireSunRules#isDaytime} so sun-safe biomes (vampire forest /
	 * ominous) keep the coat visible even when the world clock is day.
	 */
	private static boolean shouldHideForDaytime(EntityLivingBase entity) {
		if (!(entity instanceof EntityPlayer) || !ModCompat.VAMPIRISM) {
			return false;
		}
		EntityPlayer player = (EntityPlayer) entity;
		if (!WitcheryUtil.hasVampireLordClothesCap(player)) {
			return false;
		}
		return VampireSunRules.isDaytime(player);
	}

	public static boolean wearsWitcheryVampireChest(EntityPlayer player) {
		if (player == null) return false;
		ItemStack chest = player.getCurrentArmor(2);
		return chest != null && chest.getItem() instanceof ItemVampireClothes;
	}

	public static ModelBiped getArmorModel(EntityLivingBase entity, ItemStack stack, int slot) {
		if (stack == null || !(stack.getItem() instanceof ItemArmor)) {
			return null;
		}
		int armorType = ((ItemArmor) stack.getItem()).armorType;

		// Non-chest always hidden; chest also hidden by day for Lord + full set
		if (armorType != 1 || shouldHideForDaytime(entity)) {
			return HIDDEN;
		}

		ModelBiped model = ClientProxy.getArmorModel(LEGENDS_DRACULA_MODEL_ID);
		if (model == null) {
			return null;
		}

		// Chest pass: body + arms; never draw Dracula head/face.
		setShow(model.bipedHead, false);
		setShow(model.bipedHeadwear, false);
		setShow(model.bipedBody, true);
		setShow(model.bipedRightArm, true);
		setShow(model.bipedLeftArm, true);
		setShow(model.bipedRightLeg, false);
		setShow(model.bipedLeftLeg, false);

		if (model instanceof ModelDracula) {
			ModelDracula dracula = (ModelDracula) model;
			setShow(dracula.Head, false);
			setShow(dracula.Teeth1, false);
			setShow(dracula.Teeth2, false);
			setShow(dracula.Teeth3, false);
			setShow(dracula.Teeth4, false);
			setShow(dracula.Teeth5, false);
			setShow(dracula.Teeth6, false);
			setShow(dracula.Brow1, false);
			setShow(dracula.Brow2, false);
		}

		model.isSneak = entity.isSneaking();
		model.isRiding = entity.isRiding();
		model.isChild = entity.isChild();
		model.aimedBow = false;

		if (model instanceof ModelPlayer) {
			((ModelPlayer) model).armorSlot = 1;
		}

		if (entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) entity;
			model.heldItemRight = player.getHeldItem() != null ? 1 : 0;
			if (player.getItemInUseCount() > 0) {
				ItemStack held = player.getHeldItem();
				if (held != null) {
					EnumAction action = held.getItemUseAction();
					if (action == EnumAction.block) {
						model.heldItemRight = 3;
					} else if (action == EnumAction.bow) {
						model.aimedBow = true;
					}
				}
			}
		}

		return model;
	}

	public static String getArmorTexture(ItemStack stack, int slot) {
		if (stack == null || !(stack.getItem() instanceof ItemArmor)) {
			return null;
		}
		// Only override texture when we supply a custom chest model
		if (((ItemArmor) stack.getItem()).armorType != 1) {
			return null;
		}
		return CHEST_TEXTURE;
	}

	private static void setShow(ModelRenderer part, boolean show) {
		if (part != null) {
			part.showModel = show;
		}
	}
}
