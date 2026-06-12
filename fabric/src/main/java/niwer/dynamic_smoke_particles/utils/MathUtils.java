package niwer.dynamic_smoke_particles.utils;

public final class MathUtils {

	private MathUtils() {}

    /**
     * Returns the square of the given value.
     * 
     * @param value the value to square
     * @return the square of the given value
     */
	public static double square(double value) {
		return value * value;
	}

    /**
     * Returns the squared length of the vector defined by the given components.
     * 
     * @param x the x component of the vector
     * @param y the y component of the vector
     * @param z the z component of the vector
     * @return the squared length of the vector
     */
	public static double lengthSquared(double x, double y, double z) {
		return square(x) + square(y) + square(z);
	}

    /**
     * Returns the squared horizontal speed defined by the given x and z components.
     * 
     * @param x the x component of the horizontal velocity
     * @param z the z component of the horizontal velocity
     * @return the squared horizontal speed
     */
	public static double horizontalSpeedSquared(double x, double z) {
		return square(x) + square(z);
	}

    /**
     * Returns the horizontal speed defined by the given x and z components.
     * 
     * @param x the x component of the horizontal velocity
     * @param z the z component of the horizontal velocity
     * @return the horizontal speed
     */
	public static double horizontalSpeed(double x, double z) {
		return Math.sqrt(horizontalSpeedSquared(x, z));
	}

    /**
     * Scales the given x and z components by the same factor if their combined horizontal speed exceeds the specified maximum magnitude, otherwise returns 1.0.
     * 
     * @param x the x component of the horizontal velocity
     * @param z the z component of the horizontal velocity
     * @param maxMagnitude the maximum magnitude of the horizontal speed
     * @return the scale factor
     */
	public static double clampScale(double x, double z, double maxMagnitude) {
		double speedSquared = horizontalSpeedSquared(x, z);
		double maxMagnitudeSquared = square(maxMagnitude);
		return speedSquared > maxMagnitudeSquared ? maxMagnitude / Math.sqrt(speedSquared) : 1.0D;
	}

    /**
     * Returns true if the given value is within the specified epsilon distance from the reference value, otherwise returns false.
     * 
     * @param value the value to check
     * @param reference the reference value
     * @param epsilon the epsilon distance
     * @return true if the given value is within the specified epsilon distance from the reference value, otherwise returns false
     */
	public static boolean isWithinEpsilon(double value, double reference, double epsilon) {
		return Math.abs(value - reference) < epsilon;
	}
}