import java.net.*;
import java.io.*;

public class WordleClient {
	
	// Interagisce con l'utente tramite terminale di sistema
	public Console c;
	// Si connette al serve con socket TCP
	private Socket clientSocket;
	public static int port = 6789;
	private InetAddress host = InetAddress.getLocalHost();
	// Due stream per comunicare con server
	private PrintStream toServer;
	private BufferedReader fromServer;
	
	// Metodo costruttore
	public WordleClient() throws Exception {
		c = System.console();
		clientSocket = new Socket(host.getHostName(),port);	//TO-DO: exception
		toServer = new PrintStream(clientSocket.getOutputStream());
		fromServer = new BufferedReader( new InputStreamReader(clientSocket.getInputStream()));
	}
	
	// Chiede all'utente se desidera accedere o registrarsi                                                        
	public boolean greet() {
		String ans = c.readLine("Ciao! Se desideri effettuare il login scrivi YES.\nSe non hai un account e desideri registrarti, scrivi NO");
		return (Boolean.parseBoolean(ans));
	}
	
	// Chiede username e password e controlla che siano corrette, altrimenti segnala errore
	public void getLogin() {
		String name = c.readLine("Username: ");
		char[] password = c.readPassword("Password: ");
		// TO-DO send to server and getanswer
	}
	
	// Chiede credenziale con cui registrarsi
	public void getSignup() {
		String name = c.readLine("Username: ");
		char[] password = c.readPassword("Password: ");
		// TO-DO with RMI
	}
	
	public static void main (String[] args) throws Exception {
		WordleClient client = new WordleClient();
		if (client.greet()) client.getLogin();
		else client.getSignup();
	}
}