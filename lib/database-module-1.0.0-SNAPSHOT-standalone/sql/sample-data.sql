INSERT INTO warehouses (warehouse_id, warehouse_name)
VALUES ('WH-001', 'Central Warehouse')
ON DUPLICATE KEY UPDATE warehouse_name = VALUES(warehouse_name);

INSERT INTO orders (
    order_id, customer_id, order_status, order_date, total_amount, payment_status, sales_channel
) VALUES (
    'ORD-1001', 'CUST-77', 'CONFIRMED', NOW(), 420.00, 'PAID', 'ONLINE'
)
ON DUPLICATE KEY UPDATE order_status = VALUES(order_status), total_amount = VALUES(total_amount);

INSERT INTO order_items (
    order_item_id, order_id, product_id, ordered_quantity, unit_price, line_total
) VALUES (
    'ITEM-1001', 'ORD-1001', 'PROD-APPLE-001', 3, 140.00, 420.00
)
ON DUPLICATE KEY UPDATE ordered_quantity = VALUES(ordered_quantity), line_total = VALUES(line_total);

INSERT INTO price_list (
    price_id, sku_id, region_code, channel, price_type, base_price, price_floor,
    currency_code, effective_from, effective_to, status
) VALUES (
    'PRICE-001', 'SKU-APPLE-001', 'SOUTH', 'RETAIL', 'RETAIL', 120.00, 100.00,
    'INR', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'ACTIVE'
)
ON DUPLICATE KEY UPDATE base_price = VALUES(base_price), price_floor = VALUES(price_floor);

INSERT INTO delivery_orders (
    delivery_id, order_id, customer_id, delivery_address, delivery_status, delivery_type,
    delivery_cost, assigned_agent, warehouse_id, created_at, updated_at
) VALUES (
    'SHIP-001', 'ORD-1001', 'CUST-77', 'Bengaluru, Karnataka', 'PENDING', 'STANDARD',
    180.00, 'AGENT-22', 'WH-001', NOW(), NOW()
)
ON DUPLICATE KEY UPDATE delivery_status = VALUES(delivery_status), updated_at = VALUES(updated_at);

INSERT INTO demand_forecasts (
    forecast_id, product_id, forecast_period, predicted_demand, confidence_score, generated_at, source_event_reference
) VALUES (
    'DF-001', 'PROD-APPLE-001', '2026-Q2', 850, 92.50, NOW(), 'BAR-001'
)
ON DUPLICATE KEY UPDATE predicted_demand = VALUES(predicted_demand), confidence_score = VALUES(confidence_score);

INSERT INTO sales_records (
    sale_id, product_id, store_id, sale_date, quantity_sold, unit_price, revenue, region
) VALUES (
    'SALE-001', 'PROD-APPLE-001', 'STORE-01', CURDATE(), 10, 140.00, 1400.00, 'SOUTH'
)
ON DUPLICATE KEY UPDATE quantity_sold = VALUES(quantity_sold), revenue = VALUES(revenue);

INSERT INTO holiday_calendar (
    holiday_id, holiday_date, holiday_name, holiday_type, region_applicable
) VALUES (
    'HOL-001', CURDATE(), 'Regional Festival', 'PUBLIC', 'SOUTH'
)
ON DUPLICATE KEY UPDATE holiday_name = VALUES(holiday_name);

INSERT INTO promotional_calendar (
    promo_calendar_id, promo_id, promo_name, promo_start_date, promo_end_date, discount_percentage, promo_type, applicable_products
) VALUES (
    'PC-001', 'PROMO-APPLE', 'Apple Fest', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 7 DAY), 10.00, 'SEASONAL', 'PROD-APPLE-001'
)
ON DUPLICATE KEY UPDATE promo_name = VALUES(promo_name), discount_percentage = VALUES(discount_percentage);

INSERT INTO product_lifecycle_stages (
    lifecycle_id, product_id, current_stage, stage_start_date, previous_stage, transition_date
) VALUES (
    'LC-001', 'PROD-APPLE-001', 'GROWTH', CURDATE(), 'INTRODUCTION', CURDATE()
)
ON DUPLICATE KEY UPDATE current_stage = VALUES(current_stage);

INSERT INTO forecast_performance_metrics (
    eval_id, forecast_id, forecast_date, predicted_qty, actual_qty, mape, rmse, model_used
) VALUES (
    'FPM-001', 'DF-001', CURDATE(), 850, 820, 3.66, 12.4200, 'ARIMA'
)
ON DUPLICATE KEY UPDATE actual_qty = VALUES(actual_qty), mape = VALUES(mape), rmse = VALUES(rmse);

INSERT INTO barcode_rfid_events (
    event_id, product_id, warehouse_id, event_timestamp, description, status, source
) VALUES (
    'BAR-001', 'PROD-APPLE-001', 'WH-001', NOW(), '{"tag":"RFID-7788"}', 'SCAN_IN', 'RFID_GATE_A1'
)
ON DUPLICATE KEY UPDATE description = VALUES(description), event_timestamp = VALUES(event_timestamp);

INSERT INTO packaging_jobs (
    package_id, order_id, quantity, total_amount, discounts, packaging_status, packed_by, created_at
) VALUES (
    'PKG-001', 'ORD-1001', 3, 420.00, 0.00, 'PACKED', 'EMP-01', NOW()
)
ON DUPLICATE KEY UPDATE packaging_status = VALUES(packaging_status);

