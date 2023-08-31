package server;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import client.ClientRemoteInterface;

// Classe per operazioni dell'utente da remoto
public class RemoteWordleServer extends java.rmi.server.UnicastRemoteObject implements RemoteServerInterface {

	// Lista di client registrati per callback
	private List <ClientRemoteInterface> clients;
	private List <RankingEntry> top3 = Collections.synchronizedList(new ArrayList<RankingEntry>());

	// Metodo costruttore
	public RemoteWordleServer() throws RemoteException {
		super();
		clients = Collections.synchronizedList(new ArrayList<ClientRemoteInterface>());
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

	// Registrazione di un client per il callback
	public void registerForCallback (ClientRemoteInterface clientIn) throws RemoteException {
		// Registro client se non compare già tra i client registrati
		if (!clients.contains(clientIn)) {
			clients.add(clientIn);
			// Invio classifica attuale
			ClientRemoteInterface ci = (ClientRemoteInterface) clientIn;
			ci.rankingUpdate(top3);
		}
	}

	// Rimozione di client da lista per callback
	public void unregisterForCallback (ClientRemoteInterface clientIn) throws RemoteException {
		clients.remove(clientIn);
	}

	// Aggiorno top3
	public void updateTop3 (List <RankingEntry> rank) throws RemoteException {
		if (rank == null || rank.size()<=3) top3 = rank;
		else {
			top3.clear();
			for (int i=0; i<3; i++) top3.add(rank.get(i));
		}
		// Informo i client del cambiamento
		Iterator<ClientRemoteInterface> i = clients.iterator();
		while (i.hasNext()) {
			ClientRemoteInterface cl = (ClientRemoteInterface) i.next();
			cl.rankingUpdate(new ArrayList<RankingEntry>(top3));
		}
	}

}