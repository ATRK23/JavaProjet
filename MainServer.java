//import java.util.concurrent.*;
//import java.io.*;
//import java.net.Socket;

public class MainServer{
    public static void main(String[] args){
        try {
            Machine a = new Machine();
            Machine b = new Machine();
            Machine c = new Machine();
            a.createRessource(3, "A");
            a.createRessource(2, "B");
            b.createRessource(1, "C");
            c.createRessource(0, "D");
            c.createRessource(3, "A");
            Server geacie = new Server(12345, 10);
            geacie.manageRequest();
        } catch (Exception e){
            
            System.err.println(e.getMessage());
        }
    }
}