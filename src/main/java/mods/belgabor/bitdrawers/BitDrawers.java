package mods.belgabor.bitdrawers;

/**
 * Created by Belgabor on 02.06.2016.
 */

import com.jaquadro.minecraft.storagedrawers.network.CountUpdateMessage;
import mod.chiselsandbits.api.ChiselsAndBitsAddon;
import mod.chiselsandbits.api.IChiselAndBitsAPI;
import mod.chiselsandbits.api.IChiselsAndBitsAddon;
import mods.belgabor.bitdrawers.config.ConfigManager;
import mods.belgabor.bitdrawers.core.BDLogger;
import mods.belgabor.bitdrawers.core.BlockRegistry;
import mods.belgabor.bitdrawers.core.CommonProxy;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;

@Mod(modid = BitDrawers.MODID,
        version = BitDrawers.VERSION,
        name = BitDrawers.MODNAME,
        dependencies = "required-after:chiselsandbits@[14.0,);required-after:storagedrawers@[1.12-"+BitDrawers.SD_VERSION+",);required-after:chameleon"
)
@ChiselsAndBitsAddon
public class BitDrawers implements IChiselsAndBitsAddon
{
    public static final String MODNAME = "Drawers & Bits";
    public static final String MODID = "bitdrawers";
    public static final String VERSION = "0.41";
    public static final String SD_VERSION = "5.3.5";
    public static final int[] SD_VERSIONS = {5, 3, 5};
    
    @SidedProxy(
            clientSide = "mods.belgabor.bitdrawers.client.ClientProxy",
            serverSide = "mods.belgabor.bitdrawers.core.CommonProxy"
    )
    public static CommonProxy proxy;
    public static ConfigManager config;
    
    public static IChiselAndBitsAPI cnb_api;

    public static SimpleNetworkWrapper network;

    public static boolean sdVersionCheckFailed = true;
    public static boolean sdMajorMismatch = false;
    public static boolean sdMinorMismatch = false;
    public static String detectedSdVersion = "";
    
    public static BlockRegistry blocks = new BlockRegistry();

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        BDLogger.logger = event.getModLog();
        config = new ConfigManager(new File(event.getModConfigurationDirectory(), "DrawersBits.cfg"));
        network = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);

        MinecraftForge.EVENT_BUS.register(this);
        
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            network.registerMessage(CountUpdateMessage.Handler.class, CountUpdateMessage.class, 1, Side.CLIENT);
        }
        else {
            network.registerMessage(CountUpdateMessage.HandlerStub.class, CountUpdateMessage.class, 1, Side.CLIENT);
        }
        
        ModContainer testContainer = Loader.instance().getIndexedModList().get("StorageDrawers");
        if (testContainer != null) {
            String[] testVersion = testContainer.getDisplayVersion().split("-");
            if (testVersion.length == 2) {
                String[] testVersionParts = testVersion[1].split("\\.");
                detectedSdVersion = testVersion[1];
                if (testVersionParts.length == 3) {
                    sdVersionCheckFailed = false;
                    try {
                        if ((Integer.parseInt(testVersionParts[0]) != SD_VERSIONS[0]) || (Integer.parseInt(testVersionParts[1]) != SD_VERSIONS[1])) {
                            sdMajorMismatch = true;
                            BDLogger.warn("Your version of Storage Drawers (%s) differs majorly from the one Drawers & Bits was compiled with (%s). Use at your own discretion, in case of issues please open an issue on GitHub and revert the version of Storage Drawers to the one known working until I can fix compatibility.", detectedSdVersion, SD_VERSION);
                        } else if (Integer.parseInt(testVersionParts[2]) != SD_VERSIONS[2]) {
                            sdMinorMismatch = true;
                            BDLogger.warn("Your version of Storage Drawers (%s) differs from the one Drawers & Bits was compiled with (%s). The difference is minor so issues are unlikely, but if one occurs, please open an issue on GitHub and revert the version of Storage Drawers to the one known working until I can fix compatibility.", detectedSdVersion, SD_VERSION);
                        } else if (config.debugTrace) {
                            BDLogger.info("Drawers & Bits: Storage Drawers version check OK (%s).", detectedSdVersion);
                        }
                    } catch (NumberFormatException e) {
                        sdVersionCheckFailed = true;
                    }
                }
            }
        } 
        if (sdVersionCheckFailed) {
            BDLogger.error("Drawers & Bits: Unable to verify StorageDrawers version. This probably isn't going to end well...");
        }
    }

    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> event) {
        blocks.initBlocks(event.getRegistry());
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        blocks.initItems(event.getRegistry());
    }

    /*
    @SubscribeEvent
    @SideOnly( Side.CLIENT )
    public void openMainMenu(final GuiOpenEvent event ) {
        // if the max shades has changed in form the user of the new usage.
        if ((!detectedSdVersion.equals(config.lastSDVersionWarned)) && (sdMajorMismatch || sdMinorMismatch || sdVersionCheckFailed)) {
            event.setGui(new GuiScreenStartup());
        }
    }
    */
    
    @Override
    public void onReadyChiselsAndBits(IChiselAndBitsAPI api) {
        cnb_api = api;
    }
}
