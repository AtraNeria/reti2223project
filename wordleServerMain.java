import java.net.*;
import java.io.*;

public class wordleServerMain {
	
	//Socket del server
	private static ServerSocket server;
	//Numero porta su cui è in ascolto
	private static int port = 6789;
	
	public static void main(String[] args) throws IOException {
		//Creo la socket
		server = new ServerSocket(port);
		//Entro in un loop di ascolto richieste
		while(true) {
			//TO-DO
		}
	}

}