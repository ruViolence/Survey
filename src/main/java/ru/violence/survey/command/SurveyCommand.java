package ru.violence.survey.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.violence.coreapi.common.api.util.CommonUtil;
import ru.violence.survey.SurveyPlugin;
import ru.violence.survey.model.RunningSurvey;

public class SurveyCommand implements CommandExecutor {
    private final SurveyPlugin plugin;

    public SurveyCommand(SurveyPlugin plugin) {
        this.plugin = plugin;
    }

    private static @Nullable Boolean parseBoolean(@NotNull String str) {
        if (str.equals("1") || str.equals("true")) return true;
        if (str.equals("0") || str.equals("false")) return false;
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            onPlayerCommand((Player) sender, command, label, args);
        }

        return true;
    }

    private void onPlayerCommand(Player sender, Command command, String label, String[] args) {
        if (args.length == 0) return;

        if (args[0].equals("open")) {
            if (args.length != 2) return;

            String surveyKey = args[1];

            plugin.getSurveyManager().openSurvey(sender, surveyKey);
            return;
        }

        if (args[0].equals("end")) {
            if (args.length != 2) return;

            String surveyKey = args[1];

            if (plugin.getSurveyManager().endSurvey(sender, surveyKey)) {
                sender.sendMessage("§aБлагодарим вас за участие в опросе!");
            }
            return;
        }

        if (args[0].equals("opt_in")) {
            if (args.length != 2) return;

            String surveyKey = args[1];

            RunningSurvey runningSurvey = plugin.getSurveyManager().getExactRunningSurvey(sender, surveyKey);
            if (runningSurvey == null) return;

            sender.closeInventory();
            runningSurvey.openQuestion(0);
            return;
        }

        if (args[0].equals("opt_out")) {
            if (args.length != 2) return;

            String surveyKey = args[1];

            if (plugin.getSurveyManager().optOut(sender, surveyKey)) {
                sender.sendMessage("§7Вы отказались от участия в опросе.");
            }
            return;
        }

        if (args[0].equals("set_answer")) {
            if (args.length != 5) return;

            String surveyKey = args[1];
            int questionId = CommonUtil.parseInt(args[2], -1);
            String optionKey = args[3];
            Boolean value = parseBoolean(args[4]);

            if (questionId == -1 || value == null) return;

            sender.closeInventory();
            plugin.getSurveyManager().setAnswer(sender, surveyKey, questionId, optionKey, value);
            return;
        }

        if (args[0].equals("go_back")) {
            if (args.length != 2) return;

            String surveyKey = args[1];

            RunningSurvey runningSurvey = plugin.getSurveyManager().getRunningSurvey(sender);
            if (runningSurvey == null || !runningSurvey.getSurvey().getKey().equals(surveyKey)) return;

            sender.closeInventory();
            runningSurvey.handleGoBack();
            return;
        }

        if (args[0].equals("go_forward")) {
            if (args.length != 2) return;

            String surveyKey = args[1];

            RunningSurvey runningSurvey = plugin.getSurveyManager().getRunningSurvey(sender);
            if (runningSurvey == null || !runningSurvey.getSurvey().getKey().equals(surveyKey)) return;

            sender.closeInventory();
            runningSurvey.handleGoForward();
            return;
        }
    }
}
