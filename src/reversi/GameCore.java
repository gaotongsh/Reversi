package reversi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Scanner;
import java.util.Stack;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.DataFormatException;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;

class Move {
  int changedPlayer;
  int i;
  int j;
  Move(int _player, int _i, int _j) {
    changedPlayer = _player;
    i = _i;
    j = _j;
  }
}

class ChangeOfBoard {
  Move placedChess;
  ArrayList<Move> list;
}

public class GameCore {
  /*
   * format of data
   * [datatype] [content] 
   * 0: [initialcountdown]
   * 1: [countdown]
   * 2: [changeOfBoard]
   * 3: [askforundo]
   * 4: 0/1 [ifacceptundo]
   * 5: ready
   * 6: network interrupt
   * 7: replay for undo reply
   */

  // Final values
  public final static int EMPTY = 0;
  public final static int BLACKCHESS = 1;
  public final static int WHITECHESS = 2;
  public final static int PLAYABLE = 3;
  public final static int TIE = 0;
  
  // Chess-playing properties
  private static int size = 8;
  private int[][] grid;
  private Stack<ChangeOfBoard> allChange;
  private ChangeOfBoard currChange;
  private int nowPlaying;
  private int[] chessCount;
  private int[] undoCount;
  private boolean isUndoing = false;
  
  private int interval = 20;
  private int playableCount = 0;
  private int winner = -1;
  
  // Count down properties
  private IntegerProperty countDown;
  private ScheduledThreadPoolExecutor timer;
  private Thread count;
  private ScheduledFuture<?> future;
  
  // Network properties
  private PlayerService playerService;
  private SendService sender;
  private ReceiveService receiver;
  private boolean isNetwork = false;
  private int playerRole = 0;
  private int readyNum = 0;
  
  private StringProperty chatString;
  
  GameCore() {
    grid = new int[size][size];
    allChange = new Stack<ChangeOfBoard>();
    countDown = new SimpleIntegerProperty(interval);
    undoCount = new int[]{0, 2, 2};
    chatString = new SimpleStringProperty("");
  }
  
  public void setServer(int port) throws IOException {
    isNetwork = true;
    playerRole = BLACKCHESS;
    playerService = new ServerService(port);
    playerService.start();
  }
  
  public void setClient(InetAddress ip, int port) {
    isNetwork = true;
    playerRole = WHITECHESS;
    playerService = new ClientService(ip, port);
    playerService.start();
  }
  
