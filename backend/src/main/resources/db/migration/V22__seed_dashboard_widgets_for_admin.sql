-- Seed Dashboard Widgets for the Administrator role
-- Gives them access to all KPIs, Alerts, and Activity feeds

INSERT INTO dashboard_configs (role_id, widget_code, is_enabled, sort_order, created_at, updated_at, created_by, updated_by)
SELECT r.id, w.code, TRUE, w.idx, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'system', 'system'
FROM roles r
CROSS JOIN (
    SELECT 'KPI_SALES' as code, 1 as idx UNION ALL
    SELECT 'KPI_PURCHASES', 2 UNION ALL
    SELECT 'KPI_FUNDS', 3 UNION ALL
    SELECT 'KPI_AGING', 4 UNION ALL
    SELECT 'ALERT_STOCK', 5 UNION ALL
    SELECT 'ACTIVITY_FEED', 6 UNION ALL
    SELECT 'SYSTEM_STATUS', 7 UNION ALL
    SELECT 'KPI_AGING_DETAIL', 8
) w
WHERE r.name = 'ROLE_ADMINISTRATOR';
