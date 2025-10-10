import mongoose, { mongo, ObjectId } from "mongoose";
import { orderModel } from "../models/order.model";
import {WAREHOUSES} from "../constants/warehouses"
import { CreateOrderRequest, QuoteRequest, GetQuoteResponse, CancelOrderResponse, CreateOrderResponse, Order, OrderStatus, GetAllOrdersResponse, ACTIVE_ORDER_STATUSES } from "../types/order.types";
import logger from "../utils/logger.util";
import { getIo } from "../socket";
import { jobService } from "./job.service";
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

    async createOrder(reqData: CreateOrderRequest & { idempotencyKey?: string }): Promise<CreateOrderResponse> {
        try {
            const idempotencyKey = (reqData as any).idempotencyKey as string | undefined;

            // If idempotency key provided, return existing order with that key
            if (idempotencyKey) {
                const byKey = await orderModel.findByIdempotencyKey(idempotencyKey);
                if (byKey) {
                    return {
                        id: (byKey as any)._id.toString(),
                        studentId: (byKey as any).studentId.toString(),
                        moverId: (byKey as any).moverId?.toString(),
                        status: byKey.status,
                        volume: byKey.volume,
                        price: byKey.price,
                        studentAddress: byKey.studentAddress,
                        warehouseAddress: byKey.warehouseAddress,
                        returnAddress: byKey.returnAddress,
                        pickupTime: byKey.pickupTime,
                        returnTime: byKey.returnTime,
                    };
                }
            }

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
            } = reqData as any;

            const studentObjectId = new mongoose.Types.ObjectId(studentId);

            const newOrder: any = {
                studentId: studentObjectId,
                status: OrderStatus.PENDING,
                volume,
                price: totalPrice,
                studentAddress,
                warehouseAddress,
                returnAddress: returnAddress || studentAddress, // Default to student address if not provided
                pickupTime,
                returnTime,
                idempotencyKey: idempotencyKey,
            };

            if (idempotencyKey) newOrder.idempotencyKey = idempotencyKey;

            // Create jobs for this order (storage and return)
            const finalReturnAddress = returnAddress || studentAddress;
            try {
                const createdOrder = await orderModel.create(newOrder);

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
                // Emit order.created to the student and order room (do not block the response)
                try {
                    const io = getIo();
                    const orderPayload = {
                        event: 'order.created',
                        order: {
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
                            createdAt: createdOrder.createdAt,
                            updatedAt: createdOrder.updatedAt,
                        },
                        meta: { by: reqData.studentId, ts: new Date().toISOString() }
                    };
                    io.to(`user:${createdOrder.studentId.toString()}`).emit('order.created', orderPayload);
                    io.to(`order:${createdOrder._id.toString()}`).emit('order.created', orderPayload);
                } catch (err) {
                    logger.warn('Failed to emit order.created event:', err);
                }
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
            } catch (err: any) {
                // If duplicate key error due to race/uniqueness, try to find existing by idempotencyKey or by student+status
                const isDup = err && err.code === 11000;
                if (isDup) {
                    if (idempotencyKey) {
                        const byKey = await orderModel.findByIdempotencyKey(idempotencyKey);
                        if (byKey) {
                            return {
                                id: (byKey as any)._id.toString(),
                                studentId: (byKey as any).studentId.toString(),
                                moverId: (byKey as any).moverId?.toString(),
                                status: byKey.status,
                                volume: byKey.volume,
                                price: byKey.price,
                                studentAddress: byKey.studentAddress,
                                warehouseAddress: byKey.warehouseAddress,
                                returnAddress: byKey.returnAddress,
                                pickupTime: byKey.pickupTime,
                                returnTime: byKey.returnTime,
                            };
                        }
                    }
                }

                logger.error("Error creating order:", err);
                throw err;
            }
            


            
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

    async cancelOrder(studentId: ObjectId | undefined): Promise<CancelOrderResponse> {
        try {
            const order = await orderModel.findActiveOrder({
                studentId,
                status: { $in: ACTIVE_ORDER_STATUSES }
            });

            if (!order) {
                return { success: false, message: "Order not found" };
            }

            if (order.status !== OrderStatus.PENDING) {
                return { success: false, message: "Only pending orders can be cancelled" };
            }

            // Update the order status to CANCELLED
            const orderId = (order as any)._id as mongoose.Types.ObjectId;
            const updated = await orderModel.update(orderId, { status: OrderStatus.CANCELLED });

            // Emit order.updated
            try {
                const io = getIo();
                const orderPayload = {
                    event: 'order.updated',
                    order: {
                        id: updated._id.toString(),
                        studentId: updated.studentId.toString(),
                        moverId: updated.moverId?.toString(),
                        status: updated.status,
                        volume: updated.volume,
                        price: updated.price,
                        studentAddress: updated.studentAddress,
                        warehouseAddress: updated.warehouseAddress,
                        returnAddress: updated.returnAddress,
                        pickupTime: updated.pickupTime,
                        returnTime: updated.returnTime,
                        createdAt: updated.createdAt,
                        updatedAt: updated.updatedAt,
                    },
                    meta: { by: studentId?.toString() ?? null, ts: new Date().toISOString() }
                };
                io.to(`user:${updated.studentId.toString()}`).emit('order.updated', orderPayload);
                io.to(`order:${updated._id.toString()}`).emit('order.updated', orderPayload);
            } catch (err) {
                logger.warn('Failed to emit order.updated event:', err);
            }

            return { success: true, message: "Order cancelled successfully" };
        } catch (error) {
            logger.error("Error in cancelOrder service:", error);
            throw new Error("Failed to cancel order");
        }
    }

}

export const orderService = new OrderService();
