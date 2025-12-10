import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
* Classe Slave correspondant au thread qui gere la communication entre la machine et l'executor
* Chaque Slave est un thread qui gere un client
* client : Socket du client
* machine : Machine associée au Slave
 */
public class Slave implements Runnable {
    private Socket client;
    private Machine machine;
    
    /*
    * Constructeur de la classe Slave
    * @param client : Socket du client
    * @param machine : Machine associée au Slave
    */
    public Slave(Socket client, Machine machine){
        this.client = client;
        this.machine = machine;
    }

    //Getters

    /**
     * 
     * @return Instance de la machine associée
     */
    public Machine getMachine(){
        return this.machine;
    }

    /**
     * 
     * @return Socket du client
     */
    public Socket getClient(){
        return this.client;
    }

    /*
    * Méthode run
    * Gère la communication entre la machine et l'executor
    */
    @Override
    public void run() {
        try{
            ObjectOutputStream output_client = new ObjectOutputStream(client.getOutputStream());
            ObjectInputStream input_client = new ObjectInputStream(client.getInputStream());
            Object o = input_client.readObject();
            if(o instanceof String){
                String entry = ((String) o);
                
                System.out.println("Message recu : " + o);
                //Convertis la reaction en Map<String, Integer>
                Map<String, Map<String, Integer>> termes = parse_msg(entry);
                Map<String, Integer> consommables = termes.get("Besoin");
                Map<String, Integer> produits = termes.get("Resultat");
                
                Map<String, Integer> stock = notre_Stock(consommables, produits);
                output_client.writeObject(stock);
                System.out.println("Action effectuable envoyé : " + stock);
                System.out.println("Stock actuel : "+ machine.get_ressources());
            }
            else if(o instanceof Map){
                @SuppressWarnings("unchecked")
                Map<String, Integer> reac = (Map<String, Integer>) o;
                System.out.println("Action à effectuer recu : " + reac);
                for (Map.Entry<String, Integer> r : reac.entrySet()) {
                    String ressource = r.getKey();
                    int nb = r.getValue();
                    if(machine.can_Receive(ressource)){
                        if(nb<0){
                            machine.removeRessource(nb * -1, ressource);
                        }else{
                            machine.addRessource(nb, ressource);
                        }
                    }
                }
                System.out.println("Processed\nNouveau Stock : " + machine.get_ressources());
            }
        } catch (Exception e) {
            System.err.println(e);
        }

    }

    /*
    * Méthode get_ressources_produce
    * Vérifie si la machine possède les ressources nécessaires pour produire une réaction
    * 
    * @param besoins : Map des ressources consommables
    * @param machine : Machine à vérifier
    */
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

    /*
    * Méthode notre_Stock
    * Calcule le stock que la machine peut fournir pour une réaction donnée
    * 
    * @param besoins : Map des ressources consommables
    * @param produits : Map des ressources produites
    * @return une Map représentant le stock que la machine peut fournir pour la réaction
    */
    public Map<String, Integer> notre_Stock(Map<String, Integer> besoins, Map<String, Integer> produits){
        Map<String, Integer> stock = new HashMap<>();
        for (Map.Entry<String, Integer> entree : besoins.entrySet()) {
            String ressource = entree.getKey();
            int nb = entree.getValue();
            if( nb <= machine.is_ressources_in(ressource)){ 
                stock.put(ressource, stock.getOrDefault(ressource, 0) - nb);
            }
        }
        for (Map.Entry<String, Integer> entree : produits.entrySet()) {
            String ressource = entree.getKey();
            int nb = entree.getValue(); 
            if( machine.can_Receive(ressource)){ 
                stock.put(ressource, stock.getOrDefault(ressource, 0) + nb);
            }
        }
        return stock;
    }

    //Parseur de message

    /*
    * Parse un message de la forme "3A + 2C -> 4A" en une Map<String, Map<String, Integer>>
    * La clé "Besoin" contient une Map des ressources consommables
    * La clé "Resultat" contient une Map des ressources produites 
    *   
     * @param entry : chaîne de caractères à analyser
     * @return une Map avec les ressources consommables et produites
     */
    public Map<String, Map<String, Integer>> parse_msg(String entry) throws IllegalArgumentException{
        entry = entry.replaceFirst("(?i)^\\s*LIST\\s+", "").trim();
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

    /*
    * Méthode parseur
    * Convertis une chaîne de caractères en une Map<String, Integer>
    * Les lettres représentent les ressources et les chiffres représentent les quantités
    * 
    * @param s : chaîne de caractères à analyser
    * @return une Map avec les lettres comme clés (ressource) et les chiffres comme valeurs (quantité)
    */
    public static Map<String, Integer> parseur(String s){
        //Ajoute des "1" devant chaque lettre si recoit A + 2C -> D
        String regex = "(?<!\\d)([A-Z]+)";
        String messagePrepare = s.replaceAll(regex, "1$1");

        Pattern pattern = Pattern.compile("(\\d+)([A-Z])");
        Map<String, Integer> res = new HashMap<>();

        Matcher match = pattern.matcher(messagePrepare);
        while (match.find()){
            String lettre = match.group(2);
            int chiffre = Integer.parseInt(match.group(1));
            res.put(lettre, chiffre);
        }
        return res;
    }
}
