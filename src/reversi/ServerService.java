package reversi;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class ServerService extends PlayerService {

  private ServerSocket serverSocket;
  
  public ServerService(int port) {
    this.port = port;
  }
  
  @Override
  protected Task<Socket> createTask() {
    return new Task<Socket>() {
      @Override
      protected Socket call() throws Exception {
        try {
          serverSocket = new ServerSocket(port);
        } catch (IllegalArgumentException e) {
          (new Alert(AlertType.WARNING, "端口数字格式不符合要求！")).showAndWait();
        } catch (Exception e) {
          (new Alert(AlertType.WARNING, "监听端口时遇到错误！")).showAndWait();
        }
        return serverSocket.accept();
      }
    };
  }

  public void closeport() {
    if (serverSocket != null)
      try {
        serverSocket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
  }
}
