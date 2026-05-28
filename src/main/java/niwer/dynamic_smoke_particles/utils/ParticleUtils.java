package niwer.dynamic_smoke_particles.utils;

import java.util.List;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import niwer.dynamic_smoke_particles.CampfireSmokeParticleAccessor;

public final class ParticleUtils {

	private ParticleUtils() {}

	private static final List<VoxelShape> EMPTY_LIST = List.of();
	private static final double MAXIMUM_COLLISION_VELOCITY_SQUARED = 10000.0D;
	private static final double COLLISION_HORIZONTAL_BOOST = 1.02D;
	private static final double COLLISION_HORIZONTAL_PUSH = 0.01D;
	private static final double MAX_HORIZONTAL_SPEED = 0.05D;
	private static final double EPS_MOTION = 1.0E-5D;
	private static final double[][] ESCAPE_DIRECTIONS = {
		{ 1.0D, 0.0D },
		{ -1.0D, 0.0D },
		{ 0.0D, 1.0D },
		{ 0.0D, -1.0D },
		// { 0.7071067811865476D, 0.7071067811865476D },
		// { 0.7071067811865476D, -0.7071067811865476D },
		// { -0.7071067811865476D, 0.7071067811865476D },
		// { -0.7071067811865476D, -0.7071067811865476D }
	};
	private static final int ESCAPE_DIRECTION_STEPS = 4;
	private static final int ESCAPE_DIRECTION_VERTICAL_SAMPLES = 4;
	
	private static boolean moveHorizontally(CampfireSmokeParticleAccessor access, double xa, double za) {
		Vec3 movement = applyMovement(access, xa, 0.0D, za, false);
		return movement.x == xa && movement.z == za;
	}

	/**
	 * Applies a simple horizontal push to the particle in a random direction when vertical stalling is detected, without checking for a preferred escape direction or attempting to boost the horizontal speed.
	 * 
	 * @param access the particle accessor
	 * @param randomSource the random source to use for determining the push direction
	 */
	public static void applySimpleVerticalStallResponse(CampfireSmokeParticleAccessor access, RandomSource randomSource) {
		double pushX = COLLISION_HORIZONTAL_PUSH * direction(randomSource, access.xd());
		double pushZ = COLLISION_HORIZONTAL_PUSH * direction(randomSource, access.zd());
		applyHorizontalImpulse(access, pushX, pushZ);
		moveHorizontally(access, pushX, pushZ);
	}

	/**
	 * Applies a more complex response to vertical stalling by first determining a preferred escape direction based on the surrounding environment, then applying a horizontal boost in that direction if it is available, or a random push if not, while also ensuring that the horizontal speed does not exceed the defined maximum.
	 * 
	 * @param access the particle accessor
	 * @param randomSource the random source to use for determining the push direction if no preferred escape direction is available
	 */
	public static void applyComplexVerticalStallResponse(CampfireSmokeParticleAccessor access, RandomSource randomSource) {
		ensurePreferredEscapeDirection(access);

		double escapeX = access.isPreferredEscapeDirectionReady() ? access.getPreferredEscapeDirectionX() : direction(randomSource, access.xd());
		double escapeZ = access.isPreferredEscapeDirectionReady() ? access.getPreferredEscapeDirectionZ() : direction(randomSource, access.zd());
		double pushX = escapeX * COLLISION_HORIZONTAL_PUSH;
		double pushZ = escapeZ * COLLISION_HORIZONTAL_PUSH;

		if (access.isPreferredEscapeDirectionReady()) {
			final double horizontalSpeed = Math.max(MathUtils.horizontalSpeed(access.xd(), access.zd()), COLLISION_HORIZONTAL_PUSH);
			access.setXd(escapeX * horizontalSpeed * COLLISION_HORIZONTAL_BOOST);
			access.setZd(escapeZ * horizontalSpeed * COLLISION_HORIZONTAL_BOOST);
		} else applyHorizontalImpulse(access, pushX, pushZ);

		clampHorizontalSpeed(access);

		if (!moveHorizontally(access, pushX, pushZ)) access.setPreferredEscapeDirectionReady(false);
	}

	private static void ensurePreferredEscapeDirection(CampfireSmokeParticleAccessor access) {
		if (access.isPreferredEscapeDirectionReady()) return;

		double bestScore = Double.NEGATIVE_INFINITY;
		double bestX = 0.0D;
		double bestZ = 0.0D;

		for (double[] direction : ESCAPE_DIRECTIONS) {
			double candidateX = direction[0];
			double candidateZ = direction[1];
			double score = scoreEscapeDirection(access, candidateX, candidateZ);

			if (score > bestScore) {
				bestScore = score;
				bestX = candidateX;
				bestZ = candidateZ;
			}
		}

		if (bestScore > Double.NEGATIVE_INFINITY) {
			access.setPreferredEscapeDirectionX(bestX);
			access.setPreferredEscapeDirectionZ(bestZ);
			access.setPreferredEscapeDirectionReady(true);
		}
	}

