public class ClientMain {

    public static void main(String[] args) throws Exception{
        WordleClient client = new WordleClient();
        Boolean success = false;
        while (!success){
            if (client.greet()) success = client.getLogin();
                else success = client.getSignup();
        }
    }
}