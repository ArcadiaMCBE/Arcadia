package Arcadia.ClexaGod.arcadia.config;

import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.util.ResourceUtils;
import org.allaymc.api.message.I18n;
import org.allaymc.api.utils.config.Config;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigService {

    private final Path dataFolder;
    private final ClassLoader classLoader;
    private final Logger logger;
    private final Path configPath;

    private Config config;
    private CoreConfig coreConfig;

    public ConfigService(Path dataFolder, ClassLoader classLoader, Logger logger) {
        this.dataFolder = dataFolder;
        this.classLoader = classLoader;
        this.logger = logger;
        this.configPath = dataFolder.resolve("config.yml");
    }

    public void load() {
        ensureDataFolder();
        ensureDefaultConfig();
        config = new Config(configPath.toFile(), Config.YAML);
        coreConfig = CoreConfig.from(config);
        I18n.get().setDefaultLangCode(coreConfig.getDefaultLangCode());
        for (ConfigIssue issue : coreConfig.getIssues()) {
            logger.warn(I18n.get().tr(issue.key(), issue.args()));
        }
    }

    public CoreConfig getCoreConfig() {
        return coreConfig;
    }

    public void reload() {
        load();
    }

    private void ensureDataFolder() {
        try {
            if (!Files.exists(dataFolder)) {
                Files.createDirectories(dataFolder);
            }
        } catch (IOException e) {
            logger.error(I18n.get().tr(LangKeys.LOG_CONFIG_FOLDER_CREATE_FAILED, dataFolder.toString()), e);
        }
    }

    private void ensureDefaultConfig() {
        if (Files.exists(configPath)) {
            return;
        }
        try {
            ResourceUtils.copyResource(classLoader, "config.yml", configPath);
        } catch (IOException e) {
            logger.error(I18n.get().tr(LangKeys.LOG_CONFIG_DEFAULT_WRITE_FAILED, configPath.toString()), e);
        }
    }
}
