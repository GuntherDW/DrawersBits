package mods.belgabor.bitdrawers.client;

import mods.belgabor.bitdrawers.BitDrawers;
import mods.belgabor.bitdrawers.core.CommonProxy;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Created by Belgabor on 18.07.2016.
 */

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = { Side.CLIENT })
public class ClientProxy extends CommonProxy {

    public ClientProxy () {
        // TODO: Is this required?
        MinecraftForge.EVENT_BUS.register( this );
    }

    @SubscribeEvent
    public static void registerModels (ModelRegistryEvent event) throws Exception {
        BitDrawers.blocks.initClient();
    }
}
