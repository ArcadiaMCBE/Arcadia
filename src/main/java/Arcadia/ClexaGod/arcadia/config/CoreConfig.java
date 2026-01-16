package Arcadia.ClexaGod.arcadia.config;

import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import org.allaymc.api.message.I18n;
import org.allaymc.api.message.LangCode;
import org.allaymc.api.utils.config.Config;
import org.allaymc.api.utils.config.ConfigSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CoreConfig {

    private final String owner;
    private final String serverName;
    private final boolean debug;
    private final String defaultLang;
    private final String storageType;
    private final Map<String, Boolean> moduleToggles;
    private final List<ConfigIssue> issues;

    private CoreConfig(String owner, String serverName, boolean debug, String defaultLang, String storageType, Map<String, Boolean> moduleToggles, List<ConfigIssue> issues) {
        this.owner = owner;
        this.serverName = serverName;
        this.debug = debug;
        this.defaultLang = defaultLang;
        this.storageType = storageType;
        this.moduleToggles = Collections.unmodifiableMap(moduleToggles);
        this.issues = Collections.unmodifiableList(issues);
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

    public List<ConfigIssue> getIssues() {
        return issues;
    }

    public boolean isModuleEnabled(String moduleName) {
        if (moduleName == null || moduleName.isBlank()) {
            return false;
        }
        String key = moduleName.toLowerCase(Locale.ROOT);
        return moduleToggles.getOrDefault(key, true);
    }

    public static CoreConfig from(Config config) {
        List<ConfigIssue> issues = new ArrayList<>();

        String owner = config.getString("core.owner", "ClexaGod").trim();
        if (owner.isBlank()) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_OWNER_INVALID));
            owner = "ClexaGod";
        }

        String serverName = config.getString("core.server-name", "Arcadia").trim();
        if (serverName.isBlank()) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_SERVER_NAME_INVALID));
            serverName = "Arcadia";
        }

        boolean debug = config.getBoolean("core.debug", false);
        String defaultLangRaw = config.getString("core.default-lang", I18n.FALLBACK_LANG.name()).trim();
        LangCode langCode = LangCode.byName(defaultLangRaw);
        String defaultLang = langCode != null ? langCode.name() : I18n.FALLBACK_LANG.name();
        if (langCode == null) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_DEFAULT_LANG_INVALID, defaultLangRaw));
        }

        String storageRaw = config.getString("storage.type", "yaml").trim().toLowerCase(Locale.ROOT);
        String storageType = "yaml".equals(storageRaw) ? storageRaw : "yaml";
        if (!"yaml".equals(storageRaw)) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_INVALID, storageRaw));
        }

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

        return new CoreConfig(owner, serverName, debug, defaultLang, storageType, moduleToggles, issues);
    }
}
