package client;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import server.RankingEntry;

public interface ClientRemoteInterface extends Remote {
    
    // Callback per modifiche alla classifica dei primi tre utenti
    public void rankingUpdate (List<RankingEntry> top3) throws RemoteException;

}
