/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.connect.videoServer;

import static com.connect.videoServer.VideoServer.connectedSockets;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jevaughnferguson
 */
public class VideoThread extends Thread{
    
    private Socket clientSocket;
    private ObjectInputStream clientReader;
    private ObjectOutputStream clientWriter;
    private String clientName;
    
    
    public VideoThread(Socket s){
        try {
            this.clientSocket = s;
            clientWriter = new ObjectOutputStream(clientSocket.getOutputStream());
            clientReader = new ObjectInputStream(clientSocket.getInputStream());
            
        } catch (IOException ex) {
            Logger.getLogger(VideoThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
  
    @Override
    public void run(){
       
        try {
            boolean loop = true;
            Object readObj;
            
            //accepts the name of the two persons in the video call
            String[] names = (String[])clientReader.readObject();
            clientName = names[0];
            String receiverName = names[1];
            System.out.println("client name "+ names[0]) ;
            System.out.println("receiver name " + names[1]);
            connectedSockets.put(clientName, this);
            
            while(loop){
                try {
                    if(!clientSocket.isClosed() && clientSocket.isConnected()){
                        
                        if(clientWriter != null){
                            
                            //get audio from client
                            if(clientReader != null){
                                
                                //keep listening for bytes which are images from the webcam
                                readObj = clientReader.readObject();
                                
                                if(readObj != null){
                                    //send video I receive from client to receivers outputstream
                                    VideoThread tmp = VideoServer.getClientWithUsername(receiverName);
                                    if(tmp != null){
                                        if(!tmp.getClient().isClosed()){
                                            if(tmp.getClientWriter() != null){
                                                //send image to --
                                                //System.out.println("image send");
                                                tmp.getClientWriter().writeObject(readObj);
                                                tmp.getClientWriter().flush();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException ex) {
                    //System.exit(1);
                    
                    //Logger.getLogger(VideoServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(VideoServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    
    public ObjectOutputStream getClientWriter() {
        return clientWriter;
    }

    public ObjectInputStream getClientReader() {
        return clientReader;
    }

    public Socket getClient() {
        return clientSocket;
    }
    
    public String getClientUsername(){
            return clientName;
      }

}
