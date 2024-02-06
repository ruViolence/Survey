package ru.violence.survey.model;

import com.google.gson.JsonObject;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.violence.survey.SurveyPlugin;
import ru.violence.survey.api.event.AsyncAvailabilityCheckEvent;
import ru.violence.survey.api.event.PreOpenSurveyEvent;
import ru.violence.survey.model.answer.Answer;
import ru.violence.survey.model.answer.CheckboxAnswer;
import ru.violence.survey.model.answer.Option;
import ru.violence.survey.model.answer.RadioAnswer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class SurveyManager {
    private final SurveyPlugin plugin;
    private final Map<String, Survey> surveyMap = new HashMap<>();
    private final Map<UUID, RunningSurvey> runningSurveysByPlayer = new HashMap<>();

    public SurveyManager(SurveyPlugin plugin) {
        this.plugin = plugin;
        loadSurveys();
    }

    private void loadSurveys() {
        FileConfiguration config = plugin.getConfig();

        config.getConfigurationSection("surveys").getKeys(false)
                .stream()
                .map(key -> config.getConfigurationSection("surveys." + key))
                .forEach(surveySect -> {
                    String surveyKey = surveySect.getName();
                    String surveyName = surveySect.getString("name");
                    String surveyDescription = surveySect.getString("description");
                    List<Question> questions = surveySect.getConfigurationSection("questions").getKeys(false)
                            .stream()
                            .map(key -> surveySect.getConfigurationSection("questions." + key))
                            .map(questionSect -> {
                                String questionKey = questionSect.getName();
                                String questionText = questionSect.getString("text");

                                Answer.Type answerType = Answer.Type.valueOf(questionSect.getString("answer.type"));
                                List<Option> options = questionSect.getConfigurationSection("answer.options").getKeys(false)
                                        .stream()
                                        .map(key -> new Option(key, questionSect.getString("answer.options." + key)))
                                        .collect(Collectors.toList());

                                Answer answer = answerType == Answer.Type.CHECKBOX
                                        ? new CheckboxAnswer(options)
                                        : new RadioAnswer(options);

                                return new Question(questionKey, questionText, answer);
                            })
                            .collect(Collectors.toList());

                    surveyMap.put(surveyKey, new Survey(surveyKey, surveyName, surveyDescription, questions));
                });
    }

    public void notifyIfAvailable(@NotNull Player player) {
        for (Survey survey : surveyMap.values()) {
            if (plugin.getSQLite().isCompleted(survey.getKey(), player.getUniqueId())) continue;
            if (plugin.getSQLite().isOptOut(survey.getKey(), player.getUniqueId())) continue;
            if (!new AsyncAvailabilityCheckEvent(survey, player).callEvent()) continue;

            {
                ComponentBuilder cb = new ComponentBuilder("");
                cb.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§eКликни, чтобы открыть опрос!")));
                cb.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/survey open " + survey.getKey()));
                cb.append("§5§m=====================================================", ComponentBuilder.FormatRetention.EVENTS);
                cb.append("\n§5§m=§r §fВам доступен для прохождения опрос: \"" + survey.getName() + "\"", ComponentBuilder.FormatRetention.EVENTS);
                cb.append("\n§5§m=§r §fКликните на это сообщение, чтобы открыть его!", ComponentBuilder.FormatRetention.EVENTS);
                cb.append("\n§5§m=====================================================", ComponentBuilder.FormatRetention.EVENTS);
                player.sendMessage(cb.create());
            }

            return;
        }
    }

    public void openSurvey(@NotNull Player player, @NotNull String surveyKey) {
        Survey survey = surveyMap.get(surveyKey);
        if (survey == null) return;

        openSurvey(player, survey);
    }

    public void openSurvey(@NotNull Player player, @NotNull Survey survey) {
        player.closeInventory();

        RunningSurvey alreadyRunning = getRunningSurvey(player);
        if (alreadyRunning != null) {
            alreadyRunning.showBook();
            return;
        }

        if (plugin.getSQLite().isCompleted(survey.getKey(), player.getUniqueId())) return;
        if (plugin.getSQLite().isOptOut(survey.getKey(), player.getUniqueId())) return;

        if (!new PreOpenSurveyEvent(survey, player).callEvent()) return;
        RunningSurvey runningSurvey = new RunningSurvey(player, survey);
        runningSurveysByPlayer.put(player.getUniqueId(), runningSurvey);

        runningSurvey.showBook();
    }

    public boolean endSurvey(@NotNull Player player, @NotNull String surveyKey) {
        player.closeInventory();

        RunningSurvey runningSurvey = getExactRunningSurvey(player, surveyKey);
        if (runningSurvey == null) return false;
        if (!runningSurvey.isCanBeEnded()) return false;

        JsonObject resultJson = runningSurvey.createResultJson();
        plugin.getSQLite().insertResultData(surveyKey, player.getUniqueId(), resultJson);

        runningSurveysByPlayer.remove(player.getUniqueId());
        return true;
    }

    public void setAnswer(@NotNull Player player, @NotNull String surveyKey, int questionId, @NotNull String optionKey, boolean value) {
        RunningSurvey runningSurvey = getExactRunningSurvey(player, surveyKey);
        if (runningSurvey == null) return;

        Question question = runningSurvey.getSurvey().getQuestions().get(questionId);
        if (question == null) return;

        Option option = question.getAnswer().getOptions().get(optionKey);
        if (option == null) return;

        runningSurvey.setAnswer(question, option, value);
    }

    public boolean optOut(@NotNull Player sender, @NotNull String surveyKey) {
        RunningSurvey runningSurvey = plugin.getSurveyManager().getRunningSurvey(sender);
        if (runningSurvey == null || !runningSurvey.getSurvey().getKey().equals(surveyKey)) return false;

        sender.closeInventory();
        runningSurveysByPlayer.remove(sender.getUniqueId());

        plugin.getSQLite().insertOptOut(surveyKey, sender.getUniqueId());
        return true;
    }

    public @Nullable RunningSurvey getRunningSurvey(@NotNull Player player) {
        return runningSurveysByPlayer.get(player.getUniqueId());
    }

    public @Nullable RunningSurvey getExactRunningSurvey(@NotNull Player player, @NotNull String surveyKey) {
        RunningSurvey runningSurvey = runningSurveysByPlayer.get(player.getUniqueId());
        return runningSurvey != null && runningSurvey.getSurvey().getKey().equals(surveyKey) ? runningSurvey : null;
    }

    public void removeRunningSurvey(@NotNull Player player) {
        runningSurveysByPlayer.remove(player.getUniqueId());
    }
}
