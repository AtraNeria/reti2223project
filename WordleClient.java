import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.*;

public class WordleClient {
	
	// Interagisce con l'utente tramite terminale di sistema
	public Console c;
	// Si connette al serve con socket TCP
	private SocketChannel clientSocket;
	private static int port;
	private InetSocketAddress host;
	// Porta UDP per gruppo multicast
	private static int udpPort;
	// Due stream per comunicare con server
	private ByteBuffer out;
	private ByteBuffer in;
	// Info utente
	private boolean login;
	private String user;
	// Notifiche
	private List<String> notifs = Collections.synchronizedList(new ArrayList<String>());

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
			// dalla lista ricavo il numero di porta per connessione TCP
			port = Integer.parseInt(param.get(0));
			// E il numero di porta per connessione UDP
			udpPort = Integer.parseInt(param.get(1));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// Chiede all'utente cosa desidera fare                                                        
	public void greet() {
		boolean exit = false;
		int op;
		while (!exit ){
			String ans = c.readLine("Ciao! Scrivi:\n1 -Login\n2 -Signup\n3 -Play\n4 -Condividi risultato ultima partita\n5 -Mostrami le mie statistiche\n6 -Mostrami le notifiche ricevute\n7 -Logout\n8 -close\n");
			try	{
				op = Integer.parseInt(ans);
				switch (op) {
					case 1:
						if (!login) getLogin();
						else System.out.println("Sei già connesso!");
						if (login) {
							System.out.println("Connesso con successo!");
							joinMulticastGroup();
						}
						break;
					case 2:
						if (!login) getSignup();
						else System.out.println("Hai già effettuato l'accesso con un account!");
						if (login) {
							System.out.println("Registrato con successo! Sei ancora connesso!");
							joinMulticastGroup();
						}
						break;
					case 3:
						if (login) play();
						else System.out.println("Accedi o registrati prima di giocare!");
						break;
					case 4:
						if (login) share();
						else System.out.println("Devi prima accedere!");
						break;
					case 5:
						if (login) showMyStats();
						else System.out.println("Accedi per vedere le tue statistiche!");
						break;
					case 6:
						if (login) printNotifs();
						else System.out.println("Devi accedere per ricevere notifiche!");
						break;
					case 7:
						login = false;
						break;
					case 8:
						login = false;
						exit = true;
						break;
					default:
						System.out.println("Richiesta non supportata!");
						break;
				}
			}
			catch (NumberFormatException e) {System.out.println("Richiesta non supportata!");}
		}
	}
	
