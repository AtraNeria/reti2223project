import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.management.remote.rmi.RMIServer;
import java.io.*;

public class WordleClient {
	
	// Interagisce con l'utente tramite terminale di sistema
	public Console c;
	// Si connette al serve con socket TCP
	private SocketChannel clientSocket;
	public static int port;
	private InetSocketAddress host;
	// Due stream per comunicare con server
	private ByteBuffer out;
	private BufferedReader in;
	private boolean login;
	
	// Metodo costruttore
	public WordleClient() throws Exception {
		// Console per interfaccia con lo user
		c = System.console();
		// Configurazione per numero di porta
		this.getConfig();
		host = new InetSocketAddress("localhost", port);
		// login non ancora effettuato
		login = false;
		// Avvio della socket
		clientSocket = SocketChannel.open(host);
	}

	// Configurazione
	public void getConfig() {
		// ricavo il path del file di configurazione
		Path configFile = Paths.get("clientConfig.txt");
		// estraggo le linee del file
		List<String> param;
		try {
			param = Files.readAllLines(configFile);
			// dalla lista ricavo il numero di porta
			port = Integer.parseInt(param.get(0));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// Chiede all'utente se desidera accedere o registrarsi                                                        
	public void greet() {
		boolean exit = false;
		while (!exit ){
			String ans = c.readLine("Ciao! Se desideri effettuare il login scrivi YES.\nSe non hai un account e desideri registrarti, scrivi NO");
			if (Boolean.parseBoolean(ans)) exit = getLogin();
			else {
				exit = getSignup();
				if (!exit) System.out.println("Username già in utilizzo");
			}
		}
	}
	
	// Chiede username e password e controlla che siano corrette, altrimenti segnala errore
	// Restituisce true se l'operazione ha avuto successo, false altrimenti
	public boolean getLogin() {
		// Leggo Username
		String name = c.readLine("Username: ");
		//Massimo 12 char
		if (name.length()> 12) {
			System.out.println("Username troppo lungo! Deve essere massimo 12 caratteri.");
			return false;
		}
		// Leggo password
		char[] password = c.readPassword("Password: ");
		// Massimo 24 char
		if (password.length > 24) {
			System.out.println("Password troppo lunga! Deve essere massimo 24 caratteri.");
			return false;
		}

		// Credenziali valide -> Procedura di login
		try {
			int rc;
			//connetto al server remoto
			RemoteWordleServer remoteServer = (RemoteWordleServer) java.rmi.Naming.lookup("RemoteWordleService");
			rc = remoteServer.login(name, password);
			switch (rc) {
				case 0:
					System.out.println("Login avvenuto con successo!");
					login = true;
					break;
				case 1:
					System.out.println("Password errata.");
					break;
				case 2:
					System.out.println("Utente non trovato.");
					break;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return login;
	}
	
	// Chiede credenziale con cui registrarsi
	// Restituisce true se l'operazione ha avuto successo, false altrimenti
	public boolean getSignup() {
		String name = c.readLine("Username: ");
		char[] password = c.readPassword("Password: ");
		boolean exit = false;
		// Controllo lunghezza nome
		if (name.length()> 12) {
			System.out.println("Username troppo lungo! Deve essere massimo 12 caratteri.");
			return false;
		}
		// Controllo lunghezza password
		if (password.length > 24) {
			System.out.println("Password troppo lunga! Deve essere massimo 24 caratteri.");
			return false;
		}
		
		try {
			//Connetto al server remoto
			RemoteWordleServer remoteServer = (RemoteWordleServer) java.rmi.Naming.lookup("RemoteWordleService");
			//Richiedo servizio remoto
			exit = remoteServer.signUp(name, password);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return exit;
	}
	
}