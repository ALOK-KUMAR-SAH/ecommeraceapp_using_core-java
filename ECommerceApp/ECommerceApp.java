import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ECommerceApp {
    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASSWORD;
    private JTable productTable;
    private DefaultTableModel tableModel;

    // Load database credentials from config.properties
    static {
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            Properties properties = new Properties();
            properties.load(fis);
            DB_URL = properties.getProperty("DB_URL");
            DB_USER = properties.getProperty("DB_USER");
            DB_PASSWORD = properties.getProperty("DB_PASSWORD");
        } catch (IOException e) {
            System.err.println("Error loading database credentials: " + e.getMessage());
            System.exit(1); // Exit if config file is missing
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ECommerceApp app = new ECommerceApp();
            app.testDBConnection();
            app.createAndShowGUI();
        });
    }

    //  Check database connection
    private void testDBConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println(" Database connection successful!");
        } catch (ClassNotFoundException e) {
            System.err.println(" MySQL JDBC Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println(" Database connection failed: " + e.getMessage());
        }
    }

    //  Create GUI window to display products
    private void createAndShowGUI() {
        JFrame frame = new JFrame("E-Commerce Product List");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        JPanel panel = new JPanel(new BorderLayout());

        // Table setup
        String[] columnNames = {"ID", "Product Name", "Price (Rs.)", "Stock"};
        tableModel = new DefaultTableModel(columnNames, 0);
        productTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(productTable);

        // Fetch and display products
        loadProducts();

        // Buy Now button
        JButton buyNowButton = new JButton("Buy Now");
        buyNowButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buyProduct();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(buyNowButton);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        frame.add(panel);
        frame.setVisible(true);
    }

    //  Load products into JTable
    private void loadProducts() {
        String query = "SELECT id, name, price, stock FROM products";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            tableModel.setRowCount(0); // Clear table before loading data
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                int stock = rs.getInt("stock");

                tableModel.addRow(new Object[]{id, name, price, stock});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error retrieving products: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    //  Buy Product Feature
    private void buyProduct() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(null, "Please select a product to buy.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int productId = (int) tableModel.getValueAt(selectedRow, 0);
        String productName = (String) tableModel.getValueAt(selectedRow, 1);
        double productPrice = (double) tableModel.getValueAt(selectedRow, 2);
        int productStock = (int) tableModel.getValueAt(selectedRow, 3);

        if (productStock <= 0) {
            JOptionPane.showMessageDialog(null, "Sorry, " + productName + " is out of stock.", "Out of Stock", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(null, "Do you want to buy " + productName + " for Rs. " + productPrice + "?", "Confirm Purchase", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            processPurchase(productId, productStock - 1);
            JOptionPane.showMessageDialog(null, "Successfully purchased " + productName + "!", "Purchase Success", JOptionPane.INFORMATION_MESSAGE);
            loadProducts(); // Refresh product list
        }
    }

    //  Process purchase and update stock in database
    private void processPurchase(int productId, int newStock) {
        String updateQuery = "UPDATE products SET stock = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {

            pstmt.setInt(1, newStock);
            pstmt.setInt(2, productId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error processing purchase: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
//javac -cp ".;mysql-connector-j-9.2.0.jar" ECommerceApp.java
//java -cp ".;mysql-connector-j-9.2.0.jar" ECommerceApp
