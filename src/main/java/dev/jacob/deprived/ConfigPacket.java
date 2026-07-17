package dev.jacob.deprived;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public final class ConfigPacket {
    public static final ResourceLocation ID=DeprivedMod.id("config_update");
    private ConfigPacket(){}

    public static FriendlyByteBuf create(DeprivedConfig c){
        FriendlyByteBuf b=PacketByteBufs.create();
        b.writeBoolean(c.enabled);b.writeBoolean(c.extremeStageCausesDamage);
        b.writeVarInt(c.eventPacingPercent);b.writeVarInt(c.audioDistanceMultiplier);
        b.writeVarInt(c.firstSymptomsAfterDays);b.writeVarInt(c.extremeAfterDays);
        b.writeBoolean(c.customAudioEnabled);b.writeVarInt(c.masterAudioVolumePercent);b.writeVarInt(c.footstepVolumePercent);
        b.writeVarInt(c.disturbanceVolumePercent);b.writeVarInt(c.voiceVolumePercent);b.writeVarInt(c.ambienceVolumePercent);
        return b;
    }

    public static void register(){ServerPlayNetworking.registerGlobalReceiver(ID,(server,player,handler,b,response)->{
        boolean enabled=b.readBoolean(),damage=b.readBoolean();int pacing=b.readVarInt(),distance=b.readVarInt(),first=b.readVarInt(),extreme=b.readVarInt();
        boolean audio=b.readBoolean();int master=b.readVarInt(),steps=b.readVarInt(),disturbance=b.readVarInt(),voices=b.readVarInt(),ambience=b.readVarInt();
        server.execute(()->{
            if(!player.hasPermissions(2))return;
            DeprivedConfig c=DeprivedMod.config();c.enabled=enabled;c.extremeStageCausesDamage=damage;c.eventPacingPercent=pacing;c.audioDistanceMultiplier=distance;c.firstSymptomsAfterDays=first;c.extremeAfterDays=extreme;
            c.customAudioEnabled=audio;c.masterAudioVolumePercent=master;c.footstepVolumePercent=steps;c.disturbanceVolumePercent=disturbance;c.voiceVolumePercent=voices;c.ambienceVolumePercent=ambience;c.sanitize();c.save();
        });
    });}
}
