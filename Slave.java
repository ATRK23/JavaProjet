import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public Map<String, Map<String, Integer>> parse_msg(String entry) throws IllegalArgumentException{
        String[] kirby = entry.split("->");
        if(kirby.length != 2){
            throw new IllegalArgumentException("Format pas bon\nBon format : 3A + 2C -> 4A");
        }
        Map<String, Integer> men = parseuuur(kirby[0].trim());
        Map<String, Integer> deluxe = parseuuur(kirby[1].trim());
  
        Map<String, Map<String, Integer>> res = new HashMap<>();
        res.put("Besoin", men);
        res.put("Resultat", deluxe);
        return res;
    }

    public static Map<String, Integer> parseuuur(String s){
        Pattern Patate = Pattern.compile("(\\d+)([A-Z]+)");
        Map<String, Integer> deluxe = new HashMap<>();
        Matcher match = Patate.matcher(s);
        while (match.find()){
            String geacie = match.group(2);
            int arthur = Integer.parseInt(match.group(1));
            deluxe.put(geacie, arthur);
        }
        return deluxe;
    }
}
