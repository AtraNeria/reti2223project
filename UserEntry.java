import java.lang.System;
import java.util.Arrays;

public class UserEntry {

    char[] password;
    int score;

    // Metodo costruttore
    public UserEntry(char[] pass) {
        password = Arrays.copyOf(pass, pass.length);
        score = 0;
    }

    // Metodo per aumentare il punteggio di un utente
    public void ScoreIncrease(int plus) {
        // TO-DO: check if plus >= 0
        score = score + plus;
    }
}