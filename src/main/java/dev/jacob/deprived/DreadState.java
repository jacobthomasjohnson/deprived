package dev.jacob.deprived;

import java.util.UUID;

final class DreadState {
    final SymptomProfile profile;
    int stage;
    int cooldownTicks;
    int footstepTicks;
    int footstepIndex;
    double footstepAngle;
    double footstepX;
    double footstepY;
    double footstepZ;
    double footstepDx;
    double footstepDz;
    int heartbeatTicks;
    int disturbanceTicks;
    int disturbanceKind;
    double disturbanceAngle;
    double disturbanceX;
    double disturbanceY;
    double disturbanceZ;
    int lastRestTicks;
    int pressureSeconds;
    boolean debugHud;
    String lastEvent="none";
    int secondsSinceLastEvent;
    int lastPoolSize;
    int contextLight=15;
    boolean contextUnderground;
    boolean contextIsolated;
    boolean contextStorm;

    DreadState(UUID id) { profile=SymptomProfile.from(id); }

    void clearHallucinations() {
        footstepTicks = heartbeatTicks = disturbanceTicks = 0;
        footstepIndex = 0;
        pressureSeconds = 0;
    }
}
