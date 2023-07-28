import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import javax.management.remote.rmi.RMIServer;

import java.beans.Encoder;
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
	private ByteBuffer in;
	private boolean login;
	private String user;
	
	// Metodo costruttore
	public WordleClient() throws Exception {
		// Console per interfaccia con lo user
		c = System.console();
		// Configurazione per numero di porta
		this.getConfig();
		host = new InetSocketAddress("localhost", port);
		System.out.println(host);	// TEST
		// login non ancora effettuato
		login = false;
	}

	// Configurazione
	private void getConfig() {
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
			String ans = c.readLine("Ciao! Scrivi:\n1 -Login\n2 -Signup\n3 -Play\n4 -Condividi score\n5 -Mostrami statistiche degli altri utenti\n6 -Logout\n7 -close\n");
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
					if (login) play();
					else System.out.print("Accedi o registrati prima di giocare!");
					break;
				case 4:
					// TO-DO: share
					break;
				case 5:
					// TO-DO: show me stats
					break;
				case 6:
					login = false;
					break;
				case 7:
					login = false;
					exit = true;
					break;
				default:
					System.out.print("Richiesta non supportata!");
					break;
			}
		}
	}
	
	// Chiede username e password e controlla che siano corrette, altrimenti segnala errore
	// Restituisce true se l'operazione ha avuto successo, false altrimenti
	private boolean getLogin() {
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
		if (login) user = name;
		return login;
	}
	
	// Chiede credenziale con cui registrarsi
	// Restituisce true se l'operazione ha avuto successo, false altrimenti
	private boolean getSignup() {
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
		if (exit) user = name;
		return exit;
	}
	
	// Avvia una partita di wordle
	private void play() {
		try {
			// Avvio della socket
			clientSocket = SocketChannel.open(host);
		}
		catch (IOException e) {
			System.err.println("Impossibile connettersi");
		}
		// Controllo se l'utente ha già giocato per questa parola
		if (hasPlayed()) {
			System.out.println("Hai già giocato per questa parola!");
			//TO-DO: stucks
		}
		else {		
			// 12 tentativi
			int chances = 12;
			boolean won = false;
			int outcome;
			while (chances!=0 && !won) {
				String guess = c.readLine("Prova a indovinare\n");
				// La parola deve essere di 10 lettere
				if (guess.length()!=10) {
					System.out.println("La parola deve essere di 10 lettere!");
					continue;
				}
				outcome = sendWord(guess);
				switch (outcome) {
					// Errata
					case 0:
						chances--;
						System.out.println("Hai ancora "+(chances)+" tentativi!");
						break;
					// Corretta
					case 1:
						won=true;
						System.out.println("Hai indovinato in "+(12-chances)+" tentativi!");
						break;
					// Non nel vocabolario
					case 2:
						System.out.println("Parola non nel vocabolario; non ti verranno sottratti tentativi!");
						break;
					// Problemi a raggiungere il server
					case 3:
						chances = 0;
						System.out.println("Server non raggiungibile!");
						break;
				}
			}
			// TO-DO
			//if (won) updateScore(); sendMeStats(); updateLastPlayed;
		}	
	}

	// Chiede al server se si è già giocato per la parola corrente
	private boolean hasPlayed() {
		boolean played = false;
		// Faccio richiesta di auth al server
		String toSend= "5"+user;
		out = ByteBuffer.wrap(toSend.getBytes());
		try {
			clientSocket.write(out);
			// Leggo risposta
			in = ByteBuffer.allocate(32);
			clientSocket.read(in);
			in.flip();
			int ans = in.getInt();
			// Se è 0 user ha già giocato per la parola corrente
			if (ans==0) played=true;
			else played=false;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return played;
	}

	// Invia un tentativo al server
	// Restituisce 0 in caso di errore, 1 in caso di successo, 
	// 2 se la parola non è contemplata, 3 in caso di problemi col server
	private int sendWord(String guess) {
		int outcome = 0;
		try {
			// Scrivo richiesta di gioco con guess al server
			String toSend= "1"+guess;
			out = ByteBuffer.wrap(toSend.getBytes());
			int sent = clientSocket.write(out);
			// Leggo risposta
			in = ByteBuffer.allocate(32);
			int received = clientSocket.read(in);
			in.flip();
			String hint = StandardCharsets.UTF_8.decode(in).toString();
			// Se il tentativo è corretto
			if (hint.equals("gggggggggg")) outcome = 1;
			// Se il tentativo è valido
			else if (hint.equals("notvalid")) outcome = 2;
			else {
				final String ANSI_WHITE = "\u001B[37m";
				final String ANSI_YELLOW = "\u001B[33m";
				final String ANSI_GREEN = "\u001B[32m";
				final String ANSI_RESET = "\u001B[0m";
				outcome = 0;

				for (int i=0;i<10;i++){
					switch (hint.charAt(i)){
						case 'w':
							System.out.print(ANSI_WHITE+guess.charAt(i)+ANSI_RESET);
							break;
						case 'y':
							System.out.print(ANSI_YELLOW+guess.charAt(i)+ANSI_RESET);
							break;
						case 'g':
							System.out.print(ANSI_GREEN+guess.charAt(i)+ANSI_RESET);
							break;
					}
				}
				System.out.print("\n");
			}
			System.out.println(hint); //TEST
		}
		// Se il server non è raggiungibile
		catch (IOException e) {
			e.printStackTrace();
			outcome = 3;
		}
		return outcome;
	}
}