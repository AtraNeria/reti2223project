import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Database {

    // Oggetto DB
    private static Database db;
    private ConcurrentHashMap<String, UserEntry> usersDB;

    // Costruttore privato
    private Database() {

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

    // Metodo pubblico per accedere al DB
    // Thread-safe
    public static synchronized Database getDB() {
        if (db == null) {
            db = new Database();
        }
        // restituisce singleton
        return db;
    }

    // Salva su file json lo stato del database degli utenti
	public void serializeDB () {

		// hashmap -> stringa
		Gson gson = new Gson(); 
		String json = gson.toJson(usersDB);

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

    // Aggiunge user a DB
    public boolean addUser (String username, char[] password) {
		boolean ex = true;
		UserEntry userData = new UserEntry(password);
		if (usersDB.putIfAbsent(username, userData) != null) ex=false;
		return ex;
    }

    // Controlla se c'è un match per username, e se la password corrispettiva è corretta
    public int userMatch (String username, char[] password) {
		int ex = 0;
		UserEntry loginAttempt = usersDB.get(username);
		if (loginAttempt==null) ex = 2;
		//TO-DO: custom exceptions
		else if (!Arrays.equals(password, loginAttempt.password)) ex = 1;
		return ex;
    }

    // Restituisce un timestamp che indica l'ultima volta in cui il giocatore username ha giocato
    public String getLastPlayed (String username) {
        return usersDB.get(username).lastPlayed;
    }
	
    // Aggiorno last played word per username
	public void updateLastPlayed (String username, String word) {
		UserEntry userInfo = usersDB.get(username);
		userInfo.setLastPlayed(word);
	}

    // Aggiorna punteggio di username dopo che ha indovinato in tries tentativi
    public void updateScore (String username, int tries) {
        UserEntry toUpdate = usersDB.get(username);
        toUpdate.totTries+=tries;
        toUpdate.gamesWon++;
        toUpdate.score = toUpdate.gamesWon / (toUpdate.totTries/toUpdate.gamesWon);
    }

}