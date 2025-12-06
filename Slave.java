import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;

public class Slave implements Runnable {
    private Socket client;
    
    public Slave(Socket client){
        this.client = client;
    }

    public void run() {
        try{
            System.out.println("[START] new client.");
            ObjectInputStream input_client = new ObjectInputStream(client.getInputStream());
            ObjectOutputStream output_client = new ObjectOutputStream(client.getOutputStream());
            Map<String, Map<String, Integer>> message = 1;

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

    }

    public Socket getClient(){
        return this.client;
    }
}
