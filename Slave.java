import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Slave implements Runnable {
    private Socket client;
    private ArrayList<Machine> machines;
    
    public Slave(Socket client, ArrayList<Machine> machines){
        this.client = client;
        this.machines = machines;
    }

    public void run() {
        try{
            System.out.println("[START] new client.");
            ObjectInputStream input_client = new ObjectInputStream(client.getInputStream());
            ObjectOutputStream output_client = new ObjectOutputStream(client.getOutputStream());
            String message = ((String) input_client.readObject());

            Map<String, Map<String, Integer>> termes = parse_msg(message);

            Map<String, Integer> consommables = termes.get("Besoin");
            Map<String, Integer> produits = termes.get("Resultat");

            //pas terminé
            for(Machine m : machines){
                boolean b = check_ressources(consommables, m);
            }


        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

    }

    public boolean check_ressources(Map<String, Integer> besoins, Machine machine){
        synchronized (machine) {
            for (Map.Entry<String, Integer> entree : besoins.entrySet()) {
                String ressource = entree.getKey();
                int quantite = entree.getValue();

                int nb_dispo = machine.is_ressources_in(ressource);
                if (nb_dispo < quantite) {
                    return false;
                }
            }
            return true;
        }
    }

    public Socket getClient(){
        return this.client;
    }

    public Map<String, Map<String, Integer>> parse_msg(String entry) throws IllegalArgumentException{
        String[] message = entry.split("->");
        if(message.length != 2){
            throw new IllegalArgumentException("Format pas bon\nBon format : 3A + 2C -> 4A");
        }
        Map<String, Integer> consommables = parseur(message[0].trim());
        Map<String, Integer> produits = parseur(message[1].trim());
  
        Map<String, Map<String, Integer>> res = new HashMap<>();
        res.put("Besoin", consommables);
        res.put("Resultat", produits);
        return res;
    }

    public static Map<String, Integer> parseur(String s){
        Pattern pattern = Pattern.compile("(\\d+)([A-Z]+)");
        Map<String, Integer> res = new HashMap<>();

        Matcher match = pattern.matcher(s);
        while (match.find()){
            String lettre = match.group(2);
            int chiffre = Integer.parseInt(match.group(1));
            res.put(lettre, chiffre);
        }
        return res;
    }
}
