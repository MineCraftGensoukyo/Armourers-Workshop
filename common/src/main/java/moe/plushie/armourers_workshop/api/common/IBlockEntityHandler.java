package moe.plushie.armourers_workshop.api.common;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public interface IBlockEntityHandler {

    /**
     * Called when you receive a TileEntityData packet for the location this
     * TileEntity is currently in. On the client, the NetworkManager will always
     * be the remote server. On the server, it will be whomever is responsible for
     * sending the packet.
     */
    default void handleUpdatePacket(BlockState state, CompoundTag tag) {
    }

    /**
     * Return an {@link AABB} that controls the visible scope of a {@link BlockEntityRenderer} associated with this {@link BlockEntity}
     * at this location.
     *
     * @return an appropriately size {@link AABB} for the {@link BlockEntity}
     */
    default AABB getCustomRenderBoundingBox(BlockState blockState) {
        return getDefaultRenderBoundingBox();
    }

    default AABB getDefaultRenderBoundingBox() {
        return new AABB(0, 0, 0, 1, 1, 1);
    }

    default double getViewDistance() {
        return 64;
    }
}
