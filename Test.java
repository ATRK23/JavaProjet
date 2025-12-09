//import java.util.concurrent.*;
//import java.io.*;
//import java.net.Socket;

public class Test{
    public static void main(String[] args){
        try {
            Machine a = new Machine(30000, 6);
            Machine b = new Machine(30001, 6);
            Machine c = new Machine(30002, 6);
            a.createRessource(3, "A");
            a.createRessource(2, "B");
            b.createRessource(1, "C");
            c.createRessource(0, "D");
            c.createRessource(3, "A");
            a.manageRequest();
            b.manageRequest();
            c.manageRequest();
        } catch (Exception e){
            
            System.err.println(e.getMessage());
        }
    }
}