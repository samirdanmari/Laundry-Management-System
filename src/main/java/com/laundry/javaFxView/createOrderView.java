package com.laundry.javaFxView;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import com.laundry.model.ClothingItem;
import com.laundry.model.order;
import com.laundry.model.user;
import com.laundry.services.databasemanager;

public class createOrderView extends VBox {
    private final databasemanager dbManager;
    private final user currentUser;
    
    // Form fields as instance variables for better access
    private TextField customerNameField;
    private TextField roomNumberField;
    private ComboBox<order.ServiceType> serviceTypeCombo;
    private TableView<ClothingItem> itemsTable;
    private DatePicker expectedDatePicker;
    private ComboBox<user> staffCombo;
    private Label totalAmountLabel;
    private Button saveButton;
    private ObservableList<ClothingItem> orderItems;
    
    public createOrderView(databasemanager dbManager, user currentUser) {
        this.dbManager = dbManager;
        this.currentUser = currentUser;
        this.orderItems = FXCollections.observableArrayList();
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        
        // Load initial data
        refreshStaffList();
    }
    
    private void initializeComponents() {
        // Form fields
        customerNameField = new TextField();
        customerNameField.setPromptText("Enter customer name");
        
        roomNumberField = new TextField();
        roomNumberField.setPromptText("Enter room number");
        
        serviceTypeCombo = new ComboBox<>();
        serviceTypeCombo.getItems().addAll(order.ServiceType.values());
        serviceTypeCombo.setPromptText("Select service type");
        
        expectedDatePicker = new DatePicker(LocalDate.now().plusDays(1));
        
        // Staff combo with proper cell factories
        staffCombo = new ComboBox<>();
        setupStaffCombo();
        
        // Items table
        itemsTable = new TableView<>();
        itemsTable.setItems(orderItems);
        setupItemsTable();
        
        // Total amount label
        totalAmountLabel = new Label("Total Amount: ₦0.00");
        totalAmountLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Buttons
        saveButton = new Button("Save Order");
        saveButton.setDisable(true); // Initially disabled
        saveButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
    }
    
    private void setupStaffCombo() {
        staffCombo.setCellFactory(lv -> new ListCell<user>() {
            @Override
            protected void updateItem(user staff, boolean empty) {
                super.updateItem(staff, empty);
                if (empty || staff == null) {
                    setText(null);
                } else {
                    setText(staff.getFullName() + " (" + staff.getRole() + ")");
                }
            }
        });

        staffCombo.setButtonCell(new ListCell<user>() {
            @Override
            protected void updateItem(user staff, boolean empty) {
                super.updateItem(staff, empty);
                if (empty || staff == null) {
                    setText("Select staff member");
                } else {
                    setText(staff.getFullName() + " (" + staff.getRole() + ")");
                }
            }
        });
    }
    
    private void setupLayout() {
        // Main heading
        Label titleLabel = new Label("Create New Laundry Order");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Form layout
        GridPane formGrid = new GridPane();
        formGrid.setHgap(15);
        formGrid.setVgap(12);
        formGrid.setPadding(new Insets(20));
        formGrid.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8;");
        
        // Add form fields to grid
        formGrid.add(new Label("Customer Name:"), 0, 0);
        formGrid.add(customerNameField, 1, 0);
        
        formGrid.add(new Label("Room Number:"), 0, 1);
        formGrid.add(roomNumberField, 1, 1);
        
        formGrid.add(new Label("Service Type:"), 0, 2);
        formGrid.add(serviceTypeCombo, 1, 2);
        
        formGrid.add(new Label("Expected Completion:"), 0, 3);
        formGrid.add(expectedDatePicker, 1, 3);
        
        formGrid.add(new Label("Assign Staff:"), 0, 4);
        formGrid.add(staffCombo, 1, 4);
        
        // Items section
        Label itemsLabel = new Label("Clothing Items");
        itemsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        Button addItemButton = new Button("+ Add Item");
        addItemButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        
        Button removeItemButton = new Button("- Remove Selected");
        removeItemButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        
        HBox itemsButtonBox = new HBox(10, addItemButton, removeItemButton);
        
        // Action buttons
        Button clearButton = new Button("Clear Form");
        clearButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        
        HBox actionButtons = new HBox(15, clearButton, saveButton);
        actionButtons.setStyle("-fx-alignment: center;");
        
        // Add everything to main VBox
        this.getChildren().addAll(
            titleLabel,
            formGrid,
            itemsLabel,
            itemsButtonBox,
            itemsTable,
            totalAmountLabel,
            actionButtons
        );
        
        this.setSpacing(15);
        this.setPadding(new Insets(20));
        
        // Set up button handlers
        addItemButton.setOnAction(e -> showAddItemDialog());
        removeItemButton.setOnAction(e -> removeSelectedItem());
        clearButton.setOnAction(e -> clearForm());
    }
    
