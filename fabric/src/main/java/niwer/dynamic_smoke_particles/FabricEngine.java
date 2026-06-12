package niwer.dynamic_smoke_particles;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import niwer.dynamic_smoke_particles.compatibility.ClothConfigCompat;
import niwer.dynamic_smoke_particles.config.Config;
import niwer.dynamic_smoke_particles.platform.Services;

public class FabricEngine implements ClientModInitializer {
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
	 * Gets the config screen for the mod. Returns null if no compatible config mod is loaded.
	 * 
	 * @param parent The parent screen.
	 * @return The config screen or null if no compatible config mod is loaded.
	 */
	public static Screen getConfigScreen(Screen parent) {
		if(Services.PLATFORM.isModLoaded(CLOTH_CONFIG_ID)) return ClothConfigCompat.getConfigScreen(parent, configuration);
		return null;
	}
}