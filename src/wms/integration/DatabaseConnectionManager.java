package wms.integration;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import wms.views.WarehouseTerminalView;

public class DatabaseConnectionManager {

	private static DatabaseConnectionManager instance;
	private Connection connection;

	private DatabaseConnectionManager() {
		Properties properties = new Properties();

		try (InputStream inputStream = new FileInputStream("lib/database.properties")) {
			properties.load(inputStream);

			String url = properties.getProperty("db.url");
			String user = properties.getProperty("db.user");
			String password = properties.getProperty("db.password");

			connection = DriverManager.getConnection(url, user, password);
			WarehouseTerminalView.printSystemEvent("DATABASE", "Connection established successfully.");
		} catch (Exception e) {
			SafeExceptionAdapter.handle(e);
		}
	}

	public static synchronized DatabaseConnectionManager getInstance() {
		if (instance == null) {
			instance = new DatabaseConnectionManager();
		}
		return instance;
	}

	public Connection getConnection() {
		return connection;
	}

	public void closeConnection() {
		try {
			if (connection != null && !connection.isClosed()) {
				connection.close();
			}
		} catch (Exception e) {
			SafeExceptionAdapter.handle(e);
		}
	}
}
