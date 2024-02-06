package ru.violence.survey.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.violence.survey.SurveyPlugin;

public class JoinListener implements Listener {
    private final SurveyPlugin plugin;

    public JoinListener(SurveyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            if (!player.isOnline()) return;
            plugin.getSurveyManager().notifyIfAvailable(player);
        }, 3 * 20);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getSurveyManager().removeRunningSurvey(player);
    }
}
