import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class parseur{
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