package niwer.dynamic_smoke_particles.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Config {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Config DEFAULT_CONFIG = new Config();
    private static final String CONFIG_FILE = "config.json";
    private static File configFile;

    private boolean enabled = true;
    private PerformanceProfile performanceProfile = PerformanceProfile.COMPLEX;

    private Config() {}

    public PerformanceProfile performanceProfile() { return performanceProfile; }

    public void setPerformanceProfile(PerformanceProfile performanceProfile) { this.performanceProfile = performanceProfile; }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /**
     * Loads the configuration from the config file. If the file does not exist, it will be created with the default configuration.
     * 
     * @return The loaded configuration.
     */
    public static Config loadConfig() {
        try(var reader = new FileReader(configFile)) {
            return GSON.fromJson(reader, Config.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return DEFAULT_CONFIG;
    }

    /**
     * Saves the current configuration to the config file. If the file does not exist, it will be created.
     */
    public void saveConfig() {
        try(var writer = new FileWriter(configFile)) {
            GSON.toJson(this, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setConfigPath(Path path) {
        configFile = new File(path.toString(), CONFIG_FILE);

        /* If the file does not exist */
        if(!configFile.exists()) {
            try {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
                DEFAULT_CONFIG.saveConfig();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
