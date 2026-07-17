package dev.jacob.deprived;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class VisualPacket {
    public static final ResourceLocation ID = DeprivedMod.id("visual");
    public static final int SYNC = 0, BLINK = 1, FLASH = 2, STATIC = 3, PULSE = 4, SHAKE = 5, FRACTURE = 6;

    private VisualPacket() {}

    public static void send(ServerPlayer player, int type, int duration, float strength, int color) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(type);
        buffer.writeVarInt(duration);
        buffer.writeFloat(strength);
        buffer.writeInt(color);
        ServerPlayNetworking.send(player, ID, buffer);
    }

    public static void sync(ServerPlayer player, int stage) {
        send(player, SYNC, 0, stage, 0);
    }
}
