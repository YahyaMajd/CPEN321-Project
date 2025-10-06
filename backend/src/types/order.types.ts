import { z } from "zod";
import mongoose from "mongoose";

export const addressSchema = z.object({
  lat: z.number(),
  lon: z.number(),
  formattedAddress: z.string(),
});


// Order zod Schema
// ------------------------------------------------------------
export const quoteSchema = z.object({
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
  pickupTime: z.string().datetime(),
  returnTime: z.string().datetime(),
  returnAddress: addressSchema.optional(), // Optional return address
});


// Request types
// ------------------------------------------------------------
export type QuoteRequest = z.infer<typeof quoteSchema>;

// ToDo: maybe we dont need to send the full address? the user dosnt need to see lat lon
export type GetQuoteResponse = {
  distancePrice: number,
  warehouseAddress: Address
};

export type CreateOrderRequest = z.infer<typeof createOrderSchema>;

export type CreateOrderResponse = Order & {
  id: string;
}

// Generic type
// ------------------------------------------------------------
export type Address = z.infer<typeof addressSchema>;

export enum OrderStatus {
  PENDING = "PENDING",
  ACCEPTED = "ACCEPTED",
  PICKED_UP = "PICKED_UP",
  IN_STORAGE = "IN_STORAGE",
  COMPLETED = "COMPLETED", // Add COMPLETED status
  CANCELLED = "CANCELLED",
}

export type Order = {
    studentId: mongoose.Types.ObjectId;
    moverId?: mongoose.Types.ObjectId;
    status: OrderStatus;
    volume: number;
    price: number;
    studentAddress: Address;
    warehouseAddress: Address;  
    returnAddress?: Address; // Make it optional in type as well
    pickupTime: string; // ISO date string
    returnTime: string;  // ISO date string
};
