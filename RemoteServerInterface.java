import java.rmi.RemoteException;
import java.sql.Timestamp;

public interface RemoteServerInterface extends java.rmi.Remote {
    
    // Method for remote Signup
    public boolean signUp(String username, char[] password) throws RemoteException;
    // Method for remote Login
    public int login(String username, char[] password) throws RemoteException;
    // Method to check last time user has played
    public Timestamp getLastPlayed(String username) throws RemoteException;
}
