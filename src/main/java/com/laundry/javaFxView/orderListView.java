package com.laundry.javaFxView;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.laundry.model.order;
import com.laundry.model.user;
import com.laundry.services.databasemanager;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class orderListView extends VBox {
    private final databasemanager dbManager;
    private final order.OrderStatus statusFilter;
    private final user currentUser;
    private final TableView<order> table = new TableView<>();

    public orderListView(databasemanager dbManager, order.OrderStatus statusFilter, user currentUser) {
        this.dbManager = dbManager;
        this.statusFilter = statusFilter;
        this.currentUser = currentUser;
    
        setupOrderTable();
        
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refreshTable());
        
        this.getChildren().addAll(table, refreshButton);
        this.setSpacing(10);
        this.setPadding(new Insets(10));
        
        // Initial load
        refreshTable();
    }
    
    private void setupOrderTable() {
        //existing columns to prevent duplicates
        table.getColumns().clear();
        
        //columns
        TableColumn<order, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(cellData -> {
            String customerName = cellData.getValue().getCustomerName();
            return new SimpleStringProperty(customerName != null ? customerName : "");
        });
        customerCol.setPrefWidth(120);
        
        TableColumn<order, String> roomCol = new TableColumn<>("Room");
        roomCol.setCellValueFactory(cellData -> {
            String roomNumber = cellData.getValue().getRoomNumber();
            return new SimpleStringProperty(roomNumber != null ? roomNumber : "");
        });
        roomCol.setPrefWidth(80);
        
        TableColumn<order, String> serviceCol = new TableColumn<>("Service");
        serviceCol.setCellValueFactory(cellData -> {
            try{ 
            order.ServiceType serviceType = cellData.getValue().getServiceType();
            return new SimpleStringProperty(serviceType != null ? serviceType.toString() : "");
            }catch (Exception e) {
           // Handle enum conversion errors gracefully
                System.err.println("Error getting service type: " + e.getMessage());
                return new SimpleStringProperty("Unknown Service");
            }
        });
        serviceCol.setPrefWidth(120);
        
        TableColumn<order, Double> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(cellData -> {
            double total = cellData.getValue().getTotalAmount();
            return new SimpleDoubleProperty(total).asObject();
        });
        totalCol.setPrefWidth(80);


        TableColumn<order, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> {
            order.OrderStatus status = cellData.getValue().getStatus();
            return new SimpleStringProperty(status != null ? status.toString() : "");
        });
        statusCol.setPrefWidth(100);
        
        TableColumn<order, String> paymentCol = new TableColumn<>("Payment");
        paymentCol.setCellValueFactory(cellData -> {
            order.PaymentStatus paymentStatus = cellData.getValue().getPaymentStatus();
            return new SimpleStringProperty(paymentStatus != null ? paymentStatus.toString() : "");
        });
        paymentCol.setPrefWidth(100);
        
        TableColumn<order, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(param -> new ActionButtonCell());
        actionsCol.setPrefWidth(120);
        actionsCol.setSortable(false);
        
        // Add columns to table
        table.getColumns().addAll(customerCol, roomCol, serviceCol, totalCol, statusCol, paymentCol , actionsCol);
        
        // Set table properties
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setRowFactory(tv -> {
            TableRow<order> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    order rowData = row.getItem();
                    showOrderDetails(rowData);
                }
            });
            return row;
        });
    }
    
    // Custom TableCell class for action buttons
    private class ActionButtonCell extends TableCell<order, Void> {
        private final Button actionButton = new Button();
        
        public ActionButtonCell() {
            actionButton.setOnAction(event -> {
                order currentOrder = getTableView().getItems().get(getIndex());
                if (currentOrder != null) {
                    handleAction(currentOrder);
                }
            });
        }
        
        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getIndex() >= getTableView().getItems().size()) {
                setGraphic(null);
            } else {
                order currentOrder = getTableView().getItems().get(getIndex());
                if (currentOrder != null) {
                    updateButtonText(currentOrder);
                    setGraphic(actionButton);
                }
            }
        }
        
        private void updateButtonText(order currentOrder) {
            switch (statusFilter) {
                case QUEUED:
                    actionButton.setText("Assign Staff");
                    actionButton.setDisable(false);
                    break;
                case PROCESSING:
                    actionButton.setText("Mark Complete");
                    //actionButton.setText("Collect Payment");
                    actionButton.setDisable(false);
                    break;
                case COMPLETED:
                    if (currentOrder.getPaymentStatus() == order.PaymentStatus.UNPAID) {
                        actionButton.setText("Collect Payment");
                        actionButton.setDisable(false);
                    } else {
                        actionButton.setText("Paid");
                        actionButton.setDisable(true);
                    }
                    break;
                default:
                    actionButton.setText("N/A");
                    actionButton.setDisable(true);
                    break;
            }
        }
    }
    
    private void refreshTable() {
        try {
            List<order> orders = dbManager.getOrdersByStatus(statusFilter);
            ObservableList<order> observableOrders = FXCollections.observableArrayList(orders);
            
            // Update on JavaFX Application Thread
            Platform.runLater(() -> {
                table.setItems(observableOrders);
                table.refresh();
            });
            
        } catch (Exception e) {
            showAlert("Error loading orders: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleAction(order currentOrder) {
        if (currentOrder == null) {
            showAlert("No order selected");
            return;
        }
        
        try {
            switch (statusFilter) {
                case QUEUED:
                    showAssignStaffDialog(currentOrder);
                    break;
                case PROCESSING:
                    markOrderComplete(currentOrder);
                    break;
                case COMPLETED:
                    collectPayment(currentOrder);
                    break;
            }
        } catch (Exception e) {
            showAlert("Error processing action: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void markOrderComplete(order currentOrder) {
        currentOrder.setStatus(order.OrderStatus.COMPLETED);
        boolean success = dbManager.updateOrder(currentOrder);
        if (success) {
            refreshTable();
            showAlert("Order marked as complete!");
        } else {
            showAlert("Failed to update order status");
        }
    }
    
    private void collectPayment(order currentOrder) {
        if (currentOrder.getPaymentStatus() == order.PaymentStatus.UNPAID) {
            currentOrder.setPaymentStatus(order.PaymentStatus.PAID);
            boolean success = dbManager.updateOrder(currentOrder);
            if (success) {
                refreshTable();
                printCollectionReceipt(currentOrder);
                showAlert("Payment collected successfully!");
            } else {
                showAlert("Failed to update payment status");
            }
        } else {
            showAlert("Payment already collected for this order");
        }
    }
    
    private void showAssignStaffDialog(order currentOrder) {
        try {
            List<user> availableStaff = dbManager.getAllUsers();
            if (availableStaff.isEmpty()) {
                showAlert("No staff available to assign");
                return;
            }
            
            Dialog<user> dialog = new Dialog<>();
            dialog.setTitle("Assign Staff");
            dialog.setHeaderText("Assign staff to order for " + currentOrder.getCustomerName());
            
            ComboBox<user> staffCombo = new ComboBox<>();
            staffCombo.setItems(FXCollections.observableArrayList(availableStaff));
            
            // Setup display of staff names
            staffCombo.setCellFactory(lv -> new ListCell<user>() {
                @Override
                protected void updateItem(user staff, boolean empty) {
                    super.updateItem(staff, empty);
                    setText(empty || staff == null ? null : staff.getFullName());
                }
            });
            
            staffCombo.setButtonCell(new ListCell<user>() {
                @Override
                protected void updateItem(user staff, boolean empty) {
                    super.updateItem(staff, empty);
                    setText(empty || staff == null ? null : staff.getFullName());
                }
            });
            
            // Select first staff member by default
            if (!staffCombo.getItems().isEmpty()) {
                staffCombo.getSelectionModel().selectFirst();
            }
            
            VBox content = new VBox(10);
            content.getChildren().addAll(
                new Label("Select staff to assign:"),
                staffCombo
            );
            
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            
            // Enable OK button only when staff is selected
            dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(staffCombo.getValue() == null);
            staffCombo.valueProperty().addListener((obs, oldVal, newVal) -> 
                dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(newVal == null));
            
            dialog.setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    return staffCombo.getValue();
                }
                return null;
            });
            
            // Process assignment
            Optional<user> result = dialog.showAndWait();
            result.ifPresent(staff -> {
                if (staff != null) {
                    boolean success = dbManager.assignStaffToOrder(currentOrder.getId(), staff.getId());
                    if (success) {
                        currentOrder.setAssignedStaffId(staff.getId());
                        currentOrder.setStatus(order.OrderStatus.PROCESSING);
                        refreshTable();
                        showAlert(staff.getFullName() + " assigned successfully!");
                    } else {
                        showAlert("Failed to assign staff. Please try again.");
                    }
                } else {
                    showAlert("No staff member selected");
                }
            });
            
        } catch (Exception e) {
            showAlert("Error showing assign staff dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showOrderDetails(order selectedOrder) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Order Details");
        alert.setHeaderText("Order Information");
        
        String details = String.format(
            "Customer: %s\nRoom: %s\nService: %s\nTotal: %.2f\nStatus: %s\nPayment: %s",
            selectedOrder.getCustomerName(),
            selectedOrder.getRoomNumber(),
            selectedOrder.getServiceType(),
            selectedOrder.getTotalAmount(),
            selectedOrder.getStatus(),
            selectedOrder.getPaymentStatus()
        );
        
        alert.setContentText(details);
        alert.showAndWait();
    }
    
    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    private void printCollectionReceipt(order currentOrder) {
        try {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job != null && job.showPrintDialog(null)) {
                TextFlow receipt = new TextFlow();
                
                Text header = new Text("SOKOTO GUEST INN LTD\n\n");
                header.setStyle("-fx-font-weight: bold; -fx-font-size: 17;");
                
                Text customer = new Text("Customer: " + currentOrder.getCustomerName() + "\n");
                Text room = new Text("Room: " + currentOrder.getRoomNumber() + "\n");
                Text service = new Text("Service: " + currentOrder.getServiceType() + "\n");
                Text date = new Text("Collection Date: " + LocalDate.now() + "\n\n");
                
                Text total = new Text(String.format("Total Paid: $%.2f\n", currentOrder.getTotalAmount()));
                total.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
                
                Text collectedBy = new Text("\nCollected by: " + currentUser.getFullName() + "\n");
                Text footer = new Text("\nThank you for your business!");
                
                receipt.getChildren().addAll(header, customer, room, service, date, total, collectedBy, footer);
                
                if (job.printPage(receipt)) {
                    job.endJob();
                    showAlert("Receipt printed successfully!");
                } else {
                    showAlert("Failed to print receipt");
                }
            }
        } catch (Exception e) {
            showAlert("Error printing receipt: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
