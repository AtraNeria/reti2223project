import java.lang.System;
import java.util.Arrays;

public class UserEntry {

    char[] password;
    int score;
    int games;
    int triesAvrg;

    // Metodo costruttore
    public UserEntry(char[] pass) {
        password = Arrays.copyOf(pass, pass.length);
        score = 0;
        games = 0;
        triesAvrg = 0;
    }

    // Metodo per aumentare il punteggio di un utente
    public void ScoreIncrease(int plus) {
        if (score >= 0) score = score + plus;
    }
}