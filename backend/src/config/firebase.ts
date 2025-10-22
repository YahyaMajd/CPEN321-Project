import admin from 'firebase-admin';
import serviceAccount from '../serviceAccountKey.json'; //have to generate it for yourslef

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount as admin.ServiceAccount),
});

export default admin;
