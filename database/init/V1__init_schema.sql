-- Users table
CREATE TABLE IF NOT EXISTS users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL UNIQUE,
    hashed_password TEXT NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    date_of_birth DATE,
    address TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Accounts table
CREATE TABLE IF NOT EXISTS accounts (
    account_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED')),
    verified_at TIMESTAMP,
    verification_token VARCHAR(255),
    token_expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Wallets table
CREATE TABLE IF NOT EXISTS wallets (
    wallet_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL UNIQUE REFERENCES accounts(account_id) ON DELETE CASCADE,
    balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD' CHECK (currency = 'USD'),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Transactions table
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID NOT NULL REFERENCES wallets(wallet_id) ON DELETE CASCADE,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    amount NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    transaction_type VARCHAR(20) NOT NULL CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SETTLED', 'FAILED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    settled_at TIMESTAMP,
    failure_reason TEXT
);

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(account_id) ON DELETE CASCADE,
    client_order_id VARCHAR(50) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    side VARCHAR(10) NOT NULL CHECK (side IN ('BUY', 'SELL')),
    order_type VARCHAR(20) NOT NULL CHECK (order_type IN ('MARKET', 'LIMIT')),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    price NUMERIC(15, 4),
    time_in_force VARCHAR(10) NOT NULL CHECK (time_in_force IN ('DAY', 'IOC', 'FOK')),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW', 'WORKING', 'FILLED', 'CANCELLED', 'REJECTED')),
    filled_quantity INTEGER NOT NULL DEFAULT 0 CHECK (filled_quantity >= 0),
    remaining_quantity INTEGER NOT NULL CHECK (remaining_quantity >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    UNIQUE (account_id, client_order_id),
    CHECK (filled_quantity + remaining_quantity = quantity)
);

-- Audit logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    performed_by UUID,
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    old_values TEXT,
    new_values TEXT
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_wallets_account_id ON wallets(account_id);
CREATE INDEX IF NOT EXISTS idx_transactions_wallet_id ON transactions(wallet_id);
CREATE INDEX IF NOT EXISTS idx_orders_account_id ON orders(account_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_performed_at ON audit_logs(performed_at);



INSERT INTO users (user_id, email, phone, hashed_password, first_name, last_name, date_of_birth, address)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'alice.trader@brokerx.com',
    '5551234567',
    '$2a$10$VrXeIq53mUmG86qKck0Rf.F8B.CR5u8cqeJqSJMz6KHtx.seqlovW',
    'Alice',
    'Trader',
    '1990-05-15',
    '123 Wall Street, New York, NY 10005'
);

INSERT INTO accounts (account_id, user_id, status, verified_at)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'ACTIVE',
    CURRENT_TIMESTAMP
);

INSERT INTO wallets (wallet_id, account_id, balance, currency)
VALUES (
    '33333333-3333-3333-3333-333333333333',
    '22222222-2222-2222-2222-222222222222',
    10000.00,
    'USD'
);

INSERT INTO transactions (transaction_id, wallet_id, idempotency_key, amount, transaction_type, status, settled_at)
VALUES (
    '44444444-4444-4444-4444-444444444444',
    '33333333-3333-3333-3333-333333333333',
    'INITIAL_DEPOSIT_ALICE_001',
    10000.00,
    'DEPOSIT',
    'SETTLED',
    CURRENT_TIMESTAMP
);

INSERT INTO users (user_id, email, phone, hashed_password, first_name, last_name, date_of_birth, address)
VALUES (
    '55555555-5555-5555-5555-555555555555',
    'bob.investor@brokerx.com',
    '5559876543',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Bob',
    'Investor',
    '1985-08-22',
    '456 Trading Plaza, Chicago, IL 60601'
);

INSERT INTO accounts (account_id, user_id, status, verification_token, token_expires_at)
VALUES (
    '66666666-6666-6666-6666-666666666666',
    '55555555-5555-5555-5555-555555555555',
    'PENDING',
    '123456',
    CURRENT_TIMESTAMP + INTERVAL '5 minutes');

INSERT INTO orders (
    order_id, account_id, client_order_id, symbol, side, order_type,
    quantity, price, time_in_force, status, filled_quantity, remaining_quantity
)
VALUES
    (
        '77777777-7777-7777-7777-777777777777',
        '22222222-2222-2222-2222-222222222222',
        'ALICE_ORDER_001',
        'AAPL',
        'BUY',
        'LIMIT',
        10,
        150.00,
        'DAY',
        'WORKING',
        0,
        10
    ),
    (
        '88888888-8888-8888-8888-888888888888',
        '22222222-2222-2222-2222-222222222222',
        'ALICE_ORDER_002',
        'GOOGL',
        'BUY',
        'LIMIT',
        20,
        2800.00,
        'DAY',
        'WORKING',
        5,
        15
    ),
    (
        '99999999-9999-9999-9999-999999999999',
        '22222222-2222-2222-2222-222222222222',
        'ALICE_ORDER_003',
        'MSFT',
        'BUY',
        'MARKET',
        15,
        NULL,
        'IOC',
        'FILLED',
        15,
        0
    );

INSERT INTO audit_logs (entity_type, entity_id, action, performed_by, ip_address, new_values)
VALUES
    ('USER', '11111111-1111-1111-1111-111111111111', 'CREATE', NULL, '127.0.0.1', 
     '{"email": "alice.trader@brokerx.com", "firstName": "Alice", "lastName": "Trader"}'),
    ('ACCOUNT', '22222222-2222-2222-2222-222222222222', 'CREATE', '11111111-1111-1111-1111-111111111111', '127.0.0.1',
     '{"status": "ACTIVE"}'),
    ('WALLET', '33333333-3333-3333-3333-333333333333', 'CREATE', '11111111-1111-1111-1111-111111111111', '127.0.0.1',
     '{"balance": 10000.00, "currency": "USD"}'),
    ('ORDER', '77777777-7777-7777-7777-777777777777', 'CREATE', '11111111-1111-1111-1111-111111111111', '127.0.0.1',
     '{"symbol": "AAPL", "side": "BUY", "quantity": 10, "price": 150.00, "status": "WORKING"}');