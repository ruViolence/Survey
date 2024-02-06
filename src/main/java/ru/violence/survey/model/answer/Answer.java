package ru.violence.survey.model.answer;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Answer {
    private final @NotNull Map<String, Option> options;

    protected Answer(@NotNull List<Option> options) {
        HashMap<String, Option> map = new HashMap<>();
        for (Option option : options) map.put(option.getKey(), option);
        this.options = Collections.unmodifiableMap(map);
    }

    public abstract @NotNull Type getType();

    public final @NotNull Map<String, Option> getOptions() {
        return options;
    }

    public enum Type {
        RADIO, CHECKBOX
    }
}
