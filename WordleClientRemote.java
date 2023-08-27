import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.List;

public class WordleClientRemote extends RemoteObject implements ClientRemoteInterface {
    
    private List<RankingEntry> leaderboard;

    // Metodo costruttore
    public WordleClientRemote (List<RankingEntry> lb) throws RemoteException {
        super();
        leaderboard = lb;
    }

    // Metodo che server può richiamare per notificare cambiamenti nella classifica
    public synchronized void rankingUpdate (List<RankingEntry> top3) throws RemoteException {
        leaderboard.clear();
        leaderboard.addAll(top3);
    }

}
