package mods.belgabor.bitdrawers.core;

import com.jaquadro.minecraft.chameleon.Chameleon;
import com.jaquadro.minecraft.chameleon.resources.ModelRegistry;
import com.jaquadro.minecraft.storagedrawers.client.renderer.TileEntityDrawersRenderer;
import mods.belgabor.bitdrawers.BitDrawers;
import mods.belgabor.bitdrawers.block.BlockBitController;
import mods.belgabor.bitdrawers.block.BlockBitDrawers;
import mods.belgabor.bitdrawers.block.tile.TileBitController;
import mods.belgabor.bitdrawers.block.tile.TileBitDrawers;
import mods.belgabor.bitdrawers.client.model.BitDrawerModel;
import mods.belgabor.bitdrawers.item.ItemBitController;
import mods.belgabor.bitdrawers.item.ItemBitDrawer;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * Created by Belgabor on 18.07.2016.
 */
public class BlockRegistry {
    public static BlockBitDrawers bitDrawer;
    public static BlockBitController bitController;
    
    public void initBlocks(IForgeRegistry<Block> registry) {
        bitDrawer = new BlockBitDrawers(BitDrawers.MODID + ":bitdrawer", "bitdrawer");
        registry.register(bitDrawer);
        GameRegistry.registerTileEntity(TileBitDrawers.class, bitDrawer.getRegistryName().toString());
        bitController = new BlockBitController(BitDrawers.MODID + ":bitcontroller", "bitcontroller");
        if (BitDrawers.config.enableBitController) {
            registry.register(bitController);
            GameRegistry.registerTileEntity(TileBitController.class, bitController.getRegistryName().toString());
        }
    }

    public void initItems(IForgeRegistry<Item> registry) {
        registry.register((new ItemBitDrawer(bitDrawer)).setRegistryName(bitDrawer.getRegistryName()));
        if (BitDrawers.config.enableBitController) {
            registry.register((new ItemBitController(bitController)).setRegistryName(bitController.getRegistryName()));
        }
    }

    @SideOnly(Side.CLIENT)
    public void initClient() {
        bitDrawer.initDynamic();
        ClientRegistry.bindTileEntitySpecialRenderer(TileBitDrawers.class, new TileEntityDrawersRenderer());
        ModelRegistry modelRegistry = Chameleon.instance.modelRegistry;

        modelRegistry.registerModel(new BitDrawerModel.Register());
        if (BitDrawers.config.enableBitController) {
            modelRegistry.registerItemVariants(bitController);
        }
    }
}
