package mods.belgabor.bitdrawers.block;

import com.jaquadro.minecraft.storagedrawers.api.storage.BlockType;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawer;
import com.jaquadro.minecraft.storagedrawers.api.storage.INetworked;
import com.jaquadro.minecraft.storagedrawers.block.BlockDrawers;
import com.jaquadro.minecraft.storagedrawers.block.EnumCompDrawer;
import com.jaquadro.minecraft.storagedrawers.block.dynamic.StatusModelData;
import com.jaquadro.minecraft.storagedrawers.block.tile.TileEntityDrawers;
import com.jaquadro.minecraft.storagedrawers.config.ConfigManager;
import com.jaquadro.minecraft.storagedrawers.config.PlayerConfigSetting;
import com.jaquadro.minecraft.storagedrawers.inventory.DrawerInventoryHelper;
import com.jaquadro.minecraft.storagedrawers.security.SecurityManager;
import mcp.MethodsReturnNonnullByDefault;
import mod.chiselsandbits.api.APIExceptions;
import mod.chiselsandbits.api.IBitBag;
import mod.chiselsandbits.api.IBitBrush;
import mod.chiselsandbits.api.ItemType;
import mods.belgabor.bitdrawers.BitDrawers;
import mods.belgabor.bitdrawers.block.tile.TileBitDrawers;
import mods.belgabor.bitdrawers.core.BDLogger;
import mods.belgabor.bitdrawers.core.BitHelper;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

/**
 * Created by Belgabor on 02.06.2016.
 * Based on BlockCompDrawers by jaquadro
 */
public class BlockBitDrawers extends BlockDrawers implements INetworked
{
    public static final PropertyEnum SLOTS = PropertyEnum.create("slots", EnumCompDrawer.class);

    @SideOnly(Side.CLIENT)
    private StatusModelData statusInfo;

    public BlockBitDrawers (String registryName, String blockName) {
        super(Material.ROCK, registryName, blockName);

        setSoundType(SoundType.STONE);
    }

