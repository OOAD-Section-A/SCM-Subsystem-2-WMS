-- Subsystem access requirements for the pricing-related additions.
-- Apply this manually using a privileged MySQL account after schema creation.
-- The application bootstrap does not execute GRANT statements.
--
-- Note: the user request referenced `customer_tier_cash`; this project
-- interprets that as `customer_tier_cache`.

CREATE ROLE IF NOT EXISTS `pricing_discount_rw`;
CREATE ROLE IF NOT EXISTS `packing_repairs_receipt_ro`;
CREATE ROLE IF NOT EXISTS `reporting_dashboard_ro`;

GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`promotion_eligible_skus` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`bundle_promotions` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`bundle_promotion_skus` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`rebate_programs` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`volume_discount_schedules` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`volume_tier_rules` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`customer_tier_cache` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`customer_tier_overrides` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`regional_pricing_multipliers` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`contracts` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`contract_sku_prices` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`approval_requests` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`audit_log` TO `pricing_discount_rw`;
GRANT SELECT, INSERT, UPDATE, DELETE ON `OOAD`.`profitability_analytics` TO `pricing_discount_rw`;

GRANT SELECT ON `OOAD`.`contract_sku_prices` TO `packing_repairs_receipt_ro`;
GRANT SELECT ON `OOAD`.`discount_policies` TO `packing_repairs_receipt_ro`;
GRANT SELECT ON `OOAD`.`bundle_promotions` TO `packing_repairs_receipt_ro`;
GRANT SELECT ON `OOAD`.`promotions` TO `packing_repairs_receipt_ro`;
GRANT SELECT ON `OOAD`.`promotion_eligible_skus` TO `packing_repairs_receipt_ro`;

GRANT SELECT ON `OOAD`.`customer_tier_cache` TO `reporting_dashboard_ro`;
