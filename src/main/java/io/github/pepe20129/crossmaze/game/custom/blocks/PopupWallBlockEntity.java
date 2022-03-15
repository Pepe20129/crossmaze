package io.github.pepe20129.crossmaze.game.custom.blocks;

import io.github.pepe20129.crossmaze.CrossMaze;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PopupWallBlockEntity extends BlockEntity {
	public PopupWallBlockEntity(BlockPos pos, BlockState state) {
		super(CrossMaze.POPUP_WALL_BLOCK_ENTITY, pos, state);
		timeLeft = 6 * 20;
	}

	private int timeLeft;

	@Override
	public void writeNbt(NbtCompound tag) {
		tag.putInt("timeLeft", timeLeft);
		super.writeNbt(tag);
	}

	@Override
	public void readNbt(NbtCompound tag) {
		super.readNbt(tag);
		timeLeft = tag.getInt("timeLeft");
	}

	public static void tick(World world, BlockPos pos, BlockState state, PopupWallBlockEntity blockEntity) {
		blockEntity.timeLeft -= 1;
		if (blockEntity.timeLeft < 0) {
			world.removeBlock(pos, false);
			BlockState newBlockState = switch (state.get(PopupWallBlock.ORIGINAL_BLOCK)) {
				case 0 -> Blocks.LIGHT.getDefaultState();
				case 1 -> Blocks.LIGHT.getDefaultState().with(Properties.WATERLOGGED, true);
				case 2 -> Blocks.FIRE.getDefaultState();
				case 3 -> Blocks.SOUL_FIRE.getDefaultState();
				default -> throw new IllegalStateException("Unexpected value: " + state.get(PopupWallBlock.ORIGINAL_BLOCK));
			};
			world.setBlockState(pos, newBlockState);
		} else {
			state = state.with(PopupWallBlock.COLOR, Math.min(blockEntity.timeLeft/20, 5));
			world.setBlockState(pos, state);
		}
	}
}