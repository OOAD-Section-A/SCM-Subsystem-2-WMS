-- Migration for already-existing OOAD databases.
--
-- Purpose:
-- 1. Bring older databases up to the current canonical runtime schema.
-- 2. Preserve existing data where possible.
-- 3. Avoid destructive resets for teams already integrating locally.
--
-- Usage:
--   mysql -u <user> -p OOAD < src/main/resources/sql/migration-to-canonical-schema.sql
--
-- Notes:
-- - This migration intentionally avoids dropping legacy tables/columns.
-- - Runtime source of truth is the root-level schema.sql packaged in the JAR.
-- - Access grants remain separate in sql/access-requirements.sql.

SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE promotions
    ADD COLUMN IF NOT EXISTS expired BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE discount_policies
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS promotion_eligible_skus (
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    promo_id            VARCHAR(50)    NOT NULL,
    sku_id              VARCHAR(100)   NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_promotion_eligible_sku (promo_id, sku_id),
    FOREIGN KEY (promo_id) REFERENCES promotions(promo_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bundle_promotions (
    promo_id            VARCHAR(50)    NOT NULL,
    promo_name          VARCHAR(200)   NOT NULL,
    discount_pct        DECIMAL(5,4)   NOT NULL,
    start_date          DATE           NOT NULL,
    end_date            DATE           NOT NULL,
    expired             BOOLEAN        NOT NULL DEFAULT FALSE,

    PRIMARY KEY (promo_id),
    CONSTRAINT chk_bundle_promo_discount_pct CHECK (discount_pct BETWEEN 0 AND 1),
    CONSTRAINT chk_bundle_promo_date_range CHECK (end_date >= start_date)
);

CREATE TABLE IF NOT EXISTS bundle_promotion_skus (
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    promo_id            VARCHAR(50)    NOT NULL,
    sku_id              VARCHAR(100)   NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_bundle_promo_sku (promo_id, sku_id),
    FOREIGN KEY (promo_id) REFERENCES bundle_promotions(promo_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS rebate_programs (
    program_id           VARCHAR(100)   NOT NULL,
    customer_id          VARCHAR(100)   NOT NULL,
    sku_id               VARCHAR(100)   NOT NULL,
    target_spend         DECIMAL(19,4)  NOT NULL,
    accumulated_spend    DECIMAL(19,4)  NOT NULL DEFAULT 0.0000,
    rebate_pct           DECIMAL(5,4)   NOT NULL,

    PRIMARY KEY (program_id),
    UNIQUE KEY uq_rebate_program_customer_sku (customer_id, sku_id),
    CONSTRAINT chk_rebate_target_spend CHECK (target_spend >= 0),
    CONSTRAINT chk_rebate_accumulated_spend CHECK (accumulated_spend >= 0),
    CONSTRAINT chk_rebate_pct_range CHECK (rebate_pct BETWEEN 0 AND 1)
);

CREATE TABLE IF NOT EXISTS volume_discount_schedules (
    schedule_id          VARCHAR(50)    NOT NULL,
    sku_id               VARCHAR(100)   NOT NULL,

    PRIMARY KEY (schedule_id),
    UNIQUE KEY uq_volume_schedule_sku (sku_id)
);

CREATE TABLE IF NOT EXISTS volume_tier_rules (
    id                   BIGINT         NOT NULL AUTO_INCREMENT,
    schedule_id          VARCHAR(50)    NOT NULL,
    min_qty              INT            NOT NULL,
    max_qty              INT            NOT NULL,
    discount_pct         DECIMAL(5,4)   NOT NULL,

    PRIMARY KEY (id),
    FOREIGN KEY (schedule_id) REFERENCES volume_discount_schedules(schedule_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_volume_tier_min_qty CHECK (min_qty > 0),
    CONSTRAINT chk_volume_tier_max_qty CHECK (max_qty >= min_qty),
    CONSTRAINT chk_volume_tier_discount_pct CHECK (discount_pct BETWEEN 0 AND 1)
);

CREATE TABLE IF NOT EXISTS customer_tier_cache (
    customer_id          VARCHAR(100)   NOT NULL,
    tier                 VARCHAR(20)    NOT NULL,
    evaluated_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (customer_id)
);

CREATE TABLE IF NOT EXISTS customer_tier_overrides (
    customer_id          VARCHAR(100)   NOT NULL,
    override_tier        VARCHAR(20)    NOT NULL,
    override_set_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (customer_id)
);

CREATE TABLE IF NOT EXISTS regional_pricing_multipliers (
    region_code          VARCHAR(20)    NOT NULL,
    multiplier           DECIMAL(6,4)   NOT NULL,

    PRIMARY KEY (region_code),
    CONSTRAINT chk_regional_multiplier_positive CHECK (multiplier > 0)
);

CREATE TABLE IF NOT EXISTS contracts (
    contract_id          VARCHAR(50)    NOT NULL,
    customer_id          VARCHAR(100)   NOT NULL,
    status               VARCHAR(20)    NOT NULL,
    start_date           DATE           NOT NULL,
    end_date             DATE           NOT NULL,

    PRIMARY KEY (contract_id),
    CONSTRAINT chk_contracts_date_range CHECK (end_date >= start_date)
);

CREATE TABLE IF NOT EXISTS contract_sku_prices (
    id                   BIGINT         NOT NULL AUTO_INCREMENT,
    contract_id          VARCHAR(50)    NOT NULL,
    sku_id               VARCHAR(100)   NOT NULL,
    negotiated_price     DECIMAL(19,4)  NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_contract_sku_price (contract_id, sku_id),
    FOREIGN KEY (contract_id) REFERENCES contracts(contract_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_contract_sku_negotiated_price CHECK (negotiated_price >= 0)
);

CREATE TABLE IF NOT EXISTS approval_requests (
    approval_id            VARCHAR(36)    NOT NULL,
    request_type           VARCHAR(50)    NOT NULL,
    order_id               VARCHAR(100)   NOT NULL,
    requested_discount_amt DECIMAL(19,4)  NOT NULL,
    status                 VARCHAR(20)    NOT NULL,
    submission_time        TIMESTAMP      NOT NULL,
    escalation_time        TIMESTAMP      NULL,
    approval_timestamp     TIMESTAMP      NULL,
    routed_to_approver_id  VARCHAR(100)   NULL,
    approving_manager_id   VARCHAR(100)   NULL,
    rejection_reason       TEXT           NULL,
    audit_log_flag         BOOLEAN        NOT NULL DEFAULT FALSE,

    PRIMARY KEY (approval_id),
    CONSTRAINT chk_approval_requests_discount CHECK (requested_discount_amt >= 0)
);

CREATE TABLE IF NOT EXISTS audit_log (
    id                   BIGINT         NOT NULL AUTO_INCREMENT,
    approval_id          VARCHAR(36)    NOT NULL,
    timestamp            TIMESTAMP      NOT NULL,
    event_type           VARCHAR(50)    NOT NULL,
    actor                VARCHAR(100)   NOT NULL,
    detail               TEXT           NULL,

    PRIMARY KEY (id),
    KEY idx_audit_log_approval_id (approval_id),
    FOREIGN KEY (approval_id) REFERENCES approval_requests(approval_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS profitability_analytics (
    id                   BIGINT         NOT NULL AUTO_INCREMENT,
    approval_id          VARCHAR(36)    NOT NULL,
    request_type         VARCHAR(50)    NOT NULL,
    discount_amount      DECIMAL(19,4)  NOT NULL,
    final_status         VARCHAR(20)    NOT NULL,
    recorded_at          TIMESTAMP      NOT NULL,

    PRIMARY KEY (id),
    KEY idx_profitability_approval_id (approval_id),
    FOREIGN KEY (approval_id) REFERENCES approval_requests(approval_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_profitability_discount_amount CHECK (discount_amount >= 0)
);

CREATE TABLE IF NOT EXISTS SCM_EXCEPTION_LOG (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exception_id INT NOT NULL,
    exception_name VARCHAR(100) NOT NULL,
    severity VARCHAR(10) NOT NULL,
    subsystem VARCHAR(100) NOT NULL,
    error_message TEXT NOT NULL,
    logged_at DATETIME(3) NOT NULL
);

CREATE TABLE IF NOT EXISTS forecast_timeseries (
    id VARCHAR(50) PRIMARY KEY,
    forecast_id VARCHAR(50) NOT NULL,
    time_index INT NOT NULL,
    forecast_value DECIMAL(10,2) NOT NULL,
    lower_bound DECIMAL(10,2),
    upper_bound DECIMAL(10,2),
    CONSTRAINT fk_forecast_timeseries_forecast
        FOREIGN KEY (forecast_id)
        REFERENCES demand_forecasts(forecast_id)
        ON DELETE CASCADE
);

ALTER TABLE barcode_rfid_events
    ADD COLUMN IF NOT EXISTS rfid_tag VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS product_name VARCHAR(150) NULL,
    ADD COLUMN IF NOT EXISTS category VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS description TEXT NULL,
    ADD COLUMN IF NOT EXISTS transaction_id VARCHAR(50) NULL,
    ADD COLUMN IF NOT EXISTS status VARCHAR(50) NULL,
    ADD COLUMN IF NOT EXISTS source VARCHAR(100) NULL;

SET @has_barcode_event_type := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'barcode_rfid_events'
      AND column_name = 'event_type'
);
SET @has_barcode_source_device := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'barcode_rfid_events'
      AND column_name = 'source_device'
);
SET @has_barcode_raw_payload := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'barcode_rfid_events'
      AND column_name = 'raw_payload'
);

SET @barcode_status_sql := IF(
    @has_barcode_event_type > 0,
    'UPDATE barcode_rfid_events SET status = COALESCE(status, event_type) WHERE status IS NULL',
    'SELECT 1'
);
PREPARE stmt FROM @barcode_status_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @barcode_source_sql := IF(
    @has_barcode_source_device > 0,
    'UPDATE barcode_rfid_events SET source = COALESCE(source, source_device) WHERE source IS NULL',
    'SELECT 1'
);
PREPARE stmt FROM @barcode_source_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @barcode_description_sql := IF(
    @has_barcode_raw_payload > 0,
    'UPDATE barcode_rfid_events SET description = COALESCE(description, raw_payload) WHERE description IS NULL',
    'SELECT 1'
);
PREPARE stmt FROM @barcode_description_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE barcode_rfid_events
SET status = COALESCE(status, 'UNKNOWN'),
    source = COALESCE(source, 'UNKNOWN')
WHERE status IS NULL OR source IS NULL;

SET @has_legacy_exception_table := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'subsystem_exceptions'
);

SET @migrate_exception_sql := IF(
    @has_legacy_exception_table > 0,
    'INSERT INTO SCM_EXCEPTION_LOG (exception_id, exception_name, severity, subsystem, error_message, logged_at)
     SELECT derived.exception_id, derived.exception_name, derived.severity, derived.subsystem, derived.error_message, derived.logged_at
     FROM (
         SELECT
             CASE
                 WHEN NULLIF(REGEXP_REPLACE(se.exception_id, ''[^0-9]'', ''''), '''') IS NOT NULL
                     THEN CAST(NULLIF(REGEXP_REPLACE(se.exception_id, ''[^0-9]'', ''''), '''') AS UNSIGNED)
                 ELSE CAST(CRC32(se.exception_id) AS UNSIGNED)
             END AS exception_id,
             COALESCE(se.exception_name, ''LegacyException'') AS exception_name,
             LEFT(se.severity, 10) AS severity,
             se.subsystem_name AS subsystem,
             se.exception_message AS error_message,
             se.timestamp_utc AS logged_at
         FROM subsystem_exceptions se
     ) AS derived
     WHERE NOT EXISTS (
         SELECT 1
         FROM SCM_EXCEPTION_LOG existing
         WHERE existing.exception_id = derived.exception_id
     )',
    'SELECT 1'
);
PREPARE stmt FROM @migrate_exception_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE OR REPLACE VIEW vw_reporting_dashboard AS
    SELECT
        o.order_id                          AS order_id,
        o.order_date                        AS order_date,
        do.delivery_date                    AS delivery_date,
        o.order_status                      AS order_status,
        NULL                                AS fulfillment_time,
        oi.ordered_quantity                 AS order_quantity,
        oi.product_id                       AS product_id,
        p.product_name                      AS product_name,
        sl.current_stock_qty                AS current_stock_level,
        sl.reorder_threshold                AS reorder_level,
        CASE
            WHEN sl.current_stock_qty IS NOT NULL AND sl.current_stock_qty <= 0 THEN TRUE
            ELSE FALSE
        END                                 AS stock_out_flag,
        NULL                                AS inventory_turnover_rate,
        p.supplier_id                       AS supplier_id,
        NULL                                AS supplier_name,
        NULL                                AS supplier_performance_score,
        NULL                                AS lead_time,
        NULL                                AS on_time_supply_rate,
        do.delivery_id                      AS shipment_id,
        do.created_at                       AS dispatch_date,
        do.delivery_status                  AS delivery_status,
        NULL                                AS transit_time,
        CASE
            WHEN do.delivery_status IN ('DELAYED', 'FAILED') THEN TRUE
            ELSE FALSE
        END                                 AS delay_flag,
        do.delivery_address                 AS delivery_location,
        do.warehouse_id                     AS warehouse_id,
        NULL                                AS storage_capacity,
        NULL                                AS utilization_rate,
        NULL                                AS inbound_quantity,
        NULL                                AS outbound_quantity,
        pl.base_price                       AS product_price,
        NULL                                AS discount_applied,
        df.predicted_demand                 AS sales_volume,
        cs.sale_amount                      AS revenue,
        df.predicted_demand                 AS demand_forecast,
        df.forecast_period                  AS forecast_period,
        df.suggested_order_qty              AS predicted_inventory_needs,
        CAST(se.exception_id AS CHAR)       AS exception_id,
        se.exception_name                   AS exception_type,
        se.severity                         AS severity_level,
        se.logged_at                        AS timestamp
    FROM orders o
    LEFT JOIN order_items oi
        ON o.order_id = oi.order_id
    LEFT JOIN delivery_orders do
        ON o.order_id = do.order_id
    LEFT JOIN stock_levels sl
        ON oi.product_id = sl.product_id
    LEFT JOIN products p
        ON oi.product_id = p.product_id
    LEFT JOIN price_list pl
        ON pl.sku_id = p.sku AND pl.status = 'ACTIVE'
    LEFT JOIN demand_forecasts df
        ON df.product_id = oi.product_id
    LEFT JOIN SCM_EXCEPTION_LOG se
        ON se.subsystem = 'REPORTING'
    LEFT JOIN commission_sales cs
        ON cs.sale_id = o.order_id;

SET FOREIGN_KEY_CHECKS = 1;
