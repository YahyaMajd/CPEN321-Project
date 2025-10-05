import mongoose from "mongoose";
import { orderModel } from "../models/order.model";
import {WAREHOUSES} from "../constants/warehouses"
import { CreateOrderRequest, QuoteRequest, GetQuoteResponse, CreateOrderResponse, Order, OrderStatus } from "../types/order.types";
import logger from "../utils/logger.util";


// OrderService Class
// ------------------------------------------------------------
export class OrderService {
    private findClosestWarehouse(lat: number, lon: number) {
        // ToDo: integrate with google maps to actually find the closest warehouse
        // google maps also returns time it takes to drive there (based on trafic and ...)
        // we can also use that in our price calc when we integrate with maps
        return { 
            closestWarehouse: WAREHOUSES[0],
            distanceToWarehouse: 5 //km
         }
    }

    async getQuote(reqData: QuoteRequest): Promise<GetQuoteResponse> {
        try {
            const { studentAddress } = reqData;
            
            let {closestWarehouse, distanceToWarehouse} = this.findClosestWarehouse(studentAddress.lat, studentAddress.lon);

            // $2 per km ?
            const distancePrice = Number((distanceToWarehouse * 2).toFixed(2));

            return {
                distancePrice,
                warehouseAddress: closestWarehouse,
            };
        } catch (error) {
            logger.error("Error in getQuote service:", error);
            throw new Error("Failed to calculate quote");
        }
    }

    async createOrder(reqData: CreateOrderRequest): Promise<CreateOrderResponse> {
        try {
            // Extract required data
            const {
                studentId,
                volume,
                totalPrice,
                studentAddress,
                warehouseAddress,
                pickupTime,
                returnTime,
            } = reqData;

            const studentObjectId = new mongoose.Types.ObjectId(studentId);

            const newOrder: Order = {
                studentId: studentObjectId,
                status: OrderStatus.PENDING,
                volume,
                price: totalPrice,
                studentAddress,
                warehouseAddress,
                returnAddress: undefined,
                pickupTime,
                returnTime,
            };

            const createdOrder = await orderModel.create(newOrder);

            return {
                success: true, 
                id: createdOrder._id,
                message: "Order created successfully",
            };
        } catch (error) {
            logger.error("Error in createOrder service:", error);
            throw new Error("Failed to create order");
        }
    }
}

export const orderService = new OrderService();
