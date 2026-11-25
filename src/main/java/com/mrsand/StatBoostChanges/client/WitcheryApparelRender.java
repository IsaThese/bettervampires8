package com.mrsand.StatBoostChanges.client;

import org.lwjgl.opengl.GL11;

import com.emoniph.witchery.item.ItemVampireClothes;
import com.mrsand.StatBoostChanges.ModCompat;
import com.mrsand.StatBoostChanges.StatBoostConfig;
import com.mrsand.StatBoostChanges.abilities.VampireSunRules;
import com.mrsand.StatBoostChanges.util.WitcheryUtil;
import com.tihyo.horror.client.models.characters.ModelDracula;
import com.tihyo.legends.client.models.ModelPlayer;
import com.tihyo.legends.handlers.LegendsClientEventHandler;
import com.tihyo.legends.proxies.ClientProxy;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

/**
 * Maps Witchery {@code ItemVampireClothes} render to Legends apparel:
 * chest/coat + cape via {@link ModelDracula} (head hidden); legs/boots via
 * Vampire Apparel V1. Helmet stays equipped for set bonuses but uses a hidden
 * ModelBiped. Lord + full set: coat/legs/boots hide (or swap to Apparel V1) in
 * true sunlight — same as {@link VampireSunRules#isInSunlight} / day-night V1.
 */
@SideOnly(Side.CLIENT)
public final class WitcheryApparelRender {

	/** ClientProxy id for {@link ModelDracula}. */
	private static final int LEGENDS_DRACULA_MODEL_ID = 154;
	/** ClientProxy id for Vampire Apparel V1 (legs/boots). */
	private static final int LEGENDS_APPAREL_V1_MODEL_ID = 141;
	private static final String LAYER1_TEXTURE =
			"horror:textures/models/armors/legacy/vampire_apparel_v1/vampire_apparel_v1_1_0.png";
	private static final String LAYER2_TEXTURE =
			"horror:textures/models/armors/legacy/vampire_apparel_v1/vampire_apparel_v1_2_0.png";
	private static final String PACK_DIR = "textures/models/armors/legacy/vampire_apparel_v1/";
	private static final ResourceLocation CAPE_RED = new ResourceLocation("horror", PACK_DIR + "cape_red.png");
	private static final ResourceLocation CAPE_RED_2 = new ResourceLocation("horror", PACK_DIR + "cape_red_2.png");
	private static final ResourceLocation CAPE_0 = new ResourceLocation("horror", PACK_DIR + "cape_0.png");
	private static final ResourceLocation CAPE_1 = new ResourceLocation("horror", PACK_DIR + "cape_1.png");
	private static final ResourceLocation CAPE_2 = new ResourceLocation("horror", PACK_DIR + "cape_2.png");
	private static final ResourceLocation STOCK_CAPE_0 = new ResourceLocation(
			"horror", "textures/models/armors/dracula/default/cape_0.png");

	/** Helmet (and daytime hide): all parts off. */
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
	 * True sunlight exposure (brightness + sky), same as day/night Apparel V1.
	 * Sun-safe biomes never count as sunlight. Calendar {@code isDay} is not used —
	 * shade by day keeps the coat.
	 */
	private static boolean inSunlightForApparel(EntityPlayer player) {
		return VampireSunRules.isInSunlight(player);
	}

	/**
	 * Vampire Lord + full Witchery set in sunlight:
	 * {@code lordDaylightShowVampireApparel=false} → hide (regular skin);
	 * {@code true} → keep armor visible as basic Apparel V1 (not Dracula coat).
	 */
	private static boolean shouldHideForDaytime(EntityLivingBase entity) {
		if (StatBoostConfig.lordDaylightShowVampireApparel) {
			return false;
		}
		if (!(entity instanceof EntityPlayer) || !ModCompat.VAMPIRISM) {
			return false;
		}
		EntityPlayer player = (EntityPlayer) entity;
		if (!WitcheryUtil.hasVampireLordClothesCap(player)) {
			return false;
		}
		return inSunlightForApparel(player);
	}

