export interface NotificationPayload {
  fcmToken: string;             
  title: string;             
  body: string;              
  data?: { [key: string]: string }; // Optional custom data
}
