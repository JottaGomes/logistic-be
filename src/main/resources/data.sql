-- Seed data: four shipments, each with its own income and cost records.
-- Guarded so the script stays idempotent when spring.sql.init runs on every start.

INSERT INTO shipment (id, reference, customer)
SELECT * FROM (
    SELECT 1, 'SHP-2026-0001', 'Sonae Distribuicao'
    UNION ALL SELECT 2, 'SHP-2026-0002', 'Continental Mabor'
    UNION ALL SELECT 3, 'SHP-2026-0003', 'Bosch Termotecnologia'
    UNION ALL SELECT 4, 'SHP-2026-0004', 'Renault Cacia'
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM shipment);

INSERT INTO income (shipment_id, income_type, amount, description)
SELECT * FROM (
    SELECT 1, 'CUSTOMER_PAYMENT', 4200.00, 'Main carriage invoice'
    UNION ALL SELECT 1, 'CUSTOMER_PAYMENT',  800.00, 'Fuel surcharge'
    UNION ALL SELECT 1, 'AGENT_INCOME',      300.00, 'Destination agent share'
    UNION ALL SELECT 2, 'CUSTOMER_PAYMENT', 2500.00, 'Main carriage invoice'
    UNION ALL SELECT 2, 'AGENT_INCOME',      150.00, 'Destination agent share'
    UNION ALL SELECT 3, 'CUSTOMER_PAYMENT', 1800.00, 'Main carriage invoice'
    UNION ALL SELECT 4, 'CUSTOMER_PAYMENT', 3000.00, 'Main carriage invoice'
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM income);

INSERT INTO cost (shipment_id, cost_type, amount, description)
SELECT * FROM (
    SELECT 1, 'MAIN_CARRIAGE', 3000.00, 'Line haul'
    UNION ALL SELECT 1, 'HANDLING',      200.00, 'Terminal handling'
    UNION ALL SELECT 2, 'MAIN_CARRIAGE', 2000.00, 'Line haul'
    UNION ALL SELECT 3, 'MAIN_CARRIAGE', 2200.00, 'Line haul'
    UNION ALL SELECT 3, 'CUSTOMS',       150.00, 'Customs clearance'
    UNION ALL SELECT 4, 'MAIN_CARRIAGE', 4000.00, 'Line haul'
    UNION ALL SELECT 4, 'HANDLING',      500.00, 'Terminal handling'
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM cost);

-- shipment 1 profits (5300 - 3200), shipment 4 loses (3000 - 4500)
INSERT INTO profit_calculation (shipment_id, total_income, total_costs, profit_or_loss)
SELECT * FROM (
    SELECT 1, 5300.00, 3200.00,  2100.00
    UNION ALL SELECT 2, 2650.00, 2000.00,   650.00
    UNION ALL SELECT 3, 1800.00, 2350.00,  -550.00
    UNION ALL SELECT 4, 3000.00, 4500.00, -1500.00
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM profit_calculation);

INSERT INTO users (username, password)
SELECT * FROM (
    SELECT 'adriano', '$2a$10$o8eEJ.i2U1LzX0Ys0pjroeIqRl/sRKewlQvkZnzLV.MdCQQUoWAPa'
    UNION ALL SELECT 'dachser', '$2a$10$NGEzy3scgS1L5nfq.x3o8.H5ChneJvtysDoJbMI0nzWMQ9KMkRMs.'
    UNION ALL SELECT 'joao', '$2y$10$FtIy9WdS32NSv.hL085tCunGuj421rNWNGhlGFGEZEia3hPp.jf4m'
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM users);
