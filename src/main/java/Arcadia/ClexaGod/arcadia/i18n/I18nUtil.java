package Arcadia.ClexaGod.arcadia.i18n;

import lombok.experimental.UtilityClass;
import org.allaymc.api.command.CommandSender;
import org.allaymc.api.entity.interfaces.EntityPlayer;
import org.allaymc.api.message.I18n;
import org.allaymc.api.message.LangCode;
import org.allaymc.api.player.Player;

@UtilityClass
public class I18nUtil {

    public static LangCode getLangCode(CommandSender sender) {
        if (sender instanceof EntityPlayer player && player.isActualPlayer()) {
            Player controller = player.getController();
            if (controller != null && controller.getLoginData() != null) {
                return controller.getLoginData().getLangCode();
            }
        }
        return I18n.get().getDefaultLangCode();
    }

    public static String tr(CommandSender sender, String key, Object... args) {
        return I18n.get().tr(getLangCode(sender), key, args);
    }
}
