package Arcadia.ClexaGod.arcadia.storage.repository.meta;

import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.storage.repository.StorageRepository;
import org.allaymc.api.message.I18n;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public final class PostgresMetaRepository implements StorageRepository<MetaRecord> {

    private final DataSource dataSource;
    private final Logger logger;

    public PostgresMetaRepository(DataSource dataSource, Logger logger) {
        this.dataSource = dataSource;
        this.logger = logger;
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
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, record.getId());
            ps.setString(2, record.getValue());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_POSTGRES_WRITE_FAILED, getName(), record.getId()), e);
        }
    }

    @Override
    public void delete(String id) {
        String sql = "DELETE FROM arcadia_meta WHERE key = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error(I18n.get().tr(LangKeys.LOG_STORAGE_POSTGRES_DELETE_FAILED, getName(), id), e);
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
}
