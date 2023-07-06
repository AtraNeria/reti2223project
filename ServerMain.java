import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;

public class ServerMain {

    public static void main(String[] args){
        // Creo server
        WordleServer server = new WordleServer();
        // Creo server per servizi da remoto
        try {
            RemoteWordleServer rws = new RemoteWordleServer();
            Registry reg = LocateRegistry.createRegistry(2020);
            reg.bind("RemoteWordleService", rws);
            // Avvio server
            server.startServer();
        }
        catch (IOException | AlreadyBoundException e) {
            e.printStackTrace();
        }
    }
}