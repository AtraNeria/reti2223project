import java.lang.System;

public class UserEntry {

    char[] password;
    int score;

    // Metodo costruttore
    public UserEntry(char[] pass){
        arraycopy(pass,0,password,0,pass.length);
        score = 0;
    }

    // Metodo per aumentare il punteggio di un utente
    public void ScoreIncrease(int plus) {
        score = score + plus;
    }
}