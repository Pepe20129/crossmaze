package io.github.pepe20129.crossmaze.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.pepe20129.crossmaze.game.map.CrossMazeMapConfig;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;

public record CrossMazeConfig(
		int itemRate,
		CrossMazeMapConfig mapConfig,
		PlayerConfig playerConfig
) {
	public static final Codec<CrossMazeConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.optionalFieldOf("item_rate", 30).forGetter(CrossMazeConfig::itemRate),
		CrossMazeMapConfig.CODEC.fieldOf("map").forGetter(config -> config.mapConfig),
		PlayerConfig.CODEC.fieldOf("players").forGetter(config -> config.playerConfig)
	).apply(instance, CrossMazeConfig::new));
}