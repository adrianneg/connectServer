package com.connect.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;

import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientThread extends Thread {

    private Socket client;
    private ObjectOutputStream clientWriter;
    private ObjectInputStream clientReader ;
    private int clientUniqueId;
    private String clientUsername;
    private ChatMessage clientMessage;
    boolean loopForever = true;
    boolean busy = false;

    public ClientThread(Socket s){

        //socket to read and write
        this.client = s;
        //the server keeps count of the number of clients, but the client counts itself not the server
        

        try{
                clientWriter = new ObjectOutputStream(this.client.getOutputStream());
                clientReader= new ObjectInputStream(this.client.getInputStream());
                //first thing the client sends to the server is a request of a user name

                //try to login in user
        try {
                //register or login
                ActionMessage actionObj =  ((ActionMessage)clientReader.readObject());
                
                
                if(actionObj.getCommand().equalsIgnoreCase("register")){
                    //register user
                   // System.out.println(actionObj.getMessage());
                    int status = registerUser( ((ChatMessage)actionObj.getMessage()).getMessage() );
                    
                    if(status == 1){
                         clientWriter.writeObject(new String[]{"registered","User successfully registered"});
                    }else if(status == 3){
                        clientWriter.writeObject(new String[]{"already","Username already taken, choose another"});
                    }else if(status == 2){
                         clientWriter.writeObject(new String[]{"serverdown","Server not ready to accept connection, try again later"});
                    }else{
                        clientWriter.writeObject(new String[]{"error","User registration unsuccessful"});
                    }
                    return;
                }
                
                //BEYOND HERE IS FROM LOGIN
                
                
                //before you go to the database to compare username and md5 password -- 
                //check if the username is already assigned to a client
                String username = ((String[])actionObj.getMessage())[0];
                
                Collection<ClientThread> temp;
                temp = Server.connectedSockets;
                for (ClientThread client : temp) {
                    if(client.getClient().isConnected()){
                        if(client.getClientUsername()!=null){
                            if(client.getClientUsername().equalsIgnoreCase(username)){
                                //user already logged in
                                loopForever= false;//stop thread and close connection in @Override start()
                                clientWriter.writeObject(new String[]{"already","User already connected"});//user name password combination not in database
                                return;//do not execute beyond this point
                            }
                        }
                    }
                }

                //attempting to validate because user is no logged in
                System.out.println("Attempting to validate -- signing in");
                int serverResponse =  validateUser(actionObj);
                if( serverResponse == 0 ){
                        loopForever= false;//stop thread and close connection in @Override start()
                        clientWriter.writeObject(new String[]{"error","User not connected"});//user name password combination not in database
                }else if(serverResponse == 1){
                    //Success checked out on database side
                    clientWriter.writeObject(new String[]{"success","User successfully connected"});
                    //then tell the client which FRIEND is connected
                    //the friends of this client
                    //System.out.println(this.clientUsername);
                    //String[] connectedFriends = getConnectedFriends();
                    //clientWriter.writeObject(new ChatMessage(ChatMessage.AVAILABLE, connectedFriends));
                    Server.connectedSockets.add(this);
                    this.clientUniqueId = Server.uniqueClient++;
                    Server.broadcast();
                    //start();
                }else if(serverResponse == 2){
                    loopForever= false;//stop thread and close connection in @Override start()
                    clientWriter.writeObject(new String[]{"serverdown","Server not ready to accept connection, try again later"});//user name password combination not in database
                }

        } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
        }

        }catch(IOException ioe){
                ioe.printStackTrace();
        }
    }

    private int validateUser(ActionMessage actionObj) {
        //read the user name
        //USERNAME WILL BE  SENT TO SERVER FROM THE SERVER IF AUTHENTICATED

        String action =  actionObj.getCommand(),
         username = ((String[])actionObj.getMessage())[0],
         password = ((String[])actionObj.getMessage())[1];
         DbConnect con = new DbConnect();
         
        if(action.equalsIgnoreCase("login")){
            //check if the username and password match on database
            int valid =con.validateUser(username, password);
            if(valid == 1){//successfully logged in
                clientUsername = username;
                //clientWriter.writeObject(clientUsername + " just connected");
                //HERE I WANT TO BROADCAST TO ALL USERS WHICH ARE FRIENDS WITH THIS USER
                System.err.println(clientUsername + " just connected");

                return 1;
            } else if(valid == 2){//if 2 then server database couldn't be connected to
                return 2;
            }else{
                return 0;//if 0 user password combination not found
            }
        }
        return 0;
    }

    @Override
    public synchronized void run(){
    //run forever(well) waiting for client to interact with the server

    while(loopForever){
            //continue accepting ChatMessages from server
        try{
            //System.out.print("isClosed" + client.isClosed() + " isconnected "+ client.isConnected());
            ChatMessage messageObj = ((ChatMessage)clientReader.readObject());
            //System.out.println(messageObj);
            switch(messageObj.getType()){
                
                case ChatMessage.MESSAGE:
                    sendMessage(messageObj);
                    break;
                case ChatMessage.IMAGEMESSAGE:
                    sendImageMessage(messageObj);
                    break;
                case ChatMessage.GROUP:
                    broadcast(messageObj);
                    break;
                case ChatMessage.BROADCAST:
                    broadcast(messageObj);
                    break;
                case ChatMessage.IMAGEBROADCAST:
                    broadcastImage(messageObj);
                    break;
                case ChatMessage.AVAILABLE:
                    getOnlineUser(messageObj);
                    break;
                case ChatMessage.REQUEST:
                    friendRequest(messageObj);
                    break;
                case ChatMessage.REQUESTINFO:
                    getFriendsRequestList();
                    break;
                case ChatMessage.REQUESTUPDATE:
                    updateFriendShipStatus(messageObj.getMessage().toString());
                    break;
                case ChatMessage.DELETEREQUEST:
                    deleteFriendShipStatus(messageObj.getMessage().toString());
                    break;
                case ChatMessage.CREATEGROUP:
                    createGroup(messageObj);
                    break;
                case ChatMessage.LISTGROUPREQUEST:
                    listOfGroupRequest(messageObj.getMessage().toString());
                    break;
                case ChatMessage.LISTGROUPS:
                    listMyGroups(messageObj.getMessage().toString(),ChatMessage.LISTGROUPS);
                    break;
                case ChatMessage.LISTMYGROUPS:
                    listMyGroups(messageObj.getMessage().toString(),ChatMessage.LISTMYGROUPS);
                    break;
                case ChatMessage.DELETEMEMBERFROMGROUP:
                    deleteMemberFromGroup(messageObj);
                    break;
                case ChatMessage.ADDMEMBERTOGROUP:
                    addMemberToGroup(messageObj);
                    break;
                case ChatMessage.DELETEGROUP:
                    deleteMyGroup(messageObj);
                    break;
                case ChatMessage.UPDATEMEMBERTOGROUP:
                    updateMemberToGroup(messageObj);
                    break;
                case ChatMessage.DELETEMEMBERGROUPREQUEST:
                    deleteMemberGroupRequest(messageObj);
                    break;
                case ChatMessage.LEAVEGROUP:
                    leaveGroup(messageObj);
                    break;
                case ChatMessage.GETFRIENDS:
                    getFriends(messageObj.getMessage().toString());
                    break;
                case ChatMessage.DELETEFRIEND:
                    deleteFriend(messageObj);
                    break;
                case ChatMessage.AUDIOCHATREQUEST:
                    sendAudioChatReqest(messageObj);
                    break;
                case ChatMessage.VIDEOCHATREQUEST:
                    sendVideoChatReqest(messageObj);
                    break;
                case ChatMessage.AUDIOCHAT:
                    startAudioChat(messageObj);
                    break;
                case ChatMessage.VIDEOCHAT:
                    startVideoChat(messageObj);
                    break;
                case ChatMessage.STOPAUDIOCHAT:
                   //System.out.println(messageObj.getMessage().toString());
                    stopAudioChat(messageObj.getMessage().toString());
                    break;
                case ChatMessage.STOPVIDEOCHAT:
                    stopVideoChat(messageObj.getMessage().toString());
                    break;
                case ChatMessage.GETMESSAGES:
                   //System.out.println(messageObj.getMessage().toString());
                    getMessages(messageObj);
                    break;
                case ChatMessage.GETGROUPMESSAGES:
                    getGroupMessages(messageObj);
                    break;    
                case ChatMessage.LOGOUT:
                    //request to log out -- stop infinite loop
                   
                    clientWriter.writeObject(new ChatMessage(ChatMessage.LOGOUT,""));
                    
                    //throw exception so I can perform the action there... mhmm
                    throw new IOException();
                   
            }
//            System.out.println("I don't know what's up");
//            loopForever = false;
            }catch(IOException | ClassNotFoundException ioe){
                //ioe.printStackTrace();
                //client disconnected
                try {
                        this.client.close();
                } catch (IOException e1) {
                        System.out.println("Unable to close socket of "+this.clientUsername);
                        e1.printStackTrace();
                }
                //remove client from list of connected client stored on server
                removeClientFromServerList();
                
                Server.broadcast();

                System.out.println(clientUsername + " disconnected");
                loopForever = false;
            }
        }//end of while loop
            closeStream();

    }

    private void friendRequest(ChatMessage messageObj) {
        String requester =  ((String[])messageObj.getMessage())[0];//current user
        String requestName =  ((String[])messageObj.getMessage())[1];//user to be friended
        DbConnect dbConnection = new DbConnect();
        //if this client is disconnected
        if(!client.isConnected()){
                //remove client from list of connected clients
                Server.connectedSockets.remove(this);
                System.out.println(clientUsername +" disconnected");
        }else{
            try{
                //query the database for the username
                //return success --  added to friends database, unconfirmed
                //return fail is user unavailable
                System.out.println("requester "+requester+ " clientUsername"+clientUsername);
                if(requestName.equalsIgnoreCase(clientUsername)){
                        clientWriter.writeObject(new ChatMessage(ChatMessage.REQUEST,new String[]{"error", "Sorry wise guy, can't be friends with yourself"}));
                        System.out.println("Sorry wise guy, can't be friends with yourself");
                        return;
                }

                if(!dbConnection.isAUser(requestName)){
                        clientWriter.writeObject(new ChatMessage(ChatMessage.REQUEST,new String[]{"error", "Oops...No such user available"}));
                        System.out.println("Oops...No such user available");
                        return;
                }

                    //but first see if they are friends already
                    ArrayList<String> temp = dbConnection.getFriendsOfUsername(requester);
                    if(temp != null){
                            if(temp.contains(requestName)){
                                clientWriter.writeObject(new ChatMessage(ChatMessage.REQUEST,new String[]{"error", "Oops...We are already friends, carry on"}));
                                System.out.println("Oops...We are already friends, carry on");
                                return;
                            }else{
                                temp=  dbConnection.getAwaitingRequests(requester) ;
                                if(temp != null){
                                    if(temp.contains(requestName)){
                                        clientWriter.writeObject(new ChatMessage(ChatMessage.REQUEST,new String[]{"error", "Oops...A request was already made between you and "+requestName+" \nCheck your request list or wait to be accepted"}));
                                        System.out.println("Oops...You already sent a friend request to "+requestName+" ,sorry it hasn't been accepted yet");
                                        return;
                                    }
                                }
                            }
                        }
                    if(dbConnection.registerFriendRequest((new String[]{requester, requestName}))){
                            clientWriter.writeObject(new ChatMessage(ChatMessage.REQUEST,new String[]{"success", "We are on our way to be friends, I'll tell "+requestName+" to accept your request" }));
                            System.out.println("We are on our way to be friends");
                    }else{
                            clientWriter.writeObject(new ChatMessage(ChatMessage.REQUEST,new String[]{"error", "Can't be friends right now..try again later"}));
                            System.out.println("Can't be friends right now");
                    }
            } catch(IOException ioe){
                    System.out.println("Client " + clientUsername + " writer unavaible");
            }
        }
    }

    private void sendMessage(ChatMessage messageObj) {

       // SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");

        //normal one to one messages only have one index
        String messageToUser = messageObj.getReceiver()[0];
        ClientThread receiver = Server.getClientWithUsername(messageToUser);

        //if this client is disconnected
        if(!client.isConnected()){
                //remove client from list of connected clients
                Server.connectedSockets.remove(this);
                System.out.println(clientUsername +" disconnected");
        }else{
                //try to write to the output stream of this client
            try{
                String message = clientUsername +" : " + messageObj.getMessage().toString();
                //save to database --  doesn't matter if it's save or not... there is nothing I can do if it's not
                new DbConnect().addMessage(clientUsername, messageToUser, messageObj.getMessage().toString());
                
                if( receiver.getClient().isConnected()  ){
                    receiver.getClientWriter().writeObject(new ChatMessage(ChatMessage.MESSAGE, message));
                }else{
                    //pig backs on ChatMessage.BUSY route because I'm took lazy to create another
                    this.getClientWriter().writeObject(new ChatMessage(ChatMessage.BUSY, new String[]{"not","User not available for chat"}));
                    //need to send to user that client is not available
                }
                    
                //write to this specific clientThread
                //Server will have a GUI that logs all messages between users
                //just log to console for now

                System.out.println(message );
            }catch(IOException ioe){
                System.out.println("Error sending message to " + receiver.clientUsername);
            }
        }

    }

    private void sendImageMessage(ChatMessage messageObj) {

        //normal one to one messages only have one index
        String messageToUser = messageObj.getReceiver()[0];
        ClientThread receiver = Server.getClientWithUsername(messageToUser);

        //if this client is disconnected
        if(!client.isConnected()){
                //remove client from list of connected clients
                Server.connectedSockets.remove(this);
                System.out.println(clientUsername +" disconnected");
        }else{
            //try to write to the output stream of this client
            try{
                //write to this specific clientThread
                Object message = messageObj.getMessage();
                if( receiver.getClient().isConnected()  ){
                    receiver.getClientWriter().writeObject(new ChatMessage(ChatMessage.IMAGEMESSAGE, message));
                }else{
                    //pig backs on ChatMessage.BUSY route because I'm took lazy to create another
                    this.getClientWriter().writeObject(new ChatMessage(ChatMessage.BUSY, new String[]{"not","User not available for chat"}));
                    //need to send to user that client is not available
                }
            }catch(IOException ioe){
                     System.out.println("Error sending message to " + receiver.clientUsername);
            }
        }
    }

    private synchronized void broadcast(ChatMessage messageObj) {
        
       // SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
        //normal one to one messages only have one index
        //the groupname is returned not a array of users in the group
        String groupName = messageObj.getReceiver()[0];
        ArrayList<String> allFriends = new DbConnect().getMembersOfGroup(clientUsername, groupName);
        String[] stockArr = new String[allFriends.size()];
        String[]messageToUsers = allFriends.toArray(stockArr);
         
        System.out.println("Number of connected cliented "+Server.connectedSockets.size());

        //if this client is disconnected
        if(!client.isConnected()){
                //remove client from list of connected clients
                Server.connectedSockets.remove(this);
                System.out.println(clientUsername +" disconnected");
        }else{
                //try to write to the output stream of this client
                try{
                    //write to this specific clientThread
                    for (String username : messageToUsers) {
                        ClientThread receiver = Server.getClientWithUsername(username);
                        if(receiver != null){
                            String message = this.clientUsername + " : " + messageObj.getMessage() ;
                            
                            //save to database --  doesn't matter if it's save or not... there is nothing I can do if it's not
                            new DbConnect().addGroupMessage(clientUsername, groupName, messageObj.getMessage().toString());
                            
                            receiver.getClientWriter().writeObject(new ChatMessage(ChatMessage.MESSAGE, message));
                            //Server will have a GUI that logs all messages between users
                            //just log to console for now
                            System.out.println(message + " " + receiver.clientUsername);
                            
                            //save message to server
                            
                        }
                    }
                }catch(IOException ioe){
                         System.out.println("Error sending messages to group");
                }
        }

    }

    private synchronized void broadcastImage(ChatMessage messageObj) {
        
        //normal one to one messages only have one index
        //the groupname is returned not a array of users in the group
        String groupName = messageObj.getReceiver()[0];
        ArrayList<String> allFriends = new DbConnect().getMembersOfGroup(clientUsername, groupName);
        String[] stockArr = new String[allFriends.size()];
        String[]messageToUsers = allFriends.toArray(stockArr);
         
        System.out.println("Number of connected cliented "+Server.connectedSockets.size());

        //if this client is disconnected
        if(!client.isConnected()){
                //remove client from list of connected clients
                Server.connectedSockets.remove(this);
                System.out.println(clientUsername +" disconnected");
        }else{
                //try to write to the output stream of this client
                try{
                    //write to this specific clientThread
                    for (String username : messageToUsers) {
                        ClientThread receiver = Server.getClientWithUsername(username);
                        if(receiver != null){
                            Object message =  messageObj.getMessage() ;
                            receiver.getClientWriter().writeObject(new ChatMessage(ChatMessage.IMAGEMESSAGE, message));
                            //Server will have a GUI that logs all messages between users
                            //just log to console for now
                            //System.out.println(message + " " + receiver.clientUsername);
                            
                        }
                    }
                }catch(IOException ioe){
                         System.out.println("Error sending messages to group");
                }
        }

    }
    
    private synchronized void removeClientFromServerList() {
        //remove this client from the list of connected client because loopForever equal false
        //System.out.println(this.clientUsername);
        //Server.connectedSockets.remove(this);
        if(Server.connectedSockets.remove(this)){
                    System.out.println("Client deleted");
                }else{
                    System.out.println("Client "+ this.clientUsername +" not deleted");
                }
        //reduce the number of connected clients
        Server.uniqueClient--;
    }

    public String[] getConnectedFriends(){
        //all friends
        ArrayList<String> allFriends = new DbConnect().getFriendsOfUsername(clientUsername);
        if(allFriends ==null || allFriends.isEmpty()){
            System.out.println("No friends");
                return new String[0];
        }
        //online friends
       // System.out.println("Some friends online");
        ArrayList<String> onlineFriends =new ArrayList<>();
        for (String username : allFriends) {
            if(Server.getClientWithUsername(username) != null){
              //  System.out.println(username +" is a connected friend");
                onlineFriends.add(username);
            }else{
              //  System.out.println(username +" is not a connected friend");
            }
        }
        //System.out.println("Number of online friends " + onlineFriends.size());
        String[] stockArr = new String[onlineFriends.size()];
        stockArr = onlineFriends.toArray(stockArr);
        return stockArr;
    }

    private void closeStream() {
        try{
           // Thread.interrupted();
            //close streams of client
            if(clientWriter != null){
                clientWriter.close();
            }
            if(clientReader != null){
                clientReader.close();
            }
            if(client != null){
                client.close();
            }
        }catch(Exception e){
                e.printStackTrace();
        }
    }

    private void getFriendsRequestList() {
        try {
            //all friends
            ArrayList<String> allFriends = new DbConnect().getAwaitingRequests(clientUsername);
            if(allFriends ==null || allFriends.isEmpty()){
                System.out.println("No friends");
                getClientWriter().writeObject(new ChatMessage(ChatMessage.REQUESTINFO, new String[0]));
                return;
            }
                       
            String[] stockArr = new String[allFriends.size()];
            stockArr = allFriends.toArray(stockArr);
            
            getClientWriter().writeObject(new ChatMessage(ChatMessage.REQUESTINFO, stockArr));
            
        } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void updateFriendShipStatus(String user) {
        try {
            //all friends
            //System.out.println("updateFriendShipStatuss "+ user);
            boolean requestStat = new DbConnect().updateRequest(user,clientUsername);
            if(requestStat){
                clientWriter.writeObject(new ChatMessage(ChatMessage.REQUESTUPDATE,new String[]{"success", clientUsername +" you and "+ user +" are now friends, happy chat" }));
            }else{
                clientWriter.writeObject(new ChatMessage(ChatMessage.REQUESTUPDATE,new String[]{"error", "Can't be friends right now..try again later"}));
            }
            
        } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void deleteFriendShipStatus(String user) {
        try {
            //all friends
            //System.out.println("updateFriendShipStatuss "+ user);
            boolean requestStat = new DbConnect().deleteRequest(user,clientUsername);
            if(requestStat){
                clientWriter.writeObject(new ChatMessage(ChatMessage.REQUESTUPDATE,new String[]{"success", clientUsername +" you successful deleted "+ user +" friend request" }));
            }else{
                clientWriter.writeObject(new ChatMessage(ChatMessage.REQUESTUPDATE,new String[]{"error", "Please try deleting request later"}));
            }
            
        } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void createGroup(ChatMessage groupInfo) {
        try {
            
            String groupOwner = ((String[])groupInfo.getMessage())[0];
            String groupName = ((String[])groupInfo.getMessage())[1];
            //all friends
            //System.out.println("updateFriendShipStatuss "+ user);
            int requestStat = new DbConnect().createGroup(groupOwner,groupName);
            if(requestStat == 1){
                clientWriter.writeObject(new ChatMessage(ChatMessage.CREATEGROUP,new String[]{"success", clientUsername +" you successful created "+ groupName +" group" }));
            }else if(requestStat == 2){
                clientWriter.writeObject(new ChatMessage(ChatMessage.CREATEGROUP,new String[]{"error", "Group already exist, try another name"}));
            }
            else if(requestStat == 0){
                clientWriter.writeObject(new ChatMessage(ChatMessage.CREATEGROUP,new String[]{"error", "Please try creating group later"}));
            }
            
        } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void listOfGroupRequest(String messageObj) {
         try {
            
            String membername = messageObj;
            //all friends
            //System.out.println("updateFriendShipStatuss "+ user);
            ArrayList<String> requestStat = new DbConnect().getGroupsRequest(membername);
            if(requestStat != null){
                String[] stockArr = new String[requestStat.size()];
                stockArr = requestStat.toArray(stockArr);
                clientWriter.writeObject(new ChatMessage(ChatMessage.LISTGROUPREQUEST,stockArr));
            }else{
                clientWriter.writeObject(new ChatMessage(ChatMessage.LISTGROUPREQUEST,new String[0]));
            }
            
        } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void listMyGroups(String membername, int LISTMYGROUPS) {
       try {
            
            //all groups client is apart of            
            Object[] groupAndMembers = new Object[4];
            ArrayList<String> requestGroups = new DbConnect().getAllMyGroups(membername);
            //groupAndMembers[0] = requestGroups;
            //System.out.println(requestGroups);
            ArrayList<String> groupsImIn = new DbConnect().getAllGroupsImInButDontOwn(membername);
            
            groupsImIn = groupsImIn == null? new ArrayList<String>(): groupsImIn;
            requestGroups = requestGroups == null? new ArrayList<String>(): requestGroups;
            
            //loop through and get all members of all groups that client is apart of
            LinkedHashMap<String,ArrayList> requestMembers  = new LinkedHashMap<>();
            for(String l : requestGroups){
                requestMembers.put(l, new DbConnect().getMembersOfGroup(membername, l ) );
            }
            LinkedHashMap<String,ArrayList> groupsImInMembers  = new LinkedHashMap<>();
            for(String l : groupsImIn){                
                groupsImInMembers.put(l, new DbConnect().getMembersOfGroup(membername, l ) );
            }
           // System.out.println(requestMembers);
            groupAndMembers[0] = requestGroups;
            groupAndMembers[1] = requestMembers;
            groupAndMembers[2] = groupsImIn;
            groupAndMembers[3] = groupsImInMembers;
            System.out.println(groupsImInMembers);
            //System.out.println(groupAndMembers[0]);
            //System.out.println(groupAndMembers[1]);
            if(!requestGroups.isEmpty() || !groupsImIn.isEmpty()){
                clientWriter.writeObject(new ChatMessage(LISTMYGROUPS,groupAndMembers));
            }else{
                clientWriter.writeObject(new ChatMessage(LISTMYGROUPS,new String[0]));
            }
            
        } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void deleteMemberFromGroup(ChatMessage messageObj) {
        try {
             String membername = ((String[])messageObj.getMessage())[0];
             String groupName = ((String[])messageObj.getMessage())[1];

            if(new DbConnect().deleteMemberFromGroup(membername,groupName)){
                clientWriter.writeObject(new ChatMessage(ChatMessage.DELETEMEMBERFROMGROUP,new String[]{"success", membername +" successful removed from "+ groupName +" group" }));
            }else{
                clientWriter.writeObject(new ChatMessage(ChatMessage.DELETEMEMBERFROMGROUP,new String[]{"error", "Please try deleting member later"}));
            }
            
        } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void addMemberToGroup(ChatMessage messageObj) {
        try{
            String membername = ((String[])messageObj.getMessage())[0];
             String groupName = ((String[])messageObj.getMessage())[1];
             
             //check if membername is a user
             
             if(!new DbConnect().isAUser(membername)){
                 clientWriter.writeObject(new ChatMessage(ChatMessage.ADDMEMBERTOGROUP,new String[]{"error", "Sorry, "+membername+ " is not a registered user"}));
                 return;
             }
             
             if(new DbConnect().alreadyAMember(membername, groupName)){
                 clientWriter.writeObject(new ChatMessage(ChatMessage.ADDMEMBERTOGROUP,new String[]{"error", "Sorry, "+membername+ " is already a member of this group"}));
                 return;
             }
             
             if(new DbConnect().addMemberToGroup(membername, groupName)){
                  clientWriter.writeObject(new ChatMessage(ChatMessage.ADDMEMBERTOGROUP,new String[]{"success", membername +" successful add to "+ groupName +" group" }));
            }else{
                clientWriter.writeObject(new ChatMessage(ChatMessage.ADDMEMBERTOGROUP,new String[]{"error", "Please try add member later"}));
            }
             
             } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void deleteMyGroup(ChatMessage messageObj) {
        try{
            String membername = ((String[])messageObj.getMessage())[0];
             String groupName = ((String[])messageObj.getMessage())[1];
             
             //check if membername is a user

             if(new DbConnect().deleteGroup(membername, groupName)){
                  clientWriter.writeObject(new ChatMessage(ChatMessage.ADDMEMBERTOGROUP,new String[]{"success", membername +" successful deleted "+ groupName +" group" }));
            }else{
                clientWriter.writeObject(new ChatMessage(ChatMessage.ADDMEMBERTOGROUP,new String[]{"error", "Please try deleting group later"}));
            }
             
             } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void updateMemberToGroup(ChatMessage messageObj) {
        try{
            String membername = ((String[])messageObj.getMessage())[0];
             String groupName = ((String[])messageObj.getMessage())[1];
             
             //check if membername is a user

             if(new DbConnect().updateGroupRequest(membername, groupName)){
                  clientWriter.writeObject(new ChatMessage(ChatMessage.ADDMEMBERTOGROUP,new String[]{"success", membername +" successful added to "+ groupName +" group" }));
            }else{
                clientWriter.writeObject(new ChatMessage(ChatMessage.ADDMEMBERTOGROUP,new String[]{"error", "Please try again later"}));
            }
             
             } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }

    private void deleteMemberGroupRequest(ChatMessage messageObj) {
       try{
            String membername = ((String[])messageObj.getMessage())[0];
             String groupName = ((String[])messageObj.getMessage())[1];
             
             //check if membername is a user

             if(new DbConnect().deleteMemberGroupRequest(membername, groupName)){
                  clientWriter.writeObject(new ChatMessage(ChatMessage.ADDMEMBERTOGROUP,new String[]{"success", membername +" successful deleted "+ groupName +"request" }));
            }else{
                clientWriter.writeObject(new ChatMessage(ChatMessage.ADDMEMBERTOGROUP,new String[]{"error", "Please try deleting group request later"}));
            }
             
             } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void leaveGroup(ChatMessage messageObj) {
        try{
            String membername = ((String[])messageObj.getMessage())[0];
             String groupName = ((String[])messageObj.getMessage())[1];
             
            if(new DbConnect().deleteMemberFromGroup(membername, groupName)){
                  clientWriter.writeObject(new ChatMessage(ChatMessage.LEAVEGROUP,new String[]{"success", membername +" successful left "+ groupName +"group" }));
            }else{
                clientWriter.writeObject(new ChatMessage(ChatMessage.LEAVEGROUP,new String[]{"error", "Please try leaving group "}));
            }
             
             } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
     
    private void getFriends(String membername) {
       try {
            
            //all friends
            //System.out.println("updateFriendShipStatuss "+ user);
            ArrayList<String> requestStat = new DbConnect().getFriendsOfUsername(membername);
            if(requestStat != null){
                String[] stockArr = new String[requestStat.size()];
                stockArr = requestStat.toArray(stockArr);
                clientWriter.writeObject(new ChatMessage(ChatMessage.GETFRIENDS,stockArr));
            }else{
                clientWriter.writeObject(new ChatMessage(ChatMessage.GETFRIENDS,new String[0]));
            }
            
        } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void getOnlineUser(ChatMessage messageObj) {
        try{
            clientWriter.writeObject(new ChatMessage(ChatMessage.AVAILABLE, getConnectedFriends()));
        } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void deleteFriend(ChatMessage messageObj) {
         try{
            String membername = ((String[])messageObj.getMessage())[0];
             String friendname = ((String[])messageObj.getMessage())[1];
             if(new DbConnect().deleteFriend(membername, friendname)){
                clientWriter.writeObject(new ChatMessage(ChatMessage.DELETEFRIEND,new String[]{"success", membername +" successful delete "}));
            }else{
                clientWriter.writeObject(new ChatMessage(ChatMessage.DELETEFRIEND,new String[]{"error", "Please try leaving group "}));
            }
           } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void sendAudioChatReqest(ChatMessage messageObj) {
         try{
            String chatTo = ((String[])messageObj.getMessage())[0];
            //if this user is avaiables
            //client want to chat to this user
             System.out.println("I want to chat to " +chatTo);
            ClientThread sendAudioChatToThisClient = Server.getClientWithUsername(chatTo);
            
            if(sendAudioChatToThisClient != null){
                if(sendAudioChatToThisClient.getBusy()){
                    this.getClientWriter().writeObject(new ChatMessage(ChatMessage.BUSY,new String[]{"busy", chatTo  +" can't talk right now" }));
                     return;
                }
                //make sure any other request for a call is blocked if already in a call
                busy = true;
                sendAudioChatToThisClient.setBusy(true);
                //send call request
                if(sendAudioChatToThisClient.clientWriter != null){
                    sendAudioChatToThisClient.clientWriter.writeObject(new ChatMessage(ChatMessage.AUDIOCHATREQUEST,new String[]{"success", clientUsername +" would like to start a audio call with you?",clientUsername}));
                }else{
                    clientWriter.writeObject(new ChatMessage(ChatMessage.AUDIOCHATREQUEST,new String[]{"error", chatTo+" can't with you at the moment "}));
                }
            }else{
                clientWriter.writeObject(new ChatMessage(ChatMessage.AUDIOCHATREQUEST,new String[]{"error", chatTo+" can't with you at the moment "}));
            }
           } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void sendVideoChatReqest(ChatMessage messageObj) {
         try{
            String chatTo = ((String[])messageObj.getMessage())[0];
            //if this user is avaiables
            //client want to chat to this user
            ClientThread sendVideoChatToThisClient = Server.getClientWithUsername(chatTo);
             System.out.println(chatTo);
            if(sendVideoChatToThisClient != null){
                if(sendVideoChatToThisClient.getBusy()){
                    this.getClientWriter().writeObject(new ChatMessage(ChatMessage.BUSY,new String[]{"busy", chatTo  +" can't talk right now" }));
                    
                     return;
                }
                //make sure any other request for a call is blocked if already in a call
                busy = true;
                sendVideoChatToThisClient.setBusy(true);
                if(sendVideoChatToThisClient.getClientWriter() != null){
                    sendVideoChatToThisClient.getClientWriter().writeObject(new ChatMessage(ChatMessage.VIDEOCHATREQUEST,new String[]{"success", clientUsername +" would like to start a video call with you", clientUsername}));
                }else{
                    clientWriter.writeObject(new ChatMessage(ChatMessage.VIDEOCHATREQUEST,new String[]{"error", chatTo+" can't with you at the moment "}));
                }
            }else{
                clientWriter.writeObject(new ChatMessage(ChatMessage.VIDEOCHATREQUEST,new String[]{"error", chatTo+" can't with you at the moment "}));
            }
           } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void startAudioChat(ChatMessage messageObj) {
        //listen for audio messages
        String decision = ((String[])messageObj.getMessage())[0];
        String clientName = ((String[])messageObj.getMessage())[1];
        String receiverName = ((String[])messageObj.getMessage())[2];
        ClientThread sendAudioChatToThisClient = Server.getClientWithUsername(receiverName);
         try {
        if(decision.equalsIgnoreCase("accept")){
            System.out.println("Starting audio call");
            if(sendAudioChatToThisClient != null){
                    if(sendAudioChatToThisClient.getClientWriter() != null){
                        sendAudioChatToThisClient.getClientWriter().writeObject(new ChatMessage(ChatMessage.AUDIOCHATREQUEST,new String[]{"start", clientName, receiverName}));
                        this.clientWriter.writeObject(new ChatMessage(ChatMessage.AUDIOCHATREQUEST,new String[]{"start", receiverName, clientName }));
                    } else{
                        clientWriter.writeObject(new ChatMessage(ChatMessage.AUDIOCHATREQUEST,new String[]{"error", receiverName+" can't talk with you at the moment "}));        
                    }
                }
        }else{
           
            //tell user receiver rejected the call
            // System.out.println("updateFriendStatus " +updateUser);
            if(sendAudioChatToThisClient != null){
                if(sendAudioChatToThisClient.getClientWriter() != null){
                    sendAudioChatToThisClient.getClientWriter().writeObject(new ChatMessage(ChatMessage.AUDIOCHATREQUEST,new String[]{"reject", clientUsername +" declined your call request, sorry"}));
                }
                busy = false;
                sendAudioChatToThisClient.setBusy(false);
            }
        }
            } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void startVideoChat(ChatMessage messageObj) {

       //listen for video messages
        String decision = ((String[])messageObj.getMessage())[0];
        String clientName = ((String[])messageObj.getMessage())[1];
        String receiverName = ((String[])messageObj.getMessage())[2];
         System.out.println(receiverName);
        ClientThread sendAudioChatToThisClient = Server.getClientWithUsername(receiverName);
        // System.out.println(sendAudioChatToThisClient.toString());
         try {
        if(decision.equalsIgnoreCase("accept")){
            System.out.println("Starting video call");
            if(sendAudioChatToThisClient != null){
                    if(sendAudioChatToThisClient.getClientWriter() != null){
                        sendAudioChatToThisClient.getClientWriter().writeObject(new ChatMessage(ChatMessage.VIDEOCHATREQUEST,new String[]{"start", clientName, receiverName}));
                        this.getClientWriter().writeObject(new ChatMessage(ChatMessage.VIDEOCHATREQUEST,new String[]{"start", receiverName, clientName }));
                    } else{
                        clientWriter.writeObject(new ChatMessage(ChatMessage.VIDEOCHATREQUEST,new String[]{"error", receiverName+" can't talk with you at the moment "}));        
                    }
                }
        }else{
            //tell user receiver rejected the call
                // System.out.println("updateFriendStatus " +updateUser);
                if(sendAudioChatToThisClient != null){
                    if(sendAudioChatToThisClient.getClientWriter() != null){
                        sendAudioChatToThisClient.getClientWriter().writeObject(new ChatMessage(ChatMessage.VIDEOCHATREQUEST,new String[]{"reject", clientUsername +" declined your call request, sorry"}));
                    }
                    busy = false;
                    sendAudioChatToThisClient.setBusy(false);
                    
                }
                
        }
            } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
     }
    
    private void stopAudioChat(String receiverName) {
        try{
            //System.out.println("Stopping " + receiverName);
        ClientThread sendAudioChatToThisClient = Server.getClientWithUsername(receiverName);
        if(sendAudioChatToThisClient != null){
            if(sendAudioChatToThisClient.getClientWriter() != null){
                busy = false;
                sendAudioChatToThisClient.setBusy(false);
                sendAudioChatToThisClient.getClientWriter().writeObject(new ChatMessage(ChatMessage.STOPAUDIOCHAT,new String[]{"stop", clientUsername + " stopped the call"}));
            } else{
                clientWriter.writeObject(new ChatMessage(ChatMessage.STOPAUDIOCHAT,new String[]{"stoperror", "try to stop chat failed can't talk with you at the moment "}));        
            }
        }
        }catch(IOException ie){
            ie.printStackTrace();
        }
    }

    private void stopVideoChat(String receiverName) {
         try{
            ClientThread sendVideoChatToThisClient = Server.getClientWithUsername(receiverName);
            if(sendVideoChatToThisClient != null){
                busy = false;
                sendVideoChatToThisClient.setBusy(busy);
                sendVideoChatToThisClient.getClientWriter().writeObject(new ChatMessage(ChatMessage.STOPVIDEOCHAT,new String[]{"stop", clientUsername + " stopped the call"}));
            } else{
                    clientWriter.writeObject(new ChatMessage(ChatMessage.STOPAUDIOCHAT,new String[]{"stoperror", "try to stop chat failed can't talk with you at the moment "}));        
            }
        }catch(IOException ie){
            ie.printStackTrace();
        }
    }
    
    private int registerUser(Object message) {
        //System.out.println("registerUser"  +message);
        String messageObj[] = (String[])message;
        String fname = messageObj[0],
                lname = messageObj[1],
                username = messageObj[2],
                password = messageObj[3];
        return new DbConnect().registerUser(fname, lname, username, password);
    }
    
    private void getMessages(ChatMessage messageObj) {
        String from = ((String[])messageObj.getMessage())[0];
        String to  = ((String[])messageObj.getMessage())[1];
        ArrayList<String[]> results;
         try { 
             //retrieve messages of 'from' < > 'to'  
            results = new DbConnect().getAllMessage(from, to);
            
            if(results != null){
                this.getClientWriter().writeObject(new ChatMessage(ChatMessage.GETMESSAGES, results));
            }else{
                this.getClientWriter().writeObject(new ChatMessage(ChatMessage.GETMESSAGES, null));
            }
        } catch (IOException ex) {
                Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
            }
    }

    private void getGroupMessages(ChatMessage messageObj) {
        //client name and groupname passed --  only one used here obviously
        //String me = ((String[])messageObj.getMessage())[0];
        String groupName = ((String[])messageObj.getMessage())[1];
        ArrayList<String[]> results;
         try { 
             //retrieve messages of 'from' < > 'to'  
            results = new DbConnect().getAllMessageOfGroup(groupName);
            
            if(results != null){
                this.getClientWriter().writeObject(new ChatMessage(ChatMessage.GETGROUPMESSAGES, results));
            }else{
                this.getClientWriter().writeObject(new ChatMessage(ChatMessage.GETGROUPMESSAGES, null));
            }
        } catch (IOException ex) {
                Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
            }
    }

    public Socket getClient() {
            return client;
    }

    public ObjectOutputStream getClientWriter() {
            return clientWriter;
    }

    public ObjectInputStream getClientReader() {
            return clientReader;
    }

    public int getClientUniqueId() {
            return clientUniqueId;
    }

    public void setClientUniqueId(int clientUniqueId) {
            this.clientUniqueId = clientUniqueId;
    }

    public String getClientUsername() {
            return clientUsername;
    }

    public void setClientUsername(String clientUsername) {
            this.clientUsername = clientUsername;
    }

    public ChatMessage getClientMessage() {
            return clientMessage;
    }

    public boolean getBusy(){
        return busy;
    }
    
    public void setBusy(boolean t){
        busy  = t;
    }
    
    public void setClientMessage(ChatMessage clientMessage) {
            this.clientMessage = clientMessage;
    }

    public void setClient(Socket client) {
            this.client = client;
    }

    public void setClientWriter(ObjectOutputStream clientWriter) {
            this.clientWriter = clientWriter;
    }

    public void setClientReader(ObjectInputStream clientReader) {
        this.clientReader = clientReader;
    }

}
