package Arcadia.ClexaGod.arcadia.config;

import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.storage.PostgresConfig;
import Arcadia.ClexaGod.arcadia.storage.StorageType;
import lombok.Getter;
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

@Getter
public final class CoreConfig {

    private final String owner;
    private final String serverName;
    private final boolean debug;
    private final String defaultLang;
    private final StorageType storageType;
    private final String storageJsonPath;
    private final PostgresConfig postgresConfig;
    private final Map<String, Boolean> moduleToggles;
    private final List<ConfigIssue> issues;

    private CoreConfig(String owner, String serverName, boolean debug, String defaultLang,
                       StorageType storageType, String storageJsonPath, PostgresConfig postgresConfig,
                       Map<String, Boolean> moduleToggles, List<ConfigIssue> issues) {
        this.owner = owner;
        this.serverName = serverName;
        this.debug = debug;
        this.defaultLang = defaultLang;
        this.storageType = storageType;
        this.storageJsonPath = storageJsonPath;
        this.postgresConfig = postgresConfig;
        this.moduleToggles = Collections.unmodifiableMap(moduleToggles);
        this.issues = Collections.unmodifiableList(issues);
    }

    public LangCode getDefaultLangCode() {
        LangCode lang = LangCode.byName(defaultLang);
        return lang != null ? lang : I18n.FALLBACK_LANG;
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

        String storageRaw = config.getString("storage.type", "json").trim();
        StorageType storageType = StorageType.from(storageRaw);
        if (storageType == null) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_TYPE_INVALID, storageRaw));
            storageType = StorageType.JSON;
        }

        String storageJsonPath = config.getString("storage.json.path", "data").trim();
        if (storageJsonPath.isBlank()) {
            issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_JSON_PATH_INVALID));
            storageJsonPath = "data";
        }

        String pgHost = config.getString("storage.postgresql.host", "127.0.0.1").trim();
        int pgPort = config.getInt("storage.postgresql.port", 5432);
        String pgDatabase = config.getString("storage.postgresql.database", "arcadia").trim();
        String pgUser = config.getString("storage.postgresql.user", "arcadia").trim();
        String pgPassword = config.getString("storage.postgresql.password", "").trim();
        boolean pgSsl = config.getBoolean("storage.postgresql.ssl", false);
        int pgMaxPool = config.getInt("storage.postgresql.pool.max-size", 10);
        int pgMinIdle = config.getInt("storage.postgresql.pool.min-idle", 2);
        int pgConnTimeout = config.getInt("storage.postgresql.pool.connection-timeout-ms", 10000);
        int pgIdleTimeout = config.getInt("storage.postgresql.pool.idle-timeout-ms", 600000);

        if (storageType == StorageType.POSTGRESQL) {
            if (pgHost.isBlank()) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_HOST_INVALID));
                pgHost = "127.0.0.1";
            }
            if (pgPort <= 0 || pgPort > 65535) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_PORT_INVALID, String.valueOf(pgPort)));
                pgPort = 5432;
            }
            if (pgDatabase.isBlank()) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_DB_INVALID));
                pgDatabase = "arcadia";
            }
            if (pgUser.isBlank()) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_USER_INVALID));
                pgUser = "arcadia";
            }
            if (pgMaxPool <= 0) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_POOL_INVALID, "max-size", String.valueOf(pgMaxPool)));
                pgMaxPool = 10;
            }
            if (pgMinIdle < 0) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_POOL_INVALID, "min-idle", String.valueOf(pgMinIdle)));
                pgMinIdle = 2;
            }
            if (pgMinIdle > pgMaxPool) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_POOL_INVALID, "min-idle", String.valueOf(pgMinIdle)));
                pgMinIdle = pgMaxPool;
            }
            if (pgConnTimeout <= 0) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_POOL_INVALID, "connection-timeout-ms", String.valueOf(pgConnTimeout)));
                pgConnTimeout = 10000;
            }
            if (pgIdleTimeout <= 0) {
                issues.add(new ConfigIssue(LangKeys.LOG_CONFIG_STORAGE_POSTGRES_POOL_INVALID, "idle-timeout-ms", String.valueOf(pgIdleTimeout)));
                pgIdleTimeout = 600000;
            }
        }

        PostgresConfig postgresConfig = new PostgresConfig(
                pgHost, pgPort, pgDatabase, pgUser, pgPassword, pgSsl,
                pgMaxPool, pgMinIdle, pgConnTimeout, pgIdleTimeout
        );

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

        return new CoreConfig(owner, serverName, debug, defaultLang, storageType, storageJsonPath, postgresConfig, moduleToggles, issues);
    }
}
