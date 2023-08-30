import java.net.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Timestamp;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WordleServer {
	private int port;
	private int udpPort;
	private int maxThreads;
	private long wordLapse;
	private ServerSocketChannel welcomeSocket;
	private DatagramSocket udpSocket;
	InetAddress multicastGroup;
	private Selector selector;
	private Timestamp wordExtraction;
	private String word;
	private String translation;
	private List<RankingEntry> userRank;
	public RemoteWordleServer rmi;

	public WordleServer() {
		// Ottiene le info per configurare il server dal file apposito
		this.getConfig();
		Database usersDB = Database.getDB();
		userRank = usersDB.populateRanking();
	}

	// Metodo di inizializzazione
	private void getConfig() {
		// ricavo il path del file di configurazione
		Path configFile = Paths.get("serverConfig.txt");
		// estraggo le linee del file
		List<String> param;
		try {
			param = Files.readAllLines(configFile);
			// dalla lista ricavo il numero di porta
			port = Integer.parseInt(param.get(0));
			// il numero di porta UDP
			udpPort = Integer.parseInt(param.get(1));
			// il numero massimo di thread
			maxThreads = Integer.parseInt(param.get(2));
			// ogni quanto cambiare parola in millisecondi
			wordLapse = TimeUnit.HOURS.toMillis(Long.parseLong(param.get(3)));
			// se sono specificate anche parola, tempo di estrazione di questa e traduzione
			if (param.size()>4) {
				word = param.get(4);
				wordExtraction = new Timestamp(0);
				wordExtraction.setTime(Long.parseLong(param.get(5)));
				translation = param.get(6);
			}
			else extractWord();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Avvio il server, che entra in ascolto
	public void startServer() throws IOException {

		// Avvio server RMI
		rmi = new RemoteWordleServer();
		Registry reg = LocateRegistry.createRegistry(2020);
        try {
			reg.bind("RemoteWordleService", rmi);
		} catch (AlreadyBoundException e) {
			e.printStackTrace();
		}
		rmi.updateTop3(userRank);

		// Creo shutdown hook
		Thread sdHook = new Thread(()-> closeServer());
		Runtime.getRuntime().addShutdownHook(sdHook);
		// Creo socket UDP per gruppo multicast
		udpSocket = new DatagramSocket();
		multicastGroup = InetAddress.getByName("225.0.0.0");
		// Creo la socket di ascolto passiva e la collego
		welcomeSocket = ServerSocketChannel.open();
		welcomeSocket.bind(new InetSocketAddress("localhost", port));
		welcomeSocket.configureBlocking(false);
		// Istanzio il selector
		selector = Selector.open();
		// Registro per operazioni accept
		welcomeSocket.register(selector, SelectionKey.OP_ACCEPT);
		// Avvio la threadpool che gestirà le richieste con un factory method
		ThreadPoolExecutor exec = (ThreadPoolExecutor)Executors.newFixedThreadPool(maxThreads);

		// Loop di lettura
		while (true) {
			// Controllo se devo estrarre nuova parola
			Timestamp now = new Timestamp(System.currentTimeMillis());
			if (now.getTime()-wordExtraction.getTime()>=wordLapse) extractWord();
			selector.select();
			// Insieme di chiavi
			Set<SelectionKey> selKeys = selector. selectedKeys();
			// Iteratore sulle chiavi per monitorare i canali
			Iterator<SelectionKey> iter = selKeys.iterator();

			while (iter.hasNext()) {
				SelectionKey currKey = iter.next();
				// Se un client richiede connessione
				if(currKey.isAcceptable()) {
					acceptConnection(selector, welcomeSocket);
				}
				
				// Se un client ha richiesta
				if(currKey.isReadable()) {
					SocketChannel chWithRequest = (SocketChannel)currKey.channel();
					// Leggo la richiesta
					ByteBuffer req = ByteBuffer.allocate(32);
					int bytesRead = chWithRequest.read(req);
					req.flip();
					int reqCode=-1;
					String opArg = null;
					// Se ho letto >= 1 bytes
					if (bytesRead >= 1) {
						// Primo byte = codice richiesta
						byte [] code = new byte[1];
						req.get(code,0,1);
						reqCode = Integer.parseInt(new String(code,StandardCharsets.UTF_8));
						if (reqCode!=0)	{
							byte [] guessArr = new byte[req.remaining()];
							req.get(guessArr);
							opArg = new String(guessArr, StandardCharsets.UTF_8);
						}
					}
					// Se client chiede logout gestisco chiusura socket
					if (reqCode==0 || bytesRead==-1) {
						currKey.cancel();
						currKey.channel().close();
					}

					// Passo a thread worker
					else if (reqCode!=-1) {
						WordleTask task = new WordleTask(chWithRequest, reqCode, opArg);
						exec.submit(task);
					}
				}
				iter.remove();
			}
		}
	}

	// Accetta connessione dei client
 	private void acceptConnection(Selector selector, ServerSocketChannel serverSocket) throws IOException {
		// Accetto la connessione richiesta
		SocketChannel client = serverSocket.accept();
		// La inserisco tra le connessioni da monitorare
		client.configureBlocking(false);
		client.register(selector, SelectionKey.OP_READ);
	}

	// Estraggo una parola da word.txt
	private void extractWord(){
		try {
			Path vocab = Paths.get("words.txt");
			// Conto numero di parole disponibili
			BufferedReader counter = new BufferedReader(new FileReader("words.txt"));
			int lines = 0;
			while (counter.readLine()!=null) lines++;
			counter.close();
			// Estraggo parola casuale dalla pool
			int chosenLine = ThreadLocalRandom.current().nextInt(0,lines+1);
			word = Files.readAllLines(vocab).get(chosenLine);
			// Salvo timestamp dell'estrazione
			wordExtraction = new Timestamp(System.currentTimeMillis());
			// Ottengo traduzione della parola
			getTranslation();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Ottiene e salva la traduzione della parola corrente
	private void getTranslation () {
		try {
			// Mi connetto al servizio
			URL url = new URL("https://api.mymemory.translated.net/get?q="+word+"&langpair=en|it");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			// Estraggo il field della traduzione
            String response = in.readLine();
			String before = "\"translatedText\":\"";
			String after = "\",\"match";
            translation = response.substring(response.indexOf(before)+before.length(), response.indexOf(after));
            connection.disconnect();
        } catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Gestisce chiusura del server in caso di SIG
	private void closeServer() {
		// Server salva ultima parola usata e tempo di estrazione
		Path p = Paths.get("serverConfig.txt");
		try {
			List <String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
			// Se config non aveva questi parametri : Append
			if (lines.size()<=4) {
				String toAppend = String.format("%s\n%s\n%s", word, Long.toString(wordExtraction.getTime()),translation);
				Files.write(p, toAppend.getBytes(), StandardOpenOption.APPEND);
			}
			// Se già ne aveva dall'avvio precedente li sostituisco
			else {
				lines.set(4, word);
				lines.set(5, Long.toString(wordExtraction.getTime()));
				lines.set(6, translation);
				Files.write(p, lines, StandardCharsets.UTF_8);
			}
			// Serializzo database
			Database usersDB = Database.getDB();
			usersDB.serializeDB();
			// Chiudo socket udp
			udpSocket.close();
			// Chiudo selettore
			selector.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
    }

	// Classe interna: task per i thread
	private class WordleTask implements Runnable {
	
		private SocketChannel client;
		private int op;
		private String parameter;
		
		@Override
		public void run() {

			// Switch per controllare quale operazione deve essere eseguita
			switch (op) {
				//Ricevo guess e invio hint a user
				case 1:
					play();
					break;
				// Invio traduzione per la parola
				case 2:
					sendTranslation();
					break;
				// Condivido esito dell'ultima partita di un utente
				case 4:
					shareUserMatch();
					break;
				// Invio ad un utente le sue statistiche
				case 5:
					sendStats();
					break;
				// Aggiorno ultima parola per cui user ha già giocato
				case 6:
					updateLastTimePlayed();
					break;
				// Aggiorno punteggio di un utente
				case 7:
					updateUserScore();
					break;
				// Controllo se uno user ha già giocato per la parola corrente
				case 8:
					hasPlayed();
					break;
				// Controllo se lo user ha dei tentativi in sospeso
				case 9:
					hasPending();
					break;
			}
	
		}
	
		// Metodo costruttore: chiede che socket deve servire e quale operazione deve svolgere
		public WordleTask(SocketChannel toServe, int toDo, String opArg) {
			client = toServe;
			op = toDo;
			parameter = opArg;
		}
	
		// Controllo se un giocatore identificato da user ha già giocato per la parola corrente
		public void hasPlayed () {
			try {
				// Chiedo al DB l'ultima parola per cui il client (identificato da username) ha giocato
				Database usersDB = Database.getDB();
				String lastWordGuess = usersDB.getLastPlayed(parameter);
				int code = 0;
				// Controllo se gioca per la prima volta
				if (lastWordGuess == null) code = 1;
				// Altrimenti controllo se la parola attuale è diversa
				else if (!lastWordGuess.equals(word)) code = 1;
				// Invio risposta al giocatore
				ByteBuffer out = ByteBuffer.allocate(32);
				out.putInt(code);
				out.flip();
				client.write(out);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Controllo se user ha una partita avviata per la parola corrente
		public void hasPending () {
			// Chiedo al DB se l'utente ha già dei tentativi per la parola attuale
			Database usersDB = Database.getDB();
			int triesMade = usersDB.hasPending(parameter, word);
			// Invio a utente la risposta
			ByteBuffer out = ByteBuffer.allocate(4);
			out.putInt(triesMade);
			out.flip();
			try {
				client.write(out);
				// Se ci sono stati dei tentativi li invio, ognuno seguito dal suggerimento relativo
				if (triesMade!=-1) {
					String tries = usersDB.getAttemptString(parameter);
					out = ByteBuffer.wrap(tries.getBytes());
					client.write(out);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Aggiorno ultima parola per cui uno user ha giocato e esito partita
		public void updateLastTimePlayed () {
			String [] pars = parameter.split(" ");
			String username = pars[0];
			String result = pars[1];
			Database usersDB = Database.getDB();
			boolean won = (Integer.valueOf(result)==0);
			usersDB.updateLastPlayed(username, word, won);
			sendAck(0);
		}

		// Aggiorno punteggio di uno user dopo una partita vinta in tries tentativi
		public void updateUserScore () {
			// Token stringa: username + numero di tentativi impiegati
			String [] pars = parameter.split(" ");
			String username = pars[0];
			String tries = pars[1];
			// Aggiorno database
			Database usersDB = Database.getDB();
			float oldScore = usersDB.getScore(username);
			usersDB.updateScore(username, Integer.valueOf(tries));
			// Controllo se top3 viene modificata
			float newScore = usersDB.getScore(username);
			boolean modified = false;
			if (userRank.size()>=3) {
				for (int i=0; i<3; i++)
					if (userRank.get(i).score < newScore) modified = true;
			}
			else modified = true;
			// Aggiorno ranking
			if (oldScore == 0) {
				userRank.add(new RankingEntry(username,newScore));}
			else {
				int ind = userRank.indexOf(new RankingEntry(username,oldScore));
				userRank.get(ind).score = newScore;
			}
			userRank.sort(null);
			// Se la top3 è stata modificata aggiorno server remoto che eseguirà callback
			if (modified)
				try {
					rmi.updateTop3(userRank);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				sendAck(0);
		}

		// Gestisce gioco per un client
		// Il parametro extra contiene username e guess del client
		private void play () {
			// Genero hint 
			String [] pars = parameter.split(" ");
			String username = pars[0];
			String guess = pars[1];
			char [] hint = generateHint(guess);
			ByteBuffer out = ByteBuffer.wrap(new String(hint).getBytes());
			// Invio Hint al client
			try {
				client.write(out);
				// Registro tentativo nel database
				Database db = Database.getDB();
				db.addAttempt(username, guess, String.valueOf(hint), word);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Genera hint comparando guess del client alla parola
		private char [] generateHint (String guess) {
			// Porto tentativo in minuscolo -> not case sensitive 
			guess = guess.toLowerCase();
			// Controllo se il tentativo è valido
			if (!isInVocab(guess)) {
				char [] notValid = {'n','o','t','v','a','l','i','d'};
				return notValid;
			}

			// Inizializzo indizio a bianco
			char[] hint = {'w','w','w','w','w','w','w','w','w','w'};
			// Lista delle lettere non matchate dal tentativo
			ArrayList<Character> notMatched = new ArrayList<>();
			// Per ogni coppia di lettere
			for (int i=0; i<10; i++){
				// Se corrispondono coloro in verde
				if (word.charAt(i)==guess.charAt(i)) hint[i] ='g';
				// Altrimenti aggiungo a notMatched
				else notMatched.add(word.charAt(i));
			}
			// Se ancora mancano dei match
			if (!notMatched.isEmpty()) {
				for (int i=0; i<10; i++) {
					// Se le lettere non corrispondono
					if (word.charAt(i) != guess.charAt(i)) {
						Object ch = guess.charAt(i);
						// Indizio viene colorato di giallo se la lettera è nella parola segreta e rimosso da notMatched
						if (notMatched.remove(ch)) hint[i]='y';
					}
				}
			}
			return hint;	
		}

		// Invia parola e traduzione
		private void sendTranslation () {
			// Controllo che il client abbia effettivamente concluso la partita
			boolean concluded = false;
			Database usersDB = Database.getDB();
			String lastWordGuess = usersDB.getLastPlayed(parameter);
			if (word.equals(lastWordGuess)) concluded = true;
			String toSend;
			// Invio la coppia parola/traduzione o un warnings
			if (concluded) toSend = word+" "+translation;
			else toSend = "Not concluded";
			ByteBuffer out = ByteBuffer.wrap(toSend.getBytes());
			try {
				client.write(out);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Condivido su gruppo multicast l'esito dell'ultima partita dello user
		private void shareUserMatch() {
			// Recupero da DB esito dell'ultima partita dello user
			Database usersDB = Database.getDB();
			String lastMatch = usersDB.getLastMatchResult(parameter);
			// Invio su gruppo multicast
			byte [] buffer = lastMatch.getBytes();
			DatagramPacket pkg = new DatagramPacket(buffer, buffer.length,multicastGroup,udpPort);
			try {
				udpSocket.send(pkg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Controlla se un tentativo di un utente è presente nel vocabolario
		private boolean isInVocab (String guess) {
			boolean ans = false;
			try {ans = Files.lines(Paths.get("words.txt")).anyMatch(l -> l.contains(guess));
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			return ans;
		}

		// Invio le statistiche ad un utente
		private void sendStats() {
			Database usersDB = Database.getDB();
			String stats = usersDB.getUserStats(parameter);
			ByteBuffer out = ByteBuffer.wrap(stats.getBytes());
			// Invio statistiche al client
			try {
				client.write(out);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Invio int ret come ack a client
		private void sendAck(int ret) {
			ByteBuffer ack = ByteBuffer.allocate(4);
			ack.putInt(ret);
			ack.flip();
			try {
				client.write(ack);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}