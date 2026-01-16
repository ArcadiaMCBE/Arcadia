package Arcadia.ClexaGod.arcadia.i18n;

import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.message.I18n;
import org.allaymc.api.message.LangCode;
import org.allaymc.api.utils.TextFormat;

public final class MessageService {

    public String render(EntityPlayer player, String template, Object... args) {
        return render(player, true, template, args);
    }

    public String renderInline(EntityPlayer player, String template, Object... args) {
        return render(player, false, template, args);
    }

    private String render(EntityPlayer player, boolean includePrefix, String template, Object... args) {
        String message = translate(player, template, args);
        if (includePrefix) {
            message = translate(player, LangKeys.MESSAGE_PREFIX) + message;
        }
        return TextFormat.colorize(message);
    }

    private String translate(EntityPlayer player, String template, Object... args) {
        if (template == null) {
            return "";
        }
        LangCode lang = I18n.get().getDefaultLangCode();
        if (player != null && player.getController() != null && player.getController().getLoginData() != null) {
            lang = player.getController().getLoginData().getLangCode();
        }
        return I18n.get().tr(lang, template, args);
    }
}
