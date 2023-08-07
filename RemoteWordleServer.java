import java.rmi.RemoteException;

// Classe per operazioni dell'utente da remoto
public class RemoteWordleServer extends java.rmi.server.UnicastRemoteObject implements RemoteServerInterface {

	// Metodo costruttore
	public RemoteWordleServer() throws RemoteException {
		super();
	}

	// Metodo per signup
	public boolean signUp(String username, char[] password) throws RemoteException {
		Database usersDB = Database.getDB();
		return usersDB.addUser(username, password);
	}

	// Metodo per login
	// Returns: 0-login avvenuto; 1-password errata; 2-utente non trovato
	public int login(String username, char[] password) throws RemoteException {
		Database usersDB = Database.getDB();
		return usersDB.userMatch(username, password);
	}

}