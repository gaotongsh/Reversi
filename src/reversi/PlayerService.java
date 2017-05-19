package reversi;

import java.net.Socket;

import javafx.concurrent.Service;

public abstract class PlayerService extends Service<Socket> {
  protected int port;
  
  public void closeport() {}
}
