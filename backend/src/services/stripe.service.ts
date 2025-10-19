import Stripe from 'stripe';
import { PaymentIntent, PaymentIntentStatus, PaymentResult, PaymentStatus } from '../types/payment.types';
import logger from '../utils/logger.util';

export class StripeService {
    private stripe: Stripe | null = null;

    private initializeStripe() {
        if (!this.stripe) {
            const stripeSecretKey = process.env.STRIPE_SECRET_KEY;
            if (!stripeSecretKey) {
                throw new Error('STRIPE_SECRET_KEY environment variable is required');
            }
            
            this.stripe = new Stripe(stripeSecretKey, {
                apiVersion: '2025-09-30.clover',
            });
        }
        return this.stripe;
    }

    /**
     * Create a payment intent for the given amount in CAD
     */
    async createPaymentIntent(amount: number, currency: string = 'CAD'): Promise<PaymentIntent> {
        try {
            const stripe = this.initializeStripe();
            
            // Convert to cents (Stripe requires amounts in smallest currency unit)
            const amountInCents = Math.round(amount * 100);

            const paymentIntent = await stripe.paymentIntents.create({
                amount: amountInCents,
                currency: currency.toLowerCase(),
                automatic_payment_methods: {
                    enabled: true,
                },
                metadata: {
                    integration: 'student-storage-app'
                }
            });

            return {
                id: paymentIntent.id,
                amount: amount, // Return original amount in dollars
                currency: currency,
                clientSecret: paymentIntent.client_secret!,
                status: this.mapStripeStatusToOur(paymentIntent.status)
            };
        } catch (error: any) {
            logger.error('Error creating payment intent:', error);
            throw new Error(`Failed to create payment intent: ${error.message || error}`);
        }
    }

    /**
     * Confirm a payment intent using payment method
     */
    async confirmPayment(paymentIntentId: string, paymentMethodId: string): Promise<PaymentResult> {
        try {
            const stripe = this.initializeStripe();
            const paymentIntent = await stripe.paymentIntents.confirm(paymentIntentId, {
                payment_method: paymentMethodId,
                return_url: 'https://your-app.com/return', // Not needed for our use case but required by Stripe
            });

            return {
                paymentId: paymentIntent.id,
                status: this.mapStripeStatusToPaymentStatus(paymentIntent.status),
                amount: paymentIntent.amount / 100, // Convert back to dollars
                currency: paymentIntent.currency.toUpperCase(),
                failureReason: paymentIntent.last_payment_error?.message
            };
        } catch (error: any) {
            logger.error('Error confirming payment:', error);
            
            return {
                paymentId: paymentIntentId,
                status: PaymentStatus.FAILED,
                amount: 0,
                currency: 'CAD',
                failureReason: error.message || 'Payment confirmation failed'
            };
        }
    }

    /**
     * Get payment intent status
     */
    async getPaymentIntent(paymentIntentId: string): Promise<PaymentResult> {
        try {
            const stripe = this.initializeStripe();
            const paymentIntent = await stripe.paymentIntents.retrieve(paymentIntentId);

            return {
                paymentId: paymentIntent.id,
                status: this.mapStripeStatusToPaymentStatus(paymentIntent.status),
                amount: paymentIntent.amount / 100,
                currency: paymentIntent.currency.toUpperCase(),
                failureReason: paymentIntent.last_payment_error?.message
            };
        } catch (error) {
            logger.error('Error retrieving payment intent:', error);
            throw new Error('Failed to retrieve payment status');
        }
    }

    /**
     * Refund a payment intent
     */
    async refundPayment(paymentIntentId: string, amount?: number): Promise<PaymentResult> {
        try {
            const stripe = this.initializeStripe();
            
            // Create refund for the payment intent
            const refund = await stripe.refunds.create({
                payment_intent: paymentIntentId,
                amount: amount ? Math.round(amount * 100) : undefined, // Convert to cents if partial refund
            });

            logger.info(`Refund created: ${refund.id} for payment intent: ${paymentIntentId}`);

            return {
                paymentId: refund.id,
                status: refund.status === 'succeeded' ? PaymentStatus.SUCCEEDED : PaymentStatus.PENDING,
                amount: refund.amount / 100,
                currency: refund.currency.toUpperCase(),
            };
        } catch (error: any) {
            logger.error('Error creating refund:', error);
            throw new Error(`Failed to refund payment: ${error.message || error}`);
        }
    }

    /**
     * Map Stripe payment intent status to our PaymentIntentStatus
     */
    private mapStripeStatusToOur(stripeStatus: string): PaymentIntentStatus {
        switch (stripeStatus) {
            case 'requires_payment_method':
                return PaymentIntentStatus.REQUIRES_PAYMENT_METHOD;
            case 'requires_confirmation':
                return PaymentIntentStatus.REQUIRES_CONFIRMATION;
            case 'succeeded':
                return PaymentIntentStatus.SUCCEEDED;
            case 'canceled':
                return PaymentIntentStatus.CANCELED;
            default:
                return PaymentIntentStatus.FAILED;
        }
    }

    /**
     * Map Stripe payment intent status to our PaymentStatus
     */
    private mapStripeStatusToPaymentStatus(stripeStatus: string): PaymentStatus {
        switch (stripeStatus) {
            case 'succeeded':
                return PaymentStatus.SUCCEEDED;
            case 'requires_payment_method':
            case 'requires_confirmation':
            case 'processing':
                return PaymentStatus.PENDING;
            case 'canceled':
                return PaymentStatus.CANCELED;
            default:
                return PaymentStatus.FAILED;
        }
    }
}

export const stripeService = new StripeService();