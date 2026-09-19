-- Relational model for the Calculate Profit use case.
--
-- Normalised to 3NF: a shipment owns many income records and many cost records,
-- each atomic and each in its own table, so totals are derived by summing rows
-- rather than stored on the shipment. The calculated result is kept separately,
-- because it is a point-in-time outcome rather than an attribute of the shipment.

CREATE TABLE IF NOT EXISTS shipment (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    reference   VARCHAR(30)  NOT NULL UNIQUE,
    customer    VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS income (
    id           BIGINT         AUTO_INCREMENT PRIMARY KEY,
    shipment_id  BIGINT         NOT NULL,
    income_type  VARCHAR(20)    NOT NULL,
    amount       DECIMAL(12, 2) NOT NULL,
    description  VARCHAR(255),
    CONSTRAINT fk_income_shipment FOREIGN KEY (shipment_id) REFERENCES shipment (id)
);

CREATE TABLE IF NOT EXISTS cost (
    id           BIGINT         AUTO_INCREMENT PRIMARY KEY,
    shipment_id  BIGINT         NOT NULL,
    cost_type    VARCHAR(20)    NOT NULL,
    amount       DECIMAL(12, 2) NOT NULL,
    description  VARCHAR(255),
    CONSTRAINT fk_cost_shipment FOREIGN KEY (shipment_id) REFERENCES shipment (id)
);

CREATE TABLE IF NOT EXISTS profit_calculation (
    id             BIGINT         AUTO_INCREMENT PRIMARY KEY,
    shipment_id    BIGINT         NOT NULL,
    total_income   DECIMAL(12, 2) NOT NULL,
    total_costs    DECIMAL(12, 2) NOT NULL,
    profit_or_loss DECIMAL(12, 2) NOT NULL,
    calculated_by  VARCHAR(50)    NOT NULL DEFAULT 'system',
    calculated_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_calculation_shipment FOREIGN KEY (shipment_id) REFERENCES shipment (id)
);

-- the three queries that matter all filter or join on shipment_id
CREATE INDEX IF NOT EXISTS idx_income_shipment ON income (shipment_id);
CREATE INDEX IF NOT EXISTS idx_cost_shipment ON cost (shipment_id);
CREATE INDEX IF NOT EXISTS idx_calculation_shipment ON profit_calculation (shipment_id);
-- the listing is ordered by most recent first
CREATE INDEX IF NOT EXISTS idx_calculation_calculated_at ON profit_calculation (calculated_at);

CREATE TABLE IF NOT EXISTS users (
    id        BIGINT       AUTO_INCREMENT PRIMARY KEY,
    username  VARCHAR(50)  NOT NULL UNIQUE,
    password  VARCHAR(255) NOT NULL
);
