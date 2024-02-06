package ru.violence.survey.model.answer;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class Option {
    private final @NotNull String key;
    private final @NotNull String text;

    public Option(@NotNull String key, @NotNull String text) {
        this.key = key;
        this.text = text;
    }
}
