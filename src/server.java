import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;


public class server {
    static int port;
    String hostaddress;
    static ServerSocket serverSocket;
    static ArrayList<String> clientList = new ArrayList<String>();
    static ArrayList<PrintStream> writers = new ArrayList<PrintStream>();
    static Object lock =  new Object();

    static void addClient(String name, PrintStream w) {
        synchronized(lock) {
            clientList.add(name);
            writers.add(w);
        }
    }

    static void removeClient(String name, PrintStream w) {
        synchronized(lock) {
            clientList.remove(name);
            writers.remove(w);
        }
    }    
    static boolean containsName(String name) {
        if (name == null) return false;
        synchronized(lock) {
            return clientList.contains(name);
        }
    }
    
    static ArrayList<PrintStream> getWriterList() {
        synchronized(lock) {
            return writers;
        }
    }
    
    static void printClientList() {
        synchronized(lock) {
            System.out.println("list of registered clients:");
            if (clientList.size() == 0) {
                System.out.println("empty");
            } else{
                for (String name : clientList) {
                    System.out.println(name);
                }
            }
        }
    }
    
    
    private static void initPort() {
        port = 12345;
    }
    
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        initPort();
        //initAddress();
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("server started at port:" + port);
            System.out.println("server waiting for new connections");
            // listen for new connections
            while(true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally{
            System.out.println("Server exiting...");
            try {
                serverSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static class ClientHandler extends Thread {
        Socket clientSocket;
        String clientUserName;
        BufferedReader in;
        PrintStream out;
        ClientHandler(Socket client) {
            clientSocket = client;
            System.out.println("got connection from client <port,addr>:"
                    + client.getPort() + client.getInetAddress());
        }
        public void run() {
            try {
                System.out.println("starting run");
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintStream(clientSocket.getOutputStream(), true);
                String name;
                int trials = 50;
                String s  = "FromServer:SUBMIT_USERNAME:"
                        + "tries left:" + trials;
                out.println(s);                    
                while(trials > 0) {
                    trials--;
                    name = in.readLine();
                    System.out.println("recvd name:" + name);
                    if (name == null
                                || name.equals("")
                                || name.equals(" ")
                                || containsName(name)) {                            
                        out.println(s);                        
                    } else {
                        addClient(name, out);
                        clientUserName = name;
                        printClientList();
                        out.println("FromServer:NAME_ACCEPTED");
                        break;
                    }
                }

                // accept chats from clients
                while(clientSocket.isConnected()) {
                    String line = in.readLine();
                    System.out.println(line);
                    if (line == null) {/*TBD*/}
                    else if (line.contains("EXIT_CLIENT")) {
                        out.println("GOODBYE!");
                        break;
                    } else {
                        // broadcast to all register clients
                        ArrayList<PrintStream> l = getWriterList();
                        for (PrintStream w : l) {
                            if (w != out) w.println(line);
                        }
                    }
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                try {
                    removeClient(clientUserName, out);
                    System.out.println("Client:" 
                            + clientUserName 
                            + " removed from chat");
                    printClientList();
                    if (in != null) in.close();
                    if (out != null) out.close();
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }
}
