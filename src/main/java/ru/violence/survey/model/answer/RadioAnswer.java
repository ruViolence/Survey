package ru.violence.survey.model.answer;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class RadioAnswer extends Answer {
    public RadioAnswer(@NotNull List<Option> options) {
        super(options);
    }

    @Override
    public @NotNull Type getType() {
        return Type.RADIO;
    }
}
