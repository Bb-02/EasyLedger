CREATE TABLE IF NOT EXISTS ledger_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  type VARCHAR(20) NOT NULL,
  sort INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ledger_transaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type VARCHAR(20) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  category_id BIGINT NOT NULL,
  txn_date DATE NOT NULL,
  note VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_txn_date (txn_date),
  INDEX idx_category_id (category_id),
  INDEX idx_type_txn_date (type, txn_date)
);

INSERT INTO ledger_category (name, type, sort, enabled)
SELECT '工资', 'INCOME', 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ledger_category WHERE name = '工资' AND type = 'INCOME');

INSERT INTO ledger_category (name, type, sort, enabled)
SELECT '兼职', 'INCOME', 2, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ledger_category WHERE name = '兼职' AND type = 'INCOME');

INSERT INTO ledger_category (name, type, sort, enabled)
SELECT '餐饮', 'EXPENSE', 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ledger_category WHERE name = '餐饮' AND type = 'EXPENSE');

INSERT INTO ledger_category (name, type, sort, enabled)
SELECT '交通', 'EXPENSE', 2, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ledger_category WHERE name = '交通' AND type = 'EXPENSE');

INSERT INTO ledger_category (name, type, sort, enabled)
SELECT '购物', 'EXPENSE', 3, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ledger_category WHERE name = '购物' AND type = 'EXPENSE');

