package niwer.dynamic_smoke_particles;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ParticleUtils {

	private ParticleUtils() {}

	private static final List<VoxelShape> EMPTY_LIST = List.of();
	private static final double MAXIMUM_COLLISION_VELOCITY_SQUARED = 10000.0D;
	private static final double COLLISION_HORIZONTAL_BOOST = 1.02D;
	private static final double COLLISION_HORIZONTAL_PUSH = 0.01D;
	private static final double MAX_HORIZONTAL_SPEED = 0.05D;
	private static final double EPS_MOTION = 1.0E-5D;
	private static final double ESCAPE_DIRECTION_STEP_DISTANCE = 0.25D;
	private static final double[] ESCAPE_DIRECTION_X = { 1.0D, -1.0D, 0.0D, 0.0D, 0.7071067811865476D, 0.7071067811865476D, -0.7071067811865476D, -0.7071067811865476D };
	private static final double[] ESCAPE_DIRECTION_Z = { 0.0D, 0.0D, 1.0D, -1.0D, 0.7071067811865476D, -0.7071067811865476D, 0.7071067811865476D, -0.7071067811865476D };
	private static final int ESCAPE_DIRECTION_STEPS = 4;
	private static final int ESCAPE_DIRECTION_VERTICAL_SAMPLES = 4;

	/**
	 * Moves the particle while handling collisions.
	 * If a vertical collision is detected (i.e. the particle is trying to move downwards but is blocked by a solid block), the particle's horizontal motion will be boosted and slightly pushed in a direction (either a random direction or a preferred escape direction, depending on the performance profile) to help it escape from under the block.
	 * 
	 * @param access The CampfireSmokeParticleAccessor instance for the particle being moved
	 * @param xa The desired movement in the X direction
	 * @param ya The desired movement in the Y direction
	 * @param za The desired movement in the Z direction
	 */
	public static void move(CampfireSmokeParticleAccessor access, double xa, double ya, double za) {
		if (access.isStoppedByCollision()) return;

		double originalXa = xa;
		double originalYa = ya;
		double originalZa = za;

		if (access.hasPhysics() && (xa != 0.0D || ya != 0.0D || za != 0.0D) && xa * xa + ya * ya + za * za < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
			final AABB boundingBox = access.getAABB();
			Vec3 movement = Entity.collideBoundingBox(null, new Vec3(xa, ya, za), boundingBox, access.level(), EMPTY_LIST);
			xa = movement.x;
			ya = movement.y;
			za = movement.z;
		}

		if (xa != 0.0D || ya != 0.0D || za != 0.0D) {
			access.setAABB(access.getAABB().move(xa, ya, za));
			access.setLocationFromAABB();
		}

		if (Math.abs(originalYa) >= EPS_MOTION && Math.abs(ya) < EPS_MOTION) access.setStoppedByCollision(true);

		if (originalXa != xa) access.setXd(0.0D);
		if (originalZa != za) access.setZd(0.0D);
	}

	private static boolean moveHorizontally(CampfireSmokeParticleAccessor access, double xa, double za) {
		double originalXa = xa;
		double originalZa = za;

		if (access.hasPhysics() && (xa != 0.0D || za != 0.0D) && xa * xa + za * za < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
			final AABB boundingBox = access.getAABB();
			Vec3 movement = Entity.collideBoundingBox(null, new Vec3(xa, 0.0D, za), boundingBox, access.level(), EMPTY_LIST);
			xa = movement.x;
			za = movement.z;
		}

		if (xa != 0.0D || za != 0.0D) {
			access.setAABB(access.getAABB().move(xa, 0.0D, za));
			access.setLocationFromAABB();
		}

		if (originalXa != xa) access.setXd(0.0D);
		if (originalZa != za) access.setZd(0.0D);

		return originalXa == xa && originalZa == za;
	}

	/**
	 * Applies a simple vertical stall response to the particle.
	 * 
	 * @param access The CampfireSmokeParticleAccessor instance for the particle
	 * @param randomSource The random source for generating random values
	 */
	public static void applySimpleVerticalStallResponse(CampfireSmokeParticleAccessor access, RandomSource randomSource) {
		access.setXd(access.xd() * COLLISION_HORIZONTAL_BOOST);
		access.setZd(access.zd() * COLLISION_HORIZONTAL_BOOST);

		final double pushX = access.xd() == 0.0D ? (randomSource.nextBoolean() ? COLLISION_HORIZONTAL_PUSH : -COLLISION_HORIZONTAL_PUSH) : Math.copySign(COLLISION_HORIZONTAL_PUSH, access.xd());
		final double pushZ = access.zd() == 0.0D ? (randomSource.nextBoolean() ? COLLISION_HORIZONTAL_PUSH : -COLLISION_HORIZONTAL_PUSH) : Math.copySign(COLLISION_HORIZONTAL_PUSH, access.zd());
		access.setXd(access.xd() + pushX);
		access.setZd(access.zd() + pushZ);

		final double horizontalSpeedSquared = access.xd() * access.xd() + access.zd() * access.zd();
		final double maxHorizontalSpeedSquared = MAX_HORIZONTAL_SPEED * MAX_HORIZONTAL_SPEED;
		if (horizontalSpeedSquared > maxHorizontalSpeedSquared) {
			final double scale = MAX_HORIZONTAL_SPEED / Math.sqrt(horizontalSpeedSquared);
			access.setXd(access.xd() * scale);
			access.setZd(access.zd() * scale);
		}

		moveHorizontally(access, pushX, pushZ);
	}

	/**
	 * Applies a complex vertical stall response to the particle, which attempts to find a preferred escape direction with better chances of successfully escaping from under the block that's causing the vertical stall, and boosts the particle's horizontal motion more if such a direction is found.
	 * 
	 * @param access The CampfireSmokeParticleAccessor instance for the particle
	 * @param randomSource The random source for generating random values
	 */
	public static void applyComplexVerticalStallResponse(CampfireSmokeParticleAccessor access, RandomSource randomSource) {
		ensurePreferredEscapeDirection(access);

		final double escapeX = access.isPreferredEscapeDirectionReady() ? access.getPreferredEscapeDirectionX() : (access.xd() == 0.0D ? (randomSource.nextBoolean() ? 1.0D : -1.0D) : Math.copySign(1.0D, access.xd()));
		final double escapeZ = access.isPreferredEscapeDirectionReady() ? access.getPreferredEscapeDirectionZ() : (access.zd() == 0.0D ? (randomSource.nextBoolean() ? 1.0D : -1.0D) : Math.copySign(1.0D, access.zd()));
		final double pushX = escapeX * COLLISION_HORIZONTAL_PUSH;
		final double pushZ = escapeZ * COLLISION_HORIZONTAL_PUSH;

		if (access.isPreferredEscapeDirectionReady()) {
			final double horizontalSpeed = Math.max(Math.sqrt(access.xd() * access.xd() + access.zd() * access.zd()), COLLISION_HORIZONTAL_PUSH);
			access.setXd(escapeX * horizontalSpeed * COLLISION_HORIZONTAL_BOOST);
			access.setZd(escapeZ * horizontalSpeed * COLLISION_HORIZONTAL_BOOST);
		} else {
			access.setXd(access.xd() * COLLISION_HORIZONTAL_BOOST);
			access.setZd(access.zd() * COLLISION_HORIZONTAL_BOOST);
			access.setXd(access.xd() + pushX);
			access.setZd(access.zd() + pushZ);
		}

		final double horizontalSpeedSquared = access.xd() * access.xd() + access.zd() * access.zd();
		final double maxHorizontalSpeedSquared = MAX_HORIZONTAL_SPEED * MAX_HORIZONTAL_SPEED;
		if (horizontalSpeedSquared > maxHorizontalSpeedSquared) {
			final double scale = MAX_HORIZONTAL_SPEED / Math.sqrt(horizontalSpeedSquared);
			access.setXd(access.xd() * scale);
			access.setZd(access.zd() * scale);
		}

		if (!moveHorizontally(access, pushX, pushZ)) access.setPreferredEscapeDirectionReady(false);
	}

	private static void ensurePreferredEscapeDirection(CampfireSmokeParticleAccessor access) {
		if (access.isPreferredEscapeDirectionReady()) return;

		double bestScore = Double.NEGATIVE_INFINITY;
		double bestX = 0.0D;
		double bestZ = 0.0D;

		for (int index = 0; index < ESCAPE_DIRECTION_X.length; index++) {
			double candidateX = ESCAPE_DIRECTION_X[index];
			double candidateZ = ESCAPE_DIRECTION_Z[index];
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
		final BlockPos.MutableBlockPos CHECK_POS = new BlockPos.MutableBlockPos();
		final int BASE_Y = (int) Math.floor(access.y());

		for (int step = 1; step <= ESCAPE_DIRECTION_STEPS; step++) {
			final int SAMPLE_X = (int) Math.floor(access.x() + directionX * step);
			final int SAMPLE_Z = (int) Math.floor(access.z() + directionZ * step);

			for (int vertical = 0; vertical < ESCAPE_DIRECTION_VERTICAL_SAMPLES; vertical++) {
				final int SAMPLE_Y = BASE_Y + vertical;
				CHECK_POS.set(SAMPLE_X, SAMPLE_Y, SAMPLE_Z);
				
				final var BLOCK_STATE = access.level().getBlockState(CHECK_POS);
				if(BLOCK_STATE.getBlock() instanceof TrapDoorBlock) {
					boolean isOpen = BLOCK_STATE.getValue(TrapDoorBlock.OPEN);

					score += isOpen ? 3.0D : 0.0D; // Open trapdoors are excellent escape points, as they don't have a collision box and can be easily escaped through, while closed trapdoors don't provide any escape benefit
				} else if(BLOCK_STATE.getBlock() instanceof SlabBlock) {
					score += 2.5D; // Slabs are good escape points, as they don't have a full collision box and can be easily escaped through
				} else if(BLOCK_STATE.getBlock() instanceof StairBlock) {
					score += 2.0D; // Stairs are very good escape points, as they don't have a full collision box and can be easily escaped through
				} else if (BLOCK_STATE.getCollisionShape(access.level(), CHECK_POS).isEmpty()) {
					score += vertical == 0 ? 1.5D : 1.0D;
				} else if(!BLOCK_STATE.isCollisionShapeFullBlock(access.level(), CHECK_POS)) {
					score += vertical == 0 ? 1.0D : 0.5D; // Blocks with low collision boxes (like bottom slabs or carpet) are somewhat good escape points, as they can be escaped through with a bit of vertical movement
				}
				else {
					score -= 3.0D;
					return score;
				}
			}

			score += 0.25D;
		}

		return score;
	}
}