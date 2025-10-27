// stress-test.js (version rapide ~10-12 min)
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '1m', target: 10 },   // Rampe 1
    { duration: '1m', target: 10 },   // Stabilisation courte
    { duration: '1m', target: 25 },   // Rampe 2
    { duration: '1m', target: 25 },   
    { duration: '1m', target: 50 },   // Rampe 3
    { duration: '1m', target: 50 },   
    { duration: '1m', target: 100 },  // Rampe 4
    { duration: '1m', target: 100 },  
    { duration: '1m', target: 150 },  // Rampe 5
    { duration: '1m', target: 150 },  
    { duration: '30s', target: 0 },   // Descente rapide
  ],
  thresholds: {
    http_req_duration: ['p(95)<800', 'p(99)<1500'],
    http_req_failed: ['rate<0.05'],
  },
};

export default function () {
  const userNumber = String(__VU).padStart(3, '0');
  const email = `loadtest${userNumber}@brokerx.com`;
  const password = 'password123';

  const loginPayload = JSON.stringify({
    email: email,
    hashedPassword: password,
  });

  const loginHeaders = { 'Content-Type': 'application/json' };

  // ÉTAPE 1 : LOGIN
  const loginRes = http.post('http://localhost:8080/auth/login', loginPayload, { 
    headers: loginHeaders,
    tags: { name: 'login_step1' } 
  });

  const loginCheck = check(loginRes, {
    'Login step 1 successful (MFA required)': (r) => r.status === 200 && r.json('mfaRequired') === true,
  });

  if (!loginCheck) {
    return;
  }

  const tempToken = loginRes.json('tempToken');
  const otpMessage = loginRes.json('message');
  
  if (!otpMessage || !otpMessage.includes('Code OTP:')) {
    return;
  }
  
  const otpCode = otpMessage.split('Code OTP: ')[1];

  // ÉTAPE 2 : MFA
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
    return;
  }

  const authToken = mfaRes.json('token');

  // ÉTAPE 3 : GET /orders
  const authHeaders = { 'Authorization': `Bearer ${authToken}` };
  
  const ordersRes = http.get('http://localhost:8080/orders', { 
    headers: authHeaders,
    tags: { name: 'get_orders' }
  });

  check(ordersRes, {
    'GET /orders successful': (r) => r.status === 200,
  });

  sleep(1);
}