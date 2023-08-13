import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

    // Restituisce numero di utenti
    public int getDbSize () {
        return usersDB.size();
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

    // Controlla se user ha già fatto dei tentativi per la parola word
    public int hasPending (String username, String word) {
        UserEntry u = usersDB.get(username);
        return u.hasUnfinishedMatch(word);
    }

    // Restituisce una stringa con tutti i tentativi effettuati dallo user, ciascuno seguito dalla relativa hint
    public String getAttemptString (String username) {
        UserEntry u = usersDB.get(username);
        return u.session.getAttempts();
    }

    // Registro nuovo tentativo di user per parola word e relativo suggerimento
    public void addAttempt (String username, String guess, String hint, String word) {
        UserEntry u = usersDB.get(username);
        // Controllo se la parola della sessione corrisponde a quella corrente
        // Se non corrispondono setto la parola per avviare la nuova sessione
        if (!word.equals(u.session.getWord())) u.session.setNewWord(word);
        u.session.addCouple(guess, hint);
    }

    // Restituisce punteggio di user
    public float getScore (String username) {
       return usersDB.get(username).score;
    }

    // Aggiorno last played word per username e totale partite giocate
	public void updateLastPlayed (String username, String word, boolean won) {
		UserEntry userInfo = usersDB.get(username);
		userInfo.setLastPlayed(word, won);
        userInfo.totGamesPlayed++;
	}

    // Aggiorna punteggio di username dopo che ha indovinato in tries tentativi
    public void updateScore (String username, int tries) {
        UserEntry toUpdate = usersDB.get(username);
        toUpdate.totTries+=tries;
        toUpdate.gamesWon++;
        toUpdate.score = toUpdate.gamesWon / (toUpdate.totTries/toUpdate.gamesWon);
    }

    // Restituisce una stringa che contiene le seguenti informazioni su username:
    // il punteggio, il numero di partite vinti, il numero di partite totali, il numero medio di tentativi impiegati per vincere
    // Le informazioni avranno come divisore uno spazio l'una dall'altra
    public String getUserStats (String username) {
        UserEntry user = usersDB.get(username);
        String stats = user.score +" "+user.gamesWon+" "+user.totGamesPlayed+" "+(user.totTries/user.gamesWon);
        return stats;
    }

    // Restituisce una stringa sul risultato dell'ultima partita di username
    public String getLastMatchResult (String username) {
        UserEntry user = usersDB.get(username);
        String lastMatch;
        if (user.lastPlayedWon) lastMatch = username+" ha vinto la sua ultima partita!";
        else lastMatch = username+" ha vinto la sua ultima partita!";
        return lastMatch;
    }

    // Popolo lista di classifica degli user
    public List<RankingEntry> populateRanking () {
        List <RankingEntry> ranks = Collections.synchronizedList(new ArrayList<RankingEntry>());
        for (String user : usersDB.keySet()) {
            // Aggiungo solo gli user che hanno almeno vinto una partita
            float userScore = usersDB.get(user).score;
            if (userScore!=0) ranks.add(new RankingEntry(user, userScore));
        }
        // Ordino gli users
        ranks.sort(null);
        return ranks;
    }

    

    // Classe interna per dati utente
    private class UserEntry {

        char[] password;
        float score;
        int gamesWon;
        int totGamesPlayed;
        int totTries;
        String lastPlayed;
        boolean lastPlayedWon;
        MatchSession session;
    
        // Metodo costruttore
        public UserEntry(char[] pass) {
            password = Arrays.copyOf(pass, pass.length);
            score = 0;
            gamesWon = 0;
            totGamesPlayed = 0;
            totTries = 0;
            lastPlayed = null;
            lastPlayedWon = false;
            session = new MatchSession();
        }
    
        // Aggiorno info su ultima partita
        private void setLastPlayed(String word, boolean won) {
            lastPlayed = word;
            lastPlayedWon = won;
        }
    
        // Controlla se lo user ha una partita in corso non finita per la parola word
        // Restituisce il numero di tentativi già effettuati in caso positivo, -1 altrimenti
        private int hasUnfinishedMatch (String word) {
            if (word.equals(session.getWord()) && (session.getAttemptsNumber()<12)) return session.getAttemptsNumber();
            else return -1;
        }        
    }


    // Classe per i dati legati ad una sessione di gioco lasciata incompleta di un utente
    private class MatchSession {
        // Classe per tenere traccia della sessione di un utente
            String word;
            private List<Attempt> attempts;
    
            // Costruttore
            private MatchSession() {
                word = null;
                attempts = Collections.synchronizedList(new ArrayList<Attempt>(12));
            }
    
            // Aggiungo un tentativo
            private boolean addCouple(String guess, String hint) {
                if (attempts.size()>=12) return false;
                else {
                    Attempt n = new Attempt(guess, hint);
                    return attempts.add(n);
                }
            }
    
            // Ottengo numero di tentativi già fatti
            private int getAttemptsNumber () {
                return attempts.size();
            }
    
            // Restituisce stringa descrivente i tentativi effettuati
            private String getAttempts () {
                String tries = new String();
                for (Attempt a : attempts) {
                    tries = tries.concat(a.word+" ").concat(a.hint+" ");  
                }
                return tries;
            }

            // Restituisce la parola relativa alla sessione
            private String getWord () {
                return word;
            }

            // Imposto parola relativa alla sessione nuova
            private void setNewWord (String w) {
                word = w;
                attempts.clear();
            }
    
            // Coppia parola-suggerimento
            private class Attempt {
                String word;
                String hint;
    
                public Attempt(String w, String h) {
                    word = w;
                    hint = h;
                }
    
            }
    
    }

}