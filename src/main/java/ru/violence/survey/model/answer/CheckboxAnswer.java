package ru.violence.survey.model.answer;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CheckboxAnswer extends Answer {
    public CheckboxAnswer(@NotNull List<Option> options) {
        super(options);
    }

    @Override
    public @NotNull Type getType() {
        return Type.CHECKBOX;
    }
}
