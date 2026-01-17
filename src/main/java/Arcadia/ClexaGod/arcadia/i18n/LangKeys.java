package Arcadia.ClexaGod.arcadia.i18n;

public final class LangKeys {

    private LangKeys() {
    }

    public static final String MESSAGE_PREFIX = "arcadia:message.prefix";
    public static final String MESSAGE_CORE_ENABLED = "arcadia:message.core_enabled";
    public static final String MESSAGE_CORE_DISABLED = "arcadia:message.core_disabled";
    public static final String MESSAGE_MODULE_ENABLED = "arcadia:message.module.enabled";
    public static final String MESSAGE_MODULE_DISABLED = "arcadia:message.module.disabled";

    public static final String LOG_CORE_ENABLING = "arcadia:log.core.enabling";
    public static final String LOG_CORE_ENABLED = "arcadia:log.core.enabled";
    public static final String LOG_CORE_DISABLING = "arcadia:log.core.disabling";
    public static final String LOG_CORE_DISABLED = "arcadia:log.core.disabled";
    public static final String LOG_CORE_RELOADING = "arcadia:log.core.reloading";
    public static final String LOG_CORE_RELOADED = "arcadia:log.core.reloaded";
    public static final String LOG_CORE_RELOAD_FAILED = "arcadia:log.core.reload_failed";
    public static final String LOG_MODULE_DISABLED_BY_CONFIG = "arcadia:log.module.disabled_by_config";
    public static final String LOG_MODULE_ENABLED = "arcadia:log.module.enabled";
    public static final String LOG_MODULE_ENABLE_FAILED = "arcadia:log.module.enable_failed";
    public static final String LOG_MODULE_DISABLED = "arcadia:log.module.disabled";
    public static final String LOG_MODULE_DISABLE_FAILED = "arcadia:log.module.disable_failed";
    public static final String LOG_SYSTEM_READY = "arcadia:log.system.ready";
    public static final String LOG_CONFIG_FOLDER_CREATE_FAILED = "arcadia:log.config.folder_create_failed";
    public static final String LOG_CONFIG_DEFAULT_WRITE_FAILED = "arcadia:log.config.default_write_failed";
    public static final String LOG_CONFIG_OWNER_INVALID = "arcadia:log.config.owner_invalid";
    public static final String LOG_CONFIG_SERVER_NAME_INVALID = "arcadia:log.config.server_name_invalid";
    public static final String LOG_CONFIG_DEFAULT_LANG_INVALID = "arcadia:log.config.default_lang_invalid";
    public static final String LOG_CONFIG_STORAGE_TYPE_INVALID = "arcadia:log.config.storage_type_invalid";
    public static final String LOG_CONFIG_STORAGE_JSON_PATH_INVALID = "arcadia:log.config.storage_json_path_invalid";
    public static final String LOG_CONFIG_STORAGE_QUEUE_MAX_INVALID = "arcadia:log.config.storage_queue_max_invalid";
    public static final String LOG_CONFIG_STORAGE_QUEUE_ON_FULL_INVALID = "arcadia:log.config.storage_queue_on_full_invalid";
    public static final String LOG_CONFIG_STORAGE_QUEUE_TIMEOUT_INVALID = "arcadia:log.config.storage_queue_timeout_invalid";
    public static final String LOG_CONFIG_STORAGE_POSTGRES_HOST_INVALID = "arcadia:log.config.storage_postgres_host_invalid";
    public static final String LOG_CONFIG_STORAGE_POSTGRES_PORT_INVALID = "arcadia:log.config.storage_postgres_port_invalid";
    public static final String LOG_CONFIG_STORAGE_POSTGRES_DB_INVALID = "arcadia:log.config.storage_postgres_db_invalid";
    public static final String LOG_CONFIG_STORAGE_POSTGRES_USER_INVALID = "arcadia:log.config.storage_postgres_user_invalid";
    public static final String LOG_CONFIG_STORAGE_POSTGRES_POOL_INVALID = "arcadia:log.config.storage_postgres_pool_invalid";
    public static final String LOG_CONFIG_STORAGE_RETRY_ATTEMPTS_INVALID = "arcadia:log.config.storage_retry_attempts_invalid";
    public static final String LOG_CONFIG_STORAGE_RETRY_BASE_DELAY_INVALID = "arcadia:log.config.storage_retry_base_delay_invalid";
    public static final String LOG_CONFIG_STORAGE_RETRY_MAX_DELAY_INVALID = "arcadia:log.config.storage_retry_max_delay_invalid";
    public static final String LOG_CONFIG_STORAGE_RETRY_JITTER_INVALID = "arcadia:log.config.storage_retry_jitter_invalid";
    public static final String LOG_CONFIG_CACHE_TTL_INVALID = "arcadia:log.config.cache_ttl_invalid";
    public static final String LOG_CONFIG_CACHE_MAX_ENTRIES_INVALID = "arcadia:log.config.cache_max_entries_invalid";
    public static final String LOG_CONFIG_CACHE_FLUSH_INTERVAL_INVALID = "arcadia:log.config.cache_flush_interval_invalid";
    public static final String LOG_CONFIG_CACHE_WARMUP_MAX_INVALID = "arcadia:log.config.cache_warmup_max_invalid";
    public static final String LOG_CONFIG_CACHE_WARMUP_DELAY_INVALID = "arcadia:log.config.cache_warmup_delay_invalid";

