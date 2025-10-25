import { z } from "zod";
import mongoose from "mongoose";
import { addressSchema } from "./order.types";

// Job Types Enum
export enum JobType {
    STORAGE = "STORAGE",  // Pickup from student to warehouse
    RETURN = "RETURN"     // Delivery from warehouse to return address
}

// Job zod Schema
// ------------------------------------------------------------
export const jobSchema = z.object({
    orderId: z.string().refine(val => mongoose.isValidObjectId(val), {
        message: "Invalid order ID",
    }),
    studentId: z.string().refine(val => mongoose.isValidObjectId(val), {
        message: "Invalid student ID",
    }),
    jobType: z.nativeEnum(JobType),
    volume: z.number().positive(),
    price: z.number().positive(),
    pickupAddress: addressSchema,
    dropoffAddress: addressSchema,
    scheduledTime: z.string().datetime(),
});

// Request types
// ------------------------------------------------------------
export type CreateJobRequest = z.infer<typeof jobSchema>;

// Generic type
// ------------------------------------------------------------
// Job status shows if a job is available for movers to pick or already taken
export enum JobStatus {
    AVAILABLE = "AVAILABLE", // Equivalent to PENDING in OrderStatus
    ACCEPTED = "ACCEPTED",
    AWAITING_STUDENT_CONFIRMATION = "AWAITING_STUDENT_CONFIRMATION",
    PICKED_UP = "PICKED_UP",
    IN_STORAGE = "IN_STORAGE",
    COMPLETED = "COMPLETED",
    CANCELLED = "CANCELLED",
}

// Reuse Address type from order types
export type Address = z.infer<typeof addressSchema>;

export type Job = {
    orderId: mongoose.Types.ObjectId;
    studentId: mongoose.Types.ObjectId;
    moverId?: mongoose.Types.ObjectId;
    jobType: JobType;
    status: JobStatus;
    volume: number;
    price: number;
    pickupAddress: Address;
    dropoffAddress: Address;
    scheduledTime: string;
    calendarEventLink?: string;
    createdAt: Date;
    updatedAt: Date;
    verificationRequestedAt?: Date;
};

export type JobResponse = {
    id: string;
    orderId: string;
    studentId: string;
    moverId?: string;
    jobType: JobType;
    status: JobStatus;
    volume: number;
    price: number;
    pickupAddress: Address;
    dropoffAddress: Address;
    scheduledTime: string;
    calendarEventLink?: string;
    createdAt: Date;
    updatedAt: Date;
};

// For listing jobs, we might not want to expose all details
export type JobListItem = Pick<JobResponse, "id" | "orderId" | "jobType" | "volume" | "price" | "pickupAddress" | "dropoffAddress" | "scheduledTime" | "status">;

export type GetAllJobsResponse = {
    message: string;
    data?: {
        jobs: JobListItem[];
    };
};

export type GetMoverJobsResponse = {
    message: string;
    data?: {
        jobs: JobListItem[];
    };
};

export type GetJobResponse = {
    message: string;
    data?: {
        job: JobResponse;
    };
};

export type UpdateJobStatusRequest = {
    status: JobStatus;
    moverId?: string; // When assigning job to mover
};

export type CreateJobResponse = {
    success: boolean;
    id: string;
    message: string;
};
