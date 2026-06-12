package niwer.dynamic_smoke_particles;

import java.util.Arrays;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import niwer.dynamic_smoke_particles.compatibility.ClothConfigCompat;
import niwer.dynamic_smoke_particles.config.Config;

public class Engine implements ClientModInitializer {
	private static final String[] CLOTH_CONFIG_ID = new String[] { "cloth-config", "cloth_config", "cloth-config2" };

	private static Config configuration;

	@Override
	public void onInitializeClient() {
		Config.setConfigPath(FabricLoader.getInstance().getConfigDir());
		configuration = Config.loadConfig();
	}

	public static Config config() {
		return configuration;
	}

	/**
	 * Checks if a mod is loaded.
	 * 
	 * @param modId The mod id to check for.
	 * @return True if the mod is loaded, false otherwise.
	 */
	public static boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	/**
	 * Checks if any of the given mods are loaded.
	 * 
	 * @param modId The mod ids to check for.
	 * @return True if any of the mods are loaded, false otherwise.
	 */
	public static boolean isModLoaded(String ...modId) {
		return Arrays.stream(modId).anyMatch(Engine::isModLoaded);
	}

	/**
	 * Gets the config screen for the mod. Returns null if no compatible config mod is loaded.
	 * 
	 * @param parent The parent screen.
	 * @return The config screen or null if no compatible config mod is loaded.
	 */
	public static Screen getConfigScreen(Screen parent) {
		if(isModLoaded(CLOTH_CONFIG_ID)) return ClothConfigCompat.getConfigScreen(parent, configuration);
		return null;
	}
}