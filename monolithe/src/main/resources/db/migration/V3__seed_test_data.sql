-- Utilisateur Alice (compte actif)
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

-- Utilisateur Bob (compte pending)
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

-- Ordres de test pour Alice
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

-- Logs d'audit initiaux
INSERT INTO audit_log (entity_type, entity_id, action, performed_by, ip_address, new_values)
VALUES
    ('USER', '11111111-1111-1111-1111-111111111111', 'CREATE', NULL, '127.0.0.1', 
     '{"email": "alice.trader@brokerx.com", "firstName": "Alice", "lastName": "Trader"}'),
    ('ACCOUNT', '22222222-2222-2222-2222-222222222222', 'CREATE', '11111111-1111-1111-1111-111111111111', '127.0.0.1',
     '{"status": "ACTIVE"}'),
    ('WALLET', '33333333-3333-3333-3333-333333333333', 'CREATE', '11111111-1111-1111-1111-111111111111', '127.0.0.1',
     '{"balance": 10000.00, "currency": "USD"}'),
    ('ORDER', '77777777-7777-7777-7777-777777777777', 'CREATE', '11111111-1111-1111-1111-111111111111', '127.0.0.1',
     '{"symbol": "AAPL", "side": "BUY", "quantity": 10, "price": 150.00, "status": "WORKING"}');