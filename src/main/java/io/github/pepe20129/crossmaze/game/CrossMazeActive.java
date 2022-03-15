package io.github.pepe20129.crossmaze.game;

import io.github.pepe20129.crossmaze.game.custom.items.CrossMazeItemStacks;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.EntityDamageSource;
import net.minecraft.entity.damage.ProjectileDamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerSet;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.util.PlayerRef;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import io.github.pepe20129.crossmaze.game.map.CrossMazeMap;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

import java.util.Set;
import java.util.stream.Collectors;

public class CrossMazeActive {
	private final CrossMazeConfig config;

	public final GameSpace gameSpace;
	private final CrossMazeMap gameMap;

	private final Object2ObjectMap<PlayerRef, CrossMazePlayer> participants;
	public final Set<PlayerRef> liveParticipants;
	private final CrossMazeSpawnLogic spawnLogic;
	private final CrossMazeStageManager stageManager;
	private final boolean ignoreWinState;
	private final CrossMazeTimerBar timerBar;
	private final ServerWorld world;

	private int ticksUntilNextItem;

	private CrossMazeActive(GameSpace gameSpace, ServerWorld world, CrossMazeMap map, GlobalWidgets widgets, CrossMazeConfig config, Set<PlayerRef> participants) {
		this.gameSpace = gameSpace;
		this.config = config;
		gameMap = map;
		spawnLogic = new CrossMazeSpawnLogic(world, map);
		this.participants = new Object2ObjectOpenHashMap<>();
		this.world = world;

		for (PlayerRef player : participants) {
			this.participants.put(player, new CrossMazePlayer());
		}

		liveParticipants = participants;
		stageManager = new CrossMazeStageManager(this);
		ignoreWinState = this.participants.size() <= 1;
		timerBar = new CrossMazeTimerBar(widgets);

		ticksUntilNextItem = 20 * config.itemRate();
	}

	public static void open(GameSpace gameSpace, ServerWorld world, CrossMazeMap map, CrossMazeConfig config) {
		gameSpace.setActivity(game -> {
			Set<PlayerRef> participants = gameSpace.getPlayers().stream()
					.map(PlayerRef::of)
					.collect(Collectors.toSet());
			GlobalWidgets widgets = GlobalWidgets.addTo(game);
			CrossMazeActive active = new CrossMazeActive(gameSpace, world, map, widgets, config, participants);

			game.setRule(GameRuleType.CRAFTING, ActionResult.FAIL);
			game.setRule(GameRuleType.PORTALS, ActionResult.FAIL);
			game.setRule(GameRuleType.PVP, ActionResult.SUCCESS);
			game.setRule(GameRuleType.HUNGER, ActionResult.FAIL);
			game.setRule(GameRuleType.FALL_DAMAGE, ActionResult.FAIL);
			game.setRule(GameRuleType.BLOCK_DROPS, ActionResult.FAIL);
			game.setRule(GameRuleType.BREAK_BLOCKS, ActionResult.FAIL);
			game.setRule(GameRuleType.THROW_ITEMS, ActionResult.FAIL);
			game.setRule(GameRuleType.UNSTABLE_TNT, ActionResult.FAIL);
			game.setRule(GameRuleType.PLAYER_PROJECTILE_KNOCKBACK, ActionResult.SUCCESS);
			game.setRule(GameRuleType.PICKUP_ITEMS, ActionResult.SUCCESS);
			game.setRule(GameRuleType.THROW_ITEMS, ActionResult.SUCCESS);
			game.setRule(GameRuleType.MODIFY_INVENTORY, ActionResult.SUCCESS);
			game.setRule(GameRuleType.MODIFY_ARMOR, ActionResult.SUCCESS);

			game.listen(GameActivityEvents.ENABLE, active::onOpen);
			game.listen(GameActivityEvents.DISABLE, active::onClose);
			game.listen(GameActivityEvents.TICK, active::tick);

			game.setRule(GameRuleType.INTERACTION, ActionResult.SUCCESS);
			game.setRule(GameRuleType.USE_ITEMS, ActionResult.SUCCESS);

			game.listen(GamePlayerEvents.OFFER, (offer) -> offer.accept(world, Vec3d.ZERO));
			game.listen(GamePlayerEvents.ADD, active::addPlayer);
			game.listen(GamePlayerEvents.REMOVE, active::removePlayer);

			game.listen(PlayerDamageEvent.EVENT, active::onPlayerDamage);
			game.listen(PlayerDeathEvent.EVENT, active::onPlayerDeath);
		});
	}

