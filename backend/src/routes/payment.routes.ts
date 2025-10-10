import { Router } from 'express';
import { PaymentController } from '../controllers/payment.controller';
import { paymentService } from '../services/payment.service';
import { CreatePaymentIntentRequest, ProcessPaymentRequest, createPaymentIntentSchema, processPaymentSchema } from '../types/payment.types';
import { validateBody } from '../middleware/validation.middleware';

const router = Router();
const paymentController = new PaymentController(paymentService);

router.post(
    '/create-intent',
    validateBody<CreatePaymentIntentRequest>(createPaymentIntentSchema),
    (req, res, next) => paymentController.createPaymentIntent(req, res, next)
);

router.post(
    '/process',
    validateBody<ProcessPaymentRequest>(processPaymentSchema),
    (req, res, next) => paymentController.processPayment(req, res, next)
);

router.get(
    '/status/:paymentIntentId',
    (req: any, res, next) => paymentController.getPaymentStatus(req, res, next)
);

export default router;