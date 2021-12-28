package xyz.epat.modelsirs;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import xyz.epat.modelsirs.uiComponents.MainMap;

public class MainViewController {
    @FXML
    private Label welcomeText;

    @FXML
    private MainMap mainMap;

    private Integer clicks = 0;




    @FXML
    protected void onHelloButtonClick() {
        clicks++;
        welcomeText.setText("Welcome to JavaFX Application!\n clicks: " + clicks.toString());
        //mainMap.drawRect();
    }
}