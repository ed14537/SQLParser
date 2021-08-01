import java.io.*;
import java.net.*;

class DBServer
{
    private final static char EOT = 4;
    private Parser parser = new Parser();

    public static void main(String[] args) {
        new DBServer();
    }

    private DBServer() {
        try {
            ServerSocket ss = new ServerSocket(8888);
            System.out.println("Server Listening");
            while(true) {
                clientConnection(ss);
            }
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void clientConnection(ServerSocket ss) {
        try{
            Socket socket = ss.accept();
            System.out.println("Client connected");
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            String input = in.readLine();
            while(input != null) {
                processNextCommand(out, input);
                input = in.readLine();
            }
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void processNextCommand(BufferedWriter out, String line) throws IOException
    {
        parser.parseCommands(line, out);
        out.write( EOT + "\n");
        out.flush();
    }
}
