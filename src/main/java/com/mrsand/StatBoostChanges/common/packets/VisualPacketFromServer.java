package com.mrsand.StatBoostChanges.common.packets;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class VisualPacketFromServer implements IMessage {
	private int entityID;
	private boolean changeToOtherForm;
	
	public VisualPacketFromServer() {}
	public VisualPacketFromServer(int entityID, Boolean changeToOtherForm) {
        this.entityID = entityID;
        this.changeToOtherForm = changeToOtherForm;    
    }

	@Override
	public void fromBytes(ByteBuf buf) {
		// TODO Auto-generated method stub
		this.entityID = buf.readInt();
		this.changeToOtherForm = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(this.entityID); //This may be optional
		buf.writeBoolean(this.changeToOtherForm);
		
	}
	public int getPlayerID(){
		return this.entityID;
	}
	public boolean getBoolean(){
		return this.changeToOtherForm;
	}

}
