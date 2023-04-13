import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

// Classe per operazioni dell'utente da remoto
public class RemoteWordleServer extends java.rmi.server.UnicastRemoteObject implements RemoteServerInterface {
	//Remote server tiene traccia degli utenti
	private ConcurrentHashMap<String, UserEntry> usersDB;

	//Metodo costruttore
	public RemoteWordleServer() throws RemoteException {
		super();
		usersDB = new ConcurrentHashMap <String,UserEntry>();
	}

	//Metodo per signup
	public boolean signUp(String username, char[] password) throws RemoteException {
		boolean ex = true;
		UserEntry userData = new UserEntry(password);
		if (usersDB.putIfAbsent(username, userData) != null) ex=false;
		return ex;
	}

}