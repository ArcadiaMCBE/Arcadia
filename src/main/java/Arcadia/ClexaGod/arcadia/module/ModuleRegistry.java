package Arcadia.ClexaGod.arcadia.module;

import Arcadia.ClexaGod.arcadia.ArcadiaCore;
import Arcadia.ClexaGod.arcadia.config.ConfigService;
import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import org.allaymc.api.message.I18n;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ModuleRegistry {

    private final Logger logger;
    private final ConfigService configService;
    private final Map<String, Module> modules = new LinkedHashMap<>();

    public ModuleRegistry(Logger logger, ConfigService configService) {
        this.logger = logger;
        this.configService = configService;
    }

    public void register(Module module) {
        String key = normalize(module.getName());
        modules.put(key, module);
    }

    public void enableAll(ArcadiaCore core) {
        for (Map.Entry<String, Module> entry : modules.entrySet()) {
            String name = entry.getKey();
            Module module = entry.getValue();
            if (!configService.getCoreConfig().isModuleEnabled(name)) {
                logger.info(I18n.get().tr(LangKeys.LOG_MODULE_DISABLED_BY_CONFIG, name));
                continue;
            }
            try {
                module.onEnable(core);
                logger.info(I18n.get().tr(LangKeys.LOG_MODULE_ENABLED, name));
            } catch (Exception e) {
                logger.error(I18n.get().tr(LangKeys.LOG_MODULE_ENABLE_FAILED, name), e);
            }
        }
    }

    public void disableAll() {
        for (Map.Entry<String, Module> entry : modules.entrySet()) {
            String name = entry.getKey();
            try {
                entry.getValue().onDisable();
                logger.info(I18n.get().tr(LangKeys.LOG_MODULE_DISABLED, name));
            } catch (Exception e) {
                logger.error(I18n.get().tr(LangKeys.LOG_MODULE_DISABLE_FAILED, name), e);
            }
        }
    }

    private String normalize(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase(Locale.ROOT);
    }
}
