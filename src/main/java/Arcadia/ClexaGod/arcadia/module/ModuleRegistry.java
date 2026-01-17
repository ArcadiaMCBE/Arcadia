package Arcadia.ClexaGod.arcadia.module;

import Arcadia.ClexaGod.arcadia.ArcadiaCore;
import Arcadia.ClexaGod.arcadia.config.ConfigService;
import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.logging.LogCategory;
import Arcadia.ClexaGod.arcadia.logging.LogService;
import lombok.RequiredArgsConstructor;
import org.allaymc.api.message.I18n;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RequiredArgsConstructor
public final class ModuleRegistry {

    private final LogService logService;
    private final ConfigService configService;
    private final Map<String, Module> modules = new LinkedHashMap<>();

    public void register(Module module) {
        String key = normalize(module.getName());
        modules.put(key, module);
    }

    public void enableAll(ArcadiaCore core) {
        for (Map.Entry<String, Module> entry : modules.entrySet()) {
            String name = entry.getKey();
            Module module = entry.getValue();
            if (!configService.getCoreConfig().isModuleEnabled(name)) {
                logService.info(LogCategory.MODULE, I18n.get().tr(LangKeys.LOG_MODULE_DISABLED_BY_CONFIG, name));
                continue;
            }
            try {
                module.onEnable(core);
                logService.info(LogCategory.MODULE, I18n.get().tr(LangKeys.LOG_MODULE_ENABLED, name));
            } catch (Exception e) {
                logService.error(LogCategory.MODULE, I18n.get().tr(LangKeys.LOG_MODULE_ENABLE_FAILED, name), e);
            }
        }
    }

    public void disableAll() {
        for (Map.Entry<String, Module> entry : modules.entrySet()) {
            String name = entry.getKey();
            try {
                entry.getValue().onDisable();
                logService.info(LogCategory.MODULE, I18n.get().tr(LangKeys.LOG_MODULE_DISABLED, name));
            } catch (Exception e) {
                logService.error(LogCategory.MODULE, I18n.get().tr(LangKeys.LOG_MODULE_DISABLE_FAILED, name), e);
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