    private void setupEventHandlers() {
        // Form validation listeners
        customerNameField.textProperty().addListener((obs, old, newVal) -> validateForm());
        roomNumberField.textProperty().addListener((obs, old, newVal) -> validateForm());
        serviceTypeCombo.valueProperty().addListener((obs, old, newVal) -> validateForm());
        
        // Service type change handler - update expected date
        serviceTypeCombo.valueProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                updateExpectedDate(newVal);
            }
        });
        
        // Items table change listener - update total
        orderItems.addListener((javafx.collections.ListChangeListener<ClothingItem>) change -> {
            updateTotalAmount();
            validateForm();
        });
        
        // Save button handler
        saveButton.setOnAction(e -> saveOrder());
    }
    
    private void setupItemsTable() {
        itemsTable.setPrefHeight(300);
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        TableColumn<ClothingItem, String> typeCol = new TableColumn<>("Item Type");
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        
        TableColumn<ClothingItem, Integer> quantityCol = new TableColumn<>("Quantity");
        quantityCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getQuantity()).asObject());
        
        TableColumn<ClothingItem, Double> priceCol = new TableColumn<>("Unit Price");
        priceCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getPrice()).asObject());
        priceCol.setCellFactory(col -> new TableCell<ClothingItem, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", price));
                }
            }
        });
        
        TableColumn<ClothingItem, Double> totalCol = new TableColumn<>("Total Price");
        totalCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getTotalPrice()).asObject());
        totalCol.setCellFactory(col -> new TableCell<ClothingItem, Double>() {
            @Override
            protected void updateItem(Double total, boolean empty) {
                super.updateItem(total, empty);
                if (empty || total == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", total));
                }
            }
        });
        
        itemsTable.getColumns().addAll(typeCol, quantityCol, priceCol, totalCol);
        
        // Enable row selection
        itemsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }
    
    private void showAddItemDialog() {
        Dialog<ClothingItem> dialog = new Dialog<>();
        dialog.setTitle("Add Clothing Item");
        dialog.setHeaderText("Enter item details");
        
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Create form fields
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(
            "Shirt","Kaftab","Babban-Riga", "Pants", "Dress", "Jacket", "Skirt", 
            "Blouse", "Jeans", "Sweater", "Underwear", "Socks", "Other"
        );
        typeCombo.setEditable(true);
        
        TextField quantityField = new TextField();
        quantityField.setPromptText("Enter quantity");
        
        TextField priceField = new TextField();
        priceField.setPromptText("Enter price per item");
        
        Label totalLabel = new Label("Total: $0.00");
        totalLabel.setStyle("-fx-font-weight: bold;");
        
        // Add real-time total calculation
        quantityField.textProperty().addListener((obs, old, newVal) -> updateItemTotal(quantityField, priceField, totalLabel));
        priceField.textProperty().addListener((obs, old, newVal) -> updateItemTotal(quantityField, priceField, totalLabel));
        
        grid.add(new Label("Item Type:"), 0, 0);
        grid.add(typeCombo, 1, 0);
        grid.add(new Label("Quantity:"), 0, 1);
        grid.add(quantityField, 1, 1);
        grid.add(new Label("Price per Item:"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(totalLabel, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        // Form validation for dialog
        Button addButton = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);
        
        // Enable button only when all fields are valid
        typeCombo.valueProperty().addListener((obs, old, newVal) -> validateItemDialog(addButton, typeCombo, quantityField, priceField));
        quantityField.textProperty().addListener((obs, old, newVal) -> validateItemDialog(addButton, typeCombo, quantityField, priceField));
        priceField.textProperty().addListener((obs, old, newVal) -> validateItemDialog(addButton, typeCombo, quantityField, priceField));
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    String type = typeCombo.getValue();
                    int quantity = Integer.parseInt(quantityField.getText());
                    double price = Double.parseDouble(priceField.getText());
                    
                    if (quantity <= 0 || price <= 0) {
                        showAlert(Alert.AlertType.ERROR, "Invalid input", "Quantity and price must be positive numbers.");
                        return null;
                    }
                    
                    return new ClothingItem(type, quantity, price);
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Invalid input", "Please enter valid numbers for quantity and price.");
                    return null;
                }
            }
            return null;
        });
        
        Optional<ClothingItem> result = dialog.showAndWait();
        result.ifPresent(item -> {
            orderItems.add(item);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Item added successfully!");
        });
    }
    
    private void updateItemTotal(TextField quantityField, TextField priceField, Label totalLabel) {
        try {
            int quantity = Integer.parseInt(quantityField.getText());
            double price = Double.parseDouble(priceField.getText());
            double total = quantity * price;
            totalLabel.setText(String.format("Total: $%.2f", total));
        } catch (NumberFormatException e) {
            totalLabel.setText("Total: ₦0.00");
        }
    }
    
    private void validateItemDialog(Button addButton, ComboBox<String> typeCombo, 
                                   TextField quantityField, TextField priceField) {
        boolean valid = typeCombo.getValue() != null && 
                       !typeCombo.getValue().trim().isEmpty() &&
                       isValidInteger(quantityField.getText()) &&
                       isValidDouble(priceField.getText());
        addButton.setDisable(!valid);
    }
    
    private void removeSelectedItem() {
        ClothingItem selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Remove Item");
            confirmation.setHeaderText("Remove selected item?");
            confirmation.setContentText("Are you sure you want to remove " + selected.getType() + "?");
            
            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                orderItems.remove(selected);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Item removed successfully!");
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an item to remove.");
        }
    }
    
    private void updateExpectedDate(order.ServiceType serviceType) {
        if (serviceType != null) {
            LocalDate expected = LocalDate.now();
            switch (serviceType) {
                case WASH_AND_IRON:
                    expected = expected.plusDays(2);
                    break;
                case WASH_ONLY:
                    expected = expected.plusDays(1);
                    break;
                case IRON_ONLY:
                    expected = expected.plusDays(1);
                    break;
                case DRY_CLEAN:
                    expected = expected.plusDays(3);
                    break;
            }
            expectedDatePicker.setValue(expected);
        }
    }
    
    private void updateTotalAmount() {
        double total = 0.0;
        for (ClothingItem item : orderItems) {
            total += item.getTotalPrice();
        }
        
        // Apply service type multiplier if available
        if (serviceTypeCombo.getValue() != null) {
            total *= serviceTypeCombo.getValue().getPriceMultiplier();
        }
        
        totalAmountLabel.setText(String.format("Total Amount: $%.2f", total));
    }
    
    private void validateForm() {
        boolean valid = !customerNameField.getText().trim().isEmpty() &&
                       !roomNumberField.getText().trim().isEmpty() &&
                       serviceTypeCombo.getValue() != null &&
                       !orderItems.isEmpty();
        saveButton.setDisable(!valid);
    }
    
    private void saveOrder() {
        try {
            // Create new order
            order newOrder = new order();
            newOrder.setId(UUID.randomUUID().toString());
            newOrder.setCustomerName(customerNameField.getText().trim());
            newOrder.setRoomNumber(roomNumberField.getText().trim());
            newOrder.setServiceType(serviceTypeCombo.getValue());
            newOrder.setItems(new ArrayList<>(orderItems));
            newOrder.setCreatedDate(LocalDate.now());
            newOrder.setExpectedCompletionDate(expectedDatePicker.getValue());
            newOrder.setAssignedStaffId(staffCombo.getValue() != null ? staffCombo.getValue().getId() : null);
            newOrder.setStatus(order.OrderStatus.QUEUED);
            newOrder.setPaymentStatus(order.PaymentStatus.UNPAID);
            newOrder.setCreatedBy(currentUser.getId());
            
            // Calculate total amount using the model's method
            newOrder.calculateTotalAmount();
            
            // Save to database
            dbManager.saveOrder(newOrder);
            
            showAlert(Alert.AlertType.INFORMATION, "Success", "Order saved successfully!");
            
            // Ask if user wants to print receipt
            Alert printAlert = new Alert(Alert.AlertType.CONFIRMATION);
            printAlert.setTitle("Print Receipt");
            printAlert.setHeaderText("Order Saved Successfully");
            printAlert.setContentText("Would you like to print a receipt?");
            
            Optional<ButtonType> printResult = printAlert.showAndWait();
            if (printResult.isPresent() && printResult.get() == ButtonType.OK) {
                printReceipt(newOrder);
            }
            
            // Clear form for next order
            clearForm();
            
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save order: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void clearForm() {
        customerNameField.clear();
        roomNumberField.clear();
        serviceTypeCombo.setValue(null);
        expectedDatePicker.setValue(LocalDate.now().plusDays(1));
        staffCombo.setValue(null);
        orderItems.clear();
        totalAmountLabel.setText("Total Amount: ₦0.00");
    }
    
    private void refreshStaffList() {
        try {
            List<user> staff = dbManager.getAllUsers();
            staffCombo.getItems().clear();
            staffCombo.getItems().addAll(staff);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load staff list: " + e.getMessage());
        }
    }
    
    private void printReceipt(order orderToPrint) {
        try {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job != null && job.showPrintDialog(null)) {
                TextFlow receipt = createReceiptContent(orderToPrint);
                
                if (job.printPage(receipt)) {
                    job.endJob();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Receipt printed successfully!");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to print receipt.");
                }
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Print Error", "Failed to print receipt: " + e.getMessage());
        }
    }
    
    private TextFlow createReceiptContent(order orderToPrint) {
        TextFlow receipt = new TextFlow();
        
        Text header1 = new Text("SOKOTO GUEST INN LTD\n");
        header1.setStyle("-fx-font-weight: bold; -fx-font-size: 16;");
        
        Text header2 = new Text("LAUNDRY RECEIPT\n");
        header2.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        
        Text separator = new Text("=====================================\n");
        
        Text orderDetails = new Text(String.format(
            "Order ID: %s\n" +
            "Customer: %s\n" +
            "Room: %s\n" +
            "Service: %s\n" +
            "Date: %s\n" +
            "Expected: %s\n\n",
            orderToPrint.getId(),
            orderToPrint.getCustomerName(),
            orderToPrint.getRoomNumber(),
            orderToPrint.getServiceType().toString(),
            orderToPrint.getCreatedDate().toString(),
            orderToPrint.getExpectedCompletionDate().toString()
        ));
        
        Text itemsHeader = new Text("ITEMS:\n");
        itemsHeader.setStyle("-fx-font-weight: bold;");
        
        receipt.getChildren().addAll(header1, header2, separator, orderDetails, itemsHeader);
        
        for (ClothingItem item : orderToPrint.getItems()) {
            Text itemText = new Text(String.format(
                "%-15s %2d x $%6.2f = $%7.2f\n",
                item.getType(),
                item.getQuantity(),
                item.getPrice(),
                item.getTotalPrice()
            ));
            receipt.getChildren().add(itemText);
        }
        
        Text separator2 = new Text("=====================================\n");
        Text total = new Text(String.format("TOTAL: $%.2f\n", orderToPrint.getTotalAmount()));
        total.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        
        Text footer = new Text("\nThank you for choosing our services!\n");
        footer.setStyle("-fx-font-style: italic;");
        
        receipt.getChildren().addAll(separator2, total, footer);
        
        return receipt;
    }
    
    // Helper methods
    private boolean isValidInteger(String text) {
        try {
            int value = Integer.parseInt(text);
            return value > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private boolean isValidDouble(String text) {
        try {
            double value = Double.parseDouble(text);
            return value > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}