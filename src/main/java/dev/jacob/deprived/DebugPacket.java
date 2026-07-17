package dev.jacob.deprived;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class DebugPacket {
    public static final ResourceLocation ID=DeprivedMod.id("debug_hud");
    private DebugPacket(){}

    public static void disable(ServerPlayer player){FriendlyByteBuf b=PacketByteBufs.create();b.writeBoolean(false);ServerPlayNetworking.send(player,ID,b);}

    public static void send(ServerPlayer player,DreadState s,int awakeTicks){
        FriendlyByteBuf b=PacketByteBufs.create();
        b.writeBoolean(true);b.writeVarInt(awakeTicks);b.writeVarInt(s.stage);b.writeVarInt(Math.max(0,s.cooldownTicks));
        b.writeUtf(s.profile.dominant());b.writeVarInt(s.profile.auditory());b.writeVarInt(s.profile.visual());b.writeVarInt(s.profile.paranoia());b.writeVarInt(s.profile.physical());
        b.writeVarInt(s.contextLight);b.writeBoolean(s.contextUnderground);b.writeBoolean(s.contextIsolated);b.writeBoolean(s.contextStorm);
        b.writeUtf(s.lastEvent);b.writeVarInt(s.secondsSinceLastEvent);b.writeVarInt(s.lastPoolSize);
        b.writeVarInt(s.footstepTicks);b.writeVarInt(s.disturbanceKind);b.writeVarInt(s.disturbanceTicks);b.writeVarInt(s.heartbeatTicks);
        ServerPlayNetworking.send(player,ID,b);
    }
}
