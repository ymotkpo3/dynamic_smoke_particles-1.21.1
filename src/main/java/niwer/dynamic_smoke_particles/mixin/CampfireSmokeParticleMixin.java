package niwer.dynamic_smoke_particles.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import niwer.dynamic_smoke_particles.CampfireSmokeParticleAccessor;
import niwer.dynamic_smoke_particles.Engine;
import niwer.dynamic_smoke_particles.ParticleUtils;

@Mixin(CampfireSmokeParticle.class)
public abstract class CampfireSmokeParticleMixin extends SingleQuadParticle implements CampfireSmokeParticleAccessor {

	@Unique private static final double EPS_MIN_MOVEMENT = 1.0E-7D;
	@Unique private boolean preferredEscapeDirectionReady;
	@Unique private double preferredEscapeDirectionX;
	@Unique private double preferredEscapeDirectionZ;
	@Unique private boolean stoppedByCollision;

	private CampfireSmokeParticleMixin(final ClientLevel level, final double x, final double y, final double z, final double xa, final double ya, final double za, final boolean isSignalFire, final TextureAtlasSprite sprite) {
		super(level, x, y, z, sprite);
	}

	@Inject(at = @At("HEAD"), method = "tick", cancellable = true)
	private void onTick(CallbackInfo info) {
		if (!Engine.config().isEnabled()) return;

		info.cancel();
		this.stoppedByCollision = false;

		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;

		if (this.age++ < this.lifetime && this.alpha > 0.0F) {
			final RandomSource RND_SOURCE = this.random;

			this.xd += (RND_SOURCE.nextBoolean() ? 1.0D : -1.0D) * (RND_SOURCE.nextFloat() * 0.0002D);
			this.zd += (RND_SOURCE.nextBoolean() ? 1.0D : -1.0D) * (RND_SOURCE.nextFloat() * 0.0002D);
			this.yd -= this.gravity;

			final double previousY = this.y;
			final double requestedYMotion = this.yd;
			ParticleUtils.move(this, this.xd, this.yd, this.zd);

			if (requestedYMotion != 0.0D && Math.abs(this.y - previousY) < EPS_MIN_MOVEMENT) {
				switch (Engine.config().performanceProfile()) {
					case SIMPLE -> ParticleUtils.applySimpleVerticalStallResponse(this, RND_SOURCE);
					case COMPLEX -> ParticleUtils.applyComplexVerticalStallResponse(this, RND_SOURCE);
				}
			}

			if (this.age >= this.lifetime - 60 && this.alpha > 0.01F) this.alpha -= 0.015F;
		} else this.remove();
	}

	@Override public ClientLevel level() { return this.level; }

	@Override public boolean hasPhysics() { return this.hasPhysics; }

	@Override public AABB getAABB() { return this.getBoundingBox(); }

	@Override public void setAABB(AABB boundingBox) { this.setBoundingBox(boundingBox); }

	@Override public void setLocationFromAABB() { this.setLocationFromBoundingbox(); }

	@Override public double x() { return this.x; }

	@Override public double y() { return this.y; }

	@Override public double z() { return this.z; }

	@Override public double xd() { return this.xd; }

	@Override public void setXd(double value) { this.xd = value; }

	@Override public double yd() { return this.yd; }

	@Override public void setYd(double value) { this.yd = value; }

	@Override public double zd() { return this.zd; }

	@Override public void setZd(double value) { this.zd = value; }

	@Override public boolean isStoppedByCollision() { return this.stoppedByCollision; }

	@Override public void setStoppedByCollision(boolean value) { this.stoppedByCollision = value; }

	@Override public boolean isPreferredEscapeDirectionReady() { return this.preferredEscapeDirectionReady; }

	@Override public void setPreferredEscapeDirectionReady(boolean value) { this.preferredEscapeDirectionReady = value; }

	@Override public double getPreferredEscapeDirectionX() { return this.preferredEscapeDirectionX; }

	@Override public void setPreferredEscapeDirectionX(double value) { this.preferredEscapeDirectionX = value; }

	@Override public double getPreferredEscapeDirectionZ() { return this.preferredEscapeDirectionZ; }

	@Override public void setPreferredEscapeDirectionZ(double value) { this.preferredEscapeDirectionZ = value; }
}
