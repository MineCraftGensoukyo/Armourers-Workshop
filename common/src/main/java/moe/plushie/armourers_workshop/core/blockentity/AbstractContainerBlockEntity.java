package moe.plushie.armourers_workshop.core.blockentity;

import moe.plushie.armourers_workshop.api.common.IBlockEntityPacketHandler;
import moe.plushie.armourers_workshop.api.common.IHasInventory;
import moe.plushie.armourers_workshop.utils.Constants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractContainerBlockEntity extends RandomizableContainerBlockEntity implements IBlockEntityPacketHandler, IHasInventory {

    public AbstractContainerBlockEntity(BlockEntityType<?> entityType) {
        super(entityType);
    }

    public abstract void readFromNBT(CompoundTag nbt);

    public abstract void writeToNBT(CompoundTag nbt);

    public void sendBlockUpdates() {
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(getBlockPos(), state, state, Constants.BlockFlags.DEFAULT_AND_RERENDER);
        }
    }

    @Override
    public void load(BlockState state, CompoundTag nbt) {
        super.load(state, nbt);
        this.readFromNBT(nbt);
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        super.save(nbt);
        this.writeToNBT(nbt);
        return nbt;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.save(new CompoundTag());
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        CompoundTag tag = new CompoundTag();
        this.writeToNBT(tag);
        return new ClientboundBlockEntityDataPacket(this.worldPosition, 0, tag);
    }

    @Override
    public void handleUpdatePacket(BlockState state, CompoundTag tag) {
        this.readFromNBT(tag);
        this.sendBlockUpdates();
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return null;
    }

    @Override
    public Container getInventory() {
        return this;
    }

    @Override
    protected Component getDefaultName() {
        return null;
    }
}