	private static double scoreEscapeDirection(CampfireSmokeParticleAccessor access, double directionX, double directionZ) {
		double score = 0.0D;
		BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
		int baseY = (int) Math.floor(access.y());
		var level = access.level();
		double originX = access.x();
		double originZ = access.z();

		for (int step = 1; step <= ESCAPE_DIRECTION_STEPS; step++) {
			int sampleX = (int) Math.floor(originX + directionX * step);
			int sampleZ = (int) Math.floor(originZ + directionZ * step);

			for (int vertical = 0; vertical < ESCAPE_DIRECTION_VERTICAL_SAMPLES; vertical++) {
				checkPos.set(sampleX, baseY + vertical, sampleZ);
				double blockScore = scoreEscapeBlock(level.getBlockState(checkPos), level, checkPos, vertical);
				if (blockScore == Double.NEGATIVE_INFINITY) return score - 3.0D;
				score += blockScore;
			}

			score += 0.25D;
		}

		return score;
	}

	/**
	 * Applies movement to the particle based on the given motion components, while also checking for collisions and optionally stopping vertical movement if a collision is detected in that direction, and returns the actual movement applied after collision resolution.
	 * 
	 * @param access the particle accessor
	 * @param xa the requested movement in the x direction
	 * @param ya the requested movement in the y direction
	 * @param za the requested movement in the z direction
	 * @param stopOnVerticalCollision whether to stop vertical movement if a collision is detected in that direction
	 * @return the actual movement applied after collision resolution
	 */
	public static Vec3 applyMovement(CampfireSmokeParticleAccessor access, double xa, double ya, double za, boolean stopOnVerticalCollision) {
		if (stopOnVerticalCollision && access.isStoppedByCollision()) return new Vec3(xa, ya, za);

		Vec3 movement = collide(access, xa, ya, za);
		if (movement.x != 0.0D || movement.y != 0.0D || movement.z != 0.0D) {
			access.setAABB(access.getAABB().move(movement.x, movement.y, movement.z));
			access.setLocationFromAABB();
		}

		if (stopOnVerticalCollision && !MathUtils.isWithinEpsilon(ya, movement.y, EPS_MOTION)) access.setStoppedByCollision(true);
		if (xa != movement.x) access.setXd(0.0D);
		if (za != movement.z) access.setZd(0.0D);
		return movement;
	}

	private static Vec3 collide(CampfireSmokeParticleAccessor access, double xa, double ya, double za) {
		if (!access.hasPhysics() || (xa == 0.0D && ya == 0.0D && za == 0.0D) || MathUtils.lengthSquared(xa, ya, za) >= MAXIMUM_COLLISION_VELOCITY_SQUARED) return new Vec3(xa, ya, za);
		return Entity.collideBoundingBox(null, new Vec3(xa, ya, za), access.getAABB(), access.level(), EMPTY_LIST);
	}

	private static void applyHorizontalImpulse(CampfireSmokeParticleAccessor access, double pushX, double pushZ) {
		access.setXd(access.xd() * COLLISION_HORIZONTAL_BOOST + pushX);
		access.setZd(access.zd() * COLLISION_HORIZONTAL_BOOST + pushZ);
		clampHorizontalSpeed(access);
	}

	private static void clampHorizontalSpeed(CampfireSmokeParticleAccessor access) {
		double scale = MathUtils.clampScale(access.xd(), access.zd(), MAX_HORIZONTAL_SPEED);
		if (scale != 1.0D) {
			access.setXd(access.xd() * scale);
			access.setZd(access.zd() * scale);
		}
	}

	private static double direction(RandomSource randomSource, double velocity) { return velocity == 0.0D ? (randomSource.nextBoolean() ? 1.0D : -1.0D) : Math.copySign(1.0D, velocity); }

	private static double scoreEscapeBlock(BlockState blockState, ClientLevel level, BlockPos.MutableBlockPos checkPos, int vertical) {
		if (blockState.getBlock() instanceof TrapDoorBlock) return 3.0D;
		if (blockState.getBlock() instanceof SlabBlock) return 2.5D;
		if (blockState.getBlock() instanceof StairBlock) return 2.0D;
		if (blockState.getCollisionShape(level, checkPos).isEmpty()) return vertical == 0 ? 1.5D : 1.0D;
		if (!blockState.isCollisionShapeFullBlock(level, checkPos)) return vertical == 0 ? 1.0D : 0.5D;
		return Double.NEGATIVE_INFINITY;
	}
}