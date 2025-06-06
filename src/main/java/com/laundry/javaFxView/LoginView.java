package com.laundry.javaFxView;
import com.laundry.services.databasemanager;
import com.laundry.model.user;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.geometry.Insets;


    public class LoginView extends VBox {
        private final databasemanager dbManager;
        
        public LoginView(databasemanager dbManager) {
            this.dbManager = dbManager;
            
            TextField usernameField = new TextField();
            PasswordField passwordField = new PasswordField();
            Button loginButton = new Button("Login");
            
            loginButton.setOnAction(e -> {
                user User = dbManager.authenticateUser(
                    usernameField.getText(), 
                    passwordField.getText()
                );
                
                if (User != null) {
                    openMainView(User);
                } else {
                    showAlert("Invalid credentials");
                }
            });
            
            this.getChildren().addAll(
                new Label("Username:"), usernameField,
                new Label("Password:"), passwordField,
                loginButton
            );
            this.setSpacing(10);
            this.setPadding(new Insets(20));
        }
        
        private void openMainView(user User) {
            Stage stage = (Stage) this.getScene().getWindow();
            stage.setScene(new Scene(new mainView(dbManager, User)));
        }
        
        private void showAlert(String message) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText(message);
            alert.showAndWait();
        }
    }

