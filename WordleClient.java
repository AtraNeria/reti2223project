import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
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
		int op;
		while (!exit ){
			String ans = c.readLine("Ciao! Scrivi:\n1 -Login\n2 -Signup\n3 -Play\n4 -Exit\n");
			op = Integer.parseInt(ans);
			switch (op) {
				case 1:
					getLogin();
					if (login) System.out.print("Connesso con successo!\n");
					break;
				case 2:
					getSignup();
					if (login) System.out.print("Registrato con successo! Sei ancora connesso!\n");
					break;
				case 3:
					//TO-DO: Play
					break;
				case 4:
					exit = true;
					break;
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
            Registry reg = LocateRegistry.getRegistry(2020);
			RemoteServerInterface remoteServer = (RemoteServerInterface) reg.lookup("RemoteWordleService");
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
		boolean exit = true;
		// Controllo lunghezza nome
		if (name.length()> 12) {
			System.out.println("Username troppo lungo! Deve essere massimo 12 caratteri.");
			exit = false;
		}
		// Controllo lunghezza password
		if (password.length > 24) {
			System.out.println("Password troppo lunga! Deve essere massimo 24 caratteri.");
			exit = false;
		}
		
		try {
			// Connetto al server remoto
			Registry reg = LocateRegistry.getRegistry(2020);
			RemoteServerInterface remoteServer = (RemoteServerInterface) reg.lookup("RemoteWordleService");
			exit = remoteServer.signUp(name, password);
			if (!exit) System.out.println("Username già in utilizzo\n");
			// Se mi registro con successo rimango collegato
			else login = true;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return exit;
	}
	
}