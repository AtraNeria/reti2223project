import java.io.IOException;

public class ServerMain {

    public static void main(String[] args){
        // Recupero database degli utenti
        Database.getDB();
        // Creo server
        WordleServer server = new WordleServer();
        try {
            // Avvio server
            server.startServer();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}