module com.example.dyplom {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.dyplom to javafx.fxml;
    exports com.example.dyplom;
}