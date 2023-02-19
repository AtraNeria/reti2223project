import java.nio.channels.SocketChannel;

public class WordleTask implements Runnable {

    private SocketChannel client;
    private int op;
    
    @Override
    public void run() {
        // Switch per controllare quale operazione deve essere eseguita
        switch (op) {
            case 0:
                //TO-DO: Login
                break;
            case 1:
                //TO-DO: Play
                break;
            case 2:
                //TO-DO: Word
                break;
            case 3:
                //TO-DO: Stats
                break;
            case 4:
                //TO-DO: Share
                break;
            case 5:
                //TO-DO: Show
                break;
        }

    }

    // Metodo costruttore: chiede che socket deve servire e quale operazione deve svolgere
    public WordleTask(SocketChannel toServe, int toDo) {
        client = toServe;
        op = toDo;
    }

}