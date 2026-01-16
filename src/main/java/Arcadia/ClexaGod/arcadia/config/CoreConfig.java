package Arcadia.ClexaGod.arcadia.config;

import org.allaymc.api.message.I18n;
import org.allaymc.api.message.LangCode;
import org.allaymc.api.utils.config.Config;
import org.allaymc.api.utils.config.ConfigSection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class CoreConfig {

    private final String owner;
    private final String serverName;
    private final boolean debug;
    private final String defaultLang;
    private final String storageType;
    private final Map<String, Boolean> moduleToggles;

    private CoreConfig(String owner, String serverName, boolean debug, String defaultLang, String storageType, Map<String, Boolean> moduleToggles) {
        this.owner = owner;
        this.serverName = serverName;
        this.debug = debug;
        this.defaultLang = defaultLang;
        this.storageType = storageType;
        this.moduleToggles = Collections.unmodifiableMap(moduleToggles);
    }

    public String getOwner() {
        return owner;
    }

    public String getServerName() {
        return serverName;
    }

    public boolean isDebug() {
        return debug;
    }

    public String getDefaultLang() {
        return defaultLang;
    }

    public LangCode getDefaultLangCode() {
        LangCode lang = LangCode.byName(defaultLang);
        return lang != null ? lang : I18n.FALLBACK_LANG;
    }

    public String getStorageType() {
        return storageType;
    }

    public boolean isModuleEnabled(String moduleName) {
        if (moduleName == null || moduleName.isBlank()) {
            return false;
        }
        String key = moduleName.toLowerCase(Locale.ROOT);
        return moduleToggles.getOrDefault(key, true);
    }

    public static CoreConfig from(Config config) {
        String owner = config.getString("core.owner", "ClexaGod");
        String serverName = config.getString("core.server-name", "Arcadia");
        boolean debug = config.getBoolean("core.debug", false);
        String defaultLang = config.getString("core.default-lang", I18n.FALLBACK_LANG.name());
        String storageType = config.getString("storage.type", "yaml");

        Map<String, Boolean> moduleToggles = new LinkedHashMap<>();
        ConfigSection section = config.getSection("modules");
        for (Map.Entry<String, Object> entry : section.entrySet()) {
            String key = entry.getKey().toLowerCase(Locale.ROOT);
            boolean enabled;
            Object value = entry.getValue();
            if (value instanceof Boolean bool) {
                enabled = bool;
            } else {
                enabled = Boolean.parseBoolean(String.valueOf(value));
            }
            moduleToggles.put(key, enabled);
        }

        return new CoreConfig(owner, serverName, debug, defaultLang, storageType, moduleToggles);
    }
}
