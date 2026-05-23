package niwer.dynamic_smoke_particles.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import niwer.dynamic_smoke_particles.Engine;

@Mixin(CampfireSmokeParticle.class)
public abstract class CampfireSmokeParticleMixin extends SingleQuadParticle {

	@Unique private static final double MAXIMUM_COLLISION_VELOCITY_SQUARED = Mth.square((double)100.0F);
	@Unique private static final double COLLISION_HORIZONTAL_BOOST = 1.02D;
	@Unique private static final double COLLISION_HORIZONTAL_PUSH = 0.01D;
	@Unique private static final double MAX_HORIZONTAL_SPEED = 0.05D;
	@Unique private boolean stoppedByCollision;

	private CampfireSmokeParticleMixin(final ClientLevel level, final double x, final double y, final double z, final double xa, final double ya, final double za, final boolean isSignalFire, final TextureAtlasSprite sprite) {
		super(level, x, y, z, sprite);
	}

	@Inject(at = @At("HEAD"), method = "tick", cancellable = true)
	private void onTick(CallbackInfo info) {
		if(!Engine.config().isEnabled()) return; // If the mod is disabled, skip the custom tick logic and use the original behavior

		info.cancel();
		this.stoppedByCollision = false;

		/* Handle previous positions */
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;

		if (this.age++ < this.lifetime && !(this.alpha <= 0.0F)) {
			this.xd += (double)(this.random.nextFloat() / 5000.0F * (float)(this.random.nextBoolean() ? 1 : -1));
			this.zd += (double)(this.random.nextFloat() / 5000.0F * (float)(this.random.nextBoolean() ? 1 : -1));
			this.yd -= (double)this.gravity;

			double previousY = this.y;
			double requestedYMotion = this.yd;
			this.move(this.xd, this.yd, this.zd);

			if (requestedYMotion != 0.0D && Math.abs(this.y - previousY) < 1.0E-7D) {
				this.xd *= COLLISION_HORIZONTAL_BOOST;
				this.zd *= COLLISION_HORIZONTAL_BOOST;
				double pushX = this.xd == 0.0D ? (this.random.nextBoolean() ? COLLISION_HORIZONTAL_PUSH : -COLLISION_HORIZONTAL_PUSH) : Math.copySign(COLLISION_HORIZONTAL_PUSH, this.xd);
				double pushZ = this.zd == 0.0D ? (this.random.nextBoolean() ? COLLISION_HORIZONTAL_PUSH : -COLLISION_HORIZONTAL_PUSH) : Math.copySign(COLLISION_HORIZONTAL_PUSH, this.zd);
				this.xd += pushX;
				this.zd += pushZ;

				double horizontalSpeedSquared = this.xd * this.xd + this.zd * this.zd;
				double maxHorizontalSpeedSquared = MAX_HORIZONTAL_SPEED * MAX_HORIZONTAL_SPEED;
				if (horizontalSpeedSquared > maxHorizontalSpeedSquared) {
					double scale = MAX_HORIZONTAL_SPEED / Math.sqrt(horizontalSpeedSquared);
					this.xd *= scale;
					this.zd *= scale;
				}

				this.moveHorizontally(pushX, pushZ);
			}

			if (this.age >= this.lifetime - 60 && this.alpha > 0.01F) this.alpha -= 0.015F;
		} else this.remove();
	}

	@Unique
	public void move(double xa, double ya, double za) {
		if (!this.stoppedByCollision) {
			double originalXa = xa;
			double originalYa = ya;
			double originalZa = za;
			if (this.hasPhysics && (xa != (double)0.0F || ya != (double)0.0F || za != (double)0.0F) && xa * xa + ya * ya + za * za < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
				Vec3 movement = Entity.collideBoundingBox((Entity)null, new Vec3(xa, ya, za), this.getBoundingBox(), this.level, List.of());
				xa = movement.x;
				ya = movement.y;
				za = movement.z;
			}

			if (xa != (double)0.0F || ya != (double)0.0F || za != (double)0.0F) {
				this.setBoundingBox(this.getBoundingBox().move(xa, ya, za));
				this.setLocationFromBoundingbox();
			}

			if (Math.abs(originalYa) >= (double)1.0E-5F && Math.abs(ya) < (double)1.0E-5F) this.stoppedByCollision = true;

			this.onGround = originalYa != ya && originalYa < (double)0.0F;
			if (originalXa != xa) this.xd = (double)0.0F;
			if (originalZa != za) this.zd = (double)0.0F;
		}
   }

	@Unique
	public void moveHorizontally(double xa, double za) {
		double originalXa = xa;
		double originalZa = za;

		if (this.hasPhysics && (xa != (double)0.0F || za != (double)0.0F) && xa * xa + za * za < MAXIMUM_COLLISION_VELOCITY_SQUARED) {
			Vec3 movement = Entity.collideBoundingBox((Entity)null, new Vec3(xa, 0.0D, za), this.getBoundingBox(), this.level, List.of());
			xa = movement.x;
			za = movement.z;
		}

		if (xa != (double)0.0F || za != (double)0.0F) {
			this.setBoundingBox(this.getBoundingBox().move(xa, 0.0D, za));
			this.setLocationFromBoundingbox();
		}

		if (originalXa != xa) this.xd = (double)0.0F;
		if (originalZa != za) this.zd = (double)0.0F;
   	}
}