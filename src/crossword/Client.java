package crossword;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;


/**
 * The game client which connects with the game server
 * Sends user commands and parses server response
 * 
 * @author tim
 */
public class Client {
    public interface CallBack {
        /**
         * Method called for new server response
         */
        public void call();
    }
    private final ObjectInputStream socketIn;
    private final PrintWriter socketOut;
    private ServerResponse resp;
    
    /*
     * AF(socketIn, socketOut, c, resp) = 
     *      A client connected to a socket
     *      with an object input socketIn and a text output socketOut
     *      With the current most recent response resp
     * 
     * RI
     *      true
     * RE
     *      No mutable inputs. All variables private. 
     *      return value of currentResponse immutable
     * Thread safety
     *      sendCommand and startResponses are synchronized on socketOut & In respectively
     *      currentResponse is an accessor to an immutable always valid object
     */
    
    
    /**
     * Generates a new game client and attempts to connect to the 
     * server at host:port
     * @param host Server hostname
     * @param port Server port number
     * @throws IOException throws IOException if unable to connect to the server
     */
    public Client(String host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        socketIn = new ObjectInputStream(socket.getInputStream());
        socketOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8), true);
        resp = ServerResponse.createStart();
        checkrep();
    }
    
    private void checkrep() {
        assert socketIn != null;
        assert socketOut != null;
        assert resp != null;
    }
 
    
    /**
     * Sends the specified command to the server and parses the response
     * @param command the command to send to the server
     */
    public void sendCommand(String command)  {
        synchronized (socketOut) {
            checkrep();
            System.out.println(command.toUpperCase());
            socketOut.println(command.toUpperCase());
        }
    }
    
    
    /**
     * @return the most current response from the server
     */
    public ServerResponse currentResponse() {
        checkrep();
        return resp; 
    }
    
    
    /**
     * Continually checks for new objects sent by the server
     * sets resp to the new one & calls the callback when one i
     * @param c The callback when a new response is read in
     */
    public void startResponsesThread(CallBack c) {
        new Thread(() -> responsesLoop(c)).start();
    }
    
    private void responsesLoop(CallBack c) {
        synchronized (socketIn) {
            while (true) {
                checkrep();
                try {
                    resp = (ServerResponse)socketIn.readObject();
                    System.out.println(resp);
                    c.call();
                }
                catch (ClassNotFoundException | IOException e) {
                    System.out.println("Server disconnected. Game Exiting");
                    System.exit(0);
                    return;
                }
            }
        }
    }
}