	/** Config true + sunlight: chest/legs/boots use Apparel V1 instead of Dracula. */
	private static boolean shouldUseBasicApparelInSunlight(EntityLivingBase entity) {
		if (!StatBoostConfig.lordDaylightShowVampireApparel) {
			return false;
		}
		if (!(entity instanceof EntityPlayer) || !ModCompat.VAMPIRISM) {
			return false;
		}
		EntityPlayer player = (EntityPlayer) entity;
		if (!WitcheryUtil.hasVampireLordClothesCap(player)) {
			return false;
		}
		return inSunlightForApparel(player);
	}

	public static boolean wearsWitcheryVampireChest(EntityPlayer player) {
		return wearsWitcheryVampireChest((EntityLivingBase) player);
	}

	/** Chest check for players and Witchery vampire NPCs (equipment slot indices differ). */
	public static boolean wearsWitcheryVampireChest(EntityLivingBase entity) {
		if (entity == null) return false;
		ItemStack chest;
		if (entity instanceof EntityPlayer) {
			chest = ((EntityPlayer) entity).getCurrentArmor(2);
		} else {
			chest = entity.getEquipmentInSlot(3);
		}
		return chest != null && chest.getItem() instanceof ItemVampireClothes;
	}

	/**
	 * Stock {@code ModelDracula.renderCape} draws Tessellator quads for
	 * {@link EntityPlayer} / EntityStand only — not {@code Cape.render()}.
	 * Mirror the EntityStand strip path for Witchery {@code EntityVampire}.
	 */
	public static void renderLivingWitcheryCape(ModelPlayer model, EntityLivingBase living, int capeLength) {
		if (model == null || living == null || model.armorSlot != 1) {
			return;
		}

		ResourceLocation capeTex = resolveWitcheryCapeTexture(0);
		Minecraft.getMinecraft().getTextureManager().bindTexture(capeTex);

		float scale = 0.0625F;
		int widthPx = 13;
		float texH = 200.0F;
		float halfW = widthPx * scale;
		float stripH = capeLength * scale / texH;
		float uSpan = capeLength / 32.0F;
		float vSpan = widthPx / 64.0F;

		float partial = LegendsClientEventHandler.renderTick;
		float limb = living.prevLimbSwingAmount
				+ (living.limbSwingAmount - living.prevLimbSwingAmount) * partial;
		float hang = 5.0F + limb * 25.0F + (float) living.motionY * -30.0F;
		hang = MathHelper.clamp_float(hang, 5.0F, 55.0F);

		Tessellator tess = Tessellator.instance;
		GL11.glPushMatrix();
		GL11.glEnable(GL11.GL_CULL_FACE);
		model.bipedBody.postRender(scale);
		GL11.glTranslatef(0.0F, -0.01F, 0.14F);
		GL11.glRotatef(hang, 1.0F, 0.0F, 0.0F);

		for (int face = 0; face < 2; face++) {
			GL11.glPushMatrix();
			for (int i = 0; i < (int) texH; i++) {
				tess.startDrawingQuads();
				float v0 = uSpan / texH * i;
				float v1 = uSpan / texH * (i + 1);
				if (face == 0) {
					tess.addVertexWithUV(halfW / 2.0F, 0.0D, 0.0D, 0.0D, v0);
					tess.addVertexWithUV(halfW / 2.0F, stripH, 0.0D, 0.0D, v1);
					tess.addVertexWithUV(-halfW / 2.0F, stripH, 0.0D, vSpan, v1);
					tess.addVertexWithUV(-halfW / 2.0F, 0.0D, 0.0D, vSpan, v0);
				} else {
					tess.addVertexWithUV(-halfW / 2.0F, stripH, 0.0D, vSpan, v1);
					tess.addVertexWithUV(halfW / 2.0F, stripH, 0.0D, vSpan * 2.0F, v1);
					tess.addVertexWithUV(halfW / 2.0F, 0.0D, 0.0D, vSpan * 2.0F, v0);
					tess.addVertexWithUV(-halfW / 2.0F, 0.0D, 0.0D, vSpan, v0);
				}
				tess.draw();
				GL11.glTranslatef(0.0F, stripH, 0.0F);
			}
			GL11.glPopMatrix();
		}

		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glPopMatrix();
	}