INSERT INTO receipt_records (
    receipt_record_id, order_id, package_id, received_amount, receipt_status, recorded_at
) VALUES (
    'REC-001', 'ORD-1001', 'PKG-001', 420.00, 'RECEIVED', NOW()
)
ON DUPLICATE KEY UPDATE receipt_status = VALUES(receipt_status);

INSERT INTO repair_requests (
    request_id, order_id, product_id, defect_details, request_status, requested_at
) VALUES (
    'REP-001', 'ORD-1001', 'PROD-APPLE-001', 'Minor packaging tear observed', 'OPEN', NOW()
)
ON DUPLICATE KEY UPDATE request_status = VALUES(request_status);

INSERT INTO product_returns (
    return_request_id, order_id, customer_id, product_details, defect_details, customer_feedback, transport_details,
    warranty_valid_until, return_status, created_at
) VALUES (
    'RET-001', 'ORD-1001', 'CUST-77', 'Fresh apples - 3 units', 'Bruising on delivery', 'Customer requested replacement',
    'Reverse pickup requested', DATE_ADD(NOW(), INTERVAL 30 DAY), 'INITIATED', NOW()
)
ON DUPLICATE KEY UPDATE return_status = VALUES(return_status), customer_feedback = VALUES(customer_feedback);

INSERT INTO return_growth_statistics (
    growth_stat_id, return_request_id, metric_period, return_rate, resolution_rate, recorded_at
) VALUES (
    'RGS-001', 'RET-001', '2026-Q2', 4.50, 92.00, NOW()
)
ON DUPLICATE KEY UPDATE return_rate = VALUES(return_rate), resolution_rate = VALUES(resolution_rate);

INSERT INTO shipments (
    shipment_id, order_id, origin_address, destination_address, package_weight, is_drop_ship, shipping_priority,
    shipment_status, supplier_id, inventory_level, route_id, carrier_id, tracking_id, min_cost_constraint,
    min_time_constraint, avoid_tolls_constraint, calculated_cost, created_at
) VALUES (
    'SHP-001', 'ORD-1001', 'Central Warehouse, Bengaluru', 'Bengaluru, Karnataka', 12.50, FALSE, 'HIGH',
    'IN_TRANSIT', 'SUP-01', 150, 'ROUTE-001', 'CARRIER-01', 'TRK-001', TRUE,
    FALSE, TRUE, 280.00, NOW()
)
ON DUPLICATE KEY UPDATE shipment_status = VALUES(shipment_status), calculated_cost = VALUES(calculated_cost);

INSERT INTO logistics_routes (
    route_id, shipment_id, current_eta, timeline_stage, route_status, requires_rerouting
) VALUES (
    'ROUTE-001', 'SHP-001', DATE_ADD(NOW(), INTERVAL 6 HOUR), 'EN_ROUTE', 'ACTIVE', FALSE
)
ON DUPLICATE KEY UPDATE route_status = VALUES(route_status), current_eta = VALUES(current_eta);

INSERT INTO shipment_alerts (
    alert_id, shipment_id, alert_message, alert_severity, created_at
) VALUES (
    'ALERT-001', 'SHP-001', 'Traffic congestion detected on planned route', 'MEDIUM', NOW()
)
ON DUPLICATE KEY UPDATE alert_message = VALUES(alert_message);

INSERT INTO delivery_tracking_routes (
    route_plan_id, delivery_id, carrier_id, tracking_api_url, planned_departure, planned_arrival, current_eta, route_status
) VALUES (
    'DR-001', 'SHIP-001', 'CARRIER-01', 'https://tracking.example/api/SHIP-001', NOW(), DATE_ADD(NOW(), INTERVAL 8 HOUR),
    DATE_ADD(NOW(), INTERVAL 9 HOUR), 'IN_PROGRESS'
)
ON DUPLICATE KEY UPDATE current_eta = VALUES(current_eta), route_status = VALUES(route_status);

INSERT INTO delivery_tracking_waypoints (
    waypoint_id, route_plan_id, waypoint_sequence, waypoint_location
) VALUES (
    'WP-001', 'DR-001', 1, 'Warehouse Exit Gate'
)
ON DUPLICATE KEY UPDATE waypoint_location = VALUES(waypoint_location);

INSERT INTO delivery_tracking_events (
    tracking_event_id, delivery_id, rider_id, vehicle_id, timeline_stage, gps_coordinates, event_timestamp, alert_message, requires_rerouting
) VALUES (
    'DTE-001', 'SHIP-001', 'RIDER-01', 'VEH-01', 'DISPATCHED', '12.9716,77.5946', NOW(), NULL, FALSE
)
ON DUPLICATE KEY UPDATE timeline_stage = VALUES(timeline_stage), gps_coordinates = VALUES(gps_coordinates);

INSERT INTO SCM_EXCEPTION_LOG (
    exception_id, exception_name, severity, subsystem, error_message, logged_at
) VALUES (
    1001, 'ShipmentDelay', 'HIGH', 'DeliveryTracking', 'Shipment delayed due to route reassignment', NOW(3)
)
ON DUPLICATE KEY UPDATE error_message = VALUES(error_message), logged_at = VALUES(logged_at);
