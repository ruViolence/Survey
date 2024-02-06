package ru.violence.survey.model;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.violence.survey.model.answer.Answer;

@Data
public class Question {
    private final @NotNull String key;
    private final @NotNull String text;
    private final @NotNull Answer answer;

    public Question(@NotNull String key, @NotNull String text, @NotNull Answer answer) {
        this.key = key;
        this.text = text;
        this.answer = answer;
    }
}
