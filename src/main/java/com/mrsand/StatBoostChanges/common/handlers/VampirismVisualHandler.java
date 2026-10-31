package com.mrsand.StatBoostChanges.common.handlers;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class VampirismVisualHandler {

//	private static boolean isRenderingDracula = false;
//	private static final ModelDracula draculaModel = new ModelDracula();
//	private static final ResourceLocation draculaTexture = new ResourceLocation("vampirism:textures/entity/dracula.png");
//
//	private final ItemStack[] EMPTY = new ItemStack[0];
//	private final LoadingCache<EntityPlayer, ItemStack[]> armorCache = CacheBuilder.newBuilder()
//			.expireAfterAccess(60L, TimeUnit.SECONDS)
//			.build(new CacheLoader<EntityPlayer, ItemStack[]>() {
//				public ItemStack[] load(EntityPlayer owner) { return EMPTY; }
//			});
//
//	// 1. Extend RenderPlayer instead of RenderBiped to avoid the EntityLiving cast crash
//	@SideOnly(Side.CLIENT)
//	private static final RenderPlayer draculaPlayerRenderer = new RenderPlayer() {
//		private final ResourceLocation draculaTexture = new ResourceLocation("vampirism:textures/entity/dracula.png");
//
//		// Initialization block to swap the default player model for ModelDracula
//		{
//			this.mainModel = new ModelDracula();
//			this.modelBipedMain = (ModelBiped) this.mainModel;
//		}
//
//		@Override
//		protected ResourceLocation getEntityTexture(AbstractClientPlayer player) {
//			return draculaTexture;
//		}
//	};
//
//	@SubscribeEvent
//	public void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
//		EntityPlayer player = event.entityPlayer;
//		if (GeneralUtil.isVampire(player)) {
//
//		}
//	}
}