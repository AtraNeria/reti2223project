import java.net.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.List;

public class WordleServer {
	private int port;
	ServerSocket welcomeSocket;
	
	public WordleServer() {
		//Ottiene le info per configurare il server dal file apposito
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
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Avvio il server, che entra in ascolto
	public void startServer() {
		// TO-DO: start threadpool

		//Attivo la socket passiva
		try {
			welcomeSocket = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// TO-DO: executor receives requests and queues them

	}
}