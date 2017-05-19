package reversi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;

public class ReceiveService extends ScheduledService<String> {

  private Socket socket;
  private BufferedReader in;
  private String recievedString;
  
  public ReceiveService(Socket socket) {
    this.socket = socket;
  }
  
  @Override
  protected Task<String> createTask() {
    return new Task<String>() {
      @Override
      protected String call() throws Exception {
        System.out.println("ReceiveService Running!");
        if (in == null)
          in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        recievedString = in.readLine();
        System.out.println("Received: " + recievedString);
        return recievedString;
      }
    };
  }
}