    public static final String LOG_STORAGE_SELECTED = "arcadia:log.storage.selected";
    public static final String LOG_STORAGE_INIT_SUCCESS = "arcadia:log.storage.init_success";
    public static final String LOG_STORAGE_INIT_FAILED = "arcadia:log.storage.init_failed";
    public static final String LOG_STORAGE_FALLBACK = "arcadia:log.storage.fallback";
    public static final String LOG_STORAGE_POSTGRES_INVALID_CONFIG = "arcadia:log.storage.postgres_invalid_config";
    public static final String LOG_STORAGE_MIGRATION_START = "arcadia:log.storage.migration_start";
    public static final String LOG_STORAGE_MIGRATION_APPLIED = "arcadia:log.storage.migration_applied";
    public static final String LOG_STORAGE_MIGRATION_COMPLETE = "arcadia:log.storage.migration_complete";
    public static final String LOG_STORAGE_MIGRATION_NONE = "arcadia:log.storage.migration_none";
    public static final String LOG_STORAGE_MIGRATION_FAILED = "arcadia:log.storage.migration_failed";
    public static final String LOG_STORAGE_JSON_READ_FAILED = "arcadia:log.storage.json_read_failed";
    public static final String LOG_STORAGE_JSON_WRITE_FAILED = "arcadia:log.storage.json_write_failed";
    public static final String LOG_STORAGE_JSON_DELETE_FAILED = "arcadia:log.storage.json_delete_failed";
    public static final String LOG_STORAGE_POSTGRES_READ_FAILED = "arcadia:log.storage.postgres_read_failed";
    public static final String LOG_STORAGE_POSTGRES_WRITE_FAILED = "arcadia:log.storage.postgres_write_failed";
    public static final String LOG_STORAGE_POSTGRES_DELETE_FAILED = "arcadia:log.storage.postgres_delete_failed";
    public static final String LOG_STORAGE_POSTGRES_EXISTS_FAILED = "arcadia:log.storage.postgres_exists_failed";
    public static final String LOG_STORAGE_QUEUE_STARTED = "arcadia:log.storage.queue_started";
    public static final String LOG_STORAGE_QUEUE_STOPPING = "arcadia:log.storage.queue_stopping";
    public static final String LOG_STORAGE_QUEUE_STOPPED = "arcadia:log.storage.queue_stopped";
    public static final String LOG_STORAGE_QUEUE_FULL = "arcadia:log.storage.queue_full";
    public static final String LOG_STORAGE_QUEUE_TASK_FAILED = "arcadia:log.storage.queue_task_failed";
    public static final String LOG_STORAGE_QUEUE_SHUTDOWN_TIMEOUT = "arcadia:log.storage.queue_shutdown_timeout";
    public static final String LOG_STORAGE_QUEUE_FORCE_DRAIN = "arcadia:log.storage.queue_force_drain";
    public static final String LOG_STORAGE_QUEUE_FORCE_DRAIN_COMPLETE = "arcadia:log.storage.queue_force_drain_complete";
    public static final String LOG_STORAGE_QUEUE_FULL_TIMEOUT = "arcadia:log.storage.queue_full_timeout";
    public static final String LOG_STORAGE_CACHE_ENABLED = "arcadia:log.storage.cache_enabled";
    public static final String LOG_STORAGE_CACHE_DISABLED = "arcadia:log.storage.cache_disabled";
    public static final String LOG_STORAGE_CACHE_FLUSH = "arcadia:log.storage.cache_flush";
    public static final String LOG_STORAGE_CACHE_OVERFLOW = "arcadia:log.storage.cache_overflow";
    public static final String LOG_STORAGE_CACHE_FLUSH_ON_SAVE_FAILED = "arcadia:log.storage.cache_flush_on_save_failed";
    public static final String LOG_STORAGE_CACHE_WARMUP_START = "arcadia:log.storage.cache_warmup_start";
    public static final String LOG_STORAGE_CACHE_WARMUP_COMPLETE = "arcadia:log.storage.cache_warmup_complete";
    public static final String LOG_STORAGE_RETRYING = "arcadia:log.storage.retrying";
}
