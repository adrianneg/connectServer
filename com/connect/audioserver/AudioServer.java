package com.connect.audioserver;

import com.connect.model.SECUREINFO;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class AudioServer {

	private int port;
	private Socket client;
	boolean loopForever;
        public static LinkedHashMap<String,SocketThread> connectedSockets;
        private InetAddress hostAddress;
        
    public AudioServer(){
            try {
                //this.receiverServerSocket = rss ;
                this.port = 2786;
                loopForever = true;
                this.hostAddress =InetAddress.getByName(SECUREINFO.AUDIOHOST);
                connectedSockets  = new LinkedHashMap<>();
                startServer();
            } catch (UnknownHostException ex) {
                Logger.getLogger(AudioServer.class.getName()).log(Level.SEVERE, null, ex);
            }
       
    }
    
        public final void startServer(){
            try{
                //server listen
                ServerSocket server = new ServerSocket(this.port);
                
                while(loopForever){
                   System.out.println("Waiting for connections...");
                   System.out.println("Connect on port: "+ this.port + " host: "+ this.hostAddress.getHostName());
                    
                   client = server.accept();
                   SocketThread s = new SocketThread(client);
                   s.start();
                    
                }
            }
            catch(IOException ioe){
                ioe.printStackTrace();
            }
        }      

    
        public static SocketThread getClientWithUsername(String username){
            for(Map.Entry<String, SocketThread> socketThread : connectedSockets.entrySet()){
                if(socketThread != null ){   
                    if(socketThread.getValue()== null) break;
                    if(socketThread.getValue().getClientUsername() != null){
                        //System.out.println(clientThread.getClientUsername() +" is connected");
                       if(socketThread.getValue().getClientUsername().equalsIgnoreCase(username)){
                           //System.out.println("a client was found");
                           return socketThread.getValue();
                       }
                   }
                }
            }
            return null;
	}
    
      
     public static void main(String[] args) throws ClassNotFoundException{
        new AudioServer();
    }
    
}
 
