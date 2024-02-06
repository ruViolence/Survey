package ru.violence.survey.database;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.violence.coreapi.common.api.util.Check;
import ru.violence.coreapi.common.api.util.SchemaReader;
import ru.violence.survey.SurveyPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class SQLite {
    private static final String SELECT_RESULT_DATA = "SELECT data FROM results WHERE survey_key = ? AND player_uuid = ?";
    private static final String INSERT_RESULT = "INSERT INTO results(survey_key, player_uuid, data) VALUES (?, ?, ?)";
    private static final String SELECT_OPT_OUT_SINCE = "SELECT since FROM opt_out WHERE survey_key = ? AND player_uuid = ?";
    private static final String INSERT_OPT_OUT = "INSERT INTO opt_out(survey_key, player_uuid, since) VALUES (?, ?, ?)";

    private final SurveyPlugin plugin;
    private Connection connection;
    private final Object lock = new Object();

    public SQLite(SurveyPlugin plugin) {
        this.plugin = plugin;
        try {
            Class.forName("org.sqlite.JDBC");
            connect();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public void terminate() {
        try {
            connection.close();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void connect() throws IOException, SQLException {
        plugin.getDataFolder().mkdirs();
        synchronized (lock) {
            connection = DriverManager.getConnection("jdbc:sqlite://" + plugin.getDataFolder().getAbsolutePath() + "/data.db");
            createMissingTables();
        }
    }

    private void createMissingTables() throws IOException, SQLException {
        synchronized (lock) {
            try (Statement st = connection.createStatement()) {
                List<String> statements;

                try (InputStream is = plugin.getResource("schema/sqlite.sql")) {
                    statements = SchemaReader.getStatements(is);
                }

                for (String statement : statements) {
                    st.addBatch(statement);
                }

                st.executeBatch();
            }
        }
    }

    private Connection getConnection() {
        try {
            synchronized (lock) {
                if (connection.isClosed()) {
                    Check.isTrue(plugin.isEnabled(), "Plugin is disabled");
                    connect();
                }
                return connection;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public void insertResultData(@NotNull String surveyKey, @NotNull UUID playerUniqueId, @NotNull JsonObject data) {
        synchronized (lock) {
            try (PreparedStatement ps = getConnection().prepareStatement(INSERT_RESULT)) {
                ps.setString(1, surveyKey);
                ps.setString(2, playerUniqueId.toString());
                ps.setString(3, new Gson().toJson(data));
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    public boolean isCompleted(@NotNull String surveyKey, @NotNull UUID playerUniqueId) {
        return selectResultData(surveyKey, playerUniqueId) != null;
    }

    public @Nullable String selectResultData(@NotNull String surveyKey, @NotNull UUID playerUniqueId) {
        synchronized (lock) {
            try (PreparedStatement ps = getConnection().prepareStatement(SELECT_RESULT_DATA)) {
                ps.setString(1, surveyKey);
                ps.setString(2, playerUniqueId.toString());
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return null;
                return rs.getString(1);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, e.getMessage(), e);
                return null;
            }
        }
    }

    public void insertOptOut(@NotNull String surveyKey, @NotNull UUID playerUniqueId) {
        synchronized (lock) {
            try (PreparedStatement ps = getConnection().prepareStatement(INSERT_OPT_OUT)) {
                ps.setString(1, surveyKey);
                ps.setString(2, playerUniqueId.toString());
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    public boolean isOptOut(@NotNull String surveyKey, @NotNull UUID playerUniqueId) {
        return selectOptOut(surveyKey, playerUniqueId) != null;
    }

    public @Nullable Long selectOptOut(@NotNull String surveyKey, @NotNull UUID playerUniqueId) {
        synchronized (lock) {
            try (PreparedStatement ps = getConnection().prepareStatement(SELECT_OPT_OUT_SINCE)) {
                ps.setString(1, surveyKey);
                ps.setString(2, playerUniqueId.toString());
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return null;
                return rs.getLong(1);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, e.getMessage(), e);
                return null;
            }
        }
    }
}
