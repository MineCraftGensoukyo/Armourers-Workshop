package moe.plushie.armourers_workshop.compatibility;

import moe.plushie.armourers_workshop.api.annotation.Available;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@Available("[1.18, )")
public interface AbstractBlockEntityProvider extends EntityBlock {

    BlockEntity createBlockEntity(BlockGetter level, BlockPos blockPos, BlockState blockState);

    @Override
    default BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return createBlockEntity(null, blockPos, blockState);
    }
}
