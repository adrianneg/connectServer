package com.connect.model;

import java.io.Serializable;
import java.util.Arrays;

public class ChatMessage implements Serializable{

	private static final long serialVersionUID = 1L;
	// The different types of message sent by the Client
	// MESSAGE an ordinary message
	// LOGOUT to disconnect from the Server

	public static final int MESSAGE = 1;
	public static final int LOGOUT = 2;
	public static final int GROUP = 3;
	public static final int AVAILABLE = 4;
	public static final int IMAGEMESSAGE = 5;
	public static final int REQUEST = 6;
        public static final int REQUESTINFO = 7;
        public static final int REQUESTUPDATE = 8;
        public static final int DELETEREQUEST = 9;
        public static final int CREATEGROUP = 10;
        public static final int LISTGROUPREQUEST = 11;
        public static final int LISTGROUPS = 12;
        public static final int DELETEMEMBERFROMGROUP = 13; 
        public static final int ADDMEMBERTOGROUP = 14;
        public static final int DELETEGROUP = 15;
        public static final int UPDATEMEMBERTOGROUP = 16;
        public static final int DELETEMEMBERGROUPREQUEST = 17;
        public static final int LEAVEGROUP = 18;
        public static final int GETFRIENDS = 19;
        public static final int BROADCAST = 20;
        public static final int LISTMYGROUPS = 21;
        public static final int DELETEFRIEND = 22;
        public static final int AUDIOCHATREQUEST = 23;
        public static final int VIDEOCHATREQUEST = 24;
        public static final int AUDIOCHAT = 25;
        public static final int VIDEOCHAT = 26;
        public static final int STOPAUDIOCHAT = 27;
        public static final int STOPVIDEOCHAT = 28;
        public static final int IMAGEBROADCAST = 29;
        public static final int REGISTER = 30;
        public static final int BUSY = 31;
         public static final int GETMESSAGES = 32;
        public static final int GETGROUPMESSAGES = 33;
	private int type;
	private Object message;
	private String[] receiver;
        
	// constructor
	public ChatMessage(int type, Object message) {
		this.type = type;
		this.message = message;
	}
        public ChatMessage(int type, Object message,String[] receiver) {
		this.type = type;
		this.message = message;
        this.receiver = receiver;
	}
	
	// getters
	public int getType() {
		return type;
	}
	public Object getMessage() {
		return message;
	}

    public String[] getReceiver() {
        return receiver;
    }
	@Override
	public String toString() {
		final int maxLen = 10;
		return "ChatMessage [type=" + type + ", message=" + message + ", receiver="
				+ (receiver != null ? Arrays.asList(receiver).subList(0, Math.min(receiver.length, maxLen)) : null)
				+ ", getType()=" + getType() + ", getMessage()=" + getMessage() + ", getReceiver()="
				+ (getReceiver() != null
						? Arrays.asList(getReceiver()).subList(0, Math.min(getReceiver().length, maxLen)) : null)
				+ ", getClass()=" + getClass() + ", hashCode()=" + hashCode() + ", toString()=" + super.toString()
				+ "]";
	}
        

}
