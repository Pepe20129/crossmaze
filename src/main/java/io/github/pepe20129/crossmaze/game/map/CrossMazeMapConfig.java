package io.github.pepe20129.crossmaze.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;

public record CrossMazeMapConfig(Identifier id) {
	public static final Codec<CrossMazeMapConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Identifier.CODEC.fieldOf("id").forGetter(CrossMazeMapConfig::id)
	).apply(instance, CrossMazeMapConfig::new));
}