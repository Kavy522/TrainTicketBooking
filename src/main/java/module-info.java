module trainapp.trainapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;
    requires itextpdf;
//    requires kernel;
//    requires layout;


    opens trainapp to javafx.fxml;
    exports trainapp;
}