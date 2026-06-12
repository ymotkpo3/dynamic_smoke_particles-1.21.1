package niwer.dynamic_smoke_particles;

import java.nio.file.Path;

import niwer.dynamic_smoke_particles.config.Config;

public abstract class Engine {

	private static Config configuration;

	protected void init(Path path) {
		Config.setConfigPath(path);
		configuration = Config.loadConfig();
	}

    public static Config config() { return configuration; }
}
