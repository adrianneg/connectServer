package com.connect.model;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
	public static int uniqueClient = 0;
	private int port;
	private InetAddress hostAddress;
	public static Collection<ClientThread> connectedSockets;
	private boolean loopForever;
        private ServerSocket serverSocket;
	
	public Server(int port){
		//connect on this port
		this.port = SECUREINFO.CLIENTPORT;
		try {
			//this.hostAddress =InetAddress.getByName("192.168.0.32");
                    this.hostAddress =InetAddress.getByName(SECUREINFO.VIDEOHOST);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		//store list on connect client sockets
		connectedSockets = Collections.synchronizedCollection(new ArrayList<ClientThread>());
	}
	
	public void startServer(){
		loopForever = true;//variable to ensure server keeps waiting for a new connection
		
		try{
                    //loop to constantly create new sockets to accept request from new clients to connect to server
                     //create a server that accepts sockets on this port
                        serverSocket = new  ServerSocket(port, 100, this.hostAddress );
                        //loop to constantly create new sockets to accept request from new clients to connect to server
                        while(loopForever){
                            
                            System.out.println("Waiting for connections...");
                            System.out.println("Connect on port: "+ this.port + " host: "+ this.hostAddress.getHostName());
                            Socket newClientSocket = serverSocket.accept();
                            
                            //stop if was asked
                            if(!loopForever){
                                break;
                            }
                            //listen for normal messages and whatever
                            ClientThread t = new ClientThread(newClientSocket);
                            t.start();
                            
                            //add some method to update the connected clients of new clients
                            
                        }
                        //server asked to stop if the loop is close so close the connection streams
	
		}
		catch(IOException ioe){
                    //check if server closed
                    stopServer();
                    //create a new instance of server
                    if(serverSocket != null  && serverSocket.isClosed()){
                        new Server(SECUREINFO.CLIENTPORT).startServer();
                    }
			//ioe.printStackTrace();
		}
	}

	public static ClientThread getClientWithUsername(String username){
            for (ClientThread clientThread : connectedSockets) {
                if(clientThread != null ){
                    if(clientThread.getClientUsername() != null){
                        //System.out.println(clientThread.getClientUsername() +" is connected");
                       if(clientThread.getClientUsername().equalsIgnoreCase(username)){
                           //System.out.println("a client was found");
                           return clientThread;
                       }
                   }
                }
            }
            return null;
	}
	
        public static void broadcast() {
        // send message to all connected users
            try {
                for (ClientThread c : connectedSockets) {
                    //System.out.println("Connected user " +connectedSockets.size());
                    if(c.getClient().isConnected()){
                        //System.out.println(c.getClientUsername()+ " Broadcasting to " );//c.getConnectedFriends().length + " connected friends");
                        c.getClientWriter().writeObject(new ChatMessage(ChatMessage.AVAILABLE, c.getConnectedFriends()));
                    }else{
                        System.out.println("Removing "+ c.getClientUsername());
                        connectedSockets.remove(c);
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
	
	@SuppressWarnings("resource")
	public void stopServer(){
            loopForever = false;
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    for (ClientThread ct : connectedSockets) {
                            //close the streams
                            if(ct == null){
                                continue;
                            }
                            if (ct.getClientWriter() != null){                                   
                                ct.getClientWriter().close();
                            }
                            if (ct.getClientReader() != null){
                                ct.getClientReader().close();
                            }
                            if (ct.getClient() != null){
                                ct.getClient().close();
                            }
			}
                    serverSocket.close();
                } catch (IOException e){
                    e.printStackTrace(System.err);
                }
            }
	}

}
