package com.laundry;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.UUID;
import com.laundry.javaFxView.LoginView;
import com.laundry.model.user;
import com.laundry.services.databasemanager;

public class LaundryApp extends Application {
    private databasemanager dbManager;
    @Override
    public void start(Stage primaryStage) {
        this.dbManager = new databasemanager();

        // Create initial admin user if none exists
        if (dbManager.getAllUsers().isEmpty()) {
            user admin = new user();
            admin.setId(UUID.randomUUID().toString());
            admin.setUsername("admin");
            admin.setPassword("admin123"); // to hash this latter
            admin.setRole("admin");
            admin.setFullName("Administrator");
            dbManager.saveUser(admin);
        }
        
        primaryStage.setTitle("Laundry Management System");
        primaryStage.setScene(new Scene(new LoginView(dbManager), 300, 200));
        primaryStage.show();
    }
    
    @Override
    public void stop() {
        dbManager.close();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
 