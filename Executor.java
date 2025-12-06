import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Executor {
    public static void main(String[] args) {
        List<String> reactions = new ArrayList<>();
        String machineAddress = "127.0.0.1:12345";
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            
            if (arg.startsWith("--")) {
                if (i + 1 >= args.length) {
                    System.err.println("Erreur: L'indicateur " + arg + " nécessite une valeur.");
                    return;
                }
                
                String value = args[i + 1];
                
                switch (arg) {
                    case "--reaction":
                        reactions.add(value);
                        break;
                    case "--machine":
                        machineAddress = value;
                        break;
                    default:
                        System.err.println("Avertissement: Indicateur inconnu ignoré: " + arg);
                        break;
                }
                i++;
            } else {
                 System.err.println("Avertissement: Argument non reconnu ignoré: " + arg);
            }
        }

        String [] mac = machineAddress.split(":");
        String message = "";

            for (String r:reactions){
                message = message + r + "/";
            }
        System.out.println(message);
        System.out.println(mac[0]);
        System.out.println(mac[1]);



        try (Socket s_client = new Socket(mac[0],  Integer. parseInt(mac[1]));
            DataOutputStream output = new DataOutputStream(s_client.getOutputStream())) {
            
            output.writeUTF(message);
            System.out.println("Connexion au Serveur [OK]");
    
           

            System.out.println("Message sent: \"" + message + "\"");
        } catch (IOException E) {
            System.err.println("Erreur Client : Impossible de se connecter ou de communiquer avec le serveur.");
            System.err.println("Détails : " + E.getMessage());
        }
        System.out.println("Client terminé (ressources fermées automatiquement).");



         //--reaction "A -> C" --reaction "B + C -> D" --machine "127.0.0.1:12345"
        
    }
}