package io.github.pepe20129.crossmaze.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.plasmid.game.GameOpenException;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class CrossMazeMapGenerator {
	private final CrossMazeMapConfig config;

	public CrossMazeMapGenerator(CrossMazeMapConfig config) {
		this.config = config;
	}

	public CrossMazeMap build(MinecraftServer server) throws GameOpenException {
		try {
			MapTemplate template = MapTemplateSerializer.loadFromResource(server, config.id());
			CrossMazeMap map = new CrossMazeMap(template, config);

			List<Vec3d> playerSpawns = template.getMetadata().getRegionBounds("player_spawn").map(bounds -> {
				Vec3d spawn = bounds.center();
				spawn = spawn.subtract(0, (bounds.max().getY() - bounds.min().getY() + 1) / 2.0D, 0);
				return spawn;
			}).collect(Collectors.toList());

			if (playerSpawns.size() == 0) {
				throw new GameOpenException(new LiteralText("No player spawns defined."));
			}

			map.playerSpawns = playerSpawns;

			List<Vec3d> itemSpawns = template.getMetadata().getRegionBounds("item_spawn").map(bounds -> {
				Vec3d spawn = bounds.center();
				spawn = spawn.subtract(0, (bounds.max().getY() - bounds.min().getY() + 1) / 2.0D, 0);
				return spawn;
			}).collect(Collectors.toList());

			if (itemSpawns.size() == 0) {
				throw new GameOpenException(new LiteralText("No item spawns defined."));
			}

			map.itemSpawns = itemSpawns;

			return map;
		} catch (IOException e) {
			throw new GameOpenException(new LiteralText("Failed to load map"));
		}
	}
}