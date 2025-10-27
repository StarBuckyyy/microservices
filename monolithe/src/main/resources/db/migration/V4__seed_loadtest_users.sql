DO $$
DECLARE
    v_user_id UUID;
    v_account_id UUID;
    v_wallet_id UUID;
    v_transaction_id UUID;
    v_batch_size INTEGER := 200;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM users WHERE email LIKE 'loadtest%@brokerx.com') THEN
        RAISE NOTICE '🚀 Creating % load test users...', v_batch_size;
        
        FOR i IN 1..v_batch_size LOOP
            v_user_id := gen_random_uuid();
            v_account_id := gen_random_uuid();
            v_wallet_id := gen_random_uuid();
            v_transaction_id := gen_random_uuid();
            
            INSERT INTO users (
                user_id,
                email,
                phone,
                hashed_password,
                first_name,
                last_name,
                date_of_birth,
                address,
                created_at,
                updated_at
            ) VALUES (
                v_user_id,
                'loadtest' || LPAD(i::text, 3, '0') || '@brokerx.com', 
                '5550' || LPAD(i::text, 6, '0'),
                '$2a$10$VrXeIq53mUmG86qKck0Rf.F8B.CR5u8cqeJqSJMz6KHtx.seqlovW',
                'LoadTest',
                'User' || LPAD(i::text, 3, '0'),
                '1990-01-01',
                'Load Test Address ' || i,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            );
            
            INSERT INTO accounts (
                account_id,
                user_id,
                status,
                verified_at,
                created_at,
                updated_at
            ) VALUES (
                v_account_id,
                v_user_id,
                'ACTIVE',
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            );
            
            INSERT INTO wallets (
                wallet_id,
                account_id,
                balance,
                currency,
                created_at,
                updated_at
            ) VALUES (
                v_wallet_id,
                v_account_id,
                10000.00,
                'USD',
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            );
            
            INSERT INTO transactions (
                transaction_id,
                wallet_id,
                idempotency_key,
                amount,
                transaction_type,
                status,
                settled_at
            ) VALUES (
                v_transaction_id,
                v_wallet_id,
                'INITIAL_DEPOSIT_LOADTEST_' || LPAD(i::text, 3, '0'),
                10000.00,
                'DEPOSIT',
                'SETTLED',
                CURRENT_TIMESTAMP
            );
            
            IF i % 50 = 0 THEN
                INSERT INTO audit_log (entity_type, entity_id, action, performed_by, ip_address, new_values)
                VALUES (
                    'USER',
                    v_user_id,
                    'CREATE',
                    NULL,
                    '127.0.0.1',
                    '{"email": "loadtest' || LPAD(i::text, 3, '0') || '@brokerx.com", "batch": "load_testing", "users_created": ' || i || '}'
                );
                RAISE NOTICE '  ✓ Created % users...', i;
            END IF;
        END LOOP;
        
        RAISE NOTICE ' % load test users created successfully', v_batch_size;
        RAISE NOTICE '   - Status: ACTIVE';
        RAISE NOTICE '   - Balance: 10,000 USD each';
        RAISE NOTICE '   - Total deposited: $%', (v_batch_size * 10000);
    ELSE
        RAISE NOTICE '⚠️  Load test users already exist, skipping creation';
    END IF;
END;
$$;

-- Verification summary block
DO $$
DECLARE
    v_count INTEGER;
    v_total_balance DECIMAL(15,2);
BEGIN
    SELECT
        COUNT(*),
        SUM(w.balance)
    INTO v_count, v_total_balance
    FROM users u
    INNER JOIN accounts a ON u.user_id = a.user_id
    INNER JOIN wallets w ON a.account_id = w.account_id
    WHERE u.email LIKE 'loadtest%@brokerx.com'
      AND a.status = 'ACTIVE';
    
    RAISE NOTICE '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━';
    RAISE NOTICE '📊 VERIFICATION SUMMARY';
    RAISE NOTICE '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━';
    RAISE NOTICE '   Active load test users: %', v_count;
    RAISE NOTICE '   Total balance: $%', v_total_balance;
    RAISE NOTICE '   Ready for k6 testing ✓';
    RAISE NOTICE '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━';
END;
$$;
