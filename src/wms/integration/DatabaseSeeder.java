package wms.integration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import wms.views.WarehouseTerminalView;

public class DatabaseSeeder {

	public static void seedInitialData() {
		Connection connection = DatabaseConnectionManager.getInstance().getConnection();

		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(
					"INSERT IGNORE INTO products (productId, category, name) VALUES (\"P-50\", \"ELECTRONICS\", \"Smartphone\")");
			statement.executeUpdate(
					"INSERT IGNORE INTO bins (binId, zone, capacity) VALUES (\"A1\", \"PICK_ZONE\", 100)");
			statement.executeUpdate(
					"INSERT IGNORE INTO warehouse_tasks (taskId, taskType, productId, targetBinId, status) VALUES (\"T-100\", \"PICK\", \"P-50\", \"A1\", \"PENDING\")");
			statement.executeUpdate(
					"INSERT IGNORE INTO warehouse_tasks (taskId, taskType, productId, targetBinId, status) VALUES (\"T-ERR\", \"PICK\", \"P-99\", \"B2\", \"PENDING\")");

			WarehouseTerminalView.printSystemEvent("DB SEEDER", "Initial test data seeded into MySQL successfully.");
		} catch (SQLException e) {
			SafeExceptionAdapter.handle(e);
		}
	}
}
