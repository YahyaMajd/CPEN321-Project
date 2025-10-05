import { z } from "zod";
import mongoose from "mongoose";

export const addressSchema = z.object({
  lat: z.number(),
  lon: z.number(),
  formattedAddress: z.string(),
});


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

// ToDo: maybe we dont need to send the full address? the user dosnt need to see lat lon
export type GetQuoteResponse = {
  distancePrice: number,
  warehouseAddress: Address
};

export type CreateOrderRequest = z.infer<typeof createOrderSchema>;

export type CreateOrderResponse = {
  success: boolean,
  id?: mongoose.Types.ObjectId,
  message: string
}

// Generic type
// ------------------------------------------------------------
export type Address = z.infer<typeof addressSchema>;

export enum OrderStatus {
  PENDING = "PENDING",
  ACCEPTED = "ACCEPTED",
  PICKED_UP = "PICKED_UP",
  IN_STORAGE = "IN_STORAGE",
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
    returnAddress?: Address;
    pickupTime: Date;
    returnTime: Date;
};
