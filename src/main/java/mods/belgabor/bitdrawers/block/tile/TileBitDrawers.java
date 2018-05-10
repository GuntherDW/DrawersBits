package mods.belgabor.bitdrawers.block.tile;

import com.jaquadro.minecraft.storagedrawers.StorageDrawers;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawer;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerAttributes;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerGroup;
import com.jaquadro.minecraft.storagedrawers.block.tile.TileEntityDrawers;
import com.jaquadro.minecraft.storagedrawers.network.CountUpdateMessage;
import mod.chiselsandbits.api.APIExceptions;
import mod.chiselsandbits.api.IBitAccess;
import mod.chiselsandbits.api.IBitBag;
import mod.chiselsandbits.api.IBitBrush;
import mod.chiselsandbits.api.ItemType;
import mods.belgabor.bitdrawers.BitDrawers;
import mods.belgabor.bitdrawers.block.tile.tiledata.BitsDrawerGroup;
import mods.belgabor.bitdrawers.core.BDLogger;
import mods.belgabor.bitdrawers.core.BitHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Created by Belgabor on 02.06.2016.
 * Based on TileEntityDrawersComp by jaquadro
 */
public class TileBitDrawers extends TileEntityDrawers {
    @CapabilityInject(IDrawerAttributes.class)
    static Capability<IDrawerAttributes> DRAWER_ATTRIBUTES_CAPABILITY = null;

    private GroupData groupData;

    private int capacity = 0;

    public TileBitDrawers() {
        groupData = new GroupData(3);
        groupData.setCapabilityProvider(this);

        injectPortableData(groupData);
    }

    @Override
    public IDrawerGroup getGroup() {
        return groupData;
    }

    @Override
    public int interactPutItemsIntoSlot (int slot, EntityPlayer player) {
        if (BitDrawers.config.debugTrace)
            BDLogger.info("TileBitDrawers:interactPutItemsIntoSlot %d", slot);
        ItemStack stack = player.inventory.getCurrentItem();
        if (!stack.isEmpty()) {
            if (stack.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
                return interactPutBagIntoSlot(slot, stack);
            } else if (slot == 2) {
                ItemType type = BitDrawers.cnb_api.getItemType(stack);
                if (type == ItemType.POSITIVE_DESIGN || type == ItemType.NEGATIVE_DESIGN || type == ItemType.MIRROR_DESIGN) {
                    return interactSetCustomSlot(stack);
                }
            }
        }

        return super.interactPutItemsIntoSlot(slot, player);
    }


    public int interactPutBagIntoSlot (int slot, @Nonnull ItemStack stack) {
        if (BitDrawers.config.debugTrace)
            BDLogger.info("TileBitDrawers:interactPutBagIntoSlot %d %s", slot, stack.isEmpty() ? "EMPTY" : stack.getDisplayName());
        int added = 0;
        IItemHandler handler = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        if (handler instanceof IBitBag) {
            slot = 1;
            if (BitDrawers.config.debugTrace)
                BDLogger.info("TileBitDrawers:interactPutBagIntoSlot Bit Bag detected");
        } else if (handler == null) {
            return 0;
        }
        for(int i = 0; i < handler.getSlots(); i++) {
            while (true) {
                ItemStack extract = handler.extractItem(i, 64, true);
                if (extract.isEmpty())
                    break;
                int extracted = extract.getCount();
                int inserted = putItemsIntoSlot(slot, extract, extracted);
                if (inserted > 0) {
                    added += inserted;
                    ItemStack test = handler.extractItem(i, inserted, false);
                    if (test.getCount() < inserted)
                        BDLogger.error("Could not extract simulated amount from bag. Something went very wrong.");
                }
                if (inserted < extracted)
                    break;
            }
        }
        return added;
    }

    public int interactSetCustomSlot (@Nonnull ItemStack stack) {
        ItemStack bit = groupData.getDrawer(1).getStoredItemPrototype();
        if (bit.isEmpty())
            return 0;

        IBitBrush brush;
        try {
            brush = BitDrawers.cnb_api.createBrush(bit);
        } catch (APIExceptions.InvalidBitItem e) {
            return 0;
        }
        ItemStack item = BitHelper.getMonochrome(stack, brush);
        if (item.isEmpty())
            groupData.populateRawSlot(2, ItemStack.EMPTY, 0);
        else
            groupData.populateRawSlot(2, item, item.getCount());

        return 1;
    }

    @Override
    public int putItemsIntoSlot (int slot, @Nonnull ItemStack stack, int count) {
        if (BitDrawers.config.debugTrace)
            BDLogger.info("TileBitDrawers:putItemsIntoSlot %d %s %d", slot, stack.isEmpty()?"EMPTY":stack.getDisplayName(), count);
        int added = 0;
        if (!stack.isEmpty()) {
            if (BitDrawers.cnb_api.getItemType(stack) == ItemType.CHISLED_BLOCK) {
                return putChiseledBlockIntoDrawer(stack, count);
            }
        }

        return added + super.putItemsIntoSlot(slot, stack, count);
    }