	private void onOpen() {
		for (PlayerRef ref : participants.keySet()) {
			ref.ifOnline(world, this::spawnParticipant);
		}
		stageManager.onOpen(world.getTime(), config);
	}

	private void onClose() {}

	private void addPlayer(ServerPlayerEntity player) {
		if (!participants.containsKey(PlayerRef.of(player))) {
			spawnSpectator(player);
		}
	}

	public ServerPlayerEntity getPlayer(PlayerRef ref) {
		return ref.getEntity(world);
	}

	private void removePlayer(ServerPlayerEntity player) {
		participants.remove(PlayerRef.of(player));
	}

	private ActionResult onPlayerDamage(ServerPlayerEntity player, DamageSource source, float amount) {
		if (source instanceof EntityDamageSource &&
				source.getAttacker() instanceof PlayerEntity &&
				!(((PlayerEntity)source.getAttacker()).getInventory().getMainHandStack().isOf(Items.STICK) ||
						((PlayerEntity)source.getAttacker()).getInventory().getMainHandStack().isOf(Items.NETHERITE_SWORD)) &&
				!(source instanceof ProjectileDamageSource))
			return ActionResult.FAIL;
		return ActionResult.PASS;
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		gameSpace.getPlayers().sendMessage(getDeathMessage(player, source));
		player.getInventory().dropAll();
		spawnSpectator(player);
		eliminatePlayer(PlayerRef.of(player));
		return ActionResult.FAIL;
	}

	private void eliminatePlayer(PlayerRef player) {
		liveParticipants.remove(player);
	}

