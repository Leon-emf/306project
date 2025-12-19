package wrk;

import bean.XboxButton;
import ctrl.ICtrlXboxInput;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Worker thread that monitors Xbox controller input and notifies the controller
 * Uses JavaFX's gamepad support through the scene graph
 * 
 * @author SpeleoThink Team
 */
public class WrkXboxController extends Thread {
    
    private volatile boolean running;
    private final ICtrlXboxInput refCtrl;
    private XboxButton xboxButton;
    private static final int POLL_RATE = 50; // milliseconds
    
    // Previous state for button press detection
    private XboxButton previousState;
    
    public WrkXboxController(ICtrlXboxInput refCtrl) {
        super("Thread Xbox Controller");
        this.refCtrl = refCtrl;
        this.xboxButton = new XboxButton();
        this.previousState = new XboxButton();
    }
    
    public void setRunning(boolean running) {
        this.running = running;
    }
    
    public XboxButton getXboxButton() {
        return xboxButton;
    }
    
    /**
     * Updates button state from keyboard input (for testing without controller)
     * Can be called from JavaFX thread
     */
    public void updateFromKeyboard(KeyEvent event, boolean pressed) {
        if (event.getCode() == KeyCode.W || event.getCode() == KeyCode.UP) {
            xboxButton.setLeftStickY(pressed ? -1.0 : 0.0);
        } else if (event.getCode() == KeyCode.S || event.getCode() == KeyCode.DOWN) {
            xboxButton.setLeftStickY(pressed ? 1.0 : 0.0);
        } else if (event.getCode() == KeyCode.A || event.getCode() == KeyCode.LEFT) {
            xboxButton.setLeftStickX(pressed ? -1.0 : 0.0);
        } else if (event.getCode() == KeyCode.D || event.getCode() == KeyCode.RIGHT) {
            xboxButton.setLeftStickX(pressed ? 1.0 : 0.0);
        } else if (event.getCode() == KeyCode.SPACE) {
            xboxButton.setButtonA(pressed);
        } else if (event.getCode() == KeyCode.L) {
            xboxButton.setButtonX(pressed);
        } else if (event.getCode() == KeyCode.H) {
            xboxButton.setButtonY(pressed);
        } else if (event.getCode() == KeyCode.U) {
            xboxButton.setdPadUp(pressed);
        } else if (event.getCode() == KeyCode.J) {
            xboxButton.setdPadDown(pressed);
        } else if (event.getCode() == KeyCode.Q) {
            xboxButton.setLeftBumper(pressed);
        } else if (event.getCode() == KeyCode.E) {
            xboxButton.setRightBumper(pressed);
        }
    }
    
    /**
     * Manual update method for gamepad values from JavaFX scene
     * This should be called from the JavaFX application thread with actual gamepad data
     */
    public synchronized void updateGamepadState(XboxButton newState) {
        this.xboxButton = newState;
    }
    
    @Override
    public void run() {
        running = true;
        System.out.println("Xbox Controller thread started");
        
        while (running) {
            _sleep(POLL_RATE);
            
            // Notify controller of state changes
            if (refCtrl != null) {
                refCtrl.onXboxInputReceived(xboxButton);
                
                // Check for button presses (transitions from false to true)
                checkButtonPress();
                
                // Update previous state
                copyState(xboxButton, previousState);
            }
        }
        
        System.out.println("Xbox Controller thread stopped");
    }
    
    /**
     * Check for button press events (state change from false to true)
     */
    private void checkButtonPress() {
        if (xboxButton.isButtonA() && !previousState.isButtonA()) {
            refCtrl.onButtonAPressed();
        }
        if (xboxButton.isButtonB() && !previousState.isButtonB()) {
            refCtrl.onButtonBPressed();
        }
        if (xboxButton.isButtonX() && !previousState.isButtonX()) {
            refCtrl.onButtonXPressed();
        }
        if (xboxButton.isButtonY() && !previousState.isButtonY()) {
            refCtrl.onButtonYPressed();
        }
    }
    
    /**
     * Copy state from source to destination
     */
    private void copyState(XboxButton source, XboxButton dest) {
        dest.setButtonA(source.isButtonA());
        dest.setButtonB(source.isButtonB());
        dest.setButtonX(source.isButtonX());
        dest.setButtonY(source.isButtonY());
        dest.setLeftBumper(source.isLeftBumper());
        dest.setRightBumper(source.isRightBumper());
        dest.setBack(source.isBack());
        dest.setStart(source.isStart());
        dest.setLeftStickButton(source.isLeftStickButton());
        dest.setRightStickButton(source.isRightStickButton());
        dest.setdPadUp(source.isdPadUp());
        dest.setdPadDown(source.isdPadDown());
        dest.setdPadLeft(source.isdPadLeft());
        dest.setdPadRight(source.isdPadRight());
        dest.setLeftStickX(source.getLeftStickX());
        dest.setLeftStickY(source.getLeftStickY());
        dest.setRightStickX(source.getRightStickX());
        dest.setRightStickY(source.getRightStickY());
        dest.setLeftTrigger(source.getLeftTrigger());
        dest.setRightTrigger(source.getRightTrigger());
    }
    
    private void _sleep(int millis) {
        try {
            sleep(millis);
        } catch (InterruptedException ex) {
            System.err.println("Erreur lors du sleep du thread " + super.getName()
                    + ". \n" + ex.getMessage());
        }
    }
}
