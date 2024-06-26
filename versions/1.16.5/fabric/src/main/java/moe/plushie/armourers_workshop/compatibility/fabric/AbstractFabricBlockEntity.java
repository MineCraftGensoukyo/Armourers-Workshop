package moe.plushie.armourers_workshop.compatibility.fabric;

import moe.plushie.armourers_workshop.api.common.IBlockEntitySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public interface AbstractFabricBlockEntity {

    static <T extends BlockEntity> T create(BlockEntityType<T> entityType, BlockGetter level, BlockPos blockPos, BlockState blockState) {
        return entityType.create();
    }

    static <T extends BlockEntity> BlockEntityType<T> createType(IBlockEntitySupplier<T> supplier, Block... blocks) {
        BlockEntityType<?>[] entityTypes = {null};
        BlockEntityType<T> entityType = BlockEntityType.Builder.of(() -> supplier.create(entityTypes[0], BlockPos.ZERO, null), blocks).build(null);
        entityTypes[0] = entityType;
        return entityType;
    }
}
