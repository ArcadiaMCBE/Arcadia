package Arcadia.ClexaGod.arcadia.storage.repository.meta;

import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.storage.repository.StorageRepository;
import Arcadia.ClexaGod.arcadia.storage.retry.RetryExecutor;
import Arcadia.ClexaGod.arcadia.storage.retry.RetryOutcome;
import Arcadia.ClexaGod.arcadia.storage.retry.RetryPolicy;
import org.allaymc.api.message.I18n;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public final class PostgresMetaRepository implements StorageRepository<MetaRecord> {

    private final DataSource dataSource;
    private final Logger logger;
    private final RetryPolicy retryPolicy;

    public PostgresMetaRepository(DataSource dataSource, Logger logger) {
        this(dataSource, logger, null);
    }

    public PostgresMetaRepository(DataSource dataSource, Logger logger, RetryPolicy retryPolicy) {
        this.dataSource = dataSource;
        this.logger = logger;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public String getName() {
        return "meta";
    }

    @Override
    public Optional<MetaRecord> load(String id) {
        String sql = "SELECT value FROM arcadia_meta WHERE key = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new MetaRecord(id, rs.getString("value")));
                }
            }
        } catch (Exception e) {
            logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_POSTGRES_READ_FAILED, getName(), id), e);
        }
        return Optional.empty();
    }

    @Override
    public void save(MetaRecord record) {
        String sql = """
                INSERT INTO arcadia_meta (key, value, updated_at)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (key)
                DO UPDATE SET value = EXCLUDED.value, updated_at = CURRENT_TIMESTAMP
                """;
        RetryOutcome outcome = RetryExecutor.run(retryPolicy, logger, "postgres/save " + getName() + "/" + record.getId(), () -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, record.getId());
                ps.setString(2, record.getValue());
                ps.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        if (!outcome.success()) {
            Exception error = outcome.error();
            if (error != null) {
                logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_POSTGRES_WRITE_FAILED, getName(), record.getId()), error);
            } else {
                logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_POSTGRES_WRITE_FAILED, getName(), record.getId()));
            }
        }
    }

    @Override
    public void delete(String id) {
        String sql = "DELETE FROM arcadia_meta WHERE key = ?";
        RetryOutcome outcome = RetryExecutor.run(retryPolicy, logger, "postgres/delete " + getName() + "/" + id, () -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        if (!outcome.success()) {
            Exception error = outcome.error();
            if (error != null) {
                logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_POSTGRES_DELETE_FAILED, getName(), id), error);
            } else {
                logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_POSTGRES_DELETE_FAILED, getName(), id));
            }
        }
    }

    @Override
    public boolean exists(String id) {
        String sql = "SELECT 1 FROM arcadia_meta WHERE key = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_POSTGRES_EXISTS_FAILED, getName(), id), e);
            return false;
        }
    }

    @Override
    public List<MetaRecord> loadAll() {
        return loadAll(Integer.MAX_VALUE);
    }

    @Override
    public List<MetaRecord> loadAll(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        String sql = "SELECT key, value FROM arcadia_meta" + (limit >= Integer.MAX_VALUE ? "" : " LIMIT ?");
        List<MetaRecord> records = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            if (limit < Integer.MAX_VALUE) {
                ps.setInt(1, limit);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getString("key");
                    String value = rs.getString("value");
                    records.add(new MetaRecord(key, value));
                }
            }
        } catch (Exception e) {
            logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_POSTGRES_READ_FAILED, getName(), "list"), e);
        }
        return records;
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM arcadia_meta";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (Exception e) {
            logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_POSTGRES_READ_FAILED, getName(), "count"), e);
        }
        return 0;
    }
}
