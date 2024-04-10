package ru.violence.survey.model;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMaps;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.violence.coreapi.bukkit.api.util.BukkitUtil;
import ru.violence.coreapi.common.api.util.Check;
import ru.violence.coreapi.common.api.util.MathUtil;
import ru.violence.survey.model.answer.Answer;
import ru.violence.survey.model.answer.Option;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunningSurvey {
    private final @Getter @NotNull Player player;
    private final @Getter @NotNull Survey survey;
    private final @Getter @NotNull List<Answer> answers = new ArrayList<>();
    // Dynamic data
    private final Map<Question, Object2BooleanMap<Option>> selectedAnswers = new HashMap<>();
    private int currentQuestionId = -1; // -1 means introduction description

    public RunningSurvey(@NotNull Player player, @NotNull Survey survey) {
        this.player = player;
        this.survey = survey;
    }

    public boolean showBook() {
        if (!player.isOnline()) return false;

        BukkitUtil.getNMS().openBook(player, createBook());
        return true;
    }

    private @NotNull ItemStack createBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);

        BookMeta meta = (BookMeta) book.getItemMeta();

        meta.setDisplayName("survey");
        meta.setAuthor("server");

        if (currentQuestionId == -1) {
            for (BaseComponent[] page : createIntroduction()) meta.spigot().addPage(page);
        } else {
            for (BaseComponent[] page : createQuestion()) meta.spigot().addPage(page);
        }

        book.setItemMeta(meta);

        return book;
    }

    private @NotNull List<BaseComponent[]> createIntroduction() {
        ComponentBuilder cb = new ComponentBuilder("");

        {
            TextComponent t = new TextComponent("Опрос: " + survey.getName());
            t.setBold(true);
            cb.append(t);
        }

        cb.append("\n" + survey.getDescription() + "\n\n", ComponentBuilder.FormatRetention.NONE);

        {
            TextComponent continueButton = new TextComponent("[Принять]");
            continueButton.setColor(ChatColor.DARK_GREEN);

            continueButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§eКликни, чтобы принять участие в опросе!")));
            continueButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/survey opt_in " + survey.getKey()));

            cb.append(continueButton, ComponentBuilder.FormatRetention.NONE);
        }

        cb.append("\n\n", ComponentBuilder.FormatRetention.NONE);

        {
            TextComponent continueButton = new TextComponent("[Отказаться]");
            continueButton.setColor(ChatColor.DARK_RED);

            continueButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§eКликни, чтобы отказаться от участия в опросе!")));
            continueButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/survey opt_out " + survey.getKey()));

            cb.append(continueButton, ComponentBuilder.FormatRetention.NONE);
        }

        return Collections.singletonList(cb.create());
    }

    private @NotNull List<BaseComponent[]> createQuestion() {
        if (currentQuestionId < 0 || currentQuestionId >= survey.getQuestions().size()) return Collections.emptyList();

        Question question = getCurrentQuestion();

        ComponentBuilder cb = new ComponentBuilder("");

        {
            TextComponent t = new TextComponent("   Вопрос " + (currentQuestionId + 1) + "/" + survey.getQuestions().size());
            t.setBold(true);
            cb.append(t, ComponentBuilder.FormatRetention.NONE);
        }

        cb.append("\n" + question.getText() + "\n\n", ComponentBuilder.FormatRetention.NONE);

        Answer answer = question.getAnswer();
        Answer.Type answerType = answer.getType();
        List<Option> answerOptions = new ArrayList<>(answer.getOptions().values());
        if (answerType == Answer.Type.RADIO) {
            for (int i = 0; i < answerOptions.size(); i++) {
                if (i != 0) cb.append("\n", ComponentBuilder.FormatRetention.NONE);
                Option option = answerOptions.get(i);
                TextComponent t = new TextComponent((isSelected(question, option) ? "✔ " : "✕ ") + option.getText());
                t.setColor(isSelected(question, option) ? ChatColor.DARK_GREEN : ChatColor.DARK_GRAY);
                t.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§eКликни, чтобы выбрать этот вариант!")));
                t.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/survey set_answer " + survey.getKey() + " " + currentQuestionId + " " + option.getKey() + " " + !isSelected(question, option)));
                cb.append(t, ComponentBuilder.FormatRetention.NONE);
            }
        } else if (answerType == Answer.Type.CHECKBOX) {
            for (int i = 0; i < answerOptions.size(); i++) {
                if (i != 0) cb.append("\n", ComponentBuilder.FormatRetention.NONE);
                Option option = answerOptions.get(i);
                TextComponent t = new TextComponent((isSelected(question, option) ? "◉ " : "◌ ") + option.getText());
                t.setColor(isSelected(question, option) ? ChatColor.DARK_GREEN : ChatColor.DARK_GRAY);
                t.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§eКликни, чтобы отметить этот вариант!")));
                t.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/survey set_answer " + survey.getKey() + " " + currentQuestionId + " " + option.getKey() + " " + !isSelected(question, option)));
                cb.append(t, ComponentBuilder.FormatRetention.NONE);
            }
        } else {
            throw new IllegalStateException("Unexpected value: " + answer.getType());
        }

        cb.append("\n\n", ComponentBuilder.FormatRetention.NONE);

        {
            TextComponent t = new TextComponent("[Назад]");
            t.setColor(ChatColor.BLUE);
            t.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§eКликни, чтобы вернуться к предыдущему вопросу!")));
            t.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/survey go_back " + survey.getKey()));
            cb.append(t, ComponentBuilder.FormatRetention.NONE);
        }

        if (currentQuestionId + 1 < survey.getQuestions().size()) {
            cb.append("    ", ComponentBuilder.FormatRetention.NONE);
            TextComponent continueButton = new TextComponent("[Далее]");
            if (isCanGoToQuestion(currentQuestionId + 1)) {
                continueButton.setColor(ChatColor.BLUE);
                continueButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§eКликни, чтобы перейти к следующему вопросу!")));
                continueButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/survey go_forward " + survey.getKey()));
            } else {
                continueButton.setColor(ChatColor.GRAY);
                continueButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§cВы должны выбрать хотя бы один вариант ответа!")));
            }
            cb.append(continueButton, ComponentBuilder.FormatRetention.NONE);
        } else {
            cb.append("\n\n", ComponentBuilder.FormatRetention.NONE);
            TextComponent endButton = new TextComponent("[Завершить]");
            if (getCurrentQuestion().getAnswer().getType() == Answer.Type.CHECKBOX || isHasSelectedOptions(currentQuestionId)) {
                endButton.setColor(ChatColor.DARK_PURPLE);
                endButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§eКликни, чтобы завершить опрос!")));
                endButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/survey end " + survey.getKey()));
            } else {
                endButton.setColor(ChatColor.GRAY);
                endButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§cВы должны выбрать хотя бы один вариант ответа!")));
            }
            cb.append(endButton, ComponentBuilder.FormatRetention.NONE);
        }

        return Collections.singletonList(cb.create());
    }

    private boolean isCanGoToQuestion(int questionId) {
        Question question = getQuestionSafely(questionId);
        if (question == null) return false;
        return getCurrentQuestion().getAnswer().getType() == Answer.Type.CHECKBOX || isHasSelectedOptions(questionId - 1);
    }

    private boolean isHasSelectedOptions(int questionId) {
        Question question = getQuestionSafely(questionId);
        if (question == null) return false;

        for (Boolean value : selectedAnswers.getOrDefault(question, Object2BooleanMaps.emptyMap()).values()) {
            if (value) return true;
        }

        return false;
    }

    public int getCurrentQuestionId() {
        return currentQuestionId;
    }

    public @NotNull Question getCurrentQuestion() {
        return Check.notNull(getQuestionSafely(currentQuestionId));
    }

    public void setAnswer(@NotNull Question question, @NotNull Option option, boolean selected) {
        Object2BooleanMap<Option> selectedOptions = selectedAnswers
                .computeIfAbsent(question, k -> new Object2BooleanOpenHashMap<>());

        if (question.getAnswer().getType() == Answer.Type.RADIO) {
            selectedOptions.clear();
        }

        if (selected) {
            selectedOptions.put(option, true);
        } else {
            selectedOptions.removeBoolean(option);
        }

        showBook();
    }

    public boolean isSelected(@NotNull Question question, @NotNull Option option) {
        return selectedAnswers
                .getOrDefault(question, Object2BooleanMaps.emptyMap())
                .getOrDefault(option, false);
    }

    public void openQuestion(int questionId) {
        this.currentQuestionId = MathUtil.clamp(questionId, -1, survey.getQuestions().size() - 1);
        showBook();
    }

    public void handleGoBack() {
        openQuestion(currentQuestionId - 1);
    }

    public void handleGoForward() {
        if (isCanGoToQuestion(currentQuestionId + 1)) {
            openQuestion(currentQuestionId + 1);
        } else {
            showBook();
        }
    }

    private @Nullable Question getQuestionSafely(int questionId) {
        if (questionId < 0 || questionId >= survey.getQuestions().size()) return null;
        return survey.getQuestions().get(questionId);
    }

    public @NotNull JsonObject createResultJson() {
        JsonObject json = new JsonObject();
        json.addProperty("survey", survey.getKey());
        json.addProperty("player_uuid", player.getUniqueId().toString());
        json.addProperty("timestamp", System.currentTimeMillis());

        {
            JsonObject answersObj = new JsonObject();
            for (Map.Entry<Question, Object2BooleanMap<Option>> entry : selectedAnswers.entrySet()) {
                Question question = entry.getKey();
                JsonObject answerObj = new JsonObject();
                for (Map.Entry<Option, Boolean> option : entry.getValue().entrySet()) {
                    answerObj.addProperty(option.getKey().getKey(), option.getValue());
                }
                answersObj.add(question.getKey(), answerObj);
            }
            json.add("answers", answersObj);
        }

        return json;
    }

    public boolean isCanBeEnded() {
        return currentQuestionId == survey.getQuestions().size() - 1;
    }
}
