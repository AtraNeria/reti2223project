import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

// Classe per operazioni dell'utente da remoto
public class RemoteWordleServer extends java.rmi.server.UnicastRemoteObject implements RemoteServerInterface {
	// Remote server tiene traccia degli utenti
	private ConcurrentHashMap<String, UserEntry> usersDB;

	// Metodo costruttore
	public RemoteWordleServer() throws RemoteException {
		super();
		usersDB = new ConcurrentHashMap <String,UserEntry>();

		// GSon + reflection per deserializzare il db
		Gson gson = new Gson();
		TypeToken<ConcurrentHashMap<String, UserEntry>> dbType = new TypeToken<ConcurrentHashMap<String, UserEntry>>(){};
		Path dbFilePath = FileSystems.getDefault().getPath("users.json");
		File f = new File("users.json");
		try {
			// Se esiste già un database
			if (f.exists()){
				String jsonS = Files.readString(dbFilePath);
				usersDB = gson.fromJson(jsonS, dbType);
			}
		}
		catch(IOException e) {e.printStackTrace();}
	}

	// Metodo per signup
	public boolean signUp(String username, char[] password) throws RemoteException {
		boolean ex = true;
		UserEntry userData = new UserEntry(password);
		if (usersDB.putIfAbsent(username, userData) != null) ex=false;
		return ex;
	}

	// Returns: 0-login avvenuto; 1-password errata; 2-utente non trovato
	public int login(String username, char[] password) throws RemoteException {
		int ex = 0;
		UserEntry loginAttempt = usersDB.get(username);
		if (loginAttempt==null) ex = 2;
		//TO-DO: custom exceptions
		else if (!Arrays.equals(password, loginAttempt.password)) ex = 1;
		return ex;
	}

	// Restituisce un timestamp che indica l'ultima volta in cui il giocatore username ha giocato
	public Timestamp getLastPlayed(String username) throws RemoteException {
		return usersDB.get(username).lastPlayed;
	}

	// Salva su file json lo stato del database utenti db
	public void serializeDB (ConcurrentHashMap<String, UserEntry> db) {

		// hashmap -> stringa
		Gson gson = new Gson(); 
		String json = gson.toJson(db);

		// apro file json e uso bufferedWriter per scriverci la string
		try {
            Path filePath = FileSystems.getDefault().getPath("users.json");
            BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8);
            writer.write(json,0,json.length());
            writer.close();
        }
        catch (InvalidPathException | IOException e) {
            System.out.println("Fail");
            e.printStackTrace();
        }
	}

}