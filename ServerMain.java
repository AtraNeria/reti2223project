import java.io.IOException;

public class ServerMain {

    public static void main(String[] args){
        WordleServer server = new WordleServer();
        try {
            server.startServer();
        } catch (IOException e) {
            // TO-DO Auto-generated catch block
            e.printStackTrace();
        }
    }
}