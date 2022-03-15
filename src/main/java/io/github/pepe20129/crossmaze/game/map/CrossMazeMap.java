package io.github.pepe20129.crossmaze.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;

import java.util.ArrayList;
import java.util.List;

public class CrossMazeMap {
	private final MapTemplate template;
	private final CrossMazeMapConfig config;
	public List<Vec3d> playerSpawns = new ArrayList<>();
	public List<Vec3d> itemSpawns = new ArrayList<>();

	public CrossMazeMap(MapTemplate template, CrossMazeMapConfig config) {
		this.template = template;
		this.config = config;
	}

	public ChunkGenerator asGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, template);
	}
}