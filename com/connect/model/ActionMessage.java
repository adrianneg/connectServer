package com.connect.model;

import java.io.Serializable;


@SuppressWarnings("serial")
public class ActionMessage implements Serializable{

    private Object message;
    private String command;

     public ActionMessage(String com, Object mess) {
        this.message = mess;
        this.command = com;
    }

     ActionMessage() {
        this.message = "";
        this.command = "";
    }
    
    public Object getMessage() {
        return message;
    }


    public void setActionMessage(Object message) {
        this.message = message;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

}
