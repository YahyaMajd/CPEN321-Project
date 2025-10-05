import { NextFunction, Request, Response } from 'express';
import { OrderService } from '../services/order.service';
import { CreateOrderRequest, CreateOrderResponse, QuoteRequest, GetQuoteResponse } from '../types/order.types';

export class OrderController {
    constructor(private orderService: OrderService) {}
    async getQuote(req: Request<{}, {}, QuoteRequest>, res: Response<GetQuoteResponse>, next: NextFunction) {
        try {
            const quote = await this.orderService.getQuote(req.body);
            res.status(200).json(quote);
        } catch (error) {
            // TODo: improve error handling
            next(error);
        }     
    }

    async createOrder(req: Request<{}, {}, CreateOrderRequest>, res: Response<CreateOrderResponse>, next: NextFunction) {
        try {
            const result = await this.orderService.createOrder(req.body);
            res.status(201).json(result);
        } catch (error) {            
            // TODo: improve error handling
            next(error);
        }
    }
}