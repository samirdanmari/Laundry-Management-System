
package com.laundry.services;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laundry.model.ClothingItem;
import com.laundry.model.order;
import com.laundry.model.user;
import com.mongodb.client.MongoClient;

public class databasemanager {
    private Connection sqliteConnection;
    private MongoClient mongoClient;
    //private boolean isOnline = false;

    private void logSyncError(String orderId, String errorMessage) {
        try (PreparedStatement stmt = sqliteConnection.prepareStatement(
            "INSERT INTO sync_errors (order_id, error_message, timestamp) VALUES (?, ?, ?)")) {
            stmt.setString(1, orderId);
            stmt.setString(2, errorMessage);
            stmt.setString(3, LocalDateTime.now().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public databasemanager() {
        initializeSQLite();
        createDefaultAdmin();
    }
    
    // Setting the database connection
    private void initializeSQLite() {
        try {
             sqliteConnection = DriverManager.getConnection("jdbc:sqlite:laundry.db");
            createSQLiteTables();
            System.out.println("SQLite database connected: laundry.db");
        } catch (SQLException e) {
            System.err.println("SQLite connection failed:");
            e.printStackTrace();
        }
    }

    private void createSQLiteTables() throws SQLException {
        try (Statement stmt = sqliteConnection.createStatement()) {
            // Users table
            stmt.execute(
            "CREATE TABLE IF NOT EXISTS users (" +
                "id TEXT PRIMARY KEY," +
                "username TEXT UNIQUE NOT NULL," +
                "password TEXT NOT NULL," +
                "role TEXT NOT NULL," +
                "full_name TEXT" +
            ")");

            // Orders table
            stmt.execute(
            "CREATE TABLE IF NOT EXISTS orders (" +
                "id TEXT PRIMARY KEY," +
                "customer_name TEXT NOT NULL," +
                "room_number TEXT NOT NULL," +
                "service_type TEXT NOT NULL," +
                "items_json TEXT NOT NULL," +
                "total_amount REAL NOT NULL," +
                "created_date TEXT NOT NULL," +
                "expected_completion_date TEXT NOT NULL," +
                "assigned_staff_id TEXT," +
                "status TEXT," +
                "payment_status TEXT," +
                "created_by TEXT," +
                "synced INTEGER DEFAULT 0," +
                "FOREIGN KEY (created_by) REFERENCES users(id)," +
                "FOREIGN KEY (assigned_staff_id) REFERENCES users(id)" +
            ")");
            
            // Sync errors table
            stmt.execute(
            "CREATE TABLE IF NOT EXISTS sync_errors (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "order_id TEXT NOT NULL," +
                "error_message TEXT NOT NULL," +
                "timestamp TEXT NOT NULL" +
            ")");
            
            System.out.println("SQLite tables created successfully");
        }
    }
    
    // Authenticate User
    public user authenticateUser(String username, String password) {
        try (PreparedStatement stmt = sqliteConnection.prepareStatement(
                "SELECT * FROM users WHERE username = ? AND password = ?")) {
            stmt.setString(1, username);
            stmt.setString(2, password); // Will change to use password hashing
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                user User = new user();
                User.setId(rs.getString("id"));
                User.setUsername(rs.getString("username"));
                User.setPassword(rs.getString("password"));
                User.setRole(rs.getString("role"));
                User.setFullName(rs.getString("full_name"));
                return User;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public List<user> getAllUsers() {
        List<user> staff = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role IN ('cashier', 'admin')";
        try (Statement stmt = sqliteConnection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                staff.add(convertResultSetToUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return staff;
    }

    public void saveUser(user User) {
        String sql = "INSERT OR REPLACE INTO users (id, username, password, role, full_name) " +
                    "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = sqliteConnection.prepareStatement(sql)) {    
            stmt.setString(1, User.getId());
            stmt.setString(2, User.getUsername());
            stmt.setString(3, User.getPassword()); // Note: Store hashed passwords in production
            stmt.setString(4, User.getRole());
            stmt.setString(5, User.getFullName());
            stmt.executeUpdate();
            System.out.println("User saved: " + User.getUsername());
        } catch (SQLException e) {
            System.err.println("Failed to save user:");
            e.printStackTrace();
        }
    }

    public void deleteUser(String userId) {
        try (PreparedStatement stmt = sqliteConnection.prepareStatement(
            "DELETE FROM users WHERE id = ?")) {
            stmt.setString(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private user convertResultSetToUser(ResultSet rs) throws SQLException {
        user User = new user();
        User.setId(rs.getString("id"));
        User.setUsername(rs.getString("username"));
        User.setPassword(rs.getString("password"));
        User.setRole(rs.getString("role"));
        User.setFullName(rs.getString("full_name"));
        return User;
    }

    // Create default admin method
    private void createDefaultAdmin() {
        if (getAllUsers().isEmpty()) {
            user admin = new user();
            admin.setId("admin1");
            admin.setUsername("admin");
            admin.setPassword("admin123");
            admin.setRole("admin");
            admin.setFullName("Administrator");
            saveUser(admin);
        }
    }

    // Order to SQLite
    public void saveOrderToSQLite(order Order) {
        String sql = "INSERT INTO orders (id, customer_name, room_number, service_type, items_json, " +
                    "total_amount, created_date, expected_completion_date, assigned_staff_id, " +
                    "status, payment_status, created_by, synced) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    
        try (PreparedStatement stmt = sqliteConnection.prepareStatement(sql)) {   
            stmt.setString(1, Order.getId());
            stmt.setString(2, Order.getCustomerName());
            stmt.setString(3, Order.getRoomNumber());
            stmt.setString(4, Order.getServiceType().toString());
            
            // Convert items list to JSON
            ObjectMapper mapper = new ObjectMapper();
            String itemsJson = mapper.writeValueAsString(Order.getItems());
            stmt.setString(5, itemsJson);
            
            stmt.setDouble(6, Order.getTotalAmount());
            stmt.setString(7, Order.getCreatedDate().toString());
            stmt.setString(8, Order.getExpectedCompletionDate().toString());
            stmt.setString(9, Order.getAssignedStaffId());
            stmt.setString(10, Order.getStatus().toString());
            stmt.setString(11, Order.getPaymentStatus().toString());
            stmt.setString(12, Order.getCreatedBy());
            stmt.setInt(13, Order.isSynced() ? 1 : 0);
            
            stmt.executeUpdate();
            System.out.println("Order saved to SQLite successfully: " + Order.getId());
        } catch (SQLException | JsonProcessingException e) {
            System.err.println("Failed to save order:");
            e.printStackTrace();
        }
    }
    
    // Get all orders - ADDED METHOD
    public List<order> getAllOrders() {
        List<order> Orders = new ArrayList<>();

        String sql = "SELECT * FROM orders ORDER BY created_date DESC";

        try (PreparedStatement stmt = sqliteConnection.prepareStatement(sql)){
            // Use PreparedStatement to prevent SQL injection
             ResultSet rs = stmt.executeQuery();
    
            while (rs.next()) {
                order Order = convertResultSetToOrder(rs);
                Orders.add(Order);
            }
        } catch (SQLException | JsonProcessingException e) {
            e.printStackTrace();
            System.err.println("Error fetching orders: " + e.getMessage());
        }
        return Orders;
    }
    
    // Get order by ID - ADDED METHOD
    public Optional<order> getOrderById(String orderId) {
        try (PreparedStatement stmt = sqliteConnection.prepareStatement(
             "SELECT * FROM orders WHERE id = ?")) {
            stmt.setString(1, orderId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(convertResultSetToOrder(rs));
            }
        } catch (SQLException | JsonProcessingException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
    
    // Get orders by status
    public List<order> getOrdersByStatus(order.OrderStatus status) {
        List<order> Orders = new ArrayList<>();

        // use plain database value, not enum constant name
        String sql = "SELECT * FROM orders WHERE LOWER(status) = LOWER(?)";

        try (PreparedStatement stmt = sqliteConnection.prepareStatement(sql)) {
            stmt.setString(1, status.getDatabaseValue());
            ResultSet rs = stmt.executeQuery();

            
            while (rs.next()) {
                order Order = convertResultSetToOrder(rs);
                Orders.add(Order);
        
            }
        } catch (SQLException | JsonProcessingException e) {
            e.printStackTrace();
            System.err.println("Error fetching orders by status: " + e.getMessage());
        }
        return Orders;
    }


    // Assign staff to order
    public boolean assignStaffToOrder(String orderId, String staffId) {
        try (PreparedStatement stmt = sqliteConnection.prepareStatement(
            "UPDATE orders SET assigned_staff_id = ?, status = ? WHERE id = ?")) {
            
            stmt.setString(1, staffId);
            stmt.setString(2, "PROCESSING");
            stmt.setString(3, orderId);
            
            int updated = stmt.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            System.err.println("SQL update failed: " + e.getMessage());
            logSyncError(orderId, "SQL assignment failed: " + e.getMessage());
        }
        return false;
    }

    private order convertResultSetToOrder(ResultSet rs) throws SQLException, JsonProcessingException {
        order Order = new order();
        Order.setId(rs.getString("id"));
        Order.setCustomerName(rs.getString("customer_name"));
        Order.setRoomNumber(rs.getString("room_number"));
        Order.setTotalAmount(rs.getDouble("total_amount"));

        //safe enum conversions usinf database values
        String serviceTypeStr = rs.getString("service_type");
        Order.setServiceType(order.ServiceType.fromDatabaseValue(serviceTypeStr));
        
        ObjectMapper mapper = new ObjectMapper();
        List<ClothingItem> items = Arrays.asList(
            mapper.readValue(rs.getString("items_json"), ClothingItem[].class));
        Order.setItems(items);
        

        Order.setCreatedDate(LocalDate.parse(rs.getString("created_date")));
        Order.setExpectedCompletionDate(LocalDate.parse(rs.getString("expected_completion_date")));
        Order.setAssignedStaffId(rs.getString("assigned_staff_id"));
        Order.setStatus(order.OrderStatus.valueOf(rs.getString("status")));
        Order.setPaymentStatus(order.PaymentStatus.valueOf(rs.getString("payment_status")));
        Order.setCreatedBy(rs.getString("created_by"));
        Order.setSynced(rs.getInt("synced") == 1);
        
        return Order;
    }

    public void markOrderAsSynced(String OrderId) {
        try (PreparedStatement stmt = sqliteConnection.prepareStatement(
            "UPDATE orders SET synced = 1 WHERE id = ?")) {
            stmt.setString(1, OrderId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveOrder(order Order) {
        // Save to SQLite
        saveOrderToSQLite(Order);
    }

    public boolean updateOrder(order Order) {
        // Update SQLite
        try {
            updateOrderInSQLite(Order);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Update the order in SQLite
    private void updateOrderInSQLite(order Order) {
        try (PreparedStatement stmt = sqliteConnection.prepareStatement(
            "UPDATE orders SET customer_name=?, room_number=?, service_type=?, " +
            "items_json=?, total_amount=?, created_date=?, expected_completion_date=?, " +
            "assigned_staff_id=?, status=?, payment_status=?, created_by=?, synced=? " +
            "WHERE id=?")) {
            
            stmt.setString(1, Order.getCustomerName());
            stmt.setString(2, Order.getRoomNumber());
            stmt.setString(3, Order.getServiceType().toString());
            
            ObjectMapper mapper = new ObjectMapper();
            stmt.setString(4, mapper.writeValueAsString(Order.getItems()));
            
            stmt.setDouble(5, Order.getTotalAmount());
            stmt.setString(6, Order.getCreatedDate().toString());
            stmt.setString(7, Order.getExpectedCompletionDate().toString());
            stmt.setString(8, Order.getAssignedStaffId());
            stmt.setString(9, Order.getStatus().toString());
            stmt.setString(10, Order.getPaymentStatus().toString());
            stmt.setString(11, Order.getCreatedBy());
            stmt.setInt(12, Order.isSynced() ? 1 : 0);
            stmt.setString(13, Order.getId());
            
            stmt.executeUpdate();
        } catch (SQLException | JsonProcessingException e) {
            e.printStackTrace();
        }
    }
    
    // Get orders between two dates
    public List<order> getOrdersBetweenDates(LocalDate start, LocalDate end) {
        List<order> orders = new ArrayList<>();
        try (PreparedStatement stmt = sqliteConnection.prepareStatement(
            "SELECT * FROM orders WHERE created_date BETWEEN ? AND ?")) {
            
            stmt.setString(1, start.toString());
            stmt.setString(2, end.toString());
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                orders.add(convertResultSetToOrder(rs));
            }
        } catch (SQLException | JsonProcessingException e) {
            e.printStackTrace();
        }
        return orders;
    }

    public Optional<user> getUserById(String id) {
        try (PreparedStatement stmt = sqliteConnection.prepareStatement(
            "SELECT * FROM users WHERE id = ?")) {
            
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                user User = new user();
                User.setId(rs.getString("id"));
                User.setFullName(rs.getString("full_name"));
                return Optional.of(User);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
    
    // Close database connections
    public void close() {
        try {
            if (sqliteConnection != null) {
                sqliteConnection.close();
            }
            if (mongoClient != null) {
                mongoClient.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
