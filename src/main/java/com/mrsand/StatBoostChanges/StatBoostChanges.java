package com.mrsand.StatBoostChanges;

import com.mrsand.StatBoostChanges.common.handlers.ClientEventHandler;
import com.mrsand.StatBoostChanges.common.handlers.ElectrobobsEventHandler;
import com.mrsand.StatBoostChanges.common.handlers.GeneralEventHandler;
import com.mrsand.StatBoostChanges.common.handlers.LegendsEventHandler;
import com.mrsand.StatBoostChanges.common.handlers.LegendsVisualHandler;
import com.mrsand.StatBoostChanges.common.handlers.SyncHandlerTarget;
import com.mrsand.StatBoostChanges.common.handlers.SyncVisualHandler;
import com.mrsand.StatBoostChanges.common.handlers.WitcheryEventHandler;
import com.mrsand.StatBoostChanges.common.packets.PacketSyncTarget;
import com.mrsand.StatBoostChanges.common.packets.PacketVignette;
import com.mrsand.StatBoostChanges.common.packets.VExtendedPlayerPacket;
import com.mrsand.StatBoostChanges.common.packets.VExtendedPlayerPacket.SyncVExtendedPlayerHandler;
import com.mrsand.StatBoostChanges.common.packets.VExtendedVillagerPacket;
import com.mrsand.StatBoostChanges.common.packets.VExtendedVillagerPacket.VExtendedVillagerHandler;
import com.mrsand.StatBoostChanges.common.packets.VisualPacketFromServer;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import net.minecraftforge.common.MinecraftForge;

@Mod(modid = "bettervampires", name = "Better Vampires", version = "1.0.0"
//, dependencies = "required-after:unimixins@[0.1.19);"
		)



public class StatBoostChanges {

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		StatBoostConfig.load(event.getSuggestedConfigurationFile());
		network = NetworkRegistry.INSTANCE.newSimpleChannel("bettervampires");
	}

	public static SimpleNetworkWrapper network;

	@EventHandler
	public void init(FMLInitializationEvent event) {
		//		//EntitySpider once again
		//		// 1. Map ID 52 to your class
		//		EntityList.IDtoClassMapping.put(52, CustomSpiderBehavior.class);
		//
		//		// 2. Map your class to ID 52 (Requires the AT above)
		//		EntityList.classToIDMapping.put(CustomSpiderBehavior.class, 52);
		//
		//		// 3. Map the name "Spider" to your class
		//		EntityList.stringToClassMapping.put("Spider", CustomSpiderBehavior.class);
		//
		//		// 4. Map the name "Spider" to ID 52 (Requires the AT above)
		//		EntityList.stringToIDMapping.put("Spider", 52);
		//		RenderingRegistry.registerEntityRenderingHandler(
		//			    CustomSpiderBehavior.class, 
		//			    new RenderSpider());


		// Register event handlers here
		network.registerMessage(SyncVExtendedPlayerHandler.class, VExtendedPlayerPacket.class, 0, Side.CLIENT);
		network.registerMessage(SyncVExtendedPlayerHandler.class, VExtendedPlayerPacket.class, 1, Side.SERVER);
		network.registerMessage(SyncHandlerTarget.class, PacketSyncTarget.class, 2, Side.SERVER);
		network.registerMessage(SyncVisualHandler.class, VisualPacketFromServer.class, 3, Side.CLIENT);
		network.registerMessage(VExtendedVillagerHandler.class, VExtendedVillagerPacket.class, 4, Side.CLIENT);
		network.registerMessage(PacketVignette.Handler.class, PacketVignette.class, 5, Side.CLIENT);

		GeneralEventHandler handler = new GeneralEventHandler();
		MinecraftForge.EVENT_BUS.register(handler);
		FMLCommonHandler.instance().bus().register(handler);

		if(Loader.isModLoaded("witchery")) {
			WitcheryEventHandler witcheryHandler = new WitcheryEventHandler();
			MinecraftForge.EVENT_BUS.register(witcheryHandler);
			FMLCommonHandler.instance().bus().register(witcheryHandler);
		}

		if(Loader.isModLoaded("legends")) {
			LegendsEventHandler legendsHandler = new LegendsEventHandler();
			MinecraftForge.EVENT_BUS.register(legendsHandler);
			FMLCommonHandler.instance().bus().register(legendsHandler);
		}

		ClientEventHandler clientHandler = new ClientEventHandler();
		MinecraftForge.EVENT_BUS.register(clientHandler);
		FMLCommonHandler.instance().bus().register(clientHandler);

		if(Loader.isModLoaded("wizardry")) {
			ElectrobobsEventHandler wizard = new ElectrobobsEventHandler();
			MinecraftForge.EVENT_BUS.register(wizard);
			FMLCommonHandler.instance().bus().register(wizard);
		}

		if (event.getSide() == Side.CLIENT) {
			if(Loader.isModLoaded("legends")) 
				MinecraftForge.EVENT_BUS.register(new LegendsVisualHandler());
			//	        
			//	    	Object renderHandler = new VampirismCameraHander();
			//		    MinecraftForge.EVENT_BUS.register(renderHandler);

		}


	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		if (Loader.isModLoaded("witchery")) {
			com.emoniph.witchery.Witchery.Items.BLOOD_GOBLET.setMaxStackSize(64);
		}
	}
}

