/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ctrl;

import wrk.WrkEtatRobot;
import wrk.WrkAudio;
import wrk.WrkXboxController;
import bean.MyRobot;
import bean.XboxButton;
import ch.emf.info.robot.links.Robot;
import ch.emf.info.robot.links.exception.UnreachableRobotException;
import ch.emf.info.robot.links.bean.RobotState;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * FXML Controller class
 *
 * @author AudergonV01
 */
public class MainViewController implements Initializable, ICtrlEtatRobot, ICtrlXboxInput, Ctrl {

    @FXML
    private TextField txtIp1;
    @FXML
    private TextField txtIp2;
    @FXML
    private TextField txtIp3;
    @FXML
    private TextField txtIp4;
    @FXML
    private HBox boxCommands;
    @FXML
    private ImageView imgView;
    @FXML
    private ProgressIndicator loader;
    @FXML
    private Circle connectionIndicator;
    @FXML
    private Button btnOnOff;
    @FXML
    private TextField txtDire;
    @FXML
    private Label lblHostName;

    private Robot robot;
    private MyRobot myRobot;
    private WrkAudio wrkAudio;
    private WrkEtatRobot wrkEtatRobot;
    private WrkXboxController wrkXboxController;
    private static final int STICK_MULTIPLIER = 200;
    private static final int MAX_SPEED = 999;
    private boolean xboxControlEnabled = false;

    private ToggleButton btnJoystick;
    @FXML
    private TextField txtId;
    @FXML
    private TextField txtPw;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        robot = new Robot();
        wrkEtatRobot = new WrkEtatRobot(robot, this);
        wrkEtatRobot.start();
        wrkAudio = new WrkAudio();
        myRobot = new MyRobot();
        
        // Initialize Xbox controller support
        wrkXboxController = new WrkXboxController(this);
        wrkXboxController.start();
        
