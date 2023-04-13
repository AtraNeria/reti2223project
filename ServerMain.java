import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.*;

public class ServerMain {

    public static void main(String[] args){
        // Creo server
        WordleServer server = new WordleServer();
        // Creo server per servizi da remoto
        try {
            RemoteWordleServer rws = new RemoteWordleServer();
            // TO-DO well-formed URL?
            Naming.rebind("RemoteWordleService", rws);
        }
        catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }
        // Avvio server
        try {
            server.startServer();
        } catch (IOException e) {
            // TO-DO Auto-generated catch block
            e.printStackTrace();
        }
    }
}