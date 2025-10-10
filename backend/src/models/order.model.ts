import mongoose, { ObjectId, Schema } from "mongoose";
import { z } from "zod";

import { ACTIVE_ORDER_STATUSES, createOrderSchema, Order, OrderStatus } from "../types/order.types";
import logger from "../utils/logger.util";

// Address subdocument schema to be used inside order schema
// ------------------------------------------------------------

const addressSubSchema = new Schema(
  {
    lat: { type: Number, required: true },
    lon: { type: Number, required: true },
    formattedAddress: { type: String, required: true, trim: true },
  },
  { _id: false }
);

// Mongoose Order schema
// ------------------------------------------------------------
const orderSchema = new Schema(
  {
    studentId: {
      type: Schema.Types.ObjectId,
      ref: "User",
      required: true,
    },
    moverId: {
      type: Schema.Types.ObjectId,
      ref: "User",
      required: false,
    },
    status: {
      type: String,
      required: true,
    },
    volume: {
      type: Number,
      required: true,
    },
    // ToDO: should this price indicate storage + delivery or just storage?
    price: {
      type: Number,
      required: true,
    },
    studentAddress: { type: addressSubSchema, required: true },
    warehouseAddress: { type: addressSubSchema, required: true },
    returnAddress: { type: addressSubSchema, required: false }, // the location that boxes from warehouse go to 
    pickupTime: { type: Date, required: true },
    returnTime: { type: Date, required: false },
  },
  {
    timestamps: true,
  }
);

// OrderModel class
// ------------------------------------------------------------
export class OrderModel {
  private order: mongoose.Model<any>;

  constructor() {
    this.order = mongoose.model("Order", orderSchema);
  }

  async create(newOrder: Order) {
    try {
      const createdOrder = await this.order.create(newOrder);
      return createdOrder;
    } catch (error) {
      logger.error("Error creating order:", error);
      throw new Error("Failed to create order");
    }
  }

  async getAllOrders(studentId: ObjectId | undefined) {
    try {
      return await this.order.find({ studentId });
    } catch (error) {
      logger.error("Error getting all orders:", error);
      throw new Error("Failed to get all orders");
    }
  }

  async findById(orderId: mongoose.Types.ObjectId) {
    try {
      return await this.order.findById(orderId);
    } catch (error) {
      logger.error("Error finding order:", error);
      throw new Error("Failed to find order");
    }
  }

  async findActiveOrder(filter: { studentId: ObjectId | undefined, status: { $in: OrderStatus[] } }): Promise<Order | null> {
    try {
        // Find the most recent non-terminal order for this student
        return await this.order.findOne({
            studentId: filter.studentId,
            status: { $in: filter.status.$in }
        }).sort({ createdAt: -1 });
    } catch (error) {
        logger.error("Error finding active order:", error);
        throw new Error("Failed to find active order");
    }
  }

  async update(orderId: mongoose.Types.ObjectId, updatedOrder: Partial<Order>) {
    try {
      return await this.order.findByIdAndUpdate(orderId, updatedOrder, { new: true });
    } catch (error) {
      logger.error("Error updating order:", error);
      throw new Error("Failed to update order");
    }
  }

  async delete(orderId: mongoose.Types.ObjectId) {
    try {
      await this.order.findByIdAndDelete(orderId);
    } catch (error) {
      logger.error("Error deleting order:", error);
      throw new Error("Failed to delete order");
    }
  }


}

export const orderModel = new OrderModel();
