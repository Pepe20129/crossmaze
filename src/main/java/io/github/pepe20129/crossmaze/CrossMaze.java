package io.github.pepe20129.crossmaze;

import eu.pb4.polymer.api.item.PolymerBlockItem;
import eu.pb4.polymer.api.item.SimplePolymerItem;
import io.github.pepe20129.crossmaze.game.custom.blocks.PopupWallBlock;
import io.github.pepe20129.crossmaze.game.custom.blocks.PopupWallBlockEntity;
import io.github.pepe20129.crossmaze.game.custom.items.CrossMazeItemStacks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.util.registry.Registry;
import xyz.nucleoid.plasmid.game.GameType;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.github.pepe20129.crossmaze.game.CrossMazeConfig;
import io.github.pepe20129.crossmaze.game.CrossMazeWaiting;

public class CrossMaze implements ModInitializer {
	public static final String ID = "crossmaze";
	public static final Logger LOGGER = LogManager.getLogger(ID);

	public static final GameType<CrossMazeConfig> TYPE = GameType.register(
			new Identifier(ID, "normal"),
			CrossMazeConfig.CODEC,
			CrossMazeWaiting::open
	);

	public static final Block POPUP_WALL_BLOCK = new PopupWallBlock();
	public static BlockEntityType<PopupWallBlockEntity> POPUP_WALL_BLOCK_ENTITY;
	public static final BlockItem POPUP_WALL_BLOCK_ITEM = new PolymerBlockItem(POPUP_WALL_BLOCK, new Item.Settings().maxCount(64), Items.GLASS);

	public static final Item HEALTHY_STEW_ITEM = new SimplePolymerItem(new Item.Settings().food(new FoodComponent.Builder().alwaysEdible().statusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 20 * 60 * 2, 1, true, false), 1).statusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 10, 1, true, false), 1).build()).maxCount(1), Items.SUSPICIOUS_STEW);

	public static final Item SUPER_HEALTHY_STEW_ITEM = new SimplePolymerItem(new Item.Settings().food(new FoodComponent.Builder().alwaysEdible().statusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 20 * 60 * 2, 2, true, false), 1).statusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 15, 1, true, false), 1).build()).maxCount(1), Items.SUSPICIOUS_STEW);

	public static final ItemGroup ITEM_GROUP = FabricItemGroupBuilder.create(
			new Identifier(ID, ID)
		)
		.icon(CrossMazeItemStacks.INFINIBOW::copy)
		.appendItems(stacks -> {
			stacks.add(CrossMazeItemStacks.INFINIBOW.copy());
			stacks.add(CrossMazeItemStacks.INFIARROW.copy());

			stacks.add(CrossMazeItemStacks.HEALING_POTION.copy());
			stacks.add(CrossMazeItemStacks.QUICKBOW.copy());
			stacks.add(CrossMazeItemStacks.SPECTRAL_ARROW.copy());
			stacks.add(CrossMazeItemStacks.QUICKBOOTS.copy());
			stacks.add(CrossMazeItemStacks.GOLDEN_APPLE.copy());
			stacks.add(CrossMazeItemStacks.SHIELD.copy());

			stacks.add(CrossMazeItemStacks.STRONG_HEALING_POTION.copy());
			stacks.add(CrossMazeItemStacks.SUPER_QUICKBOW.copy());
			stacks.add(CrossMazeItemStacks.CHAINMAIL_HELMET.copy());
			stacks.add(CrossMazeItemStacks.SUPER_QUICKBOOTS.copy());
			stacks.add(CrossMazeItemStacks.HEALTHY_STEW.copy());
			stacks.add(CrossMazeItemStacks.POISON_ARROW.copy());
			stacks.add(CrossMazeItemStacks.SEWAGE.copy());
			stacks.add(CrossMazeItemStacks.POPUP_WALL.copy());

			stacks.add(CrossMazeItemStacks.EXPLOSIVE_ROCKET.copy());
			stacks.add(CrossMazeItemStacks.SUPER_STICK.copy());
			stacks.add(CrossMazeItemStacks.REGENERATION_POTION.copy());
			stacks.add(CrossMazeItemStacks.CHAINMAIL_LEGGINGS.copy());
			stacks.add(CrossMazeItemStacks.SUPER_HEALTHY_STEW.copy());
			stacks.add(CrossMazeItemStacks.HYPER_QUICKBOOTS.copy());
			stacks.add(CrossMazeItemStacks.IRON_HELMET.copy());
			stacks.add(CrossMazeItemStacks.HYPER_QUICKBOW.copy());
			stacks.add(CrossMazeItemStacks.SLOWSON_ARROW.copy());

			stacks.add(CrossMazeItemStacks.SUPER_SWORD.copy());
			stacks.add(CrossMazeItemStacks.DESTRUCTION_ARROW.copy());
			stacks.add(CrossMazeItemStacks.FULL_HEAL.copy());
			stacks.add(CrossMazeItemStacks.CHAINMAIL_CHESTPLATE.copy());
			stacks.add(CrossMazeItemStacks.ENCHANTED_GOLDEN_APPLE.copy());
			stacks.add(CrossMazeItemStacks.IRON_LEGGINGS.copy());
			stacks.add(CrossMazeItemStacks.NUKE.copy());

			stacks.add(CrossMazeItemStacks.IRON_CHESTPLATE.copy());
			stacks.add(CrossMazeItemStacks.INVINCIBILITY.copy());
			stacks.add(CrossMazeItemStacks.DEATH_ARROW.copy());
		})
		.build();

	@Override
	public void onInitialize() {
		Registry.register(Registry.BLOCK, new Identifier(ID, "popup_wall"), POPUP_WALL_BLOCK);
		Registry.register(Registry.ITEM, new Identifier(ID, "popup_wall"), POPUP_WALL_BLOCK_ITEM);
		Registry.register(Registry.ITEM, new Identifier(ID, "healthy_stew"), HEALTHY_STEW_ITEM);
		Registry.register(Registry.ITEM, new Identifier(ID, "super_healthy_stew"), SUPER_HEALTHY_STEW_ITEM);
		POPUP_WALL_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, new Identifier(ID, "popup_wall_block_entity"), FabricBlockEntityTypeBuilder.create(PopupWallBlockEntity::new, POPUP_WALL_BLOCK).build(null));
	}
}