    public int putChiseledBlockIntoDrawer (@Nonnull ItemStack stack, int count) {
        if (BitDrawers.config.debugTrace)
            BDLogger.info("TileBitDrawers:putChiseledBlockIntoDrawer %s %d", stack.isEmpty()?"EMPTY":stack.getDisplayName(), count);
        count = Math.min(count, stack.getCount());
        IDrawer drawer = groupData.getDrawer(1);
        IBitAccess access = BitDrawers.cnb_api.createBitItem(stack);
        // TODO: Check for edge cases?
        /* if (convRate == null || convRate[0] == 0 || access == null)
            return 0; */
        BitHelper.BitCounter counter = new BitHelper.BitCounter();
        access.visitBits(counter);
        IBitBrush stored = null;
        try {
            stored = BitDrawers.cnb_api.createBrush(drawer.getStoredItemPrototype());
        } catch (APIExceptions.InvalidBitItem invalidBitItem) {
            BDLogger.error("Failed to create bit brush for stored bit");
            BDLogger.error(invalidBitItem);
            return 0;
        }
        if (counter.counts.size() != 1 || !counter.counts.containsKey(stored.getStateID()) || counter.counts.get(stored.getStateID()) == 0) {
            if (BitDrawers.config.debugTrace)
                BDLogger.info("TileBitDrawers:putChiseledBlockIntoDrawer Not Matched %d", counter.counts.size());
            return 0;
        }

        int bitSize = counter.counts.get(stored.getStateID());
        int canStore = getDrawerAttributes().isVoid() ? count : drawer.getRemainingCapacity() / bitSize;
        int toStore = Math.min(canStore, count);
        int toStoreBits = toStore * bitSize;
        ItemStack store = drawer.getStoredItemPrototype().copy();
        store.setCount(toStoreBits);
        int storedBits = super.putItemsIntoSlot(1, store, toStoreBits);
        if (storedBits != toStoreBits) {
            BDLogger.error("Couldn't store bits when inserting chiseled block. This is not supposed to happen at this point.");
            toStore = storedBits / bitSize;
        }
        stack.shrink(toStore);
        return toStore;
    }

    @Override
    protected void onAttributeChanged() {
        groupData.syncAttributes();
    }

    private class GroupData extends BitsDrawerGroup {
        public GroupData(int slotCount) {
            super(slotCount);
        }

        @Override
        protected World getWorld() {
            return TileBitDrawers.this.getWorld();
        }

        @Override
        protected void log(String message) {
            if (!getWorld().isRemote && StorageDrawers.config.cache.debugTrace)
                StorageDrawers.log.info(message);
        }

        @Override
        protected int getStackCapacity() {
            return upgrades().getStorageMultiplier() * getEffectiveDrawerCapacity();
        }

        @Override
        protected void onItemChanged() {
            if (getWorld() != null && !getWorld().isRemote) {
                markDirty();
                markBlockForUpdate();
            }
        }

        @Override
        protected void onAmountChanged() {
            if (getWorld() != null && !getWorld().isRemote) {
                IMessage message = new CountUpdateMessage(getPos(), 0, getPooledCount());
                NetworkRegistry.TargetPoint targetPoint = new NetworkRegistry.TargetPoint(getWorld().provider.getDimension(), getPos().getX(), getPos().getY(), getPos().getZ(), 500);

                StorageDrawers.network.sendToAllAround(message, targetPoint);

                markDirty();
            }
        }

        @Override
        public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == TileBitDrawers.DRAWER_ATTRIBUTES_CAPABILITY
                    || super.hasCapability(capability, facing);

        }

        @Nullable
        @Override
        public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
            if (capability == TileBitDrawers.DRAWER_ATTRIBUTES_CAPABILITY)
                return (T) TileBitDrawers.this.getDrawerAttributes();

            return super.getCapability(capability, facing);
        }
    }

    @Override
    public int getDrawerCapacity() {
        if (getWorld() == null || getWorld().isRemote)
            return super.getDrawerCapacity();

        if (capacity == 0) {
            capacity = BitDrawers.config.bitdrawerStorage;

            if (capacity <= 0)
                capacity = 1;
        }

        return capacity;
    }

    @Override
    public boolean dataPacketRequiresRenderUpdate() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void clientUpdateCount(final int slot, final int count) {
        if (!getWorld().isRemote)
            return;

        Minecraft.getMinecraft().addScheduledTask(() -> TileBitDrawers.this.clientUpdateCountAsync(count));
    }

    @SideOnly(Side.CLIENT)
    private void clientUpdateCountAsync(int count) {
        groupData.setPooledCount(count);
    }

    @Override
    public String getName() {
        return hasCustomName() ? super.getName() : "bitDrawers.container.bitDrawers";
    }
}