	private Text getDeathMessage(ServerPlayerEntity player, DamageSource source) {
		Text deathMessage = source.getDeathMessage(player);
		deathMessage = new LiteralText("")
				.styled(style -> Style.EMPTY.withColor(TextColor.fromRgb(0x858585)))
				.append(deathMessage)
				.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xbfbfbf)));
        /*
        ServerPlayerEntity attacker = null;

        if (source.getAttacker() != null) {
            if (source.getAttacker() instanceof ServerPlayerEntity adversary) {
                attacker = adversary;
            }
        } else if (player.getPrimeAdversary() != null && player.getPrimeAdversary() instanceof ServerPlayerEntity adversary) {
            attacker = adversary;
        }
        */

		return deathMessage;
	}

	private void spawnParticipant(ServerPlayerEntity player) {
		spawnLogic.resetPlayer(player, GameMode.SURVIVAL);
		spawnLogic.spawnPlayer(player);

		player.addStatusEffect(new StatusEffectInstance(
				StatusEffects.RESISTANCE,
				20 * 30,
				4,
				true,
				false
		));

		player.getInventory().setStack(1, CrossMazeItemStacks.INFINIBOW.copy());

		player.getInventory().setStack(2, CrossMazeItemStacks.HEALING_POTION.copy());
		player.getInventory().setStack(3, CrossMazeItemStacks.HEALING_POTION.copy());
		player.getInventory().setStack(4, CrossMazeItemStacks.HEALING_POTION.copy());
	}

	private void spawnSpectator(ServerPlayerEntity player) {
		spawnLogic.resetPlayer(player, GameMode.SPECTATOR);
		spawnLogic.spawnPlayer(player);
	}

	private void tick() {
		long time = world.getTime();

		CrossMazeStageManager.IdleTickResult result = stageManager.tick(time, gameSpace);

		switch (result) {
			case CONTINUE_TICK:
				break;
			case TICK_FINISHED:
				return;
			case GAME_FINISHED:
				broadcastWin(checkWinResult());
				return;
			case GAME_CLOSED:
				gameSpace.close(GameCloseReason.FINISHED);
				return;
		}

		for (PlayerRef ref : liveParticipants) {
			ServerPlayerEntity player = ref.getEntity(world);
			ItemStack itemStack = player.getInventory().getStack(8);
			if (!itemStack.isOf(Items.ARROW)) {
				player.getInventory().remove(stack -> stack.isOf(Items.ARROW), 2147483647, player.playerScreenHandler.getCraftingInput());
				player.getInventory().setStack(8, CrossMazeItemStacks.INFIARROW.copy());
				player.giveItemStack(itemStack);
			}
		}

		timerBar.update(ticksUntilNextItem, 20 * config.itemRate());

		ticksUntilNextItem -= 1;
		if (ticksUntilNextItem == 0) {
			ticksUntilNextItem = 20 * config.itemRate();
			spawnItem();
		}
	}

	private void spawnItem() {
		Vec3d pos = gameMap.itemSpawns.get(world.random.nextInt(gameMap.itemSpawns.size()));

		ItemEntity itemEntity = new ItemEntity(world, pos.x, pos.y, pos.z, getRandomItem());
		itemEntity.setGlowing(true);
		itemEntity.setVelocity(Vec3d.ZERO);

		world.spawnEntity(itemEntity);
	}

	private ItemStack getRandomItem() {
		return getRandomItem(getRandomItemRarity());
	}

	private ItemStack getRandomItem(ItemRarity itemRarity) {
		if (itemRarity == ItemRarity.COMMON) {
			return switch (world.random.nextInt(6)) {
				case 0 -> CrossMazeItemStacks.HEALING_POTION.copy();
				case 1 -> CrossMazeItemStacks.QUICKBOW.copy();
				case 2 -> CrossMazeItemStacks.SPECTRAL_ARROW.copy();
				case 3 -> CrossMazeItemStacks.QUICKBOOTS.copy();
				case 4 -> CrossMazeItemStacks.GOLDEN_APPLE.copy();
				case 5 -> CrossMazeItemStacks.SHIELD.copy();

				//just to be able to compile
				default -> ItemStack.EMPTY;
			};
		}

		if (itemRarity == ItemRarity.UNCOMMON) {
			return switch (world.random.nextInt(8)) {
				case 0 -> CrossMazeItemStacks.STRONG_HEALING_POTION.copy();
				case 1 -> CrossMazeItemStacks.SUPER_QUICKBOW.copy();
				case 2 -> CrossMazeItemStacks.CHAINMAIL_HELMET.copy();
				case 3 -> CrossMazeItemStacks.SUPER_QUICKBOOTS.copy();
				case 4 -> CrossMazeItemStacks.HEALTHY_STEW.copy();
				case 5 -> CrossMazeItemStacks.POISON_ARROW.copy();
				case 6 -> CrossMazeItemStacks.SEWAGE.copy();
				case 7 -> CrossMazeItemStacks.POPUP_WALL.copy();

				//just to be able to compile
				default -> ItemStack.EMPTY;
			};
		}

		if (itemRarity == ItemRarity.RARE) {
			return switch (world.random.nextInt(9)) {
				case 0 -> CrossMazeItemStacks.EXPLOSIVE_ROCKET.copy();
				case 1 -> CrossMazeItemStacks.SUPER_STICK.copy();
				case 2 -> CrossMazeItemStacks.REGENERATION_POTION.copy();
				case 3 -> CrossMazeItemStacks.CHAINMAIL_LEGGINGS.copy();
				case 4 -> CrossMazeItemStacks.SUPER_HEALTHY_STEW.copy();
				case 5 -> CrossMazeItemStacks.HYPER_QUICKBOOTS.copy();
				case 6 -> CrossMazeItemStacks.IRON_HELMET.copy();
				case 7 -> CrossMazeItemStacks.HYPER_QUICKBOW.copy();
				case 8 -> CrossMazeItemStacks.SLOWSON_ARROW.copy();

				//just to be able to compile
				default -> ItemStack.EMPTY;
			};
		}

		if (itemRarity == ItemRarity.EPIC) {
			return switch (world.random.nextInt(7)) {
				case 0 -> CrossMazeItemStacks.SUPER_SWORD.copy();
				case 1 -> CrossMazeItemStacks.DESTRUCTION_ARROW.copy();
				case 2 -> CrossMazeItemStacks.FULL_HEAL.copy();
				case 3 -> CrossMazeItemStacks.CHAINMAIL_CHESTPLATE.copy();
				case 4 -> CrossMazeItemStacks.ENCHANTED_GOLDEN_APPLE.copy();
				case 5 -> CrossMazeItemStacks.IRON_LEGGINGS.copy();
				case 6 -> CrossMazeItemStacks.NUKE.copy();

				//just to be able to compile
				default -> ItemStack.EMPTY;
			};
		}

		return switch (world.random.nextInt(3)) {
			case 0 -> CrossMazeItemStacks.IRON_CHESTPLATE.copy();
			case 1 -> CrossMazeItemStacks.INVINCIBILITY.copy();
			case 2 -> CrossMazeItemStacks.DEATH_ARROW.copy();

			//just to be able to compile
			default -> ItemStack.EMPTY;
		};
	}

	private ItemRarity getRandomItemRarity() {
		int r = world.random.nextInt(53);
		if (r < 28) {
			return ItemRarity.COMMON;
		}
		if (r < 42) {
			return ItemRarity.UNCOMMON;
		}
		if (r < 49) {
			return ItemRarity.RARE;
		}
		if (r < 52) {
			return ItemRarity.EPIC;
		}
		return ItemRarity.LEGENDARY;
	}

	enum ItemRarity {
		COMMON,
		UNCOMMON,
		RARE,
		EPIC,
		LEGENDARY
	}

	private void broadcastWin(WinResult result) {
		ServerPlayerEntity winningPlayer = result.getWinningPlayer();

		Text message;
		if (winningPlayer != null) {
			message = winningPlayer.getDisplayName().shallowCopy()
					.append(new TranslatableText("crossmaze.win.one")).formatted(Formatting.GOLD);
		} else {
			message = new TranslatableText("crossmaze.win.no").formatted(Formatting.GOLD);
		}

		PlayerSet players = this.gameSpace.getPlayers();
		players.sendMessage(message);
		players.playSound(SoundEvents.ENTITY_VILLAGER_YES);
	}

	public WinResult checkWinResult() {
		// for testing purposes: don't end the game if we only ever had one participant
		if (ignoreWinState) {
			return WinResult.no();
		}

		if (liveParticipants.size() == 0) {
			return WinResult.win(null);
		}

		if (liveParticipants.size() == 1) {
			return WinResult.win(((PlayerRef)liveParticipants.toArray()[0]).getEntity(world));
		}

		return WinResult.no();
	}

	static class WinResult {
		final ServerPlayerEntity winningPlayer;
		final boolean win;

		private WinResult(ServerPlayerEntity winningPlayer, boolean win) {
			this.winningPlayer = winningPlayer;
			this.win = win;
		}

		static WinResult no() {
			return new WinResult(null, false);
		}

		static WinResult win(ServerPlayerEntity player) {
			return new WinResult(player, true);
		}

		public boolean isWin() {
			return win;
		}

		public ServerPlayerEntity getWinningPlayer() {
			return winningPlayer;
		}
	}
}