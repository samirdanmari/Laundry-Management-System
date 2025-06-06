package com.laundry.javaFxView;
import com.laundry.model.user;
import com.laundry.model.order;
import com.laundry.services.databasemanager;
import com.laundry.javaFxView.createOrderView;
import com.laundry.javaFxView.orderListView;
import com.laundry.javaFxView.userManagementView;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

    public class mainView extends BorderPane {
    private final databasemanager dbManager;
    private final user currentUser;
    
    public mainView(databasemanager dbManager, user user) {
        this.dbManager = dbManager;
        this.currentUser = user;

        // Create menu bar
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem logoutItem = new MenuItem("Logout");
        logoutItem.setOnAction(e -> logout());
        fileMenu.getItems().add(logoutItem);
        menuBar.getMenus().add(fileMenu);

        //new Menu for Reports
        // In your MainView constructor:
        Menu reportMenu = new Menu("Reports");
        MenuItem monthlyReportItem = new MenuItem("Monthly Report");
        MenuItem customReportItem = new MenuItem("Custom Date Range");

        reportMenu.getItems().addAll(monthlyReportItem, customReportItem);
        menuBar.getMenus().add(reportMenu);

        monthlyReportItem.setOnAction(e -> {
    Stage reportStage = new Stage();
    reportStage.setScene(new Scene(new ReportView(dbManager), 800, 600));
    reportStage.show();
});
        
        // Add admin menu if user is admin
        if ("admin".equals(user.getRole())) {
            Menu adminMenu = new Menu("Admin");
            MenuItem manageUsersItem = new MenuItem("Manage Users");
            manageUsersItem.setOnAction(e -> showUserManagement());
            adminMenu.getItems().add(manageUsersItem);
            menuBar.getMenus().add(adminMenu);
        }
        
        // Create tab pane for different views
        TabPane tabPane = new TabPane();
        
        // Create Order Tab
        Tab createOrderTab = new Tab("Create Order");
        createOrderTab.setContent(new createOrderView(dbManager, user));
        createOrderTab.setClosable(false);
        
        // Queued Orders Tab
        Tab queuedOrdersTab = new Tab("Queued Orders");
        queuedOrdersTab.setContent(new orderListView(dbManager, order.OrderStatus.QUEUED, user));
        queuedOrdersTab.setClosable(false);
        
        // Processing Orders Tab
        Tab processingOrdersTab = new Tab("Processing Orders");
        processingOrdersTab.setContent(new orderListView(dbManager, order.OrderStatus.PROCESSING, user));
        processingOrdersTab.setClosable(false);
        
        // Completed Orders Tab
        Tab completedOrdersTab = new Tab("Completed Orders");
        completedOrdersTab.setContent(new orderListView(dbManager, order.OrderStatus.COMPLETED, user));
        completedOrdersTab.setClosable(false);
        
        tabPane.getTabs().addAll(createOrderTab, queuedOrdersTab, processingOrdersTab, completedOrdersTab);
        
        // Set layout
        this.setTop(menuBar);
        this.setCenter(tabPane);
    }
    
    private void logout() {
        Stage stage = (Stage) this.getScene().getWindow();
        stage.setScene(new Scene(new LoginView(dbManager)));
    }
    
    private void showUserManagement() {
        Stage stage = new Stage();
        stage.setScene(new Scene(new userManagementView(dbManager)));
        stage.setTitle("User Management");
        stage.show();
    }

}

//