	public static ResourceLocation resolveWitcheryCapeTexture(int stage) {
		int style = StatBoostConfig.witcheryVampireCapeStyle;
		if (style < 0 || style > 2) style = 0;
		if (style == 2) return STOCK_CAPE_0;
		if (stage < 0) stage = 0;
		if (stage > 2) stage = 2;
		if (style == 0) {
			return stage >= 2 ? CAPE_RED_2 : CAPE_RED;
		}
		if (stage <= 0) return CAPE_0;
		if (stage == 1) return CAPE_1;
		return CAPE_2;
	}

	public static ModelBiped getArmorModel(EntityLivingBase entity, ItemStack stack, int slot) {
		if (stack == null || !(stack.getItem() instanceof ItemArmor)) {
			return null;
		}
		int armorType = ((ItemArmor) stack.getItem()).armorType;

		// Helmet always hidden; sunlight + config false → regular skin.
		if (armorType == 0 || shouldHideForDaytime(entity)) {
			return HIDDEN;
		}

		// Sunlight + config true → basic Apparel V1 (not Dracula coat).
		if (shouldUseBasicApparelInSunlight(entity)) {
			if (armorType == 1) {
				return getBasicChestModel(entity);
			}
			if (armorType == 2 || armorType == 3) {
				return getLegsBootsModel(entity, armorType);
			}
			return HIDDEN;
		}

		if (armorType == 1) {
			return getChestModel(entity);
		}
		if (armorType == 2 || armorType == 3) {
			return getLegsBootsModel(entity, armorType);
		}
		return HIDDEN;
	}

	/** Apparel V1 chest (body + arms) for sunlight when cfg show-apparel is true. */
	private static ModelBiped getBasicChestModel(EntityLivingBase entity) {
		ModelBiped model = ClientProxy.getArmorModel(LEGENDS_APPAREL_V1_MODEL_ID);
		if (model == null) {
			return null;
		}
		setShow(model.bipedHead, false);
		setShow(model.bipedHeadwear, false);
		setShow(model.bipedBody, true);
		setShow(model.bipedRightArm, true);
		setShow(model.bipedLeftArm, true);
		setShow(model.bipedRightLeg, false);
		setShow(model.bipedLeftLeg, false);
		applyCommonPose(model, entity, 1);
		return model;
	}

	private static ModelBiped getChestModel(EntityLivingBase entity) {
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

		applyCommonPose(model, entity, 1);
		return model;
	}

	private static ModelBiped getLegsBootsModel(EntityLivingBase entity, int armorType) {
		ModelBiped model = ClientProxy.getArmorModel(LEGENDS_APPAREL_V1_MODEL_ID);
		if (model == null) {
			return null;
		}

		setShow(model.bipedHead, false);
		setShow(model.bipedHeadwear, false);
		setShow(model.bipedBody, false);
		setShow(model.bipedRightArm, false);
		setShow(model.bipedLeftArm, false);
		setShow(model.bipedRightLeg, true);
		setShow(model.bipedLeftLeg, true);

		applyCommonPose(model, entity, armorType);
		return model;
	}

	private static void applyCommonPose(ModelBiped model, EntityLivingBase entity, int armorSlot) {
		model.isSneak = entity.isSneaking();
		model.isRiding = entity.isRiding();
		model.isChild = entity.isChild();
		model.aimedBow = false;

		if (model instanceof ModelPlayer) {
			((ModelPlayer) model).armorSlot = armorSlot;
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
	}

	public static String getArmorTexture(ItemStack stack, int slot) {
		if (stack == null || !(stack.getItem() instanceof ItemArmor)) {
			return null;
		}
		int armorType = ((ItemArmor) stack.getItem()).armorType;
		if (armorType == 1 || armorType == 3) {
			return LAYER1_TEXTURE;
		}
		if (armorType == 2) {
			return LAYER2_TEXTURE;
		}
		return null;
	}

	private static void setShow(ModelRenderer part, boolean show) {
		if (part != null) {
			part.showModel = show;
		}
	}
}
