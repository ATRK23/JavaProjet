import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.KeyStore.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Slave implements Runnable {
    private Socket client;
    private Machine machine;
    
    public Slave(Socket client, Machine machine){
        this.client = client;
        this.machine = machine;
    }

    public Machine getMachine(){
        return this.machine;
    }

    public Socket getClient(){
        return this.client;
    }

    public void run() {
        try{
            System.out.println("[START] new client.");
            ObjectInputStream input_client = new ObjectInputStream(client.getInputStream());
            ObjectOutputStream output_client = new ObjectOutputStream(client.getOutputStream());
            String entry = ((String) input_client.readObject());
            
            
            //Ajoute des "1" devant chaque lettre si recoit A + 2C -> D
            String regex = "(?<!\\d)([A-Z]+)";
            String messagePrepare = entry.replaceAll(regex, "1$1");

            //Convertis la regles en Map <String, Integer>
            Map<String, Map<String, Integer>> termes = parse_msg(messagePrepare);
            Map<String, Integer> consommables = termes.get("Besoin");
            Map<String, Integer> produits = termes.get("Resultat");
            
            Map<String, Integer> stock = notre_Stock(consommables, produits);
            System.out.println(stock);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

    }
    //Renvoie le nom d'une ressource si elle peut etre traiter
    public String get_ressources_produce(Map<String, Integer> besoins, Machine machine){
        for (Map.Entry<String, Integer> entree : besoins.entrySet()) {
            String ressource = entree.getKey();
            int nb_dispo = machine.is_ressources_in(ressource);
            if(nb_dispo > 0 && nb_dispo >= besoins.get(ressource)){ 
                return ressource;
            }
        }
        return null;
    }


    //Parsing des regles
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
        Pattern pattern = Pattern.compile("(\\d+)([A-Z])");
        Map<String, Integer> res = new HashMap<>();

        Matcher match = pattern.matcher(s);
        while (match.find()){
            String lettre = match.group(2);
            int chiffre = Integer.parseInt(match.group(1));
            res.put(lettre, chiffre);
        }
        return res;
    }

    public Map<String, Integer> notre_Stock(Map<String, Integer> besoins, Map<String, Integer> produits){
        Map<String, Integer> stock = new HashMap<>();
        for (Map.Entry<String, Integer> entree : besoins.entrySet()) {
            String ressource = entree.getKey();
            int nb = entree.getValue();
            if( nb <= machine.is_ressources_in(ressource)){ 
                stock.put(ressource, nb * -1);
            }
        }
        for (Map.Entry<String, Integer> entree : produits.entrySet()) {
            String ressource = entree.getKey();
            int nb = entree.getValue(); 
            if( machine.can_Receive(ressource)){ 
                stock.put(ressource, nb);
            }
        }
        return stock;
    }
}
