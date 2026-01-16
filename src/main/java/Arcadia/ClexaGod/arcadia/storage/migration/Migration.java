package Arcadia.ClexaGod.arcadia.storage.migration;

import java.sql.Connection;
import java.sql.SQLException;

public interface Migration {

    int version();

    String description();

    void apply(Connection connection) throws SQLException;
}
