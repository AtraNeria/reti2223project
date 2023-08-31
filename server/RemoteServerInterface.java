package server;
import java.rmi.RemoteException;

import client.ClientRemoteInterface;

public interface RemoteServerInterface extends java.rmi.Remote {
    
    // Metodo per signup remoto
    public boolean signUp(String username, char[] password) throws RemoteException;
    // Metodo per login remoto
    public int login(String username, char[] password) throws RemoteException;
    // Metodo per registrazione del client per essere notificato dei cambiamenti della classifica
    public void registerForCallback (ClientRemoteInterface clientIn) throws RemoteException;
    // Metodo per cancellare la registrazione a servizio di callback
    public void unregisterForCallback (ClientRemoteInterface clientIn) throws RemoteException;
}
