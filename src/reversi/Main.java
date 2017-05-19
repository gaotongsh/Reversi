package reversi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

import javafx.animation.FillTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Duration;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

public class Main extends Application {
  private static int size = 8;
  
  private TabPane root;
  private GameCore core;
  private int[][] grid;
  private Button[][] buttons;
  private Label playingLabel;
  private Label countDownLabel;
  private TextArea textArea;
  private TextField textField;
  private MenuItem setInterval;
  private CheckMenuItem soundEffect;
  private Button undo;
  private Button save;
  private Button replay;
  private Media[] media;
  private MediaPlayer[] player;
  
  private Node getBlackChess() {
    Circle circle = new Circle(20, 20, 14, Color.BLACK);
    circle.setStroke(Color.BLACK);
    return circle;
  }
  
  private Node getWhiteChess() {
    Circle circle = new Circle(20, 20, 14, Color.WHITE);
    circle.setStroke(Color.BLACK);
    return circle;
  }
  
  private void setMedia() {
    media = new Media[3];
    media[2] = new Media((new File("media/beishuiyanmo.wav")).toURI().toString());
    media[1] = new Media((new File("media/buzhisuocuo.wav")).toURI().toString());
    media[0] = new Media((new File("media/ruyudeshui.wav")).toURI().toString());
    player = new MediaPlayer[3];
    for (int i = 0; i < 3; ++i)
      player[i] = new MediaPlayer(media[i]);
  }
  
  private void playMedia(int i) {
    if (!soundEffect.isSelected())
      return;
    if (i != 0) {
      player[0].seek(Duration.ZERO);
      player[i].seek(Duration.ZERO);
    }
    player[i].play();
  }
  
