package mods.belgabor.bitdrawers.item;

import com.jaquadro.minecraft.chameleon.resources.IItemMeshMapper;
import mods.belgabor.bitdrawers.BitDrawers;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Belgabor on 24.07.2016.
 */
public class ItemBitController extends ItemBlock implements IItemMeshMapper {
    public ItemBitController(Block block) {
        super(block);
    }
    
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        if (tooltip != null)
            tooltip.add(I18n.format("bitdrawers.bitcontroller.description"));
    }

    @Override
    public List<Pair<ItemStack, ModelResourceLocation>> getMeshMappings () {
        List<Pair<ItemStack, ModelResourceLocation>> mappings = new ArrayList<Pair<ItemStack, ModelResourceLocation>>();

        ModelResourceLocation location = new ModelResourceLocation(BitDrawers.MODID + ":bitcontroller", "inventory");
        mappings.add(Pair.of(new ItemStack(this, 1), location));

        return mappings;
    }
}
