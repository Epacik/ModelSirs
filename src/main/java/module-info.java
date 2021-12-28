module xyz.epat.modelsirs {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.slf4j;
    requires org.kordamp.bootstrapfx.core;


    opens xyz.epat.modelsirs to javafx.fxml;
    exports xyz.epat.modelsirs;
    opens xyz.epat.modelsirs.uiComponents to javafx.fxml;
    exports  xyz.epat.modelsirs.uiComponents;
}