    @Override
    protected void initDefaultState () {
        setDefaultState(blockState.getBaseState().withProperty(SLOTS, EnumCompDrawer.OPEN1).withProperty(FACING, EnumFacing.NORTH));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void initDynamic () {
        ResourceLocation location = new ResourceLocation(BitDrawers.MODID + ":models/dynamic/bitDrawers.json");
        statusInfo = new StatusModelData(3, location);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public StatusModelData getStatusInfo (IBlockState state) {
        return statusInfo;
    }

    @Override
    public int getDrawerCount (IBlockState state) {
        return 3;
    }

    @Override
    public boolean isHalfDepth (IBlockState state) {
        return false;
    }

    @Override
    protected int getDrawerSlot (int drawerCount, int side, float hitX, float hitY, float hitZ) {
        if (hitTop(hitY))
            return 0;

        if (hitLeft(side, hitX, hitZ))
            return 1;
        else
            return 2;
    }
    
    @Override
    @MethodsReturnNonnullByDefault
    @ParametersAreNonnullByDefault
    public IBlockState getStateForPlacement (World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState();
    }


    @Override
    public BlockType retrimType () {
        return null;
    }

    @Override
    @ParametersAreNonnullByDefault
    public TileEntityDrawers createNewTileEntity (World world, int meta) {
        return new TileBitDrawers();
    }

    @Override
    public void getSubBlocks (CreativeTabs creativeTabs, NonNullList<ItemStack> list) {
        list.add(new ItemStack(this, 1, 0));
    }

    @Override
    public IBlockState getStateFromMeta (int meta) {
        return getDefaultState();
    }

    @Override
    public int getMetaFromState (IBlockState state) {
        return 0;
    }

    @Override
    @MethodsReturnNonnullByDefault
    protected BlockStateContainer createBlockState () {
        return new ExtendedBlockState(this, new IProperty[] { SLOTS, FACING }, new IUnlistedProperty[] { STATE_MODEL });
    }

    @Override
    public IBlockState getActualState (IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntityDrawers tile = getTileEntity(world, pos);
        if (tile == null)
            return state;

        EnumFacing facing = EnumFacing.getFront(tile.getDirection());
        if (facing.getAxis() == EnumFacing.Axis.Y)
            facing = EnumFacing.NORTH;

        EnumCompDrawer slots = EnumCompDrawer.OPEN1;
        if (tile.getGroup().getDrawer(1).isEnabled())
            slots = EnumCompDrawer.OPEN2;
        if (tile.getGroup().getDrawer(2).isEnabled())
            slots = EnumCompDrawer.OPEN3;

        return state.withProperty(FACING, facing).withProperty(SLOTS, slots);
    }
    
    @Override
    public void breakBlock (World world, BlockPos pos, IBlockState state) {
        TileEntityDrawers tile = getTileEntity(world, pos);

        if (tile != null && !tile.isSealed()) {
            for (int i = 0; i < tile.upgrades().getSlotCount(); i++) {
                ItemStack stack = tile.upgrades().getUpgrade(i);
                if (!stack.isEmpty())
                    spawnAsEntity(world, pos, stack);
            }

            if (!tile.upgrades().hasVendingUpgrade())
                DrawerInventoryHelper.dropInventoryItems(world, pos, tile);
        }

        super.breakBlock(world, pos, state);
    }

    @Override
    public void onBlockClicked(World world, BlockPos pos, EntityPlayer player) {
        if (world.isRemote) {
            return;
        }

        if (BitDrawers.config.debugTrace)
            BDLogger.info("IExtendedBlockClickHandler.onBlockClicked");
        
        RayTraceResult rayResult = net.minecraftforge.common.ForgeHooks.rayTraceEyes(player, ((EntityPlayerMP) player).interactionManager.getBlockReachDistance() + 1);
        if (rayResult == null)
            return;
        
        EnumFacing side = rayResult.sideHit;
        // adjust hitVec for drawers
        float hitX = (float)(rayResult.hitVec.x - pos.getX());
        float hitY = (float)(rayResult.hitVec.y - pos.getY());
        float hitZ = (float)(rayResult.hitVec.z - pos.getZ());
        
        TileEntityDrawers tileDrawers = getTileEntitySafe(world, pos);
        if (tileDrawers.getDirection() != side.ordinal())
            return;

        if (tileDrawers.isSealed())
            return;

        if (!SecurityManager.hasAccess(player.getGameProfile(), tileDrawers))
            return;

        Map<String, PlayerConfigSetting<?>> configSettings = ConfigManager.serverPlayerConfigSettings.get(player.getUniqueID());
        boolean invertShift = false;
        if (configSettings != null) {
            PlayerConfigSetting<Boolean> setting = (PlayerConfigSetting<Boolean>) configSettings.get("invertShift");
            if (setting != null) {
                invertShift = setting.value;
            }
        }
        
        int slot = getDrawerSlot(getDrawerCount(world.getBlockState(pos)), side.ordinal(), hitX, hitY, hitZ);
        IDrawer drawer = tileDrawers.getGroup().getDrawer(slot);

        ItemStack item/* = ItemStack.EMPTY*/;

        ItemStack held = player.inventory.getCurrentItem();
        if (BitDrawers.config.debugTrace)
            BDLogger.info("  Player %s", held.isEmpty()?"does not hold an item":"is holding an item");
        ItemType heldType = BitDrawers.cnb_api.getItemType(held);
        IItemHandler handler = held.isEmpty()?null:held.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        if (handler instanceof IBitBag) {
            IBitBag bag = (IBitBag) handler;
            drawer = tileDrawers.getGroup().getDrawer(1);
            if (drawer.getStoredItemPrototype().isEmpty())
                return;
            int retrieved = 0;
            if (player.isSneaking() != invertShift) {
                for (int i = 0; i < bag.getSlots(); i++) {
                    ItemStack test = bag.getStackInSlot(i);
                    if (test.isEmpty() || (test.getCount() < bag.getBitbagStackSize() && drawer.canItemBeExtracted(test))) {
                        int local = fillBagSlot(bag, i, drawer, tileDrawers);
                        if (local == 0)
                            break;
                        retrieved += local;
                    }
                }
            } else {
                int addSlot = -1;
                for (int i = 0; i < bag.getSlots(); i++) {
                    ItemStack test = bag.getStackInSlot(i);
                    if (test.isEmpty()) {
                        if (addSlot == -1)
                            addSlot = i;
                        continue;
                    }
                    if (test.getCount() < bag.getBitbagStackSize() && drawer.canItemBeExtracted(test)) {
                        addSlot = i;
                        break;
                    }
                }
                if (addSlot >= 0) {
                    retrieved = fillBagSlot(bag, addSlot, drawer, tileDrawers);
                }
            }
            if (retrieved > 0)
                world.playSound(null, pos.getX() + .5f, pos.getY() + .5f, pos.getZ() + .5f, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, .2f, ((world.rand.nextFloat() - world.rand.nextFloat()) * .7f + 1) * 2);
            item = ItemStack.EMPTY;
        } else if (slot == 1 && heldType != null && heldType.isBitAccess) {
            ItemStack bit = drawer.getStoredItemPrototype();
            if (bit.isEmpty())
                return;

            IBitBrush brush;
            try {
                brush = BitDrawers.cnb_api.createBrush(bit);
            } catch (APIExceptions.InvalidBitItem e) {
                return;
            }
            item = BitHelper.getMonochrome(held, brush);
            if (item.isEmpty())
                return;
            int bitCount = item.getCount();

            if (player.isSneaking() != invertShift)
                item.setCount(64);
            else
                item.setCount(1);
            item.setCount(Math.min(item.getCount(), drawer.getStoredItemCount() / bitCount));
            if (item.isEmpty())
                return;

            drawer.setStoredItemCount(drawer.getStoredItemCount() - (item.getCount() * bitCount));
        } else {
            if (player.isSneaking() != invertShift)
                item = tileDrawers.takeItemsFromSlot(slot, drawer.getStoredItemStackSize());
            else
                item = tileDrawers.takeItemsFromSlot(slot, 1);

            if (BitDrawers.config.debugTrace)
                BDLogger.info((item.isEmpty()) ? "  EMPTY" : "  " + item.toString());

        }
        IBlockState state = world.getBlockState(pos);
        if (!item.isEmpty()) {
            if (!player.inventory.addItemStackToInventory(item)) {
                dropItemStack(world, pos.offset(side), player, item);
                world.notifyBlockUpdate(pos, state, state, 3);
            }
            else
                world.playSound(null, pos.getX() + .5f, pos.getY() + .5f, pos.getZ() + .5f, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, .2f, ((world.rand.nextFloat() - world.rand.nextFloat()) * .7f + 1) * 2);
        }

    }
    
    protected void dropItemStack (World world, BlockPos pos, EntityPlayer player, ItemStack stack) {
        EntityItem entity = new EntityItem(world, pos.getX() + .5f, pos.getY() + .1f, pos.getZ() + .5f, stack);
        entity.addVelocity(-entity.motionX, -entity.motionY, -entity.motionZ);
        world.spawnEntity(entity);
    }
    
    protected int fillBagSlot(IBitBag bag, int slot, IDrawer drawer, TileEntityDrawers tileDrawers) {
        int toAdd = bag.getBitbagStackSize();
        ItemStack item = bag.getStackInSlot(slot);
        if (!item.isEmpty())
            toAdd -= item.getCount();
        item = drawer.getStoredItemPrototype().copy();
        item.setCount(toAdd);
        ItemStack test = bag.insertItem(slot, item.copy(), true);
        if (!test.isEmpty()) {
            toAdd -= test.getCount();
        }
        if (toAdd == 0)
            return 0;
        int retrieved = 0;
        while (true) {
            ItemStack temp = tileDrawers.takeItemsFromSlot(1, toAdd);
            if (temp.isEmpty())
                break;
            retrieved += temp.getCount();
            toAdd -= temp.getCount();
            if (toAdd == 0)
                break;
        }
        if (retrieved == 0)
            return 0;
        item.setCount(retrieved);
        test = bag.insertItem(slot, item, false);
        if (!test.isEmpty()) {
            BDLogger.error("Could not insert simulated bit amount into bag. Something went very wrong.");
        }
        
        return retrieved;
    }

}
