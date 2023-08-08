import java.util.Arrays;

public class UserEntry {

    char[] password;
    int score;
    int gamesWon;
    int totGamesPlayed;
    int totTries;
    String lastPlayed;

    // Metodo costruttore
    public UserEntry(char[] pass) {
        password = Arrays.copyOf(pass, pass.length);
        score = 0;
        gamesWon = 0;
        totGamesPlayed = 0;
        totTries = 0;
        lastPlayed = null;
    }

    // Aggiorno lastPlayed
    public void setLastPlayed(String word) {
        lastPlayed = word;
    }

}