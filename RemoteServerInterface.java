import java.rmi.RemoteException;

public interface RemoteServerInterface extends java.rmi.Remote {
    
    // Method for remote Signup
    public boolean signUp(String username, char[] password) throws RemoteException;
    // Method for remote Login
    public int login(String username, char[] password) throws RemoteException;
}
