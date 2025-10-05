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
    orderController.getQuote
);

router.post(
    '/',
    validateBody<CreateOrderRequest>(createOrderSchema),
    orderController.createOrder
);

export default router;
