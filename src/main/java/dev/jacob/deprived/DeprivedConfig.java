package dev.jacob.deprived;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DeprivedConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("deprived.json");

    public boolean enabled = true;
    public boolean watcherSightings = true;
    public boolean intenseVisuals = true;
    public boolean flashingEffects = true;
    public boolean extremeStageCausesDamage = true;
    public boolean filmGrain = true;
    public boolean peripheralHallucinations = true;
    public boolean worldHallucinations = true;
    public boolean analogVisuals = true;
    public boolean exposureBreathing = true;
    public boolean rollingInterference = true;
    public boolean chromaticBleed = true;
    public boolean trackingSlips = true;
    public boolean filmBurns = true;
    public boolean phosphorTrails = true;
    public int grainMaximumOpacity = 22;
    public int cornerShadowMaximumOpacity = 42;
    public int analogEffectIntensity = 80;
    public int eventPacingPercent = 100;
    public int firstSymptomsAfterDays = 1;
    public int extremeAfterDays = 7;
    public int audioDistanceMultiplier = 100;
    public boolean customAudioEnabled = true;
    public int masterAudioVolumePercent = 100;
    public int footstepVolumePercent = 100;
    public int disturbanceVolumePercent = 100;
    public int voiceVolumePercent = 100;
    public int ambienceVolumePercent = 100;

    public static DeprivedConfig load() {
        try {
            if (Files.exists(PATH)) {
                DeprivedConfig config = GSON.fromJson(Files.readString(PATH), DeprivedConfig.class);
                config.sanitize();
                config.save();
                return config;
            }
        } catch (Exception exception) {
            DeprivedMod.LOGGER.warn("Could not read {}; using defaults", PATH, exception);
        }
        DeprivedConfig config = new DeprivedConfig();
        config.save();
        return config;
    }

    public void save() {
        sanitize();
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(this));
        } catch (IOException exception) {
            DeprivedMod.LOGGER.warn("Could not write {}", PATH, exception);
        }
    }

    public void sanitize() {
        firstSymptomsAfterDays = Math.max(0, Math.min(3, firstSymptomsAfterDays));
        extremeAfterDays = Math.max(firstSymptomsAfterDays + 4, Math.min(20, extremeAfterDays));
        audioDistanceMultiplier = Math.max(50, Math.min(250, audioDistanceMultiplier));
        grainMaximumOpacity = Math.max(0, Math.min(30, grainMaximumOpacity));
        cornerShadowMaximumOpacity = Math.max(0, Math.min(60, cornerShadowMaximumOpacity));
        analogEffectIntensity = Math.max(0, Math.min(100, analogEffectIntensity));
        eventPacingPercent = Math.max(50, Math.min(250, eventPacingPercent));
        masterAudioVolumePercent = clampVolume(masterAudioVolumePercent);
        footstepVolumePercent = clampVolume(footstepVolumePercent);
        disturbanceVolumePercent = clampVolume(disturbanceVolumePercent);
        voiceVolumePercent = clampVolume(voiceVolumePercent);
        ambienceVolumePercent = clampVolume(ambienceVolumePercent);
    }
    private static int clampVolume(int value){return Math.max(0,Math.min(200,value));}
}
