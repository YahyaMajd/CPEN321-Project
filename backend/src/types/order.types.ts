import { z } from "zod";
import mongoose from "mongoose";

export const addressSchema = z.object({
  lat: z.number(),
  lon: z.number(),
  formattedAddress: z.string(),
});

export const orderStatusSchema = z.enum([
  "PENDING",
  "ACCEPTED",
  "PICKED_UP",
  "IN_STORAGE",
  "CANCELLED",
]);

// Order zod Schema
// ------------------------------------------------------------
export const getQuoteSchema = z.object({
  studentId: z.string().refine(val => mongoose.isValidObjectId(val)),
  studentAddress: addressSchema,
});

export const createOrderSchema = z.object({
  studentId: z.string().refine(val => mongoose.isValidObjectId(val), {
    message: "Invalid student ID",
  }),
  volume: z.number().positive(),
  totalPrice: z.number().positive(),
  studentAddress: addressSchema,
  warehouseAddress: addressSchema,
  pickupTime: z.date(),
  returnTime: z.date(),
});


// Request types
// ------------------------------------------------------------
export type GetQuoteRequest = z.infer<typeof getQuoteSchema>;

export type GetQuoteResponse = {
  distancePrice: number,
  warehouseAddress: Address
};

export type CreateOrderRequest = z.infer<typeof createOrderSchema>;

export type CreateOrderResponse = {
  success: boolean,
  message: string
}

// Generic type
// ------------------------------------------------------------
export type Address = z.infer<typeof addressSchema>;
export type OrderStatus = z.infer<typeof orderStatusSchema>;

export type Order = {
    studentId: mongoose.Types.ObjectId;
    moverId?: mongoose.Types.ObjectId;
    volume: number;
    studentAddress: Address;
    pickupTime: Date;
    returnTime: Date;
    price: number;
    warehouseAddress: Address;    
    status: OrderStatus;
    returnAddress?: Address;
};
