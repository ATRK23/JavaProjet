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

    public Socket getClient(){
        return this.client;
    }

    public void run() {
        try{
            System.out.println("[START] new client.");
            ObjectInputStream input_client = new ObjectInputStream(client.getInputStream());
            ObjectOutputStream output_client = new ObjectOutputStream(client.getOutputStream());
            String message = ((String) input_client.readObject());

            System.out.println(message);
            
            Map<String, Map<String, Integer>> termes = parse_msg(message);
            System.out.println(termes);
            
            Map<String, Integer> consommables = termes.get("Besoin");
            //Map<String, Integer> produits = termes.get("Resultat");
            
            ArrayList<Machine> total = new ArrayList<>();
            String ressouPro;
            Integer qtDispo;
            for(Machine m : machines){
                ressouPro = get_ressources_produce(consommables, m);
                if(ressouPro != null){
                    qtDispo = m.is_ressources_in(ressouPro);

                    Integer besoinActuel = consommables.get(ressouPro);

                    if(qtDispo >= besoinActuel){
                        total.add(m);
                        consommables.put(ressouPro, besoinActuel - qtDispo);
                    }
                }
                if(consommables.size() == 0){
                    break;
                }
            }
            System.out.println(total);
            if(total.size() == 0 || consommables.size() > 0){
                output_client.writeUTF("Abord");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

    }

    public String get_ressources_produce(Map<String, Integer> besoins, Machine machine){
        for (Map.Entry<String, Integer> entree : besoins.entrySet()) {
            String ressource = entree.getKey();
            int nb_dispo = machine.is_ressources_in(ressource);
            if(nb_dispo > 0){ 
                return ressource;
            }
        }
        return null;
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
