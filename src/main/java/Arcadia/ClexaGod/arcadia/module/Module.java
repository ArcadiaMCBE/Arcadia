package Arcadia.ClexaGod.arcadia.module;

import Arcadia.ClexaGod.arcadia.ArcadiaCore;

public interface Module {

    String getName();

    void onEnable(ArcadiaCore core);

    default void onDisable() {
    }
}
