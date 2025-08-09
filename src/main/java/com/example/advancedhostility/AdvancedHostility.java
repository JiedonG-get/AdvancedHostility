package com.example.advancedhostility;

import com.example.advancedhostility.config.ConfigManager;
import com.example.advancedhostility.config.ModConfig;
import com.example.advancedhostility.config.ServerConfig;
import com.example.advancedhostility.event.ModCommands;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(AdvancedHostility.MOD_ID)
public class AdvancedHostility {
    public static final String MOD_ID = "advancedhostility";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AdvancedHostility() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // ★ MODIFICATION: We no longer need the commonSetup listener here for parsing.
        // modEventBus.addListener(this::commonSetup);

        // Register both COMMON and SERVER configuration files
        ModLoadingContext.get().registerConfig(Type.COMMON, ModConfig.SPEC, "advancedhostility-common.toml");
        ModLoadingContext.get().registerConfig(Type.SERVER, ServerConfig.SPEC, "advancedhostility-server.toml");

        // Register the main class instance to the FORGE event bus to listen for server and command events
        MinecraftForge.EVENT_BUS.register(this);
    }

    // ★ MODIFICATION: This method is no longer needed for parsing and can be removed or left empty.
    /*
    private void commonSetup(final FMLCommonSetupEvent event) {
        // We moved parsing to a later event.
    }
    */

    /**
     * This event fires right before a server starts, which is the perfect time
     * to load our layered configs, as the server-specific config is now available.
     */
    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        // ★ MODIFICATION: This is the new, correct place to parse the configs.
        ConfigManager.parse();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }
}