package io.github.pepe20129.crossmaze.game;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.sound.SoundCategory;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.player.PlayerSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket.Flag;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.Set;

public class CrossMazeStageManager {
	private final CrossMazeActive game;

	private long closeTime = -1;
	private long startTime = -1;
	private final Object2ObjectMap<ServerPlayerEntity, FrozenPlayer> frozen;
	private boolean setSpectator = false;

	public CrossMazeStageManager(CrossMazeActive game) {
		this.game = game;
		frozen = new Object2ObjectOpenHashMap<>();
	}

	public void onOpen(long time, CrossMazeConfig config) {
		startTime = time - (time % 20) + (4 * 20) + 19;
	}

	public IdleTickResult tick(long time, GameSpace space) {
		if (space.getPlayers().isEmpty()) {
			return IdleTickResult.GAME_CLOSED;
		}

		// Game has finished. Wait a few seconds before finally closing the game.
		if (closeTime > 0) {
			if (time >= closeTime) {
				return IdleTickResult.GAME_CLOSED;
			}
			return IdleTickResult.TICK_FINISHED;
		}

		// Game hasn't started yet. Display a countdown before it begins.
		if (startTime > time) {
			tickStartWaiting(time, space);
			return IdleTickResult.TICK_FINISHED;
		}

		//Only one player remaining. Game finished
		if (game.checkWinResult().isWin()) {
			closeTime = time + (5 * 20);

			return IdleTickResult.GAME_FINISHED;
		}

		// Game has just finished. Transition to the waiting-before-close state.
		if (space.getPlayers().isEmpty()) {
			if (!setSpectator) {
				setSpectator = true;
				for (ServerPlayerEntity player : space.getPlayers()) {
					player.changeGameMode(GameMode.SPECTATOR);
				}
			}

			closeTime = time + (5 * 20);

			return IdleTickResult.GAME_FINISHED;
		}

		return IdleTickResult.CONTINUE_TICK;
	}

	private void tickStartWaiting(long time, GameSpace space) {
		float sec_f = (startTime - time) / 20.0f;

		if (sec_f > 1) {
			for (ServerPlayerEntity player : space.getPlayers()) {
				if (player.isSpectator()) {
					continue;
				}

				FrozenPlayer state = frozen.computeIfAbsent(player, p -> new FrozenPlayer());

				if (state.lastPos == null) {
					state.lastPos = player.getPos();
				}

				double destX = state.lastPos.x;
				double destY = state.lastPos.y;
				double destZ = state.lastPos.z;

				// Set X and Y as relative so it will send 0 change when we pass yaw (yaw - yaw = 0) and pitch
				Set<Flag> flags = ImmutableSet.of(Flag.X_ROT, Flag.Y_ROT);

				// Teleport without changing the pitch and yaw
				player.networkHandler.requestTeleport(destX, destY, destZ, player.getYaw(), player.getPitch(), flags);
			}
		}

		int sec = (int)Math.floor(sec_f) - 1;

		if ((startTime - time) % 20 == 0) {
			PlayerSet players = space.getPlayers();

			if (sec > 0) {
				players.showTitle(new LiteralText(Integer.toString(sec)).formatted(Formatting.BOLD), 20);
				players.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1.0F, 1.0F);
			} else {
				players.showTitle(new LiteralText("Go!").formatted(Formatting.BOLD), 20);
				players.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1.0F, 2.0F);

				for (var playerRef : game.liveParticipants) {
					game.getPlayer(playerRef).changeGameMode(GameMode.SURVIVAL);
					//for the invincibility at the game start
					game.getPlayer(playerRef).addStatusEffect(new StatusEffectInstance(
							StatusEffects.RESISTANCE,
							20 * 30,
							4,
							true,
							false
					));

					game.getPlayer(playerRef).setHealth(20f);
				}
			}
		}
	}

	public static class FrozenPlayer {
		public Vec3d lastPos;
	}

	public enum IdleTickResult {
		CONTINUE_TICK,
		TICK_FINISHED,
		GAME_FINISHED,
		GAME_CLOSED
	}
}