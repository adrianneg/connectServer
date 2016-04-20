package com.connect.videoServer;



import com.connect.model.SECUREINFO;
import org.opencv.core.Core;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("serial")
public class VideoServer {
	private ServerSocket server;
	private Socket serverSocket;
	private int port;
	public static LinkedHashMap<String,VideoThread> connectedSockets;
	

	public VideoServer() {
            try {
                port = SECUREINFO.VIDEOPORT;
                connectedSockets  = new LinkedHashMap<>();
                server = new ServerSocket(port, 100, InetAddress.getByName(SECUREINFO.VIDEOHOST));
                    while(true){
                        System.out.println("Waiting for connections...");
                        System.out.println("Connect on port: "+ this.port + " host: "+ InetAddress.getByName(SECUREINFO.VIDEOHOST).getHostName());
                        serverSocket = server.accept();
                        //handle io on thread
                        VideoThread v = new VideoThread(serverSocket);
                        v.start();
                    }
                                               		
            } catch (IOException ex) {
                Logger.getLogger(VideoServer.class.getName()).log(Level.SEVERE, null, ex);
            }finally{
                try {
                    server.close();
                    serverSocket.close();
                } catch (IOException ex) {
                    Logger.getLogger(VideoServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
			
        }	

        
        public static VideoThread getClientWithUsername(String username){
            for(Map.Entry<String, VideoThread> socketThread : connectedSockets.entrySet()){
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

	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // load native library of opencv
		
                new VideoServer();
				
	}
}
