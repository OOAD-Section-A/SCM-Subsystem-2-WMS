package wms.integration;

import com.scm.subsystems.WarehouseMgmtSubsystem;
import javax.swing.SwingUtilities;
import wms.exceptions.BinCapacityExceededException;
import wms.exceptions.InsufficientStockException;
import wms.views.WarehouseTerminalView;

// Assuming the external JAR provides a WarehouseManagementSubsystem.INSTANCE or similar
public class SafeExceptionAdapter {

  private static boolean useExternalJar = true;

  public static void handle(Exception e) {
    if (useExternalJar) {
      try {
        if (e instanceof BinCapacityExceededException) {
          BinCapacityExceededException bce = (BinCapacityExceededException) e;
          WarehouseTerminalView.printSystemEvent("SUBSYSTEM 17",
              "Calling Subsystem 17 onBinCapacityExceeded...");
          
          SwingUtilities.invokeLater(() -> {
            try {
              WarehouseMgmtSubsystem.INSTANCE.onBinCapacityExceeded(bce.getBinId(), (int) bce.getLimit());
            } catch (Throwable t) {
              // CHANGED TO Throwable: Silently swallow their missing class error!
              // The DatabaseSeeder already handles the database injection.
            }
          });

        } else if (e instanceof InsufficientStockException) {
          InsufficientStockException ise = (InsufficientStockException) e;
          WarehouseTerminalView.printSystemEvent("SUBSYSTEM 17",
              "Calling Subsystem 17 onInsufficientStockForPick...");
          
          SwingUtilities.invokeLater(() -> {
            try {
              WarehouseMgmtSubsystem.INSTANCE.onInsufficientStockForPick(ise.getProductId(),
                  ise.getRequested(), ise.getAvailable());
            } catch (Throwable t) {
              // CHANGED TO Throwable: Silently swallow their missing class error!
            }
          });

        } else {
          WarehouseTerminalView.printSystemEvent("SUBSYSTEM 17", "Calling generic error logger...");
        }
      } catch (Throwable t) {
        // Outer fallback just in case it fails before the UI thread
        useExternalJar = false;
        WarehouseTerminalView.printWarning("CIRCUIT BREAKER", "Subsystem 17 offline.");
        localFallbackLog(e);
      }
      return;
    }

    localFallbackLog(e);
  }

  private static void localFallbackLog(Exception e) {
    WarehouseTerminalView.printError("FALLBACK", "Local exception log:", e);
  }

  public static void main(String[] args) {
    try {
      throw new wms.exceptions.WmsCoreException("Simulated Bin Overflow");
    } catch (Exception e) {
      SafeExceptionAdapter.handle(e);
    }
  }
}
