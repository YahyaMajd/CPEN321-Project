import jwt from 'jsonwebtoken';

// Create a test JWT token (like the app would do after login)
const testUserId = '68df460f177235df097bb797'; // Use a valid MongoDB ObjectId format
const jwtSecret = process.env.JWT_SECRET || 'b2474bfe0d77d68ed1c1829c956d06d4dfde3b21733f9e718dc6f2a4d2ba07ef';

const testToken = jwt.sign(
    { id: testUserId },
    jwtSecret,
    { expiresIn: '1h' }
);

console.log('Test JWT Token:');
console.log(testToken);
console.log('\nUse this token in the Authorization header:');
console.log(`Authorization: Bearer ${testToken}`);

// Test curl command
console.log('\nTest curl command:');
console.log(`curl -X POST http://localhost:3000/api/payment/create-intent \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer ${testToken}" \\
  -d '{
    "amount": 25.50,
    "currency": "CAD"
  }'`);