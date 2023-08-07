import java.util.Arrays;

public class UserEntry {

    char[] password;
    int score;
    int gamesWon;
    int totTries;
    String lastPlayed;

    // Metodo costruttore
    public UserEntry(char[] pass) {
        password = Arrays.copyOf(pass, pass.length);
        score = 0;
        gamesWon = 0;
        totTries = 0;
        lastPlayed = null;
    }

    // Metodo per aumentare il punteggio di un utente
    public void ScoreIncrease(int plus) {
        if (plus >= 0) score = score + plus;
    }

    // Aggiorno astPlayed
    public void setLastPlayed(String word) {
        lastPlayed = word;
    }

}