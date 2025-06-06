package com.laundry.javaFxView;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.laundry.model.order;
import com.laundry.model.user;
import com.laundry.services.databasemanager;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.print.PrinterJob;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class ReportView extends VBox {
    private final databasemanager dbManager;
    private DatePicker startDatePicker = new DatePicker(LocalDate.now().minusMonths(1));
    private DatePicker endDatePicker = new DatePicker(LocalDate.now());
    private TableView<order> reportTable = new TableView<>();
    
    public ReportView(databasemanager dbManager) {
        this.dbManager = dbManager;
        initializeUI();
    }
    
    private void initializeUI() {
        Button generateBtn = new Button("Generate Report");
        Button printBtn = new Button("Print Report");
        
        generateBtn.setOnAction(e -> {
            try {
                generateReport();
            } catch (JsonProcessingException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        });
        printBtn.setOnAction(e -> printReport());
        
        // Setup table columns
        TableColumn<order, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCreatedDate()));
        
        TableColumn<order, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getTotalAmount()).asObject());
        
        TableColumn<order, String> staffCol = new TableColumn<>("Staff");
        staffCol.setCellValueFactory(c -> new SimpleStringProperty(getStaffName(c.getValue().getAssignedStaffId())));
        
        reportTable.getColumns().addAll(dateCol, amountCol, staffCol);
        
        this.getChildren().addAll(
            new HBox(10, 
                new VBox(new Label("Start Date:"), startDatePicker),
                new VBox(new Label("End Date:"), endDatePicker),
                generateBtn,
                printBtn
            ),
            reportTable
        );
        this.setSpacing(15);
        this.setPadding(new Insets(15));
    }
    
    private void generateReport() throws JsonProcessingException {
        List<order> orders = dbManager.getOrdersBetweenDates(
            startDatePicker.getValue(),
            endDatePicker.getValue()
        );
        reportTable.setItems(FXCollections.observableArrayList(orders));
    }
    
    private String getStaffName(String staffId) {
        return dbManager.getUserById(staffId).map(user::getFullName).orElse("Unassigned");
    }
    
    private void printReport() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(null)) {
            TextFlow report = createPrintableReport();
            if (job.printPage(report)) {
                job.endJob();
            }
        }
    }
    
    private TextFlow createPrintableReport() {
        Text header = new Text(String.format(
            "Laundry Report from %s to %s\n\n",
            startDatePicker.getValue(),
            endDatePicker.getValue()
        ));
        header.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        
        // Add report content here
        return new TextFlow(header);
    }

}

// to compare


  

