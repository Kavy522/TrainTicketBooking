module trainapp.trainapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;
    requires itextpdf;
    requires kernel;
    requires layout;

    opens trainapp.controller.ui to javafx.fxml;
    opens trainapp.controller.dialog to javafx.fxml;
    opens trainapp.model to javafx.fxml;

    // Exports
    exports trainapp;
    exports trainapp.controller.ui;
    exports trainapp.controller.dialog;
    exports trainapp.service;
    exports trainapp.model;
    exports trainapp.dao;
    exports trainapp.util;

    opens trainapp to javafx.fxml;
}