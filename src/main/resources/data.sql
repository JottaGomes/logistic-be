INSERT IGNORE INTO users (username, password) VALUES
    ('adriano', '$2a$10$o8eEJ.i2U1LzX0Ys0pjroeIqRl/sRKewlQvkZnzLV.MdCQQUoWAPa'),
    ('dachser', '$2a$10$NGEzy3scgS1L5nfq.x3o8.H5ChneJvtysDoJbMI0nzWMQ9KMkRMs.'),
    ('joao', '$2y$10$FtIy9WdS32NSv.hL085tCunGuj421rNWNGhlGFGEZEia3hPp.jf4m');

INSERT INTO shipment_calculation (income, cost, additional_cost, total_costs, profit_or_loss)
SELECT * FROM (
    SELECT 5000.00, 3000.00, 200.00, 3200.00, 1800.00
    UNION ALL
    SELECT 2500.00, 2000.00, 0.00, 2000.00, 500.00
    UNION ALL
    SELECT 1800.00, 2200.00, 150.00, 2350.00, -550.00
    UNION ALL
    SELECT 3000.00, 4000.00, 500.00, 4500.00, -1500.00
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM shipment_calculation);