  public void setSenderAndReceiver() {
    sender = new SendService(playerService.getValue());
    receiver = new ReceiveService(playerService.getValue());
    receiver.setRestartOnFailure(true);
    receiver.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
      @Override
      public void handle(WorkerStateEvent event) {
        processReceivedData(receiver.getValue());
      }
    });
    receiver.start();
  }
  
  public PlayerService getPlayerService() {
    return playerService;
  }
  
  public int[][] getGrid() {
    return grid;
  }
  
  public int getNowPlaying() {
    return nowPlaying;
  }
  
  public int getWinner() {
    return winner;
  }
  
  public ScheduledThreadPoolExecutor getTimer () {
    return timer;
  }
  
  public int getInterval() {
    return interval;
  }
  
  public void setInterval(int interval) {
    this.interval = interval;
  }
  
  public boolean isNetwork() {
    return isNetwork;
  }
  
  public boolean isUndoing() {
    return isUndoing;
  }
  
  public int getPlayerRole() {
    return playerRole;
  }
  
  public int getCountDown() {
    return countDown.get();
  }
  
  public IntegerProperty countDownProperty() {
    return countDown;
  }
  
  public String getChatString() {
    return chatString.get();
  }
  
  public StringProperty chatStringProperty() {
    return chatString;
  }
  
  public void load(File f) throws Exception {
    chessCount = new int[3];
    Scanner in = new Scanner(f);
    
    if (in.hasNextLine() && !in.nextLine().equals("ReversiByTonyGao")) {
      in.close();
      throw new DataFormatException();
    }
    
    int _nowPlaying = nowPlaying;
    int _interval = interval;
    int[][] _grid = new int[size][size];
    int[] _chessCount = new int[3];
    
    String l = in.nextLine();
    Scanner scanner = new Scanner(l);
    if (scanner.hasNextInt()) _nowPlaying = scanner.nextInt(); 
    if (scanner.hasNextInt()) _interval = scanner.nextInt();
    scanner.close();
    
    for (int i = 0; i < size; ++i) {
      if (in.hasNextLine()) {
        String line = in.nextLine();
        System.out.printf("Line! %s\n", line);
        Scanner lineScanner = new Scanner(line);
        for (int j = 0; j < size; ++j) {
          if (lineScanner.hasNextInt()) {
            _grid[i][j] = lineScanner.nextInt();
            System.out.printf("Read! %d %d is %d\n", i, j, _grid[i][j]);
            if (_grid[i][j] < -1 || _grid[i][j] > 2) {
              lineScanner.close();
              throw new DataFormatException();
            } 
            _chessCount[_grid[i][j]]++;
          } else {
            lineScanner.close();
            throw new DataFormatException();
          }
        }
        if (lineScanner.hasNextInt()) {
          lineScanner.close();
          throw new DataFormatException();
        }
        lineScanner.close();
      }
    }
    
    in.close();
    if (_chessCount[EMPTY] + _chessCount[BLACKCHESS] + _chessCount[WHITECHESS] != size * size)
      throw new DataFormatException();
    
    nowPlaying = _nowPlaying;
    interval = _interval;
    for (int i = 0; i < size; ++i)
      for (int j = 0; j < size; ++j)
        grid[i][j] = _grid[i][j];
    chessCount = _chessCount;
    
    calculatePlayable();
  }
  
  public void load() {
    chessCount = new int[]{size * size - 4, 2, 2};
    grid[size / 2 - 1][size / 2 - 1] = grid[size / 2][size / 2] = BLACKCHESS;
    grid[size / 2][size / 2 - 1] = grid[size / 2 - 1][size / 2] = WHITECHESS;
    nowPlaying = BLACKCHESS;
    calculatePlayable();
    countDown.set(interval);
  }
  
  public void gameStart() {
    System.out.println(playerRole + " Start!");
    countDown.addListener(new ChangeListener<Number>() {
      @Override
      public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            if (isNetwork && playerRole != WHITECHESS)
              sendCountDown();
            if (newValue.intValue() == 0) {
              if (isNetwork && playerRole != WHITECHESS)
                future.cancel(true);
              if (!isNetwork)
                (new Alert(AlertType.INFORMATION, "超时，请对方继续下子！")).showAndWait();
              switchPlayer();
              calculatePlayable();
              
              if (chessCount[nowPlaying] != 0 && playableCount == 0) {
                switchPlayer();
                calculatePlayable();
                if (playableCount != 0) {
                  if (!isNetwork) {
                    future.cancel(true);
                    (new Alert(AlertType.INFORMATION, "无子可下，请对方继续！")).showAndWait();
                    future = timer.scheduleAtFixedRate(count, 1, 1, TimeUnit.SECONDS); 
                  }
                }
              }
              winner = testGameOver();
              
              // Force paintGrid()
              if (winner != -1)
                countDown.set(-1);
              
              if (!isNetwork || playerRole != WHITECHESS)
                countDown.set(interval);
              if (isNetwork && playerRole != WHITECHESS)
                future = timer.scheduleAtFixedRate(count, 1, 1, TimeUnit.SECONDS); 
            }
          }
        });
      }
    });
    
    if (!isNetwork || playerRole != WHITECHESS) {
      timer = new ScheduledThreadPoolExecutor(1);
      count = new Thread() {
        @Override
        public void run() {
          System.out.println("Now countDown: " + countDown.get());
          countDown.set(countDown.get() - 1);
        }
      };
      count.setDaemon(true);
      future = timer.scheduleAtFixedRate(count, 1, 1, TimeUnit.SECONDS); 
    } 
  }
  
  public void placeChess(String s) {
    if (winner != -1)
      return;
    String[] ij = s.split(":");
    int i = Integer.parseInt(ij[0]);
    int j = Integer.parseInt(ij[1]);
    if (grid[i][j] == PLAYABLE) {
      if (isNetwork && nowPlaying == playerRole)
        sendChess(s);
      
      grid[i][j] = nowPlaying;
      
      currChange = new ChangeOfBoard();
      currChange.list = new ArrayList<Move>();
      currChange.placedChess = new Move(nowPlaying, i, j);
      
      chessCount[EMPTY]--;
      chessCount[nowPlaying]++;
      reverseChess(i, j);
      switchPlayer();
      countDown.set(interval);
      calculatePlayable();
      
      if (chessCount[nowPlaying] != 0 && playableCount == 0) {
        switchPlayer();
        calculatePlayable();
        if (playableCount != 0) {
          if (!isNetwork) {
            future.cancel(true);
            (new Alert(AlertType.INFORMATION, "无子可下，请对方继续！")).showAndWait();
            future = timer.scheduleAtFixedRate(count, 1, 1, TimeUnit.SECONDS); 
          }
        }
      }
      winner = testGameOver();
      // Force paintGrid()
      if (winner != -1)
        countDown.set(-1);
    }
  }
  
  public void undo() {
    if (!isNetwork)
      actualUndo();
    else {
      isUndoing = true;
      if (playerRole == BLACKCHESS) {
        future.cancel(true);
        sendUndo();
      } else {
        sendUndo();
      }
    }
  }
  
  private void actualUndo() {
    if (!isNetwork)
      undoCount[nowPlaying]--;
    countDown.set(interval);
    while (true) {
      // Undo placed chess
      ChangeOfBoard cob = allChange.pop();
      chessCount[cob.placedChess.changedPlayer]--;
      grid[cob.placedChess.i][cob.placedChess.j] = EMPTY;
      chessCount[EMPTY]++;
      
      // Undo reversed chess
      for (Move item : cob.list) {
        chessCount[item.changedPlayer]--;
        chessCount[oppositePlayer(item.changedPlayer)]++;
        grid[item.i][item.j] = oppositePlayer(item.changedPlayer);
      }
      
      if (cob.placedChess.changedPlayer == nowPlaying)
        break;
    }
    calculatePlayable();
  }
  
  public int undoCount() {
    if (cantFindInStack(nowPlaying)) 
      return -1;
    return undoCount[nowPlaying];
  }
  
  public int save(Stage primaryStage) {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("保存游戏");
    fileChooser.getExtensionFilters().add(new ExtensionFilter("Game File", "*.reversi"));
    future.cancel(true);
    File savefile = fileChooser.showSaveDialog(primaryStage);
    future = timer.scheduleAtFixedRate(count, 1, 1, TimeUnit.SECONDS); 
    if (savefile != null) {
      PrintStream out;
      try {
        out = new PrintStream(savefile);
        out.println(encodeBoard());
        out.close();
      } catch (FileNotFoundException e) {
        (new Alert(AlertType.ERROR, "游戏保存出错，请重试！")).showAndWait();
        e.printStackTrace();
        return 1;
      }
      return 0;
    }
    return 1;
  }
  
  public void sendInterval() {
    sender.setStringToSend("0 " + interval);
    sender.restart();
  }
  
  public void sendCountDown() {
    sender.setStringToSend("1 " + countDown.get());
    sender.restart();
  }
  
  public void sendChess(String s) {
    sender.setStringToSend("2 " + s);
    sender.restart();
  }
  
  public void sendUndo() {
    sender.setStringToSend("3");
    sender.restart();
  }
  
  public void sendUndoReply(boolean reply) {
    sender.setStringToSend("4 " + reply);
    sender.restart();
  }
  
  public void sendReady() {
    readyNum++;
    if(readyNum == 2)
      gameStart();
    sender.setStringToSend("5");
    sender.restart();
  }
  
  public void sendStop() {
    sender.setStringToSend("6");
    sender.restart();
  }
  
  public void sendReplyToUndoReply() {
    sender.setStringToSend("7");
    sender.restart();
  }
  
  public void sendChat(String s) {
    if (isNetwork) {
      if (playerRole == BLACKCHESS)
        chatString.setValue(chatString.get() + " Blackchess: ");
      else 
        chatString.setValue(chatString.get() + " Whitechess: ");
      chatString.setValue(chatString.get() + s + "\n");
      sender.setStringToSend("8 " + new String(s.getBytes(), StandardCharsets.ISO_8859_1));
      sender.restart();
    }
  }
  
  public void reset() {
    if (isNetwork()) {
      sendStop();
      receiver.cancel();
      if (playerRole != WHITECHESS)
        playerService.closeport();
    }
    if (timer != null)
      timer.shutdownNow();
  }
  
  private String encodeBoard() {
    String str = "ReversiByTonyGao\n";
    str += nowPlaying + " " + interval + "\n";
    for (int i = 0; i < size; ++i) {
      for (int j = 0; j < size; ++j) {
        switch (grid[i][j]) {
          case BLACKCHESS:
            str += BLACKCHESS + " ";
            break;
          case WHITECHESS:
            str += WHITECHESS + " ";
            break;
          default:
            str += "0 ";
            break;
        }
      }
      str += "\n";
    }
    return str;
  }

  private int testGameOver() {
    if (chessCount[BLACKCHESS] == 0) {
      return gameOver(WHITECHESS);
    } else if (chessCount[WHITECHESS] == 0) {
      return gameOver(BLACKCHESS);
    } else if (playableCount == 0) {
      if (chessCount[BLACKCHESS] > chessCount[WHITECHESS]) {
        return gameOver(BLACKCHESS);
      } else if (chessCount[BLACKCHESS] < chessCount[WHITECHESS]) {
        return gameOver(WHITECHESS);
      } else {
        return gameOver(TIE);
      }
    } else if (chessCount[EMPTY] == 0) {
      return gameOver(TIE);
    }
    return -1;
  }
  
  private int gameOver(int player) {
    System.out.println("GameOver!");
    if (!isNetwork || playerRole != WHITECHESS)
      timer.shutdown();
    switch (player) {
      case TIE:
        (new Alert(AlertType.INFORMATION, "平局！\n"
            + "黑棋数：" + chessCount[BLACKCHESS] + "\n"
            + "白棋数：" + chessCount[WHITECHESS])).showAndWait();
        return TIE;
      case BLACKCHESS:
        (new Alert(AlertType.INFORMATION, "黑方获胜！\n"
            + "黑棋数：" + chessCount[BLACKCHESS] + "\n"
            + "白棋数：" + chessCount[WHITECHESS])).showAndWait();
        return BLACKCHESS;
      case WHITECHESS:
        (new Alert(AlertType.INFORMATION, "白方获胜！\n"
            + "黑棋数：" + chessCount[BLACKCHESS] + "\n"
            + "白棋数：" + chessCount[WHITECHESS])).showAndWait();
        return WHITECHESS;
      default:
        return player;
    }
  }
  
  private void switchPlayer() {
      nowPlaying = oppositePlayer(nowPlaying);
  }
  
  private int oppositePlayer(int a) {
    if (a == BLACKCHESS)
      return WHITECHESS;
    else if (a == WHITECHESS)
      return BLACKCHESS;
    return 0;
  }
  
  private void calculatePlayable() {
    playableCount = 0;
    for (int i = 0; i < size; ++i) 
      for (int j = 0; j < size; ++j) {
        if (grid[i][j] == PLAYABLE)
          grid[i][j] = EMPTY;
        if (grid[i][j] == EMPTY)
          for (int k = -1; k < 2; ++k) {
            for (int l = -1; l < 2; ++l) 
              if ((k != 0 || l != 0) && extend(i, j, k, l)) {
                grid[i][j] = PLAYABLE;
                playableCount++;
                break;
              }
            if (grid[i][j] == PLAYABLE) 
              break;
          }
      }
  }
  
  private boolean extend(int i, int j, int k, int l) {
    int ii = i + k;
    int jj = j + l;
    boolean flag = false;
    while (inRange(ii, jj) && grid[ii][jj] == oppositePlayer(nowPlaying)) {
      ii += k;
      jj += l;
      flag = true;
    }
    if (flag && inRange(ii, jj) && grid[ii][jj] == nowPlaying)
      return true;
    return false;
  }
  
  private boolean inRange(int i, int j) {
    return (i > -1 && j > -1 && i < size && j < size);
  }
  
  private void reverseChess(int i, int j) {
    for (int k = -1; k < 2; ++k) 
      for (int l = -1; l < 2; ++l) 
        if ((k != 0 || l != 0) && extend(i, j, k, l)) {
          int ii = i;
          int jj = j;
          do {
            chessCount[grid[i][j]]--;
            ii += k;
            jj += l;
            chessCount[nowPlaying]++;
            grid[ii][jj] = nowPlaying;
            currChange.list.add(new Move(nowPlaying, ii, jj));
          } while (grid[ii + k][jj + l] != nowPlaying);
        }
    allChange.push(currChange);
  }
  
  private boolean cantFindInStack(int player) {
    for (ChangeOfBoard i : allChange) {
      if (i.placedChess.changedPlayer == player)
        return false;
    }
    return true;
  }
  
  private void processReceivedData(String data) {
    if (data == null) {
      networkInterrupted();
      return;
    }
    
    Scanner scanner = new Scanner(data);
    
    int messageType = scanner.nextInt();
    switch (messageType) {
      case 0:
        setInterval(scanner.nextInt());
        load();
        (new Alert(AlertType.INFORMATION, "游戏开始！等候时间将是"+getInterval()+"秒。"))
            .showAndWait();
        sendReady();
        System.out.println(playerRole + " " + readyNum);
        break;
        
      case 1:
        countDown.set(scanner.nextInt());
        break;
        
      case 2:
        placeChess(scanner.next());
        break;
        
      case 3:
        isUndoing = true;
        if (isNetwork && playerRole != WHITECHESS)
          future.cancel(true);
        Alert confirm = new Alert(AlertType.CONFIRMATION, "对方请求悔棋，是否同意？");
        Optional<ButtonType> ans = confirm.showAndWait();
        if (ans.isPresent() && ans.get() == ButtonType.OK) {
          sendUndoReply(true);
          actualUndo();
        } else {
          sendUndoReply(false);
        }
        if (isNetwork && playerRole != WHITECHESS)
          future = timer.scheduleAtFixedRate(count, 1, 1, TimeUnit.SECONDS); 
        break;
        
      case 4:
        boolean reply = scanner.nextBoolean();
        if (reply) 
          actualUndo();
        sendReplyToUndoReply();
        isUndoing = false;
        if (isNetwork && playerRole != WHITECHESS)
          future = timer.scheduleAtFixedRate(count, 1, 1, TimeUnit.SECONDS); 
        break;
        
      case 5:
        readyNum++;
        System.out.println(playerRole + " " + readyNum);
        if(readyNum == 2)
          gameStart();
        break;
        
      case 6:
        networkInterrupted();
        break;
        
      case 7:
        isUndoing = false;
        if (isNetwork && playerRole != WHITECHESS)
          future = timer.scheduleAtFixedRate(count, 1, 1, TimeUnit.SECONDS); 
        break;
        
      case 8:
        if (isNetwork) {
          if (playerRole == BLACKCHESS)
            chatString.setValue(chatString.get() + " Whitechess: ");
          else 
            chatString.setValue(chatString.get() + " Blackchess: ");
          chatString.setValue(chatString.get() + scanner.next() + "\n");
        }
        break;
        
      default:
        break;
    }
    scanner.close();
  }
  
  private void networkInterrupted() {
    (new Alert(AlertType.INFORMATION, "网络连接中断！重新开始游戏吧！"))
      .showAndWait();
    reset();
  }
}
