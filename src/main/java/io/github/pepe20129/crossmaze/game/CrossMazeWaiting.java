package io.github.pepe20129.crossmaze.game;

import io.github.pepe20129.crossmaze.game.map.CrossMazeMap;
import io.github.pepe20129.crossmaze.game.map.CrossMazeMapGenerator;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameResult;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;

public class CrossMazeWaiting {
	private final CrossMazeConfig config;
	private final GameSpace gameSpace;
	private final ServerWorld world;
	private final CrossMazeMap map;

	private CrossMazeWaiting(GameSpace gameSpace, ServerWorld world, CrossMazeMap map, CrossMazeConfig config) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.map = map;
		this.config = config;
	}

	private GameResult requestStart() {
		CrossMazeActive.open(gameSpace, world, map, config);
		return GameResult.ok();
	}

	public static GameOpenProcedure open(GameOpenContext<CrossMazeConfig> context) {
		// get our config that got loaded by Plasmid
		CrossMazeConfig config = context.config();

		// create a chunk generator that generate the map
		CrossMazeMapGenerator generator = new CrossMazeMapGenerator(config.mapConfig());
		CrossMazeMap map = generator.build(context.server());

		// set up how the world that this minigame will take place in should be constructed
		RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
				.setGenerator(map.asGenerator(context.server()))
				.setTimeOfDay(6000)
				.setGameRule(GameRules.DO_FIRE_TICK, false)
				.setGameRule(GameRules.NATURAL_REGENERATION, false);

		return context.openWithWorld(worldConfig, (game, world) -> {
			CrossMazeWaiting waiting = new CrossMazeWaiting(game.getGameSpace(), world, map, config);

			game.listen(GamePlayerEvents.OFFER, waiting::onPlayerOffer);
			game.listen(GamePlayerEvents.ADD, waiting::onPlayerAdd);
			game.listen(GamePlayerEvents.LEAVE, waiting::onPlayerLeave);
			game.listen(GameActivityEvents.REQUEST_START, waiting::requestStart);

			game.listen(GameActivityEvents.ENABLE, waiting::onEnable);
		});
	}

	private void onEnable() {

	}

	private void onPlayerLeave(ServerPlayerEntity player) {
		TranslatableText message = new TranslatableText("crossmaze.game.join", player.getDisplayName().shallowCopy());
		gameSpace.getPlayers().sendMessage(message);
	}

	private PlayerOfferResult onPlayerOffer(PlayerOffer offer) {
		//get the player
		ServerPlayerEntity player = offer.player();
		//accept it in world at position 8 75 10
		return offer.accept(this.world, new Vec3d(8.5, 75.0, 10.5))
			.and(() -> {
				//and set it to adventure
				player.changeGameMode(GameMode.ADVENTURE);
			});
	}

	private void onPlayerAdd(ServerPlayerEntity player) {
		TranslatableText message = new TranslatableText("crossmaze.game.leave", player.getDisplayName().shallowCopy());
		this.gameSpace.getPlayers().sendMessage(message);
	}
}