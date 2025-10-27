// ============================================
// K6 Load Test - Monolithic Architecture
// ============================================
// Test de charge pour l'architecture monolithique
// Port: 8080 (Application monolithique directe)

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// ============================================
// CONFIGURATION DES MÉTRIQUES PERSONNALISÉES
// ============================================
const errorRate = new Rate('errors');
const registerSuccessRate = new Rate('register_success');
const loginSuccessRate = new Rate('login_success');
const orderSuccessRate = new Rate('order_success');
const depositSuccessRate = new Rate('deposit_success');

const registerDuration = new Trend('register_duration');
const loginDuration = new Trend('login_duration');
const orderDuration = new Trend('order_duration');
const depositDuration = new Trend('deposit_duration');

const totalRequests = new Counter('total_requests');

// ============================================
// CONFIGURATION DU TEST
// ============================================
export const options = {
  stages: [
    { duration: '1m', target: 10 },    // Montée douce
    { duration: '2m', target: 20 },    // Stabilisation
    { duration: '2m', target: 30 },    // Pic modéré (au lieu de 100)
    { duration: '1m', target: 0 }
  ],
  thresholds: {
    'http_req_duration': ['p(95)<2000', 'p(99)<5000'],  // 95% < 2s, 99% < 5s
    'http_req_failed': ['rate<0.1'],                     // Moins de 10% d'erreurs
    'errors': ['rate<0.15'],                             // Taux d'erreur global < 15%
    'register_success': ['rate>0.85'],                   // 85% de réussite inscription
    'login_success': ['rate>0.90'],                      // 90% de réussite login
    'order_success': ['rate>0.85'],                      // 85% de réussite ordres
    'deposit_success': ['rate>0.90'],                    // 90% de réussite dépôts
  },
};

// ============================================
// CONFIGURATION DE L'ENVIRONNEMENT
// ============================================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const THINK_TIME = 1; // Temps d'attente entre les actions (secondes)

// ============================================
// FONCTIONS UTILITAIRES
// ============================================

/**
 * Génère un email unique basé sur le timestamp et l'ID du VU
 */
function generateUniqueEmail() {
  const timestamp = Date.now();
  const vuId = __VU;
  const iterationId = __ITER;
  return `testuser_${timestamp}_${vuId}_${iterationId}@brokerx.com`;
}

/**
 * Génère un Client Order ID unique
 */
function generateOrderId() {
  const timestamp = Date.now();
  const random = Math.floor(Math.random() * 1000);
  return `ORD_${timestamp}_${random}`;
}

/**
 * Génère des données utilisateur aléatoires
 */
function generateUserData(email) {
  return {
    email: email,
    phone: `555${Math.floor(Math.random() * 10000000).toString().padStart(7, '0')}`,
    hashedPassword: 'TestPassword123!',
    firstName: `User${__VU}`,
    lastName: `Test${__ITER}`,
    dateOfBirth: '1990-01-01',
    address: `${Math.floor(Math.random() * 999)} Test St, Test City, TC 12345`
  };
}

/**
 * Génère des données d'ordre aléatoires (BUY uniquement pour éviter les erreurs)
 */
function generateOrderData() {
  const symbols = ['AAPL', 'GOOGL', 'MSFT', 'TSLA', 'AMZN'];
  const orderTypes = ['MARKET', 'LIMIT'];
  const timeInForce = ['DAY', 'IOC', 'FOK'];
  
  const orderType = orderTypes[Math.floor(Math.random() * orderTypes.length)];
  // Quantités réduites pour éviter les ordres trop chers
  const quantity = Math.floor(Math.random() * 3) + 1; // 1-5 actions
  
  const orderData = {
    clientOrderId: generateOrderId(),
    symbol: symbols[Math.floor(Math.random() * symbols.length)],
    side: 'BUY', // ✅ Toujours BUY pour les nouveaux comptes
    orderType: orderType,
    quantity: quantity,
    timeInForce: timeInForce[Math.floor(Math.random() * timeInForce.length)]
  };
  
  // Ajouter un prix raisonnable pour les ordres LIMIT
  if (orderType === 'LIMIT') {
    // Prix entre 100 et 300 USD par action (plus raisonnable)
    orderData.price = (Math.random() * 200 + 100).toFixed(2);
  }
  
  return orderData;
}

// ============================================
// SCÉNARIO PRINCIPAL
// ============================================

