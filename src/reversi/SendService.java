package reversi;

import java.io.DataOutputStream;
import java.net.Socket;

import javax.lang.model.type.NullType;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class SendService extends Service<NullType> {

  private Socket socket;
  private DataOutputStream out;
  private String stringToSend;
  
  public SendService(Socket socket) {
    this.socket = socket;
  }
  
  public void setStringToSend(String stringToSend) {
    this.stringToSend = stringToSend;
  }
  
  @Override
  protected Task<NullType> createTask() {
    return new Task<NullType>() {
      @Override
      protected NullType call() throws Exception {
        if (out == null)
          out = new DataOutputStream(socket.getOutputStream());
        out.writeBytes(stringToSend + "\n");
        System.out.println("String sent: " + stringToSend);
        return null;
      }
    };
  }
}
