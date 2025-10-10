import mongoose, { mongo, ObjectId } from "mongoose";
import { orderModel } from "../models/order.model";
import {WAREHOUSES} from "../constants/warehouses"
import { CreateOrderRequest, QuoteRequest, GetQuoteResponse, CreateOrderResponse, Order, OrderStatus, GetAllOrdersResponse, ACTIVE_ORDER_STATUSES } from "../types/order.types";
import { jobService } from "./job.service";
import logger from "../utils/logger.util";
import { log } from "console";


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
                returnAddress,
            } = reqData;

            const studentObjectId = new mongoose.Types.ObjectId(studentId);

            const newOrder: Order = {
                studentId: studentObjectId,
                status: OrderStatus.PENDING,
                volume,
                price: totalPrice,
                studentAddress,
                warehouseAddress,
                returnAddress: returnAddress || studentAddress, // Default to student address if not provided
                pickupTime,
                returnTime,
            };

            const createdOrder = await orderModel.create(newOrder);

            // Create jobs for this order (storage and return)
            const finalReturnAddress = returnAddress || studentAddress;

            await jobService.createJobsForOrder(
                createdOrder._id.toString(),
                reqData.studentId,
                reqData.volume,
                reqData.totalPrice,
                reqData.studentAddress,
                reqData.warehouseAddress,
                finalReturnAddress,
                reqData.pickupTime,
                reqData.returnTime
            );

            return {
                id: createdOrder._id.toString(),
                studentId: createdOrder.studentId.toString(),
                moverId: createdOrder.moverId?.toString(),
                status: createdOrder.status,
                volume: createdOrder.volume,
                price: createdOrder.price,
                studentAddress: createdOrder.studentAddress,
                warehouseAddress: createdOrder.warehouseAddress,
                returnAddress: createdOrder.returnAddress,
                pickupTime: createdOrder.pickupTime,
                returnTime: createdOrder.returnTime,
            };
        } catch (error) {
            logger.error("Error in createOrder service:", error);
            throw new Error("Failed to create order");
        }
    }

    async getUserActiveOrder(studentId: ObjectId | undefined): Promise<Order | null> {

        const activeOrder = await orderModel.findActiveOrder({
            studentId,
            status: { $in: ACTIVE_ORDER_STATUSES }
        });
            return activeOrder;
    }

    async getAllOrders(studentId: ObjectId | undefined): Promise<GetAllOrdersResponse> {
        try{
            const orders = await orderModel.getAllOrders(studentId);
            const mappedOrders = orders.map((order: Order) => ({
                studentId: order.studentId.toString(),
                status: order.status,
                volume: order.volume,
                totalPrice: order.price,
                studentAddress: order.studentAddress,
                warehouseAddress: order.warehouseAddress,
                pickupTime: order.pickupTime,
                returnTime: order.returnTime,
            }));

            return{

                success: true,
                orders: mappedOrders,
                message: "Orders retrieved successfully",
            };
        } catch (error) {
            logger.error("Error in getAllOrders service:", error);
            throw new Error("Failed to get all orders");
        }
    }

}

export const orderService = new OrderService();
