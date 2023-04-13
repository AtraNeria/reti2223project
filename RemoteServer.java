import java.rmi.Remote;
import java.rmi.*;

// Interface for remote server methods
interface RemoteServerInterface extends Remote {

    // Method for remote SignUp
    public boolean signUp(String username, char[] password) throws RemoteException;
}