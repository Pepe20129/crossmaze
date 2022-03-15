package io.github.pepe20129.crossmaze.game;

import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;
import io.github.pepe20129.crossmaze.game.map.CrossMazeMap;

import java.util.List;

public class CrossMazeSpawnLogic {
	private final ServerWorld world;
	private final List<Vec3d> unusedSpawns;

	public CrossMazeSpawnLogic(ServerWorld world, CrossMazeMap map) {
		this.world = world;
		unusedSpawns = map.playerSpawns;
	}

	public void resetPlayer(ServerPlayerEntity player, GameMode gameMode) {
		player.changeGameMode(gameMode);
		player.setVelocity(Vec3d.ZERO);
		player.fallDistance = 0f;
		player.setHealth(20f);
		player.addStatusEffect(new StatusEffectInstance(
				StatusEffects.SATURATION,
				20 * 60 * 60,
				49,
				true,
				false
		));

		player.addStatusEffect(new StatusEffectInstance(
				StatusEffects.SPEED,
				20 * 60 * 60,
				9,
				true,
				false
		));

		//TODO change the speed effect for an attribute change
		//doesn't work for some reason
		//player.setMovementSpeed(player.getMovementSpeed() * 2.5F);

		player.addStatusEffect(new StatusEffectInstance(
				StatusEffects.RESISTANCE,
				20 * 60 * 60,
				1,
				true,
				false
		));
	}

	public void spawnPlayer(ServerPlayerEntity player) {
		Vec3d pos = unusedSpawns.get(world.random.nextInt(unusedSpawns.size()));
		unusedSpawns.remove(pos);
		player.teleport(world, pos.getX(), pos.getY(), pos.getZ(), 0.0F, 0.0F);
		player.setOnGround(true);
	}
}