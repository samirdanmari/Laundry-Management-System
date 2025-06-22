package com.laundry.model;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
public class order {
    private String id;
    private String customerName;
    private String roomNumber;
    private String serviceTypeString;
    private ServiceType serviceType;
    private List<ClothingItem> items;
    private double totalAmount;
    private LocalDate createdDate;
    private LocalDate expectedCompletionDate;
    private String assignedStaffId;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private String createdBy;
    private boolean synced;

    // Default constructor
    public order() {
        this.items = new ArrayList<>();
        this.createdDate = LocalDate.now();
        this.status = OrderStatus.QUEUED;
        this.paymentStatus = PaymentStatus.UNPAID;
        this.serviceType = ServiceType.WASH_ONLY; // Default service type
        this.synced = false;
    }

    // Constructor with essential parameters
    public order(String id, String customerName, String roomNumber, ServiceType serviceType) {
        this();
        this.id = id;
        this.customerName = customerName;
        this.roomNumber = roomNumber;
        this.serviceType = serviceType;
    }

    // Enum definitions
    public enum PaymentStatus {
        PAID("PAID"),
        UNPAID("UNPAID");

        private final String databaseValue;

        PaymentStatus(String databaseValue) {
            this.databaseValue = databaseValue;
        }

        public String getDatabaseValue() {
            return databaseValue;
        }

        @Override
        public String toString() {
            return databaseValue.substring(0, 1).toUpperCase() + 
                   databaseValue.substring(1).toLowerCase();
        }
        
        //method to get enum from string
        public static PaymentStatus fromDatabaseValue(String dbValue) {
                if (dbValue == null || dbValue.trim().isEmpty()) {
                    return UNPAID;
                }

            String normalized = dbValue.trim().toUpperCase(); 
            for (PaymentStatus status : PaymentStatus.values()) {
                if (status.databaseValue.equals(normalized)) {
                    return status;
            }
        }
            System.err.println("Unknown PaymentStatus from database: " + dbValue);
            System.err.println("Unexpected value received: '" + dbValue + "'");
            return UNPAID; // Default
    }
    }

    public enum OrderStatus {
        QUEUED("QUEUED"),
        PROCESSING("PROCESSING"),
        COMPLETED("COMPLETED"),
        DELIVERED("DELIVERED"),
        CANCELLED("CANCELLED");
        
        private final String databaseValue;
        
        OrderStatus(String databaseValue) {
            this.databaseValue = databaseValue.trim().toUpperCase();
        }

        public String getDatabaseValue(){
            return databaseValue;
        }

        @Override
        public String toString() {
            return databaseValue.substring(0, 1).toUpperCase() + 
                   databaseValue.substring(1).toLowerCase();
        }
        
        // Helper method to get enum from string
        public static OrderStatus fromDatabaseValue(String dbValue) {
                if (dbValue == null || dbValue.trim().isEmpty()) {
                    return QUEUED;
                }
                String normalized = dbValue.trim().toUpperCase(); //before it was to lower case
                for(OrderStatus status: OrderStatus.values()) {
                    if (status.databaseValue.equals(normalized)) {
                        return status;
                    }
                }
                
            System.err.println("Unknown OrderStatus from database: "+dbValue);
            return QUEUED;
        }

    }
    // In your order.java file, make sure your ServiceType enum includes all possible values
public enum ServiceType {
    WASH_ONLY("WASH_ONLY", 1.0),
    WASH_AND_IRON("WASH_AND_IRON", 1.0),
    DRY_CLEAN("DRY_CLEAN", 2.0),
    IRON_ONLY("IRON_ONLY", 0.8),
    EXPRESS("EXPRESS", 2.5);
    
    private final String databaseValue;
    private final double priceMultiplier;
    
    ServiceType(String databaseValue, double priceMultiplier) {
        this.databaseValue = databaseValue.trim().toUpperCase();
        this.priceMultiplier = priceMultiplier;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }
    
    @Override
    public String toString() {
        return databaseValue.substring(0, 1).toUpperCase() + 
        databaseValue.substring(1).toLowerCase();
    }
    
    public double getPriceMultiplier() {
        return priceMultiplier;
    }
    
    // Method to safely convert from database string to enum
    public static ServiceType fromDatabaseValue(String dbValue) {
        for (ServiceType type : ServiceType.values()) {
            if (type.databaseValue.equalsIgnoreCase(dbValue)) {
                return type;
            }
        }
        if (dbValue == null || dbValue.trim().isEmpty()) {
            return WASH_ONLY; // Default value if input is null or empty
        }

        String normalized = dbValue.trim().toUpperCase();
        // First try exact match with display names
        for (ServiceType type : ServiceType.values()) {
            if (type.databaseValue.equals(normalized)) {
                return type;
            }
        }

        //if no match found, return default
        System.err.println("Unknown ServiceType from database:" +dbValue);
        return WASH_ONLY;
    }
}

    // Business logic methods
    public void addItem(ClothingItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
        calculateTotalAmount();
    }
    
