
package com.connect.audioserver;

import static com.connect.audioserver.AudioServer.connectedSockets;
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
public class SocketThread extends Thread{
    private ObjectOutputStream clientWriter;
    private ObjectInputStream clientReader;
    private String clientName;
   final int byteSize = 64;
    private Socket client;
    
    public SocketThread(Socket s){

        try {
            client = s;
            this.clientReader = new ObjectInputStream( client.getInputStream() );
            this.clientWriter = new ObjectOutputStream( client.getOutputStream() );
        } catch (IOException ex) {
            Logger.getLogger(SocketThread.class.getName()).log(Level.SEVERE, null, ex);
        }
 
    }
    
    @Override
    public void run(){
       
        try {
            boolean loop = true;
            byte[] data = new byte[byteSize];
            int readBytes = 0;
            
            String[] names = (String[])clientReader.readObject();
            clientName = names[0];
            String receiverName = names[1];
            System.out.println("client name "+ names[0]) ;
            System.out.println("receiver name " + names[1]);
            connectedSockets.put(clientName, this);
            
            while(loop){
                try {
                    if(!client.isClosed()){
                        if(clientWriter != null){
                            //get audio from client
                            if(clientReader != null){
                                readBytes = clientReader.read(data);
                                if(readBytes > 0){
                                    //send audio I receiver from client to send clients outputstream
                                    SocketThread tmp = AudioServer.getClientWithUsername(receiverName);
                                    if(tmp != null){
                                        if(!tmp.getClient().isClosed()){
                                            if(tmp.getClientWriter() != null){
                                                tmp.getClientWriter().write(data);
                                                tmp.getClientWriter().flush();
                                            }
                                        }
                                    }
                                }
                            }else break;
                        } else break;
                    } else break;
                } catch (IOException ex) {
                    //System.exit(1);
                    //stop the client and move on
                    break;
                    //Logger.getLogger(AudioServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            connectedSockets.remove(clientName, this);
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(SocketThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ObjectOutputStream getClientWriter() {
        return clientWriter;
    }

    public ObjectInputStream getClientReader() {
        return clientReader;
    }

    public Socket getClient() {
        return client;
    }

    public String getClientUsername(){
          return clientName;
    }
}