export default function () {
  const email = generateUniqueEmail();
  const userData = generateUserData(email);
  
  // ========================================
  // ÉTAPE 1: INSCRIPTION (Registration)
  // ========================================
  console.log(`[VU ${__VU}] Starting registration for: ${email}`);
  
  const registerStart = Date.now();
  const registerRes = http.post(
    `${BASE_URL}/auth/register`,
    JSON.stringify(userData),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'register' },
    }
  );
  registerDuration.add(Date.now() - registerStart);
  totalRequests.add(1);
  
  const registerSuccess = check(registerRes, {
    'register: status 200': (r) => r.status === 200,
    'register: has accountId': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.accountId !== undefined;
      } catch (e) {
        return false;
      }
    },
    'register: has OTP': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.otp !== undefined;
      } catch (e) {
        return false;
      }
    },
  });
  
  registerSuccessRate.add(registerSuccess);
  errorRate.add(!registerSuccess);
  
  if (!registerSuccess) {
    console.error(`[VU ${__VU}] Registration failed for ${email}`);
    console.error(`Response status: ${registerRes.status}`);
    console.error(`Response body: ${registerRes.body}`);
    return; // Arrêter ce VU si l'inscription échoue
  }
  
  const registerBody = JSON.parse(registerRes.body);
  const accountId = registerBody.accountId;
  const otp = registerBody.otp;
  
  console.log(`[VU ${__VU}] Registration successful. AccountId: ${accountId}, OTP: ${otp}`);
  
  sleep(THINK_TIME);
  
  // ========================================
  // ÉTAPE 2: VÉRIFICATION OTP
  // ========================================
  console.log(`[VU ${__VU}] Verifying OTP for accountId: ${accountId}`);
  
  const verifyRes = http.post(
    `${BASE_URL}/auth/verify-otp`,
    JSON.stringify({ accountId, otp }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'verify-otp' },
    }
  );
  totalRequests.add(1);
  
  const verifySuccess = check(verifyRes, {
    'verify-otp: status 200': (r) => r.status === 200,
    'verify-otp: success message': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.success === true;
      } catch (e) {
        return false;
      }
    },
  });
  
  if (!verifySuccess) {
    console.error(`[VU ${__VU}] OTP verification failed`);
    console.error(`Response: ${verifyRes.body}`);
    errorRate.add(1);
    return;
  }
  
  console.log(`[VU ${__VU}] OTP verification successful`);
  
  sleep(THINK_TIME);
  
  // ========================================
  // ÉTAPE 3: CONNEXION (Login)
  // ========================================
  console.log(`[VU ${__VU}] Logging in: ${email}`);
  
  const loginStart = Date.now();
  const loginRes = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({
      email: userData.email,
      hashedPassword: userData.hashedPassword,
    }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'login' },
    }
  );
  loginDuration.add(Date.now() - loginStart);
  totalRequests.add(1);
  
  const loginSuccess = check(loginRes, {
    'login: status 200': (r) => r.status === 200,
    'login: MFA required': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.mfaRequired === true && body.tempToken !== undefined;
      } catch (e) {
        return false;
      }
    },
  });
  
  loginSuccessRate.add(loginSuccess);
  errorRate.add(!loginSuccess);
  
  if (!loginSuccess) {
    console.error(`[VU ${__VU}] Login failed`);
    console.error(`Response: ${loginRes.body}`);
    return;
  }
  
  const loginBody = JSON.parse(loginRes.body);
  const tempToken = loginBody.tempToken;
  
  // Extraire l'OTP du message de réponse
  let mfaOtp = null;
  if (loginBody.message && loginBody.message.includes('Code OTP:')) {
    mfaOtp = loginBody.message.split('Code OTP:')[1].trim();
  }
  
  if (!mfaOtp) {
    console.error(`[VU ${__VU}] Could not extract MFA OTP from login response`);
    errorRate.add(1);
    return;
  }
  
  console.log(`[VU ${__VU}] Login successful. TempToken: ${tempToken}, MFA OTP: ${mfaOtp}`);
  
  sleep(THINK_TIME);
  
  // ========================================
  // ÉTAPE 4: VÉRIFICATION MFA
  // ========================================
  console.log(`[VU ${__VU}] Verifying MFA with OTP: ${mfaOtp}`);
  
  const mfaRes = http.post(
    `${BASE_URL}/auth/verify-mfa`,
    JSON.stringify({
      tempToken: tempToken,
      otpCode: mfaOtp,
    }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { name: 'verify-mfa' },
    }
  );
  totalRequests.add(1);
  
  const mfaSuccess = check(mfaRes, {
    'verify-mfa: status 200': (r) => r.status === 200,
    'verify-mfa: has token': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.token !== undefined && body.success === true;
      } catch (e) {
        return false;
      }
    },
  });
  
  if (!mfaSuccess) {
    console.error(`[VU ${__VU}] MFA verification failed`);
    console.error(`Response: ${mfaRes.body}`);
    errorRate.add(1);
    return;
  }
  
  const mfaBody = JSON.parse(mfaRes.body);
  const authToken = mfaBody.token;
  
  console.log(`[VU ${__VU}] MFA verification successful. Auth token obtained.`);
  
  sleep(THINK_TIME);
  
  // ========================================
  // ÉTAPE 5: RÉCUPÉRATION DU WALLET
  // ========================================
  console.log(`[VU ${__VU}] Fetching wallet information`);
  
  const walletRes = http.get(
    `${BASE_URL}/wallets/my-wallet`,
    {
      headers: { 
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json',
      },
      tags: { name: 'get-wallet' },
    }
  );
  totalRequests.add(1);
  
  const walletSuccess = check(walletRes, {
    'get-wallet: status 200': (r) => r.status === 200,
    'get-wallet: has walletId': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.walletId !== undefined;
      } catch (e) {
        return false;
      }
    },
  });
  
  if (!walletSuccess) {
    console.error(`[VU ${__VU}] Failed to fetch wallet`);
    console.error(`Response: ${walletRes.body}`);
    errorRate.add(1);
    return;
  }
  
  const walletBody = JSON.parse(walletRes.body);
  const walletId = walletBody.walletId;
  
  console.log(`[VU ${__VU}] Wallet retrieved. WalletId: ${walletId}, Balance: ${walletBody.balance}`);
  
  sleep(THINK_TIME);
  
  // ========================================
  // ÉTAPE 6: DÉPÔT DE FONDS
  // ========================================
  const depositAmount = (Math.random() * 40000 + 40000).toFixed(2);
  console.log(`[VU ${__VU}] Making deposit: ${depositAmount} USD`);
  
  const depositStart = Date.now();
  const depositRes = http.post(
    `${BASE_URL}/wallets/deposit?walletId=${walletId}&amount=${depositAmount}&paymentMethod=CARD`,
    null,
    {
      headers: { 
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json',
      },
      tags: { name: 'deposit' },
    }
  );
  depositDuration.add(Date.now() - depositStart);
  totalRequests.add(1);
  
  const depositSuccess = check(depositRes, {
    'deposit: status 200': (r) => r.status === 200,
    'deposit: success': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.success === true;
      } catch (e) {
        return false;
      }
    },
    'deposit: has transactionId': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.transactionId !== undefined;
      } catch (e) {
        return false;
      }
    },
  });
  
  depositSuccessRate.add(depositSuccess);
  errorRate.add(!depositSuccess);
  
  if (!depositSuccess) {
    console.error(`[VU ${__VU}] Deposit failed`);
    console.error(`Response: ${depositRes.body}`);
    return;
  }
  
  console.log(`[VU ${__VU}] Deposit successful: ${depositAmount} USD`);
  
  sleep(THINK_TIME);
  
  // ========================================
  // ÉTAPE 7: PLACEMENT D'ORDRES (3 ordres)
  // ========================================
  for (let i = 1; i <= 3; i++) {
    const orderData = generateOrderData();
    console.log(`[VU ${__VU}] Placing order ${i}/3: ${orderData.side} ${orderData.quantity} ${orderData.symbol}`);
    
    const orderStart = Date.now();
    const orderRes = http.post(
      `${BASE_URL}/orders`,
      JSON.stringify(orderData),
      {
        headers: { 
          'Authorization': `Bearer ${authToken}`,
          'Content-Type': 'application/json',
        },
        tags: { name: 'place-order' },
      }
    );
    orderDuration.add(Date.now() - orderStart);
    totalRequests.add(1);
    
    const orderSuccess = check(orderRes, {
      [`order-${i}: status 200`]: (r) => r.status === 200,
      [`order-${i}: has orderId`]: (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.orderId !== undefined;
        } catch (e) {
          return false;
        }
      },
      [`order-${i}: success`]: (r) => {
        try {
          const body = JSON.parse(r.body);
          return body.success === true;
        } catch (e) {
          return false;
        }
      },
    });
    
    orderSuccessRate.add(orderSuccess);
    errorRate.add(!orderSuccess);
    
    if (orderSuccess) {
      const orderBody = JSON.parse(orderRes.body);
      console.log(`[VU ${__VU}] Order ${i} placed successfully. OrderId: ${orderBody.orderId}`);
    } else {
      console.error(`[VU ${__VU}] Order ${i} failed`);
      console.error(`Response: ${orderRes.body}`);
    }
    
    sleep(THINK_TIME / 2); // Temps de réflexion plus court entre les ordres
  }
  
  sleep(THINK_TIME);
  
  // ========================================
  // ÉTAPE 8: CONSULTATION DES ORDRES
  // ========================================
  console.log(`[VU ${__VU}] Fetching orders`);
  
  const getOrdersRes = http.get(
    `${BASE_URL}/orders`,
    {
      headers: { 
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json',
      },
      tags: { name: 'get-orders' },
    }
  );
  totalRequests.add(1);
  
  check(getOrdersRes, {
    'get-orders: status 200': (r) => r.status === 200,
    'get-orders: is array': (r) => {
      try {
        const body = JSON.parse(r.body);
        return Array.isArray(body);
      } catch (e) {
        return false;
      }
    },
  });
  
  console.log(`[VU ${__VU}] Scenario completed successfully`);
}

// ============================================
// FONCTION DE SETUP (Optionnelle)
// ============================================
export function setup() {
  console.log('========================================');
  console.log('K6 Load Test - Monolithic Architecture');
  console.log('========================================');
  console.log(`Base URL: ${BASE_URL}`);
  console.log(`Think time: ${THINK_TIME}s`);
  console.log('========================================');
}

// ============================================
// FONCTION DE TEARDOWN (Optionnelle)
// ============================================
export function teardown(data) {
  console.log('========================================');
  console.log('Test completed!');
  console.log('========================================');
}