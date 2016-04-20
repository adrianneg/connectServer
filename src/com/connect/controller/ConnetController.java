package com.connect.controller;

import com.connect.model.SECUREINFO;
import com.connect.model.Server;

public class ConnetController {
	public static void main(String[] args) {
		int portNumber ;
		//run a server on portNumber = 2789 default if no if passed through command line
		switch(args.length) {
		case 1:
			try {
				portNumber = Integer.parseInt(args[0]);
			}
			catch(Exception e) {
				System.out.println("Invalid port number.");
				System.out.println("Usage is: > java Server [portNumber]");
			}
		case 0:
			portNumber = SECUREINFO.CLIENTPORT;
			break;
		default:
			System.out.println("Usage is: > java Server [portNumber]");
			return;
		}
		new Server(portNumber).startServer();
	}
}
