package ru.violence.survey;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import ru.violence.survey.command.SurveyCommand;
import ru.violence.survey.database.SQLite;
import ru.violence.survey.listener.JoinListener;
import ru.violence.survey.model.SurveyManager;

public class SurveyPlugin extends JavaPlugin {
    private SQLite sqlite;
    private SurveyManager surveyManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.sqlite = new SQLite(this);
        this.surveyManager = new SurveyManager(this);

        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getCommand("survey").setExecutor(new SurveyCommand(this));
    }

    @Override
    public void onDisable() {
        sqlite.terminate();
    }

    public @NotNull SurveyManager getSurveyManager() {
        return surveyManager;
    }

    public @NotNull SQLite getSQLite() {
        return sqlite;
    }
}