  private void setCountDownListener() {
    core.countDownProperty().addListener(new ChangeListener<Number>() {
      @Override
      public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            countDownLabel.setText("Count Down: " + core.getCountDown());
//            if (core.isNetwork() && core.getPlayerRole() != core.getNowPlaying() )
              paintGrid();
          }
        });
      }
    });
  }
  
  private void setChatListener() {
    core.chatStringProperty().addListener(new ChangeListener<String>() {
      @Override
      public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        Platform.runLater(new Runnable() {
          @Override
          public void run() {
            textArea.setText(core.getChatString());
          }
        });
      }
    });
  }
  
  private void resetAll() {
    core.reset();
    core = new GameCore();
    setChatListener();
    setCountDownListener();
    grid = core.getGrid();
    for (int i = 0; i < size; ++i)
      for (int j = 0; j < size; ++j)
        buttons[i][j].setGraphic(null);
  }
  
  /*
   * When paintGrid() will be called:
   * (1) First time change to Tab 1 (Gaming Tab)
   * (2) Chess board clicked
   * (3) Undo clicked
   * (4) Count down changed
   */
  private void paintGrid() {
    // TODO
    // Paint chess and animation
    for (int i = 0; i < size; ++i)
      for (int j = 0; j < size; ++j) {
        if (core.getWinner() != -1 || (core.isNetwork() && core.getPlayerRole() != core.getNowPlaying())
            || core.isUndoing())
          buttons[i][j].setDisable(true);
        else
          buttons[i][j].setDisable(false);
        if (grid[i][j] == GameCore.BLACKCHESS) {
          if (buttons[i][j].getGraphic() != null 
              && ((Circle)buttons[i][j].getGraphic()).getFill().equals(Color.WHITE)) {
            FillTransition ft = new FillTransition(Duration.millis(300), (Circle)buttons[i][j].getGraphic(), Color.WHITE, Color.BLACK);
            System.out.println("Animation! " + i + j + " Black to White");
            ft.play();
          } else {
            buttons[i][j].setGraphic(getBlackChess());
          }
        } else if (grid[i][j] == GameCore.WHITECHESS) {
          if (buttons[i][j].getGraphic() != null 
              && ((Circle)buttons[i][j].getGraphic()).getFill().equals(Color.BLACK)) {
            FillTransition ft = new FillTransition(Duration.millis(300), (Circle)buttons[i][j].getGraphic(), Color.BLACK, Color.WHITE);
            System.out.println("Animation! " + i + j + " White to Black");
            ft.play();
          } else {
            buttons[i][j].setGraphic(getWhiteChess());
          }
        } else if (grid[i][j] == GameCore.PLAYABLE) {
          buttons[i][j].setGraphic(new Circle(20, 20, 7, Color.RED));
        } else if (grid[i][j] == GameCore.EMPTY) {
          buttons[i][j].setDisable(true);
          buttons[i][j].setGraphic(null);
        }
      }

    // Paint "Now Playing" Label
    switch (core.getNowPlaying()) {
      case GameCore.BLACKCHESS:
        playingLabel.setGraphic(getBlackChess());
        break;
      case GameCore.WHITECHESS:
        playingLabel.setGraphic(getWhiteChess());
        break;
      default:
        break;
    }
    
    // Paint Undo
    System.out.println("Paint Undo...");
    if (core.undoCount() == -1) {
      undo.setText("步数不够，不可悔棋");
      undo.setDisable(true);
    } else {
      if (core.isNetwork()) {
        System.out.println("...isNetwork...");
        undo.setText("悔棋！");
        if ((core.getPlayerRole() == core.getNowPlaying()) && !core.isUndoing()) {
          System.out.println("...can Undo!");
          undo.setDisable(false);
        } else {
          System.out.println("...can't Undo!");
          undo.setDisable(true);
        }
      } else {
        undo.setText("悔棋！剩余"+core.undoCount()+"次");
        if (core.undoCount() == 0 || core.getWinner() != -1) {
          undo.setDisable(true);
        } else {
          undo.setDisable(false);
        }
      }
    }
    
    // Paint Save
    save.setText("保存游戏");
    if (core.isNetwork())
      save.setVisible(false);
    else
      save.setVisible(true);
    if (core.getWinner() != -1)
      save.setDisable(true);
    else
      save.setDisable(false);
    
    // Paint Count Down
    if (core.getCountDown() == -1)
      countDownLabel.setVisible(false);
    countDownLabel.setText("Count Down: " + core.getCountDown());
    
    // Paint chat
    if (!core.isNetwork()) {
      textArea.setVisible(false);
      textField.setVisible(false);
    }
    
    // Paint setInterval
    if (core.isNetwork())
      setInterval.setDisable(true);
    else
      setInterval.setDisable(false);
    
    if (core.getWinner() != -1)
      playMedia(0);
  }
  
  @Override
  public void start(Stage primaryStage) {
    try {
      core = new GameCore();
      grid = core.getGrid();
      
      root = (TabPane)FXMLLoader.load(getClass().getResource("ReversiFXML.fxml"));
      Scene scene = new Scene(root, 640, 480);
      scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
      GridPane grid = (GridPane)((StackPane)root.getTabs().get(1).getContent()).getChildren().get(0);
      buttons = new Button[size][size];
      
      Button newButton = (Button)((GridPane)
          ((StackPane)root.getTabs().get(0).getContent())
          .getChildren().get(0)).getChildren().get(1);
      newButton.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          core.load();
          core.gameStart();
          paintGrid();
          root.getSelectionModel().select(1);
        }
      });
      
      Button openButton = (Button)((GridPane)
          ((StackPane)root.getTabs().get(0).getContent())
          .getChildren().get(0)).getChildren().get(2);
      openButton.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          FileChooser fileChooser = new FileChooser();
          fileChooser.setTitle("打开游戏");
          fileChooser.getExtensionFilters().add(new ExtensionFilter("Game File", "*.reversi"));
          File openfile = fileChooser.showOpenDialog(primaryStage);
          if (openfile != null) {
            try {
              core.load(openfile);
              core.gameStart();
              paintGrid();
              root.getSelectionModel().select(1);
            } catch (FileNotFoundException e) {
              (new Alert(AlertType.ERROR, "打开游戏出错，请重试！")).showAndWait();
            } catch (Exception e) {
              (new Alert(AlertType.ERROR, "解析游戏出错，请重试！")).showAndWait();
            }
          }
        }
      });
      
      Button serverButton = (Button)((GridPane)
          ((StackPane)root.getTabs().get(0).getContent())
          .getChildren().get(0)).getChildren().get(3);
      serverButton.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          Optional<String> port = (new TextInputDialog("7890")).showAndWait();
          if (port.isPresent()) {
            try {
              int x = Integer.parseInt(port.get());
              core.setServer(x);
              core.getPlayerService().setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                  core.setSenderAndReceiver();
                  core.sendInterval();
                  core.load();
//                  core.gameStart();
                  paintGrid();
                  root.getSelectionModel().select(1);
                  (new Alert(AlertType.INFORMATION, "游戏开始！等候时间将是"+core.getInterval()+"秒。"))
                    .showAndWait();
                  core.sendReady();
                }
              });
            } catch (NumberFormatException e) {
              (new Alert(AlertType.WARNING, "端口格式不正确！")).showAndWait();
            } catch (IOException e) {
              (new Alert(AlertType.WARNING, "打开端口错误！")).showAndWait();
            }
          }
        }
      });
      
      Button clientButton = (Button)((GridPane)
          ((StackPane)root.getTabs().get(0).getContent())
          .getChildren().get(0)).getChildren().get(4);
      clientButton.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          Optional<String> address = (new TextInputDialog("localhost")).showAndWait();
          if (address.isPresent()) {
            Optional<String> port = (new TextInputDialog("7890")).showAndWait();
            if (port.isPresent()) {
              try {
                InetAddress ip = InetAddress.getByName(address.get());
                if (port.isPresent()) {
                  int x = Integer.parseInt(port.get());
                  core.setClient(ip, x);

                  core.getPlayerService().setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                    @Override
                    public void handle(WorkerStateEvent event) {
                      core.setInterval(-2); // In order to paintGrid() at beginning
                      core.setSenderAndReceiver();
                      paintGrid();
                      root.getSelectionModel().select(1);
                    }
                  });
                }
              } catch (UnknownHostException e) {
                (new Alert(AlertType.WARNING, "地址不正确！")).showAndWait();
                e.printStackTrace();
              } catch (NumberFormatException e) {
                (new Alert(AlertType.WARNING, "端口格式不正确！")).showAndWait();
              }
            }
          }
        }
      });
      
      // Set handeler
      setCountDownListener();
      setChatListener();
      
      // Add Buttons
      for (int i = 0; i < size; ++i)
        for (int j = 0; j < size; ++j) {
          Button btn = new Button();
          btn.setPadding(new Insets(0));
//          btn.setBackground(Background.EMPTY);
          btn.setId(i + ":" + j);
          btn.setPrefSize(40, 40);
          btn.setMaxSize(40, 40);
          btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
              playMedia(core.getNowPlaying());
              core.placeChess(btn.getId());
              paintGrid();
            }
          });
          buttons[i][j] = btn;
          grid.add(buttons[i][j], i, j);
        }
      
      // Add "Now Playing", "CountDown Label" Label
      VBox box = new VBox(10);
      box.setAlignment(Pos.TOP_LEFT);
      playingLabel = new Label();
      playingLabel.setText("Now Playing:");
      playingLabel.setGraphicTextGap(10);
      playingLabel.setContentDisplay(ContentDisplay.RIGHT);
      
      countDownLabel = new Label();
      
      box.getChildren().addAll(playingLabel, countDownLabel);
      grid.add(box, 9, 0, 1, 2);
      
      // Add Second Page Buttons
      VBox box2 = new VBox(10);
      box2.setAlignment(Pos.BOTTOM_LEFT);
      
      textArea = new TextArea();
      textArea.setFont(new Font(10));
      textArea.setWrapText(true);
      
      textField = new TextField("请输入聊天内容，按回车发送");
      textField.setFont(new Font(10));
      textField.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          core.sendChat(textField.getText());
          textField.setText("");
        }
      });
      
      replay = new Button("重新开始新游戏");
      replay.setPrefWidth(150);
