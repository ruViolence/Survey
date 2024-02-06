package ru.violence.survey.model;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Data
public class Survey {
    private final @NotNull String key;
    private final @NotNull String name;
    private final @NotNull String description;
    private final @NotNull List<Question> questions;

    public Survey(@NotNull String key, @NotNull String name, @NotNull String description, @NotNull List<Question> questions) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.questions = questions;
    }
}
