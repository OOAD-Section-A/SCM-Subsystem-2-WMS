package wms.integration;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import wms.views.WarehouseTerminalView;

/**
 * Seeds procurement and operational data from Excel into MySQL.
 *
 * SACRED ARCHITECTURE — do not modify insert ordering.
 * FK dependency order must be preserved:
 *   products → proc_suppliers → proc_product_supplier
 *   → proc_purchase_orders → proc_po_items → proc_asn
 *   → goods_receipts → proc_quality_inspections
 *   → proc_supplier_invoices → proc_invoice_items
 *   → proc_supplier_payments → proc_discrepancies
 *
 * Uses INSERT IGNORE throughout — safe to run multiple times.
 * All inserts are idempotent — no data is duplicated on re-runs.
 *
 * DATA SOURCE: procurement_final_dataset.xlsx (14 sheets)
 * Sheets: Product, Supplier, ProductSupplier, PurchaseOrder, POItem,
 *         ASN, GRN, GRNItem, QualityInspection, SupplierInvoice,
 *         InvoiceItem, SupplierPayment, DeliveryLog, Discrepancy
 */
public class DatabaseSeeder {

    public static void seedInitialData() {
        Connection conn = DatabaseConnectionManager.getInstance().getConnection();
        if (conn == null) {
            WarehouseTerminalView.printWarning("DB SEEDER",
                    "Database connection unavailable. Skipping seeding.");
            return;
        }

        WarehouseTerminalView.printSystemEvent("DB SEEDER",
                "Initializing enterprise data from Excel file...");

        try (FileInputStream fis = new FileInputStream("procurement_final_dataset.xlsx");
             Workbook workbook = new XSSFWorkbook(fis)) {

            DataFormatter formatter = new DataFormatter();

            // ── PREREQUISITE: Inject FK anchors ──────────────────────────────
            // WH-001 is registered by WarehouseManagementDatabaseAdapter.
            // W-01 and WH1/WH2/WH3 are needed for Excel PO warehouse_id values.
            // DEFAULT-SUP is the fallback supplier FK for products.
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT IGNORE INTO warehouses (warehouse_id, warehouse_name) VALUES ('W-01', 'Main Distribution Center')");
                stmt.execute("INSERT IGNORE INTO warehouses (warehouse_id, warehouse_name) VALUES ('WH1', 'Auto-Generated WH1')");
                stmt.execute("INSERT IGNORE INTO warehouses (warehouse_id, warehouse_name) VALUES ('WH2', 'Auto-Generated WH2')");
                stmt.execute("INSERT IGNORE INTO warehouses (warehouse_id, warehouse_name) VALUES ('WH3', 'Auto-Generated WH3')");
                stmt.execute("INSERT IGNORE INTO proc_suppliers (supplier_id, name, avg_lead_time, reliability_score, status) VALUES ('DEFAULT-SUP', 'Fallback Supplier', 5, 99.9, 'ACTIVE')");
            }

            // ── SHEET 1: Suppliers ────────────────────────────────────────────
            // Headers: supplier_id, name, avg_lead_time, reliability_score, status
            Sheet supplierSheet = workbook.getSheet("Supplier");
            if (supplierSheet != null) {
                String sql = "INSERT IGNORE INTO proc_suppliers (supplier_id, name, avg_lead_time, reliability_score, status) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (int i = 1; i <= supplierSheet.getLastRowNum(); i++) {
                        Row row = supplierSheet.getRow(i);
                        if (row == null) continue;
                        ps.setString(1, str(row, 0, formatter));
                        ps.setString(2, str(row, 1, formatter));
                        ps.setInt(3, intVal(row, 2, formatter));
                        ps.setDouble(4, dblVal(row, 3, formatter));
                        ps.setString(5, str(row, 4, formatter).toUpperCase());
                        ps.executeUpdate();
                    }
                }
                WarehouseTerminalView.printSystemEvent("DB SEEDER",
                        "Suppliers seeded: " + supplierSheet.getLastRowNum());
            }

            // ── SHEET 2: Products ─────────────────────────────────────────────
            // Headers: product_id, name, category, shelf_life_days, storage_type, base_uom
            Sheet productSheet = workbook.getSheet("Product");
            if (productSheet != null) {
                String sql = "INSERT IGNORE INTO products (product_id, product_name, sku, category, sub_category, supplier_id, unit_of_measure) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (int i = 1; i <= productSheet.getLastRowNum(); i++) {
                        Row row = productSheet.getRow(i);
                        if (row == null) continue;
                        String productId = str(row, 0, formatter);
                        String category  = str(row, 2, formatter);
                        ps.setString(1, productId);
                        ps.setString(2, str(row, 1, formatter));
                        ps.setString(3, productId + "-SKU");
                        ps.setString(4, category);
                        ps.setString(5, category);
                        ps.setString(6, "DEFAULT-SUP");
                        ps.setString(7, str(row, 5, formatter));
                        ps.executeUpdate();
                    }
                }
                WarehouseTerminalView.printSystemEvent("DB SEEDER",
                        "Products seeded: " + productSheet.getLastRowNum());
            }

            // ── SHEET 3: ProductSupplier ──────────────────────────────────────
            // Headers: product_id, supplier_id, price, min_order_qty, last_updated
            Sheet prodSupSheet = workbook.getSheet("ProductSupplier");
            if (prodSupSheet != null) {
                String sql = "INSERT IGNORE INTO proc_product_supplier (product_id, supplier_id, price, min_order_qty) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (int i = 1; i <= prodSupSheet.getLastRowNum(); i++) {
                        Row row = prodSupSheet.getRow(i);
                        if (row == null) continue;
                        ps.setString(1, str(row, 0, formatter));
                        ps.setString(2, str(row, 1, formatter));
                        ps.setDouble(3, dblVal(row, 2, formatter));
                        ps.setInt(4, intVal(row, 3, formatter));
                        ps.executeUpdate();
                    }
                }
                WarehouseTerminalView.printSystemEvent("DB SEEDER",
                        "ProductSupplier links seeded: " + prodSupSheet.getLastRowNum());
            }

            // ── SHEET 4: PurchaseOrders ───────────────────────────────────────
            // Headers: po_id, supplier_id, warehouse_id, order_date, expected_delivery, priority, status
            Sheet poSheet = workbook.getSheet("PurchaseOrder");
            if (poSheet != null) {
                String sql = "INSERT IGNORE INTO proc_purchase_orders (po_id, supplier_id, warehouse_id, order_date, expected_delivery, priority, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (int i = 1; i <= poSheet.getLastRowNum(); i++) {
                        Row row = poSheet.getRow(i);
                        if (row == null) continue;
                        String warehouseId = str(row, 2, formatter);
                        // Ensure warehouse exists before inserting PO
                        try (PreparedStatement whPs = conn.prepareStatement(
                                "INSERT IGNORE INTO warehouses (warehouse_id, warehouse_name) VALUES (?, ?)")) {
                            whPs.setString(1, warehouseId);
                            whPs.setString(2, "Auto-Generated " + warehouseId);
                            whPs.executeUpdate();
                        }
                        ps.setString(1, str(row, 0, formatter));
                        ps.setString(2, str(row, 1, formatter));
                        ps.setString(3, warehouseId);
                        ps.setDate(4, dateVal(row, 3));
                        ps.setDate(5, dateVal(row, 4));
                        ps.setString(6, str(row, 5, formatter).toUpperCase());
                        ps.setString(7, str(row, 6, formatter).toUpperCase());
                        ps.executeUpdate();
                    }
                }
                WarehouseTerminalView.printSystemEvent("DB SEEDER",
                        "Purchase Orders seeded: " + poSheet.getLastRowNum());
            }

            // ── SHEET 5: POItems ──────────────────────────────────────────────
            // Headers: po_id, product_id, ordered_qty, received_qty, pending_qty(GENERATED), agreed_price
            Sheet poItemSheet = workbook.getSheet("POItem");
            if (poItemSheet != null) {
                String sql = "INSERT IGNORE INTO proc_po_items (po_id, product_id, ordered_qty, received_qty, agreed_price) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (int i = 1; i <= poItemSheet.getLastRowNum(); i++) {
                        Row row = poItemSheet.getRow(i);
                        if (row == null) continue;
                        ps.setString(1, str(row, 0, formatter));
                        ps.setString(2, str(row, 1, formatter));
                        ps.setInt(3, intVal(row, 2, formatter));
                        ps.setInt(4, intVal(row, 3, formatter));
                        // Col 4 is pending_qty — GENERATED ALWAYS — skip it
                        ps.setDouble(5, dblVal(row, 5, formatter));
                        ps.executeUpdate();
                    }
                }
                WarehouseTerminalView.printSystemEvent("DB SEEDER",
                        "PO Items seeded: " + poItemSheet.getLastRowNum());
            }

            // ── SHEET 6: ASN ──────────────────────────────────────────────────
            // Headers: asn_id, po_id, supplier_id, expected_arrival
            Sheet asnSheet = workbook.getSheet("ASN");
            if (asnSheet != null) {
                String sql = "INSERT IGNORE INTO proc_asn (asn_id, po_id, supplier_id, expected_arrival) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (int i = 1; i <= asnSheet.getLastRowNum(); i++) {
                        Row row = asnSheet.getRow(i);
                        if (row == null) continue;
                        ps.setString(1, str(row, 0, formatter));
                        ps.setString(2, str(row, 1, formatter));
                        ps.setString(3, str(row, 2, formatter));
                        ps.setDate(4, dateVal(row, 3));
                        ps.executeUpdate();
                    }
                }
                WarehouseTerminalView.printSystemEvent("DB SEEDER",
                        "ASNs seeded: " + asnSheet.getLastRowNum());
            }

            // ── SHEET 7: GRN → goods_receipts ────────────────────────────────
            // Headers: grn_id, po_id, warehouse_id, received_date, status
            // goods_receipts needs: goods_receipt_id, purchase_order_id, supplier_id,
            //                       product_id, ordered_qty, received_qty, condition_status
            // We seed one row per GRN header with a placeholder product — GRNItem handles details
            Sheet grnSheet = workbook.getSheet("GRN");
            if (grnSheet != null) {
                // First build a po→supplier map from proc_purchase_orders
                java.util.Map<String, String> poSupplierMap = new java.util.HashMap<>();
                try (java.sql.ResultSet rs = conn.createStatement()
                        .executeQuery("SELECT po_id, supplier_id FROM proc_purchase_orders")) {
                    while (rs.next()) {
                        poSupplierMap.put(rs.getString("po_id"), rs.getString("supplier_id"));
                    }
                }
                String sql = "INSERT IGNORE INTO goods_receipts (goods_receipt_id, purchase_order_id, supplier_id, product_id, ordered_qty, received_qty, condition_status) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (int i = 1; i <= grnSheet.getLastRowNum(); i++) {
                        Row row = grnSheet.getRow(i);
                        if (row == null) continue;
                        String grnId  = str(row, 0, formatter);
                        String poId   = str(row, 1, formatter);
                        String suppId = poSupplierMap.getOrDefault(poId, "DEFAULT-SUP");
                        ps.setString(1, grnId);
                        ps.setString(2, poId);
                        ps.setString(3, suppId);
                        ps.setString(4, "DEFAULT-SUP"); // placeholder — GRNItem has real product
                        ps.setInt(5, 1);
                        ps.setInt(6, 1);
                        ps.setString(7, str(row, 4, formatter).toUpperCase());
                        ps.executeUpdate();
                    }
                }
                WarehouseTerminalView.printSystemEvent("DB SEEDER",
                        "GRN headers seeded: " + grnSheet.getLastRowNum());
            }

            // ── SHEET 8: QualityInspection ────────────────────────────────────
            // Headers: inspection_id, grn_id, product_id, passed_qty, failed_qty, remarks
            Sheet qiSheet = workbook.getSheet("QualityInspection");
            if (qiSheet != null) {
                String sql = "INSERT IGNORE INTO proc_quality_inspections (inspection_id, grn_id, product_id, passed_qty, failed_qty, remarks) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (int i = 1; i <= qiSheet.getLastRowNum(); i++) {
                        Row row = qiSheet.getRow(i);
                        if (row == null) continue;
                        ps.setString(1, str(row, 0, formatter));
                        ps.setString(2, str(row, 1, formatter));
                        ps.setString(3, str(row, 2, formatter));
                        ps.setInt(4, intVal(row, 3, formatter));
                        ps.setInt(5, intVal(row, 4, formatter));
                        ps.setString(6, str(row, 5, formatter));
                        ps.executeUpdate();
                    }
                }
                WarehouseTerminalView.printSystemEvent("DB SEEDER",
                        "Quality Inspections seeded: " + qiSheet.getLastRowNum());
            }

            // ── SHEET 9: SupplierInvoice ──────────────────────────────────────
            // Headers: invoice_id, po_id, total_amount, invoice_date
            Sheet invSheet = workbook.getSheet("SupplierInvoice");
            if (invSheet != null) {
                String sql = "INSERT IGNORE INTO proc_supplier_invoices (invoice_id, po_id, total_amount, invoice_date) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (int i = 1; i <= invSheet.getLastRowNum(); i++) {
                        Row row = invSheet.getRow(i);
                        if (row == null) continue;
                        ps.setString(1, str(row, 0, formatter));
                        ps.setString(2, str(row, 1, formatter));
                        ps.setDouble(3, dblVal(row, 2, formatter));
                        ps.setDate(4, dateVal(row, 3));
                        ps.executeUpdate();
                    }
                }
                WarehouseTerminalView.printSystemEvent("DB SEEDER",
                        "Supplier Invoices seeded: " + invSheet.getLastRowNum());
            }

            // ── SHEET 10: InvoiceItem ─────────────────────────────────────────
            // Headers: invoice_id, product_id, billed_qty, billed_price
            Sheet invItemSheet = workbook.getSheet("InvoiceItem");
            if (invItemSheet != null) {
                String sql = "INSERT IGNORE INTO proc_invoice_items (invoice_id, product_id, billed_qty, billed_price) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (int i = 1; i <= invItemSheet.getLastRowNum(); i++) {
                        Row row = invItemSheet.getRow(i);
                        if (row == null) continue;
                        ps.setString(1, str(row, 0, formatter));
                        ps.setString(2, str(row, 1, formatter));
                        ps.setInt(3, intVal(row, 2, formatter));
                        ps.setDouble(4, dblVal(row, 3, formatter));
                        ps.executeUpdate();
                    }
                }
                WarehouseTerminalView.printSystemEvent("DB SEEDER",
                        "Invoice Items seeded: " + invItemSheet.getLastRowNum());
            }

            // ── SHEET 11: SupplierPayment ─────────────────────────────────────
            // Headers: payment_id, invoice_id, amount_paid, status
            Sheet paySheet = workbook.getSheet("SupplierPayment");
            if (paySheet != null) {
                String sql = "INSERT IGNORE INTO proc_supplier_payments (payment_id, invoice_id, amount_paid, status) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (int i = 1; i <= paySheet.getLastRowNum(); i++) {
                        Row row = paySheet.getRow(i);
                        if (row == null) continue;
                        ps.setString(1, str(row, 0, formatter));
                        ps.setString(2, str(row, 1, formatter));
                        ps.setDouble(3, dblVal(row, 2, formatter));
                        ps.setString(4, str(row, 3, formatter).toUpperCase());
                        ps.executeUpdate();
                    }
                }
                WarehouseTerminalView.printSystemEvent("DB SEEDER",
                        "Supplier Payments seeded: " + paySheet.getLastRowNum());
            }

            // ── SHEET 12: Discrepancy ─────────────────────────────────────────
            // Headers: id, type, product_id, supplier_id, description
            Sheet discSheet = workbook.getSheet("Discrepancy");
            if (discSheet != null) {
                String sql = "INSERT IGNORE INTO proc_discrepancies (type, product_id, supplier_id, description) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (int i = 1; i <= discSheet.getLastRowNum(); i++) {
                        Row row = discSheet.getRow(i);
                        if (row == null) continue;
                        ps.setString(1, str(row, 1, formatter));
                        ps.setString(2, str(row, 2, formatter));
                        ps.setString(3, str(row, 3, formatter));
                        ps.setString(4, str(row, 4, formatter));
                        ps.executeUpdate();
                    }
                }
                WarehouseTerminalView.printSystemEvent("DB SEEDER",
                        "Discrepancies seeded: " + discSheet.getLastRowNum());
            }

            // ── DeliveryLog sheet is informational only — no matching DB table ─

            WarehouseTerminalView.printSystemEvent("DB SEEDER",
                    "Excel data successfully injected into MySQL!");

        } catch (Exception e) {
            WarehouseTerminalView.printWarning("DB SEEDER",
                    "Excel Parsing Error: " + e.getMessage());
        }

        // ── HARDCODED WMS OPERATIONAL DATA ────────────────────────────────────
        // T-100 and T-ERR are simulated by SCMDatabaseAdapter.fetchPendingTasks().
        // PICK_ZONE, A1, B2 bins are needed for the outbound poller demo.
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT IGNORE INTO warehouse_zones (zone_id, warehouse_id, zone_type) VALUES ('PICK_ZONE', 'W-01', 'PICKING')");
            stmt.execute("INSERT IGNORE INTO bins (bin_id, zone_id, bin_capacity, bin_status) VALUES ('A1', 'PICK_ZONE', 100, 'AVAILABLE')");
            stmt.execute("INSERT IGNORE INTO bins (bin_id, zone_id, bin_capacity, bin_status) VALUES ('B2', 'PICK_ZONE', 100, 'AVAILABLE')");
        } catch (Exception e) {
            // Already exists — safe to ignore
        }

        // ── EXCEPTION LOG SEED ────────────────────────────────────────────────
        try (java.sql.PreparedStatement ps = conn.prepareStatement(
                "INSERT IGNORE INTO scm_exception_log (exception_id, exception_name, severity, subsystem, error_message, logged_at) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, 500);
            ps.setString(2, "InsufficientStockForPick");
            ps.setString(3, "MAJOR");
            ps.setString(4, "Warehouse Mgmt");
            ps.setString(5, "Task T-ERR failed 3 times: Not enough stock for pick task in bin B2");
            ps.setTimestamp(6, new java.sql.Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("[DEBUG] Exception Seeder: " + e.getMessage());
        }
    }

    // ── HELPER METHODS ────────────────────────────────────────────────────────

    private static String str(Row row, int idx, DataFormatter fmt) {
        Cell cell = row.getCell(idx, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        return fmt.formatCellValue(cell).trim();
    }

    private static int intVal(Row row, int idx, DataFormatter fmt) {
        String val = str(row, idx, fmt);
        if (val.isEmpty()) return 0;
        try { return (int) Double.parseDouble(val); } catch (Exception e) { return 0; }
    }

    private static double dblVal(Row row, int idx, DataFormatter fmt) {
        String val = str(row, idx, fmt);
        if (val.isEmpty()) return 0.0;
        try { return Double.parseDouble(val); } catch (Exception e) { return 0.0; }
    }

    private static java.sql.Date dateVal(Row row, int idx) {
        Cell cell = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell != null && cell.getCellType() == CellType.NUMERIC
                && DateUtil.isCellDateFormatted(cell)) {
            return new java.sql.Date(cell.getDateCellValue().getTime());
        }
        return new java.sql.Date(System.currentTimeMillis());
    }
}