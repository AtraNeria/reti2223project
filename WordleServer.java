import java.net.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WordleServer {
	private int port;
	private int maxThreads;
	private long wordLapse;
	private ServerSocketChannel welcomeSocket;
	Timestamp wordExtraction;
	private String word;

	public WordleServer() {
		// Ottiene le info per configurare il server dal file apposito
		this.getConfig();
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
			// il numero massimo di thread
			maxThreads = Integer.parseInt(param.get(1));
			// ogni quanto cambiare parola in millisecondi
			wordLapse = TimeUnit.HOURS.toMillis(Long.parseLong(param.get(2)));
			// se sono specificate anche parola e tempo di estrazione di questa
			if (param.size()>3) {
				word = param.get(3);
				wordExtraction = new Timestamp(TimeUnit.HOURS.toMillis(Long.parseLong(param.get(2))));
				// TO-DO: when close server write current word and wordExtraction at the end of config
			}
			else extractWord();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Avvio il server, che entra in ascolto
	public void startServer() throws IOException {

		// Creo la socket di ascolto passiva e la collego
		welcomeSocket = ServerSocketChannel.open();
		InetSocketAddress addr = new InetSocketAddress("localhost", port); // TEST
		welcomeSocket.bind(new InetSocketAddress("localhost", port));
		System.out.println(addr);	// TEST
		welcomeSocket.configureBlocking(false);
		// Istanzio il selector
		Selector selector = Selector.open();
		// Registro per operazioni accept
		welcomeSocket.register(selector, SelectionKey.OP_ACCEPT);
		// Avvio la threadpool che gestirà le richieste con un factory method
		ThreadPoolExecutor exec = (ThreadPoolExecutor)Executors.newFixedThreadPool(maxThreads);

		// Loop di lettura
		while (true) {
			// Controllo se devo estrarre nuova parola
			Timestamp now = new Timestamp(System.currentTimeMillis());
			if (now.getTime()-wordExtraction.getTime()>= wordLapse) extractWord();
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
					System.out.println("Got connected"); // TEST
				}
				// Se un client ha richiesta
				if(currKey.isReadable()) {
					SocketChannel chWithRequest = (SocketChannel)currKey.channel();
					ByteBuffer req = ByteBuffer.allocate(32);
					chWithRequest.read(req); // TO-DO: check != 0
					//while(!req.hasRemaining()) {
					//	if (chWithRequest.read(req)==-1) {//TO-DO: handle close};
					//}
					int reqCode = req.getInt(0);
					if (reqCode==1)
						System.out.println(reqCode);   // TEST
					WordleTask task = new WordleTask(chWithRequest, reqCode);
					exec.submit(task);
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
			System.out.println(word);	// TEST

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Classe interna: task per i thread
	private class WordleTask implements Runnable {
	
		private SocketChannel client;
		private int op;
		
		@Override
		public void run() {
			// Invio Ack a client
			try{
				ByteBuffer ack = ByteBuffer.allocate(4);
				ack.putInt(0);
				ack.flip();
				client.write(ack);
			}
			catch (IOException e) {e.printStackTrace();}

			// Switch per controllare quale operazione deve essere eseguita
			switch (op) {
				case 0:
					// TO-DO: Show
					break;
				case 1:
					play();
					break;
				case 2:
					//TO-DO: Word
					break;
				case 3:
					//TO-DO: Stats
					break;
				case 4:
					//TO-DO: Share
					break;
			}
	
		}
	
		// Metodo costruttore: chiede che socket deve servire e quale operazione deve svolgere
		public WordleTask(SocketChannel toServe, int toDo) {
			client = toServe;
			op = toDo;
		}
	
		// Gestisce gioco per un client
		private void play () {
			ByteBuffer in = ByteBuffer.allocate(32);
			try {
				// Leggo guess del client
				if (client.read(in)>=0){
					in.flip();
					String guess = StandardCharsets.UTF_8.decode(in).toString();
					// TO-DO: compare word to guess
				}
			}
			catch (IOException e) {e.printStackTrace();}
		}
	}

}