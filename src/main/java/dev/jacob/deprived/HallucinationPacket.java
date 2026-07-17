package dev.jacob.deprived;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class HallucinationPacket {
    public static final ResourceLocation ID=DeprivedMod.id("hallucination");
    public static final int CLEAR=-1, WATCHER=0, CREEPER=1, HUMAN=2;
    private HallucinationPacket(){}

    public static void spawn(ServerPlayer player,int kind,double x,double y,double z,int duration) {
        FriendlyByteBuf buffer=PacketByteBufs.create();
        buffer.writeVarInt(kind);buffer.writeDouble(x);buffer.writeDouble(y);buffer.writeDouble(z);buffer.writeVarInt(duration);
        ServerPlayNetworking.send(player,ID,buffer);
    }

    public static void clear(ServerPlayer player) { spawn(player,CLEAR,0,0,0,0); }
}
