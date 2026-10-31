package com.mrsand.StatBoostChanges.common.packets;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class PacketSyncTarget implements IMessage {
    private int entityID;

    // Required empty constructor
    public PacketSyncTarget() {}

    public PacketSyncTarget(int entityID) {
        this.entityID = entityID;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityID = buf.readInt(); // Read the ID back out
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityID); // Write the ID into the packet
    }

    // Getter for the handler
    public int getEntityID() { return entityID; }
}