	// Chiede username e password e controlla che siano corrette, altrimenti segnala errore
	// Restituisce true se l'operazione ha avuto successo, false altrimenti
	private boolean getLogin() {
		// Leggo Username
		String name = c.readLine("Username: ");
		// Massimo 12 char
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
					user = name;
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
		boolean valid = false;

		// Richiedo credenziali fino a che user non ne fornisce di valide
		while (!valid) {
			// Controllo lunghezza nome
			if (name.length()> 12) {
				System.out.println("Username troppo lungo! Deve essere massimo 12 caratteri.");
			}
			else if (name.length()==0) {
				System.out.println("Username vuoto.");
			}
			// Nome non può contenere uno spazio
			else if (name.contains(" ")) {
				System.out.println("Lo username non può contenere uno spazio.");
			}
			// Controllo lunghezza password
			else if (password.length == 0) {
				System.out.println("Password vuota.");
			}
			else if (password.length > 24) {
				System.out.println("Password troppo lunga! Deve essere massimo 24 caratteri.");
			}
			else valid = true;
			if (!valid) {
				name = c.readLine("Username: ");
				password = c.readPassword("Password: ");
			}
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
	
	// Mi unisco al gruppo di multicast per ricevere notifiche
	private void joinMulticastGroup() {
		clientUDP getNotifs = new clientUDP();
		Thread udpListen = new Thread(getNotifs);
		udpListen.start();
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
		}
		else {
			// Controllo se ho già effettuato dei tentativi
			int alreadyMade = hasPending();
			// 12 tentativi - quelli già effettuati
			int chances = 12;
			if (alreadyMade>0) chances = chances-alreadyMade;
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
						chances--;
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
			// Aggiorno info sull'utente
			updatePlayerInfo(won, chances);
		}	
	}

	// Chiede al server se si è già giocato per la parola corrente
	private boolean hasPlayed() {
		boolean played = false;
		// Faccio richiesta di auth al server
		String toSend= "8"+user;
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

	// Controllo se ho dei tentativi in sospeso per la sessione
	private int hasPending () {
		String toSend= "9"+user;
		out = ByteBuffer.wrap(toSend.getBytes());
		try {
			clientSocket.write(out);
			// Leggo risposta
			in = ByteBuffer.allocate(4);
			clientSocket.read(in);
			in.flip();
			int tried = in.getInt();
			// Se ce ne sono leggo i tentativi già fatti
			if (tried != -1) {
				in = ByteBuffer.allocate(tried*22);
				clientSocket.read(in);
				in.flip();
				String attempts = StandardCharsets.UTF_8.decode(in).toString();
				String [] attArr = attempts.split(" ");
				// Li stampo a schermo
				for (int i=0;i<tried*2;i=i+2) {
					printColoredHint(attArr[i+1], attArr[i]);
				}
			}
			return tried;
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

	// Invia un tentativo al server
	// Restituisce 0 in caso di errore, 1 in caso di successo, 
	// 2 se la parola non è contemplata, 3 in caso di problemi col server
	private int sendWord(String guess) {
		int outcome = 0;
		try {
			// Scrivo richiesta di gioco con guess al server
			String toSend= "1"+user+" "+guess;
			out = ByteBuffer.wrap(toSend.getBytes());
			clientSocket.write(out);
			// Leggo risposta
			in = ByteBuffer.allocate(32);
			clientSocket.read(in);
			in.flip();
			String hint = StandardCharsets.UTF_8.decode(in).toString();
			// Se il tentativo è corretto
			if (hint.equals("gggggggggg")) {
				printColoredHint(hint, guess);
				outcome = 1;
			}
			// Se il tentativo è valido
			else if (hint.equals("notvalid")) outcome = 2;
			else {
				printColoredHint(hint, guess);
				outcome = 0;
			}
		}
		// Se il server non è raggiungibile
		catch (IOException e) {
			e.printStackTrace();
			outcome = 3;
		}
		return outcome;
	}

	// Stampa a schermo guess colorata come suggerito da hint
	private void printColoredHint (String hint, String guess) {
		final String ANSI_WHITE = "\u001B[37m";
		final String ANSI_YELLOW = "\u001B[33m";
		final String ANSI_GREEN = "\u001B[32m";
		final String ANSI_RESET = "\u001B[0m";

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

	// Aggiorna informazioni sul giocatore dopo una partita
	private void updatePlayerInfo (boolean won, int tries) {
		// Aggiorno ultima partita
		updateLastPlayed(won);
		getAck();
		// Se la partita è stata vinta aggiorno il punteggio
		if (won) updateScore(tries);
	}

	// Ho vinto in tries tentativi
	// Chiedo al server di aggiornare il mio punteggio
	private void updateScore (int tries) {
		// Invio codice richiesta 7, username, e numero tentativi
		String toSend = "7"+user+" "+(12-tries);
		out = ByteBuffer.wrap(toSend.getBytes());
		try {
			clientSocket.write(out);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Chiedo al server di aggiornare l'ultima volta in cui si è giocato e l'esito della partita
	private void updateLastPlayed (boolean won) {
		String toSend;
		if (won) toSend = "6"+user+" 0";
		else toSend = "6"+user+" 1";
		out = ByteBuffer.wrap(toSend.getBytes());
		try {
			clientSocket.write(out);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// Chiedo al server di condividere l'esito della mia ultima partita in gruppo di multicast
	private void share() {
			try {
				// Se non è già aperta apro connessione
				if (clientSocket==null) clientSocket = SocketChannel.open(host);
				// Invio richiesta di condivisione
				String toSend= "4"+user;
				out = ByteBuffer.wrap(toSend.getBytes());
				clientSocket.write(out);
				// TO-DO: getAck
			}
			catch (IOException e) {
				e.printStackTrace();
			}

	}

	// Stampo le notifiche ricevute dal gruppo di multicast
	private void printNotifs(){
		System.out.println("Hai ricevuto "+notifs.size()+" notifiche");
		for (int i=0; i<notifs.size();i++) {
			System.out.println(notifs.get(i));
			notifs.remove(i);
		}
	}

	// Richiede e stampa statistiche dell'utente
	private void showMyStats () {
		try {
			// Se non è già aperta apro connessione
			if (clientSocket==null) clientSocket = SocketChannel.open(host);
			// Invio richiesta delle statistiche
			String toSend= "5"+user;
			out = ByteBuffer.wrap(toSend.getBytes());
			clientSocket.write(out);
			// Leggo risposta server
			in = ByteBuffer.allocate(32);
			clientSocket.read(in);
			in.flip();
			// Stampo le statistiche formattate
			String stats = StandardCharsets.UTF_8.decode(in).toString();
			String [] st = stats.split(" ");
			System.out.println("Score: "+st[0]+"\nPartite vinte: "+st[1]+"\nPartite vinte: "+st[2]+"\nNumero medio di tentativi per vincere: "+st[3]);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Legge ack da server
	private int getAck () {
		int ack = 1;
		try {
			in = ByteBuffer.allocate(32);
			clientSocket.read(in);
			in.flip();
			ack = in.getInt();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ack;
	}

	// Classe interna per gestire gruppo broadcast UDP
	private class clientUDP implements Runnable {
		private MulticastSocket socket = null;
		private byte [] buf = new byte[256];
	
		public clientUDP() {}

		@Override
		public void run() {
			try {
				// Mi unisco al gruppo
				socket = new MulticastSocket(udpPort);
				InetAddress group = InetAddress.getByName("225.0.0.0");
				socket.joinGroup(group);
				// Rimango in ascolto finchè l'utente è loggato
				while (login) {
					DatagramPacket pkg = new DatagramPacket(buf, buf.length);
					socket.receive(pkg);
					String received = new String(pkg.getData(), 0, pkg.getLength());
					notifs.add(received);
				}
				socket.leaveGroup(group);
				socket.close();
				notifs.clear();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}



	}

}