    public void removeItem(ClothingItem item) {
        if (this.items != null) {
            this.items.remove(item);
            calculateTotalAmount();
        }
    }
    
    public void calculateTotalAmount() {
        if (this.items == null || this.items.isEmpty()) {
            this.totalAmount = 0.0;
            return;
        }
        
        double total = 0.0;
        for (ClothingItem item : this.items) {
            item.calculateTotalPrice(); // Ensure item total is calculated
            total += item.getTotalPrice();
        }
        
        // Apply service type multiplier
        if (this.serviceType != null) {
            total *= this.serviceType.getPriceMultiplier();
        }
        
        this.totalAmount = Math.round(total * 100.0) / 100.0; // Round to 2 decimal places
    }
    
    public void setExpectedCompletionDateFromService() {
        if (this.createdDate != null && this.serviceType != null) {
            switch (this.serviceType) {
                case WASH_AND_IRON:
                    this.expectedCompletionDate = this.createdDate.plusDays(2);
                    break;
                case WASH_ONLY:
                    this.expectedCompletionDate = this.createdDate.plusDays(1);
                    break;
                case IRON_ONLY:
                    this.expectedCompletionDate = this.createdDate.plusDays(1);
                    break;
                case DRY_CLEAN:
                    this.expectedCompletionDate = this.createdDate.plusDays(3);
                    break;
                default:
                    this.expectedCompletionDate = this.createdDate.plusDays(2);
            }
        }
    }
    
    // Check if order is overdue
    public boolean isOverdue() {
        return this.expectedCompletionDate != null && 
               LocalDate.now().isAfter(this.expectedCompletionDate) && 
               this.status != OrderStatus.COMPLETED && 
               this.status != OrderStatus.DELIVERED;
    }
    
    // Get number of items in the order
    public int getTotalItemCount() {
        if (this.items == null) return 0;
        return this.items.stream().mapToInt(ClothingItem::getQuantity).sum();
    }

    // Getters and Setters
    public String getId() { 
        return id; 
    }
    
    public void setId(String id) { 
        this.id = id; 
    }

    public String getCustomerName() { 
        return customerName; 
    }
    
    public void setCustomerName(String customerName) { 
        this.customerName = customerName; 
    }

    public String getRoomNumber() { 
        return roomNumber; 
    }
    
    public void setRoomNumber(String roomNumber) { 
        this.roomNumber = roomNumber; 
    }

    public ServiceType getServiceType() { 
        if (serviceType == null && serviceTypeString != null){
            serviceType = ServiceType.fromDatabaseValue(serviceTypeString);
        }
        return serviceType; 
    }
    
    public void setServiceType(ServiceType serviceType) { 
        this.serviceType = serviceType;
        this.serviceTypeString = serviceType != null ? serviceType.toString() : null;
        calculateTotalAmount(); // Recalculate when service type changes
        setExpectedCompletionDateFromService(); // Update completion date
    }


    public void setServiceTypeFromString(String serviceTypeString) {
        this.serviceTypeString = serviceTypeString;
        this.serviceType = ServiceType.fromDatabaseValue(serviceTypeString);
    }

    // Method to get string for database storage
    public String getServiceTypeString() {
        return serviceType != null ? serviceType.toString() : serviceTypeString;// before it was only serviceTypeString
    }


    public List<ClothingItem> getItems() { 
        return items; 
    }
    
    public void setItems(List<ClothingItem> items) { 
        this.items = items;
        calculateTotalAmount(); // Recalculate when items change
    }

    public double getTotalAmount() { 
        return totalAmount; 
    }
    
    public void setTotalAmount(double totalAmount) { 
        this.totalAmount = totalAmount; 
    }

    public LocalDate getCreatedDate() { 
        return createdDate; 
    }
    
    public void setCreatedDate(LocalDate createdDate) { 
        this.createdDate = createdDate; 
    }

    public LocalDate getExpectedCompletionDate() { 
        return expectedCompletionDate; 
    }
    
    public void setExpectedCompletionDate(LocalDate expectedCompletionDate) { 
        this.expectedCompletionDate = expectedCompletionDate; 
    }

    public String getAssignedStaffId() { 
        return assignedStaffId; 
    }
    
    public void setAssignedStaffId(String assignedStaffId) { 
        this.assignedStaffId = assignedStaffId; 
    }

    public OrderStatus getStatus() { 
        return status; 
    }
    
    public void setStatus(OrderStatus status) { 
        this.status = status; 
    }

    public PaymentStatus getPaymentStatus() { 
        return paymentStatus; 
    }
    
    public void setPaymentStatus(PaymentStatus paymentStatus) { 
        this.paymentStatus = paymentStatus; 
    }

    public String getCreatedBy() { 
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) { 
        this.createdBy = createdBy; 
    }

    public boolean isSynced() { 
        return synced; 
    }
    
    public void setSynced(boolean synced) { 
        this.synced = synced; 
    }
    
    @Override
    public String toString() {
        return String.format("Order[%s] - %s (%s) - %s -%s - $%.2f", 
                           id, customerName, roomNumber,serviceType, status, totalAmount);
    }
}
