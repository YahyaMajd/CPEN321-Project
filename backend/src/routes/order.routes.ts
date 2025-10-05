import { Router } from 'express';
import { OrderController } from '../controllers/order.controller';
import { orderService } from '../services/order.service';
import { CreateOrderRequest, createOrderSchema, QuoteRequest, quoteSchema } from '../types/order.types';
import { validateBody } from '../middleware/validation.middleware';


const router = Router();
const orderController = new OrderController(orderService);

router.post(
    '/quote',
    validateBody<QuoteRequest>(quoteSchema),
    (req, res, next) => orderController.getQuote(req, res, next)
);

router.post(
    '/',
    validateBody<CreateOrderRequest>(createOrderSchema),
    (req, res, next) => orderController.createOrder(req, res, next)
);

export default router;
