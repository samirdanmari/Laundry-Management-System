package com.laundry;
import com.laundry.javaFxView.LoginView;
import com.laundry.services.databasemanager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class laundryapptest extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Initialize database manager
        databasemanager dbManager = new databasemanager();
        
        // Create and show login screen
        LoginView loginView = new LoginView(dbManager);
        Scene scene = new Scene(loginView, 800, 600);
        
        primaryStage.setTitle("Laundry Management System - TEST");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}


