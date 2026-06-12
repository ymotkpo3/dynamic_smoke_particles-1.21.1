package niwer.dynamic_smoke_particles.platform.services;

import java.util.Arrays;

public interface IPlatformHelper {

    /**
     * Gets the name of the current platform
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
	 * Checks if any of the given mods are loaded.
	 * 
	 * @param modId The mod ids to check for.
	 * @return True if any of the mods are loaded, false otherwise.
	 */
	default boolean isModLoaded(String ...modId) {
		return Arrays.stream(modId).anyMatch(this::isModLoaded);
	}

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment.
     *
     * @return True if in a development environment, false otherwise.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Gets the name of the environment type as a string.
     *
     * @return The name of the environment type.
     */
    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }
}