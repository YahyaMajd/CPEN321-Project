// Quick FCM test script - run this to verify your Firebase credentials work
// Usage: node test-fcm.js <FCM_TOKEN>

const admin = require('firebase-admin');
const serviceAccount = require('../serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

const token = process.argv[2];

if (!token) {
  console.error('❌ Please provide an FCM token as argument');
  console.log('Usage: node test-fcm.js <FCM_TOKEN>');
  process.exit(1);
}

console.log('🔔 Testing FCM with token:', token.substring(0, 20) + '...');

const message = {
  token: token,
  notification: {
    title: 'Test Notification',
    body: 'If you see this, FCM is working! ✅',
  },
  data: {
    test: 'true',
  },
};

admin
  .messaging()
  .send(message)
  .then((response) => {
    console.log('✅ SUCCESS! Notification sent:', response);
    console.log('\n✨ FCM is working correctly!');
    process.exit(0);
  })
  .catch((error) => {
    console.error('❌ FAILED to send notification:');
    console.error('Error code:', error.code);
    console.error('Error message:', error.message);
    console.error('\nFull error:', error);
    
    if (error.code === 'messaging/registration-token-not-registered') {
      console.log('\n💡 Solution: This token is invalid/expired.');
      console.log('   1. Open your Android app');
      console.log('   2. Sign in/sign up again to generate a fresh token');
      console.log('   3. Check the Android logs for "Manual token:" or "FCM new token:"');
      console.log('   4. Re-run this script with the new token');
    }
    
    process.exit(1);
  });
