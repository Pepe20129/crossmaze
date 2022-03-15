package io.github.pepe20129.crossmaze.game.custom.blocks;

import eu.pb4.polymer.api.block.PolymerBlock;
import io.github.pepe20129.crossmaze.CrossMaze;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class PopupWallBlock extends BlockWithEntity implements PolymerBlock, BlockEntityProvider {
	public PopupWallBlock() {
		super(FabricBlockSettings.of(Material.GLASS).strength(-1.0F, 3600000.0F).dropsNothing().sounds(BlockSoundGroup.GLASS).nonOpaque());
		setDefaultState(getStateManager().getDefaultState().with(COLOR, 5).with(ORIGINAL_BLOCK, 0));
	}

	@Override
	public Block getPolymerBlock(BlockState state) {
		return switch (state.get(COLOR)) {
			case 0 -> Blocks.RED_STAINED_GLASS;
			case 1 -> Blocks.ORANGE_STAINED_GLASS;
			case 2 -> Blocks.YELLOW_STAINED_GLASS;
			case 3 -> Blocks.LIME_STAINED_GLASS;
			case 4 -> Blocks.BLUE_STAINED_GLASS;
			case 5 -> Blocks.PURPLE_STAINED_GLASS;
			default -> throw new IllegalStateException("Unexpected value: " + state.get(COLOR));
		};
	}

	public static final IntProperty COLOR = IntProperty.of("color", 0, 5);

	/**
	 * This is used for replacing the block back when the wall disappears<br><br>
	 *
	 * I couldn't get the blockstate in getPlacementState(ItemPlacementContext) and save it to the block entity since
	 * the block entity is not yet there when the method gets called,
	 * so I had to hardcode it to the blocks that are in the current maps that can be
	 * replaced by the popup wall
	 */
	public static final IntProperty ORIGINAL_BLOCK = IntProperty.of("original_block", 0, 3);

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> stateManager) {
		stateManager.add(COLOR);
		stateManager.add(ORIGINAL_BLOCK);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState blockState = ctx.getWorld().getBlockState(ctx.getBlockPos());
		if (blockState == Blocks.LIGHT.getDefaultState().with(Properties.WATERLOGGED, true)) {
			return getDefaultState().with(ORIGINAL_BLOCK, 1);
		} else if (blockState == Blocks.FIRE.getDefaultState()) {
			return getDefaultState().with(ORIGINAL_BLOCK, 2);
		} else if (blockState == Blocks.SOUL_FIRE.getDefaultState()) {
			return getDefaultState().with(ORIGINAL_BLOCK, 3);
		} else {
			return getDefaultState().with(ORIGINAL_BLOCK, 0);
		}
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new PopupWallBlockEntity(pos, state);
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return checkType(type, CrossMaze.POPUP_WALL_BLOCK_ENTITY, PopupWallBlockEntity::tick);
	}
}