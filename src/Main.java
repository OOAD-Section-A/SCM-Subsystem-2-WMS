import wms.controllers.OutboundTaskController;
import wms.integration.SCMDatabaseAdapter;
import wms.integration.Subsystem14PackingAdapter;
import wms.integration.Subsystem8ForecastAdapter;
import wms.services.SlottingOptimizerService;
import wms.views.WarehouseTerminalView;

public class Main {
  public static void main(String[] args) {
    WarehouseTerminalView.printSystemEvent("BOOT", "============================================================");
    WarehouseTerminalView.printSystemEvent("BOOT", "Initializing SCM Warehouse Management Subsystem (WMS)...");
    WarehouseTerminalView.printSystemEvent("BOOT", "Composition Root: wiring adapters, services, and controllers");
    WarehouseTerminalView.printSystemEvent("BOOT", "============================================================");

    SCMDatabaseAdapter repository = new SCMDatabaseAdapter();
    Subsystem14PackingAdapter packingAdapter = new Subsystem14PackingAdapter();
    WarehouseTerminalView.printSystemEvent(
        "BOOT",
        "Packing integration adapter ready: " + packingAdapter.getClass().getSimpleName());

    Subsystem8ForecastAdapter forecastAdapter = new Subsystem8ForecastAdapter();
    SlottingOptimizerService optimizer = new SlottingOptimizerService(forecastAdapter);
    optimizer.evaluateAndOptimizeSlotting("P-50", "BULK-ZONE-9", "FAST-PICK-1");

    WarehouseTerminalView.printSystemEvent("BOOT", "Starting asynchronous Outbound Task Poller...");
    OutboundTaskController outboundTaskController = new OutboundTaskController(repository);
    Thread pollerThread = new Thread(outboundTaskController);
    pollerThread.start();

    WarehouseTerminalView.printSystemEvent(
        "BOOT",
        "WMS Subsystem successfully initialized and awaiting integration triggers.");
  }
}