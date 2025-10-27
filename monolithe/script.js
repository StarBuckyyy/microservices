import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m', target: 10 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<800'],
    'http_req_failed': ['rate<0.01'],
  },
};

// 🔥 NOUVEAU : Pool d'utilisateurs de test
const TEST_USERS = [
  { email: 'loadtest1@brokerx.com', password: 'password123' },
  { email: 'loadtest2@brokerx.com', password: 'password123' },
  { email: 'loadtest3@brokerx.com', password: 'password123' },
  { email: 'loadtest4@brokerx.com', password: 'password123' },
  { email: 'loadtest5@brokerx.com', password: 'password123' },
  { email: 'loadtest6@brokerx.com', password: 'password123' },
  { email: 'loadtest7@brokerx.com', password: 'password123' },
  { email: 'loadtest8@brokerx.com', password: 'password123' },
  { email: 'loadtest9@brokerx.com', password: 'password123' },
  { email: 'loadtest10@brokerx.com', password: 'password123' },
];

export default function () {
  // 🔥 Chaque VU utilise un utilisateur différent
  const userIndex = (__VU - 1) % TEST_USERS.length;
  const user = TEST_USERS[userIndex];

  const loginPayload = JSON.stringify({
    email: user.email,
    hashedPassword: user.password,
  });

  const loginHeaders = { 'Content-Type': 'application/json' };

  // Étape 1 : Login initial
  const loginRes = http.post('http://localhost:8080/auth/login', loginPayload, { 
    headers: loginHeaders,
    tags: { name: 'login_step1' } 
  });

  const loginCheck = check(loginRes, {
    'Login step 1 successful (MFA required)': (r) => r.status === 200 && r.json('mfaRequired') === true,
  });

  if (!loginCheck) {
    console.error(`❌ Login failed for ${user.email}: ${loginRes.status} - ${loginRes.body}`);
    return; // Arrêter l'itération en cas d'échec
  }

  const tempToken = loginRes.json('tempToken');
  const otpMessage = loginRes.json('message');
  
  if (!otpMessage || !otpMessage.includes('Code OTP:')) {
    console.error(`❌ No OTP in response for ${user.email}`);
    return;
  }
  
  const otpCode = otpMessage.split('Code OTP: ')[1];

  // Étape 2 : Vérification MFA
  const mfaPayload = JSON.stringify({
    tempToken: tempToken,
    otpCode: otpCode,
  });

  const mfaRes = http.post('http://localhost:8080/auth/verify-mfa', mfaPayload, { 
    headers: loginHeaders,
    tags: { name: 'login_step2_mfa' }
  });

  const mfaCheck = check(mfaRes, {
    'Login step 2 successful (JWT received)': (r) => r.status === 200 && r.json('token') !== '',
  });

  if (!mfaCheck) {
    console.error(`❌ MFA failed for ${user.email}: ${mfaRes.status} - ${mfaRes.body}`);
    return;
  }

  const authToken = mfaRes.json('token');

  // Étape 3 : Requête authentifiée
  const authHeaders = { 'Authorization': `Bearer ${authToken}` };
  
  const ordersRes = http.get('http://localhost/orders', { 
    headers: authHeaders,
    tags: { name: 'get_orders' }
  });

  check(ordersRes, {
    'GET /orders successful': (r) => r.status === 200,
  });

  sleep(1);
}