package Arcadia.ClexaGod.arcadia.module.impl;

import Arcadia.ClexaGod.arcadia.ArcadiaCore;
import Arcadia.ClexaGod.arcadia.i18n.LangKeys;
import Arcadia.ClexaGod.arcadia.module.Module;
import org.allaymc.api.message.I18n;

public final class SystemModule implements Module {

    @Override
    public String getName() {
        return "system";
    }

    @Override
    public void onEnable(ArcadiaCore core) {
        core.getPluginLogger().info(I18n.get().tr(LangKeys.LOG_SYSTEM_READY, core.getConfigService().getCoreConfig().getOwner()));
    }

    @Override
    public void onDisable() {
    }
}
