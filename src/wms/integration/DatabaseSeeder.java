package wms.integration;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import wms.views.WarehouseTerminalView;

public class DatabaseSeeder {

    public static void seedInitialData() {
        Connection conn = DatabaseConnectionManager.getInstance().getConnection();
        if (conn == null) {
            WarehouseTerminalView.printWarning("DB SEEDER", "Database connection unavailable. Skipping seeding.");
            return;
        }

        WarehouseTerminalView.printSystemEvent("DB SEEDER", "Initializing enterprise data from Excel file...");

        try (FileInputStream fis = new FileInputStream("procurement_final_dataset.xlsx");
             Workbook workbook = new XSSFWorkbook(fis)) {

            DataFormatter formatter = new DataFormatter();

            // 0. Inject Prerequisites (To satisfy Foreign Keys for POs)
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT IGNORE INTO warehouses (warehouse_id, warehouse_name) VALUES ('W-01', 'Main Distribution Center')");
                stmt.execute("INSERT IGNORE INTO proc_suppliers (supplier_id, name, avg_lead_time, reliability_score, status) VALUES ('DEFAULT-SUP', 'Fallback Supplier', 5, 99.9, 'ACTIVE')");
            }

            // 1. Seed Suppliers (Sheet: "Supplier")
            Sheet supplierSheet = workbook.getSheet("Supplier");
            if (supplierSheet != null) {
                String sql = "INSERT IGNORE INTO proc_suppliers (supplier_id, name, avg_lead_time, reliability_score, status) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    for (int i = 1; i <= supplierSheet.getLastRowNum(); i++) {
                        Row row = supplierSheet.getRow(i);
                        if (row != null) {
                            pstmt.setString(1, getCellString(row, 0, formatter)); // supplier_id
                            pstmt.setString(2, getCellString(row, 1, formatter)); // name
                            pstmt.setInt(3, getIntSafely(row, 2, formatter));     // avg_lead_time
                            pstmt.setDouble(4, getDoubleSafely(row, 3, formatter)); // reliability_score
                            pstmt.setString(5, getCellString(row, 4, formatter).toUpperCase()); // status
                            pstmt.executeUpdate();
                        }
                    }
                }
            }

            // 2. Seed Products (Sheet: "Product")
            Sheet productSheet = workbook.getSheet("Product");
            if (productSheet != null) {
                String sql = "INSERT IGNORE INTO products (product_id, product_name, sku, category, sub_category, supplier_id, unit_of_measure) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    for (int i = 1; i <= productSheet.getLastRowNum(); i++) {
                        Row row = productSheet.getRow(i);
                        if (row != null) {
                            String productId = getCellString(row, 0, formatter);
                            String category = getCellString(row, 2, formatter);
                            
                            pstmt.setString(1, productId); // product_id
                            pstmt.setString(2, getCellString(row, 1, formatter)); // name
                            pstmt.setString(3, productId + "-SKU"); // Mock SKU to satisfy NOT NULL
                            pstmt.setString(4, category); // category
                            pstmt.setString(5, category); // sub_category fallback
                            pstmt.setString(6, "DEFAULT-SUP"); // FK fallback
                            pstmt.setString(7, getCellString(row, 5, formatter)); // base_uom
                            pstmt.executeUpdate();
                        }
                    }
                }
            }

            // 3. Seed Purchase Orders (Sheet: "PurchaseOrder")
            Sheet poSheet = workbook.getSheet("PurchaseOrder");
            if (poSheet != null) {
                String sql = "INSERT IGNORE INTO proc_purchase_orders (po_id, supplier_id, warehouse_id, order_date, expected_delivery, priority, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    for (int i = 1; i <= poSheet.getLastRowNum(); i++) {
                        Row row = poSheet.getRow(i);
                        if (row != null) {
                            String poId = getCellString(row, 0, formatter);
                            String supplierId = getCellString(row, 1, formatter);
                            String warehouseId = getCellString(row, 2, formatter);

                            // DYNAMIC FK FIX: Create the exact warehouse needed by Excel on the fly!
                            try (PreparedStatement whStmt = conn.prepareStatement("INSERT IGNORE INTO warehouses (warehouse_id, warehouse_name) VALUES (?, ?)")) {
                                whStmt.setString(1, warehouseId);
                                whStmt.setString(2, "Auto-Generated " + warehouseId);
                                whStmt.executeUpdate();
                            }

                            pstmt.setString(1, poId);
                            pstmt.setString(2, supplierId);
                            pstmt.setString(3, warehouseId);
                            pstmt.setDate(4, getCellDate(row, 3)); 
                            pstmt.setDate(5, getCellDate(row, 4)); 
                            pstmt.setString(6, getCellString(row, 5, formatter).toUpperCase()); 
                            pstmt.setString(7, getCellString(row, 6, formatter).toUpperCase()); 
                            pstmt.executeUpdate();
                        }
                    }
                }
            }

            // 4. Seed PO Items (Sheet: "POItem") - CRITICAL: SKIPPING INDEX 4
            Sheet poItemSheet = workbook.getSheet("POItem");
            if (poItemSheet != null) {
                String sql = "INSERT IGNORE INTO proc_po_items (po_id, product_id, ordered_qty, received_qty, agreed_price) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    for (int i = 1; i <= poItemSheet.getLastRowNum(); i++) {
                        Row row = poItemSheet.getRow(i);
                        if (row != null) {
                            pstmt.setString(1, getCellString(row, 0, formatter)); // po_id
                            pstmt.setString(2, getCellString(row, 1, formatter)); // product_id
                            pstmt.setInt(3, getIntSafely(row, 2, formatter));     // ordered_qty
                            pstmt.setInt(4, getIntSafely(row, 3, formatter));     // received_qty
                            // STRICT SKIP: Index 4 is pending_qty (GENERATED ALWAYS)
                            pstmt.setDouble(5, getDoubleSafely(row, 5, formatter)); // agreed_price
                            pstmt.executeUpdate();
                        }
                    }
                }
            }

            WarehouseTerminalView.printSystemEvent("DB SEEDER", "Excel data successfully injected into MySQL!");

        } catch (Exception e) {
            WarehouseTerminalView.printWarning("DB SEEDER", "Excel Parsing Error: " + e.getMessage());
        }

        // 5. Hardcoded WMS Data for Outbound Poller Demo
        try (Statement stmt = conn.createStatement()) {
            // Insert prerequisite zone with valid enum value
            stmt.execute("INSERT IGNORE INTO warehouse_zones (zone_id, warehouse_id, zone_type) VALUES ('PICK_ZONE', 'W-01', 'PICKING')");
            // Fix: use bin_id (snake_case) not binId (camelCase) per schema
            stmt.execute("INSERT IGNORE INTO bins (bin_id, zone_id, bin_capacity, bin_status) VALUES ('A1', 'PICK_ZONE', 100, 'AVAILABLE')");
            stmt.execute("INSERT IGNORE INTO bins (bin_id, zone_id, bin_capacity, bin_status) VALUES ('B2', 'PICK_ZONE', 100, 'AVAILABLE')");
            // NOTE: warehouse_tasks does not exist in the shared schema.
            // T-100 and T-ERR are simulated by SCMDatabaseAdapter.fetchPendingTasks() in memory.
        } catch (Exception e) {
            // Silently ignore if already exists
        }
				// 6. Inject a simulated Exception Log
            try (java.sql.PreparedStatement excStmt = conn.prepareStatement(
                    "INSERT IGNORE INTO SCM_EXCEPTION_LOG (exception_id, exception_name, severity, subsystem, error_message, logged_at) VALUES (?, ?, ?, ?, ?, ?)")) {
                excStmt.setInt(1, 500);
                excStmt.setString(2, "InsufficientStockForPick");
                excStmt.setString(3, "MAJOR"); 
                excStmt.setString(4, "Warehouse Mgmt"); 
                excStmt.setString(5, "Task T-ERR failed 3 times: Not enough stock for pick task in bin B2");
                excStmt.setTimestamp(6, new java.sql.Timestamp(System.currentTimeMillis()));
                excStmt.executeUpdate();
            } catch (Exception e) {
                System.out.println("[DEBUG] Exception Seeder Failed: " + e.getMessage());
            }
    }

    // --- Helper Methods for Safe Excel Parsing ---
    private static String getCellString(Row row, int index, DataFormatter formatter) {
        Cell cell = row.getCell(index, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        return formatter.formatCellValue(cell).trim();
    }

    private static int getIntSafely(Row row, int index, DataFormatter formatter) {
        String val = getCellString(row, index, formatter);
        if (val.isEmpty()) return 0;
        try { return (int) Double.parseDouble(val); } catch (Exception e) { return 0; }
    }

    private static double getDoubleSafely(Row row, int index, DataFormatter formatter) {
        String val = getCellString(row, index, formatter);
        if (val.isEmpty()) return 0.0;
        try { return Double.parseDouble(val); } catch (Exception e) { return 0.0; }
    }

    private static java.sql.Date getCellDate(Row row, int index) {
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return new java.sql.Date(cell.getDateCellValue().getTime());
        }
        return new java.sql.Date(System.currentTimeMillis()); // Fallback to today if empty/broken
    }
}