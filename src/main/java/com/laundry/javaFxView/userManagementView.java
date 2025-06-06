package com.laundry.javaFxView;
import java.util.Optional;
import java.util.UUID;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import com.laundry.model.user;
import com.laundry.services.databasemanager;

public class userManagementView extends VBox {
    private final databasemanager dbManager;
    private TableView<user> userTable;
    
    public userManagementView(databasemanager dbManager) {
        this.dbManager = dbManager;
        
        // Create the table with instance field to reference later
        userTable = new TableView<>();
        setupUserTable(userTable);
        
        Button addButton = new Button("Add User");
        addButton.setOnAction(e -> showAddUserDialog());
        
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refreshTable());
        
        this.getChildren().addAll(userTable, new HBox(10, addButton, refreshButton));
        this.setSpacing(10);
        this.setPadding(new Insets(10));
        
        // Initial data load
        refreshTable();
    }
    
    private void setupUserTable(TableView<user> table) {
        // Make sure table has proper width
        table.setPrefHeight(400);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Create columns with proper cell factories
        TableColumn<user, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        
        TableColumn<user, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        
        TableColumn<user, String> nameCol = new TableColumn<>("Full Name");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        
        TableColumn<user, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole()));
        
        TableColumn<user, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox buttonBox = new HBox(5, editButton, deleteButton);
            
            {
                // Edit button handler
                editButton.setOnAction(event -> {
                    user selectedUser = getTableView().getItems().get(getIndex());
                    showEditUserDialog(selectedUser);
                });
                
                // Delete button handler
                deleteButton.setOnAction(event -> {
                    user selectedUser = getTableView().getItems().get(getIndex());
                    deleteUser(selectedUser);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttonBox);
                }
            }
        });
        
        // Set preferred width for actions column
        actionsCol.setPrefWidth(150);
        
        table.getColumns().addAll(idCol, usernameCol, nameCol, roleCol, actionsCol);
    }
    
    // Refresh the table data from database
    private void refreshTable() {
        userTable.setItems(FXCollections.observableArrayList(dbManager.getAllUsers()));
    }

    // Add user dialog with validation
    private void showAddUserDialog() {
        Dialog<user> dialog = new Dialog<>();
        dialog.setTitle("Add New User");
        dialog.setHeaderText("Enter user information");
        
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        TextField fullNameField = new TextField();
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("admin", "cashier", "staff");
        roleCombo.setValue("cashier"); // Default value
        
        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("Full Name:"), 0, 2);
        grid.add(fullNameField, 1, 2);
        grid.add(new Label("Role:"), 0, 3);
        grid.add(roleCombo, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        // Form validation to enable/disable Add button
        Button addButton = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);
        
        // Enable button only when all fields have values
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> 
            validateFormFields(addButton, usernameField, passwordField, roleCombo));
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> 
            validateFormFields(addButton, usernameField, passwordField, roleCombo));
        roleCombo.valueProperty().addListener((observable, oldValue, newValue) -> 
            validateFormFields(addButton, usernameField, passwordField, roleCombo));
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                user newUser = new user();
                newUser.setId(UUID.randomUUID().toString());
                newUser.setUsername(usernameField.getText().trim());
                newUser.setPassword(passwordField.getText());
                newUser.setFullName(fullNameField.getText().trim());
                newUser.setRole(roleCombo.getValue());
                
                return newUser;
            }
            return null;
        });
        
        Optional<user> result = dialog.showAndWait();
        result.ifPresent(newUser -> {
            try {
                dbManager.saveUser(newUser);
                refreshTable();
                showAlert(Alert.AlertType.INFORMATION, "Success", "User created successfully.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to create user: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    // Edit user dialog
    private void showEditUserDialog(user existingUser) {
        Dialog<user> dialog = new Dialog<>();
        dialog.setTitle("Edit User");
        dialog.setHeaderText("Update user information");
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField usernameField = new TextField(existingUser.getUsername());
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Leave empty to keep current password");
        TextField fullNameField = new TextField(existingUser.getFullName());
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("admin", "cashier", "staff");
        roleCombo.setValue(existingUser.getRole());
        
        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("New Password:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("Full Name:"), 0, 2);
        grid.add(fullNameField, 1, 2);
        grid.add(new Label("Role:"), 0, 3);
        grid.add(roleCombo, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // Only update password if a new one was provided
                if (!passwordField.getText().isEmpty()) {
                    existingUser.setPassword(passwordField.getText());
                }
                
                existingUser.setUsername(usernameField.getText().trim());
                existingUser.setFullName(fullNameField.getText().trim());
                existingUser.setRole(roleCombo.getValue());
                
                return existingUser;
            }
            return null;
        });
        
        Optional<user> result = dialog.showAndWait();
        result.ifPresent(updatedUser -> {
            try {
                dbManager.saveUser(updatedUser);
                refreshTable();
                showAlert(Alert.AlertType.INFORMATION, "Success", "User updated successfully.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update user: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    // Delete user with confirmation
    private void deleteUser(user selectedUser) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete User");
        alert.setContentText("Are you sure you want to delete user " + selectedUser.getUsername() + "?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                dbManager.deleteUser(selectedUser.getId());
                refreshTable();
                showAlert(Alert.AlertType.INFORMATION, "Success", "User deleted successfully.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete user: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    // Helper method for form validation
    private void validateFormFields(Button addButton, TextField usernameField, 
                                   PasswordField passwordField, ComboBox<String> roleCombo) {
        boolean disableButton = 
            usernameField.getText().trim().isEmpty() || 
            passwordField.getText().isEmpty() || 
            roleCombo.getValue() == null;
        
        addButton.setDisable(disableButton);
    }
    
    // Helper method to show alerts
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Inner class for staff registration with proper form validation
    public class StaffRegistrationView extends VBox {
        private final databasemanager dbManager;
        
        public StaffRegistrationView(databasemanager dbManager) {
            this.dbManager = dbManager;
            initializeUI();
        }
        
        private void initializeUI() {
            // Use a grid for better layout organization
            GridPane formGrid = new GridPane();
            formGrid.setHgap(10);
            formGrid.setVgap(10);
            formGrid.setPadding(new Insets(10));
            
            // Create form fields
            TextField idField = new TextField();
            TextField nameField = new TextField();
            ComboBox<String> roleCombo = new ComboBox<>(FXCollections.observableArrayList("cashier", "admin", "staff"));
            roleCombo.setValue("cashier"); // Default value
            
            // Add labels and fields to grid
            formGrid.add(new Label("Staff ID:"), 0, 0);
            formGrid.add(idField, 1, 0);
            formGrid.add(new Label("Full Name:"), 0, 1);
            formGrid.add(nameField, 1, 1);
            formGrid.add(new Label("Role:"), 0, 2);
            formGrid.add(roleCombo, 1, 2);
            
            // Generate username and password fields - read-only
            TextField usernameField = new TextField();
            usernameField.setEditable(false);
            usernameField.setPromptText("Auto-generated from name");
            
            TextField passwordField = new TextField();
            passwordField.setEditable(false);
            passwordField.setPromptText("Auto-generated");
            
            formGrid.add(new Label("Username:"), 0, 3);
            formGrid.add(usernameField, 1, 3);
            formGrid.add(new Label("Password:"), 0, 4);
            formGrid.add(passwordField, 1, 4);
            
            // Generate username based on name
            nameField.textProperty().addListener((obs, oldVal, newVal) -> {
                usernameField.setText(generateUsername(newVal));
            });
            
            // Button to generate password
            Button genPasswordButton = new Button("Generate Password");
            genPasswordButton.setOnAction(e -> passwordField.setText(generatePassword()));
            formGrid.add(genPasswordButton, 1, 5);
            
            // Save button
            Button saveButton = new Button("Register Staff");
            saveButton.setDisable(true); // Initially disabled
            
            // Field validation
            idField.textProperty().addListener((obs, old, newVal) -> 
                validateStaffFields(saveButton, idField, nameField, roleCombo));
            nameField.textProperty().addListener((obs, old, newVal) -> 
                validateStaffFields(saveButton, idField, nameField, roleCombo));
            roleCombo.valueProperty().addListener((obs, old, newVal) -> 
                validateStaffFields(saveButton, idField, nameField, roleCombo));
            
            saveButton.setOnAction(e -> {
                try {
                    // Generate password now if not already done
                    if (passwordField.getText().isEmpty()) {
                        passwordField.setText(generatePassword());
                    }
                    
                    user staff = new user();
                    staff.setId(idField.getText());
                    staff.setFullName(nameField.getText());
                    staff.setRole(roleCombo.getValue());
                    staff.setUsername(usernameField.getText());
                    staff.setPassword(passwordField.getText());
                    
                    dbManager.saveUser(staff);
                    showAlert(Alert.AlertType.INFORMATION, "Success", 
                             "Staff registered successfully!\nUsername: " + staff.getUsername() + 
                             "\nPassword: " + staff.getPassword());
                    
                    // Clear form
                    idField.clear();
                    nameField.clear();
                    usernameField.clear();
                    passwordField.clear();
                    roleCombo.setValue("cashier");
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", 
                             "Failed to register staff: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
            
            // Create heading
            Label heading = new Label("Staff Registration");
            heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            
            // Add all components to the VBox
            this.getChildren().addAll(heading, formGrid, saveButton);
            this.setSpacing(15);
            this.setPadding(new Insets(15));
        }
        
        // Validate staff registration form fields
        private void validateStaffFields(Button saveButton, TextField idField, 
                                        TextField nameField, ComboBox<String> roleCombo) {
            boolean disableButton = 
                idField.getText().trim().isEmpty() || 
                nameField.getText().trim().isEmpty() || 
                roleCombo.getValue() == null;
            
            saveButton.setDisable(disableButton);
        }
        
        // Generate a username from full name
        private String generateUsername(String fullName) {
            if (fullName == null || fullName.trim().isEmpty()) {
                return "";
            }
            return fullName.toLowerCase().trim().replaceAll("\\s+", ".") + "@laundry";
        }
        
        // Generate a random password
        private String generatePassword() {
            // Generate a more readable password
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            StringBuilder password = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                int index = (int) (Math.random() * chars.length());
                password.append(chars.charAt(index));
            }
            return password.toString();
        }
    }
}