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

router.get(
    '/all-orders',    // No need for studentId in URL since we get it from auth
    (req, res, next) => orderController.getAllOrders(req, res, next)
);

router.get(
    '/active-order',  // No need for studentId in URL since we get it from auth
    (req, res, next) => orderController.getActiveOrder(req, res, next)
);

router.delete('/cancel-order',
    (req, res, next) => orderController.cancelOrder(req, res, next)
);

// IMPLEMENTATION PENDING IN CONTROLLER AND SERVICE
// router.put(
//     '/:orderId/status',
//     (req, res, next) => orderController.updateOrderStatus(req, res, next)
// );

export default router;

