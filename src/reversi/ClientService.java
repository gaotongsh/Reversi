package reversi;

import java.net.InetAddress;
import java.net.Socket;

import javafx.concurrent.Task;

public class ClientService extends PlayerService {

  private InetAddress address;
  
  public ClientService(InetAddress address, int port) {
    this.address = address;
    this.port = port;
  }
  
  @Override
  protected Task<Socket> createTask() {
    return new Task<Socket>() {
      @Override
      protected Socket call()  throws Exception {
        return new Socket(address, port);
      }
    };
  }
}
