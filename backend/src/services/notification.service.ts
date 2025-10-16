import admin from '../config/firebase';
import { NotificationPayload } from '../types/notification.types';

class NotificationService {
  async sendPushNotification(payload: NotificationPayload) {
    const { token, title, body } = payload;

    try {
      const message = {
        notification: { title, body },
        token,
      };

      await admin.messaging().send(message);
      console.log(`✅ Notification sent to ${token}`);
    } catch (error) {
      console.error('❌ Failed to send notification:', error);
    }
  }
}

export default new NotificationService();