//      replay.setVisible(false);
      replay.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          Alert confirm = new Alert(AlertType.CONFIRMATION, "你真的要重新开始游戏吗？");
          Optional<ButtonType> ans = confirm.showAndWait();
          if (ans.isPresent() && ans.get() == ButtonType.OK) {
            resetAll();
            root.getSelectionModel().select(0);
          }
        }
      });
      
      undo = new Button();
      undo.setPrefWidth(150);
      undo.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          core.undo();
          paintGrid();
        }
      });
      
      save = new Button();
      save.setPrefWidth(150);
      save.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          if (core.save(primaryStage) == 0) {
            save.setText("保存成功！");
            save.setDisable(true);
          }
        }
      });
      
      box2.getChildren().addAll(textArea, textField, replay, save, undo);
      grid.add(box2, 9, 2, 1, 6);
      
      // Add Menu bar
      MenuBar bar = new MenuBar();

      soundEffect = new CheckMenuItem("Sound Effect");
      soundEffect.setSelected(true);
      
      setInterval = new MenuItem("Set Play Interval...");
      setInterval.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          TextInputDialog inputDialog = new TextInputDialog(Integer.toString(core.getInterval()));
          inputDialog.showAndWait();
          String result = inputDialog.getResult();
          try {
            core.setInterval(Integer.parseInt(result));
          } catch (NumberFormatException e) {
            (new Alert(AlertType.WARNING, "数字格式不正确，请重新设置！")).showAndWait();
          }
        }
      });
      bar.getMenus().add(new Menu("Settings", null, soundEffect, setInterval));
      
      MenuItem about = new MenuItem("About...");
      about.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
          (new Alert(AlertType.INFORMATION, "Reversi\n by Gao Tong")).showAndWait();
        }
      });
      bar.getMenus().add(new Menu("Help", null, about));
      
      grid.add(bar, 0, 0);
      bar.setUseSystemMenuBar(true);
      
      // Add Count Down Stop
      primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
        public void handle(WindowEvent event) {
          if (core.getTimer() != null)
            core.getTimer().shutdownNow();
        };
      });
      
      // Set Media
      setMedia();
      
      primaryStage.setScene(scene);
      primaryStage.show();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    launch(args);
  }
}