        // Setup keyboard input for testing (when no physical Xbox controller)
        setupKeyboardInput();
    }
    
    /**
     * Setup keyboard input as fallback for Xbox controller testing
     */
    private void setupKeyboardInput() {
        // Attach when the scene becomes available, even if it's created after initialize()
        boxCommands.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
                newScene.addEventHandler(KeyEvent.KEY_RELEASED, this::handleKeyReleased);
                // Ensure the scene can receive key events (avoid focus trapped in text fields)
                Platform.runLater(() -> boxCommands.requestFocus());
            }
        });
        // If the scene is already ready, attach immediately
        Platform.runLater(() -> {
            if (boxCommands.getScene() != null) {
                boxCommands.getScene().addEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
                boxCommands.getScene().addEventHandler(KeyEvent.KEY_RELEASED, this::handleKeyReleased);
                boxCommands.requestFocus();
            }
        });
    }
    
    private void handleKeyPressed(KeyEvent event) {
        if (wrkXboxController != null) {
            wrkXboxController.updateFromKeyboard(event, true);
        }
    }
    
    private void handleKeyReleased(KeyEvent event) {
        if (wrkXboxController != null) {
            wrkXboxController.updateFromKeyboard(event, false);
        }
    }

    @FXML
    private void dock(ActionEvent event) {
        robot.dock();
    }

    @FXML
    private void undock(ActionEvent event) {
        robot.undock();
    }

    @FXML
    private void headUp(ActionEvent event) {
        robot.setHeadDirection(RobotState.HeadDirection.UP);
    }

    @FXML
    private void headNeutre(ActionEvent event) {
        robot.setHeadDirection(RobotState.HeadDirection.NONE);
    }

    @FXML
    private void headDown(ActionEvent event) {
        robot.setHeadDirection(RobotState.HeadDirection.DOWN);
    }

    @FXML
    private void neutre(ActionEvent event) {
        robot.setRightSpeed((short) 0);
        robot.setLeftSpeed((short) 0);
    }

    @FXML
    private void gauche(ActionEvent event) {
        robot.setRightSpeed((short) 600);
        robot.setLeftSpeed((short) 200);
    }

    @FXML
    private void droite(ActionEvent event) {
        robot.setRightSpeed((short) 200);
        robot.setLeftSpeed((short) 600);
    }

    @FXML
    private void avant(ActionEvent event) {
        robot.setRightSpeed((short) 999);
        robot.setLeftSpeed((short) 999);
    }

    @FXML
    private void arriere(ActionEvent event) {
        robot.setRightSpeed((short) -600);
        robot.setLeftSpeed((short) -600);
    }

    @FXML
    private void avantGauche(ActionEvent event) {
    }

    @FXML
    private void avantDroite(ActionEvent event) {
    }

    @FXML
    private void led(ActionEvent event) {
        robot.setLedEnabled(!robot.getRobotState().isLedEnabled());
    }

    @FXML
    private void standUp(ActionEvent event) {
        robot.standUp();
    }

    @Override
    public void onBatteryReceived(byte battery) {
    }

    @Override
    public void onImageReceived(byte[] image) {
        Platform.runLater(() -> {
            imgView.setImage(new Image(new ByteArrayInputStream(image)));
        });
    }

    @Override
    public void onAudioReceived(byte[] audio) {
    }

    public void onClose(WindowEvent e) {
        close(e);
    }

    private void connect() {
        loader.setVisible(true);
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    String ip = getIP();
                    if (ip != null) {
                        robot.connect(ip, myRobot.getId(), myRobot.getPw());
                        if (!robot.isConnected()) {
                            WindowManager.afficherErreur(robot.getLastError());
                        }
                    }
                } catch (UnreachableRobotException ex) {
                    WindowManager.afficherErreur(ex.getMessage());
                } finally {
                    Platform.runLater(() -> {
                        loader.setVisible(false);
                    });
                }
            }
        };
        t.start();
    }

    private String getIP() {
        String ip = null;
        String b1 = txtIp1.getText();
        String b2 = txtIp2.getText();
        String b3 = txtIp3.getText();
        String b4 = txtIp4.getText();
        if (b1.isEmpty() | b2.isEmpty() | b3.isEmpty() | b4.isEmpty()) {
            WindowManager.afficherAlerte("L'adresse ip n'est pas complète.");
        } else {
            try {
                Integer.parseInt(b1);
                Integer.parseInt(b2);
                Integer.parseInt(b3);
                Integer.parseInt(b4);
                ip = b1 + "." + b2 + "." + b3 + "." + b4;
            } catch (NumberFormatException ex) {
                WindowManager.afficherAlerte("L'adresse ip n'est pas valide.");
            }
        }
        return ip;
    }

    private void off(ActionEvent event) {
        if (!robot.disconnect()) {
            WindowManager.afficherErreur(robot.getLastError());
        }
    }

    @Override
    public void onConnectionStateReceived(boolean state) {
        Platform.runLater(() -> {
            connectionIndicator.setFill(state ? Color.LIME : Color.RED);
            btnOnOff.setText(state ? "OFF" : "ON");
            imgView.setVisible(state);
        });
    }

    @FXML
    private void onOff(ActionEvent event) {
        if (robot.isConnected()) {
            robot.disconnect();
        } else {
            connect();
        }
    }

    private void close(Event event) {
        ((Stage) (btnOnOff.getScene().getWindow())).close();
        wrkEtatRobot.setRunning(false);
        if (wrkXboxController != null) {
            wrkXboxController.setRunning(false);
        }
        try {
            wrkEtatRobot.join();
            if (wrkXboxController != null) {
                wrkXboxController.join();
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(MainViewController.class.getName()).log(Level.SEVERE, null, ex);
        }
        robot.disconnect();
    }

    @FXML
    private void onBtnClose(ActionEvent event) {
        close(event);
    }

    @FXML
    private void dire(ActionEvent event) {
        String dire = txtDire.getText();
        txtDire.setText("");
        if (dire.length() > 0) {
            byte[] data = wrkAudio.textToSpeech(dire);
            if (data != null) {
                robot.sendAudio(data);
            } else {
                WindowManager.afficherErreur("Erreur survenue lors du TextToSpeech.");
            }
        }
    }

    public void setRobot(MyRobot myRobot) {
        if (myRobot != null) {
            this.myRobot = myRobot;
            lblHostName.setText(myRobot.getHostname());
            txtId.setText(myRobot.getId()+"");
            txtPw.setText(myRobot.getPw()+"");
            String[] ipParts = myRobot.getIp().split("\\.");
            if (ipParts.length == 4) {
                txtIp1.setText(ipParts[0]);
                txtIp2.setText(ipParts[1]);
                txtIp3.setText(ipParts[2]);
                txtIp4.setText(ipParts[3]);
            } else {
                WindowManager.afficherAlerte("L'adresse ip du robot est invalide. (" + myRobot.getIp() + ")");
            }
        }
    }
    
    // ========== Xbox Controller Interface Implementation ==========
    
    @Override
    public void onXboxInputReceived(XboxButton xboxButton) {
        if (!robot.isConnected() || !xboxControlEnabled) {
            return;
        }
        
        // Calculate robot speeds based on left stick input
        // Y-axis: forward/backward, X-axis: turning
        double forward = -xboxButton.getLeftStickY(); // Inverted because stick Y is negative when up
        double turn = xboxButton.getLeftStickX();
        
        // Tank drive calculation
        // Left motor = forward - turn
        // Right motor = forward + turn
        double leftPower = forward - turn;
        double rightPower = forward + turn;
        
        // Normalize if values exceed 1.0
        double maxPower = Math.max(Math.abs(leftPower), Math.abs(rightPower));
        if (maxPower > 1.0) {
            leftPower /= maxPower;
            rightPower /= maxPower;
        }
        
        // Convert to robot speed values (-999 to 999)
        short leftSpeed = (short) (leftPower * MAX_SPEED);
        short rightSpeed = (short) (rightPower * MAX_SPEED);
        
        // Apply speeds to robot
        robot.setLeftSpeed(leftSpeed);
        robot.setRightSpeed(rightSpeed);
        
        // Head control with D-Pad
        if (xboxButton.isdPadUp()) {
            robot.setHeadDirection(RobotState.HeadDirection.UP);
        } else if (xboxButton.isdPadDown()) {
            robot.setHeadDirection(RobotState.HeadDirection.DOWN);
        } else if (!xboxButton.isdPadUp() && !xboxButton.isdPadDown()) {
            robot.setHeadDirection(RobotState.HeadDirection.NONE);
        }
        
        // Bumpers for dock/undock
        if (xboxButton.isLeftBumper()) {
            robot.dock();
        }
        if (xboxButton.isRightBumper()) {
            robot.undock();
        }
    }
    
    @Override
    public void onButtonAPressed() {
        if (robot.isConnected()) {
            robot.standUp();
        }
    }
    
    @Override
    public void onButtonBPressed() {
        // Toggle LED
        if (robot.isConnected()) {
            robot.setLedEnabled(!robot.getRobotState().isLedEnabled());
        }
    }
    
    @Override
    public void onButtonXPressed() {
        // Toggle Xbox control mode
        xboxControlEnabled = !xboxControlEnabled;
        Platform.runLater(() -> {
            if (xboxControlEnabled) {
                lblHostName.setText(myRobot.getHostname() + " [XBOX MODE]");
                lblHostName.setStyle("-fx-text-fill: lime;");
            } else {
                lblHostName.setText(myRobot.getHostname());
                lblHostName.setStyle("");
                // Stop robot when disabling Xbox control
                robot.setLeftSpeed((short) 0);
                robot.setRightSpeed((short) 0);
            }
        });
    }
    
    @Override
    public void onButtonYPressed() {
        // Emergency stop
        if (robot.isConnected()) {
            robot.setLeftSpeed((short) 0);
            robot.setRightSpeed((short) 0);
            robot.setHeadDirection(RobotState.HeadDirection.NONE);
        }
    }

}
