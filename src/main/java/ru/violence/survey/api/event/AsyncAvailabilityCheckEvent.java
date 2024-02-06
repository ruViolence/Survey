package ru.violence.survey.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import ru.violence.survey.model.Survey;

public class AsyncAvailabilityCheckEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Survey survey;
    private final Player player;
    private boolean cancel;

    public AsyncAvailabilityCheckEvent(@NotNull Survey survey, @NotNull Player player) {
        super(true);
        this.survey = survey;
        this.player = player;
    }

    public @NotNull Survey getSurvey() {
        return survey;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
