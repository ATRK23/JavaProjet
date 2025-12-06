//import java.util.concurrent.*;
//import java.io.*;
//import java.net.Socket;

public class MainServer{
    public static void main(String[] args){
        try {
            Machine a = new Machine();
            Machine b = new Machine();
            Machine c = new Machine();
            a.addRessource(3, "A");
            a.addRessource(2, "B");
            b.addRessource(1, "C");
            c.addRessource(0, "D");
            Server geacie = new Server(12345, 10);
            geacie.manageRequest();
        } catch (Exception e){
            
            System.err.println(e.getMessage());
        }
    }
}