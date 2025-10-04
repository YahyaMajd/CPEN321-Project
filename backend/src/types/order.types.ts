import { z } from "zod";
import mongoose from "mongoose";
import { orderStatusSchema } from "../constants/order";

export const addressSchema = z.object({
  lat: z.number(),
  lon: z.number(),
  formattedAddress: z.string(),
});

// Order zod Schema
// ------------------------------------------------------------
export const createOrderSchema = z.object({
  studentId: z.string().refine(val => mongoose.isValidObjectId(val), {
    message: "Invalid student ID",
  }),

  volume: z.number().positive(),
  pickupAddress: addressSchema,
  storageAddress: addressSchema,

  pickupTime: z.date(),
  returnTime: z.date()
});

// Request types
// ------------------------------------------------------------
export type CreateOrderRequest = z.infer<typeof createOrderSchema>;

// Generic type
// ------------------------------------------------------------
export type Address = z.infer<typeof addressSchema>;
export type OrderStatus = z.infer<typeof orderStatusSchema>;

export type Order = {
    id: string; 
    creationTime: Date;
    status: OrderStatus;
    studentId: mongoose.Types.ObjectId;
    moverId?: mongoose.Types.ObjectId;
    volume: number;
    storagePrice: number;
    pickupAddress: Address;
    dropAddress?: Address;
    storageAddress: Address;
    pickupTime: Date;
    returnTime?: Date;
};
