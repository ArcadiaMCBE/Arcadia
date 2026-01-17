package Arcadia.ClexaGod.arcadia.command;

import Arcadia.ClexaGod.arcadia.ArcadiaCore;
import Arcadia.ClexaGod.arcadia.health.HealthReportService;
import Arcadia.ClexaGod.arcadia.i18n.I18nUtil;
import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import org.allaymc.api.command.Command;
import org.allaymc.api.command.CommandResult;
import org.allaymc.api.command.CommandSender;
import org.allaymc.api.command.tree.CommandContext;
import org.allaymc.api.command.tree.CommandTree;
import org.allaymc.api.server.Server;

import java.util.List;

public final class ArcadiaCommand extends Command {

    private final HealthReportService healthReportService;

    public ArcadiaCommand(ArcadiaCore core) {
        super("arcadia", LangKeys.COMMAND_ARCADIA_DESCRIPTION, "arcadia.command");
        aliases.add("acore");
        aliases.add("arc");
        this.healthReportService = new HealthReportService(core);
    }

    @Override
    public void prepareCommandTree(CommandTree tree) {
        tree.getRoot()
                .key("health")
                    .exec(ctx -> handleHealth(ctx, false, false))
                    .key("full")
                        .exec(ctx -> handleHealth(ctx, true, false))
                    .up()
                    .key("check")
                        .exec(ctx -> handleHealth(ctx, true, true))
                    .up();
    }

    @Override
    public boolean isDebugCommand() {
        return true;
    }

    private CommandResult handleHealth(CommandContext ctx, boolean full, boolean check) {
        CommandSender sender = ctx.getSender();
        if (check) {
            sender.sendMessage(I18nUtil.tr(sender, LangKeys.COMMAND_ARCADIA_HEALTH_RUNNING));
            Server.getInstance().getScheduler().runLaterAsync(
                    ArcadiaCore.getInstance(),
                    () -> sendReport(sender, full, true)
            );
            return ctx.success();
        }
        sendReport(sender, full, false);
        return ctx.success();
    }

    private void sendReport(CommandSender sender, boolean full, boolean check) {
        List<String> lines = healthReportService.buildReport(sender, full, check);
        for (String line : lines) {
            sender.sendMessage(line);
        }
    }
}
