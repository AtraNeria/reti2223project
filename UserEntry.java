import java.lang.System;
import java.sql.Timestamp;
import java.util.Arrays;

public class UserEntry {

    char[] password;
    int score;
    int games;
    int triesAvrg;
    Timestamp lastPlayed;

    // Metodo costruttore
    public UserEntry(char[] pass) {
        password = Arrays.copyOf(pass, pass.length);
        score = 0;
        games = 0;
        triesAvrg = 0;
        lastPlayed = null;
    }

    // Metodo per aumentare il punteggio di un utente
    public void ScoreIncrease(int plus) {
        if (score >= 0) score = score + plus;
    }
}