import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UserEntry {

    char[] password;
    int score;
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
    public void setLastPlayed(String word, boolean won) {
        lastPlayed = word;
        lastPlayedWon = won;
    }

    // Controlla se lo user ha una partita in corso non finita per la parola word
    // Restituisce il numero di tentativi già effettuati in caso positivo, -1 altrimenti
    public int hasUnfinishedMatch (String word) {
        if (word.equals(session.getWord()) && (session.getAttemptsNumber()<12)) return session.getAttemptsNumber();
        else return -1;
    }

    // Restituisce una stringa contenente tentativi e relativi hint 
    public String getAttemptsString () {
        return session.getAttempts();
    }

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
            private boolean addAttempt(String guess, String hint) {
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
                String tries = null;
                for (Attempt a : attempts) {
                  tries = tries.concat(a.word).concat(a.hint);  
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