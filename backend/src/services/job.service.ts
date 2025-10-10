import mongoose from "mongoose";
import { jobModel } from "../models/job.model";
import { orderModel } from "../models/order.model";
import { 
    CreateJobRequest, 
    CreateJobResponse, 
    Job, 
    JobStatus, 
    JobType,
    GetAllJobsResponse,
    GetJobResponse,
    UpdateJobStatusRequest,
    JobListItem,
    JobResponse,
    GetMoverJobsResponse
} from "../types/job.type";
import { Address, OrderStatus } from "../types/order.types";
import logger from "../utils/logger.util";

export class JobService {
    async createJob(reqData: CreateJobRequest): Promise<CreateJobResponse> {
        try {
            const newJob: Job = {
                orderId: new mongoose.Types.ObjectId(reqData.orderId),
                studentId: new mongoose.Types.ObjectId(reqData.studentId),
                jobType: reqData.jobType,
                status: JobStatus.AVAILABLE,
                volume: reqData.volume,
                price: reqData.price,
                pickupAddress: reqData.pickupAddress,
                dropoffAddress: reqData.dropoffAddress,
                scheduledTime: reqData.scheduledTime,
                createdAt: new Date(),
                updatedAt: new Date(),
            };

            const createdJob = await jobModel.create(newJob);
            
            return {
                success: true,
                id: createdJob._id.toString(),
                message: `${reqData.jobType} job created successfully`,
            };
        } catch (error) {
            logger.error("Error in createJob service:", error);
            throw new Error("Failed to create job");
        }
    }

    // Create both storage and return jobs for an order
    async createJobsForOrder(
        orderId: string,
        studentId: string,
        volume: number,
        totalPrice: number,
        studentAddress: Address,
        warehouseAddress: Address,
        returnAddress: Address,
        pickupTime: string,
        returnTime: string
    ): Promise<{ storageJobId: string; returnJobId: string }> {
        try {
            // Create STORAGE job (student → warehouse)
            const storageJobRequest: CreateJobRequest = {
                orderId,
                studentId,
                jobType: JobType.STORAGE,
                volume,
                price: totalPrice * 0.6, // 60% for storage/pickup
                pickupAddress: studentAddress,
                dropoffAddress: warehouseAddress,
                scheduledTime: pickupTime,
            };

            // Create RETURN job (warehouse → return address)
            const returnJobRequest: CreateJobRequest = {
                orderId,
                studentId,
                jobType: JobType.RETURN,
                volume,
                price: totalPrice * 0.4, // 40% for return delivery
                pickupAddress: warehouseAddress,
                dropoffAddress: returnAddress,
                scheduledTime: returnTime,
            };

            const storageJob = await this.createJob(storageJobRequest);
            const returnJob = await this.createJob(returnJobRequest);

            return {
                storageJobId: storageJob.id,
                returnJobId: returnJob.id,
            };
        } catch (error) {
            logger.error("Error creating jobs for order:", error);
            throw new Error("Failed to create jobs for order");
        }
    }

    async getAllJobs(): Promise<GetAllJobsResponse> {
        try {
            const jobs = await jobModel.findAllJobs();
            
            const jobListItems: JobListItem[] = jobs.map(job => ({
                id: job._id.toString(),
                jobType: job.jobType,
                volume: job.volume,
                price: job.price,
                pickupAddress: job.pickupAddress,
                dropoffAddress: job.dropoffAddress,
                scheduledTime: job.scheduledTime,
                status: job.status,
            }));

            return {
                message: "All jobs retrieved successfully",
                data: { jobs: jobListItems },
            };
        } catch (error) {
            logger.error("Error in getAllJobs service:", error);
            throw new Error("Failed to get all jobs");
        }
    }

    async getAllAvailableJobs(): Promise<GetAllJobsResponse> {
        try {
            const jobs = await jobModel.findAvailableJobs();
            
            const jobListItems: JobListItem[] = jobs.map(job => ({
                id: job._id.toString(),
                jobType: job.jobType,
                volume: job.volume,
                price: job.price,
                pickupAddress: job.pickupAddress,
                dropoffAddress: job.dropoffAddress,
                scheduledTime: job.scheduledTime,
                status: job.status,
            }));

            return {
                message: "Available jobs retrieved successfully",
                data: { jobs: jobListItems },
            };
        } catch (error) {
            logger.error("Error in getAllAvailableJobs service:", error);
            throw new Error("Failed to get available jobs");
        }
    }

    async getMoverJobs(moverId: string): Promise<GetMoverJobsResponse> {
        try {
            const jobs = await jobModel.findByMoverId(new mongoose.Types.ObjectId(moverId));
            
            const jobListItems: JobListItem[] = jobs.map(job => ({
                id: job._id.toString(),
                jobType: job.jobType,
                volume: job.volume,
                price: job.price,
                pickupAddress: job.pickupAddress,
                dropoffAddress: job.dropoffAddress,
                scheduledTime: job.scheduledTime,
                status: job.status,
            }));

            return {
                message: "Mover jobs retrieved successfully",
                data: { jobs: jobListItems },
            };
        } catch (error) {
            logger.error("Error in getMoverJobs service:", error);
            throw new Error("Failed to get mover jobs");
        }
    }

    async getJobById(jobId: string): Promise<GetJobResponse> {
        try {
            const job = await jobModel.findById(new mongoose.Types.ObjectId(jobId));
            
            if (!job) {
                throw new Error("Job not found");
            }

            const jobResponse: JobResponse = {
                id: job._id.toString(),
                orderId: job.orderId.toString(),
                studentId: job.studentId.toString(),
                moverId: job.moverId?.toString(),
                jobType: job.jobType,
                status: job.status,
                volume: job.volume,
                price: job.price,
                pickupAddress: job.pickupAddress,
                dropoffAddress: job.dropoffAddress,
                scheduledTime: job.scheduledTime,
                createdAt: job.createdAt,
                updatedAt: job.updatedAt,
            };

            return {
                message: "Job retrieved successfully",
                data: { job: jobResponse },
            };
        } catch (error) {
            logger.error("Error in getJobById service:", error);
            throw new Error("Failed to get job");
        }
    }

    async updateJobStatus(jobId: string, updateData: UpdateJobStatusRequest): Promise<JobResponse> {
        try {
            logger.info(`updateJobStatus service called for jobId=${jobId} updateData=${JSON.stringify(updateData)}`);
            const updateFields: Partial<Job> = {
                status: updateData.status,
                updatedAt: new Date(),
            };

            // If assigning job to mover
            if (updateData.moverId) {
                updateFields.moverId = new mongoose.Types.ObjectId(updateData.moverId);
            }

            const updatedJob = await jobModel.update(
                new mongoose.Types.ObjectId(jobId),
                updateFields
            );

            logger.info(`Job ${jobId} updated: status=${updateFields.status}`);

            if (!updatedJob) {
                throw new Error("Job not found");
            }

            if (updateData.status === JobStatus.ACCEPTED) {
                const job = await jobModel.findById(new mongoose.Types.ObjectId(jobId));
                logger.debug(`Found job for ACCEPTED flow: ${JSON.stringify(job)}`);
                // job.orderId may be populated (document) or just an ObjectId
                const rawOrderId: any = (job as any).orderId?._id ?? (job as any).orderId;
                logger.info(`Attempting to update linked order status to ACCEPTED for orderId=${rawOrderId}`);
                try {
                    const orderUpdateResult = await orderModel.update(new mongoose.Types.ObjectId(rawOrderId), { status: OrderStatus.ACCEPTED });
                    logger.info(`Order ${rawOrderId} update result: ${JSON.stringify(orderUpdateResult)}`);
                } catch (err) {
                    logger.error(`Failed to update order status to ACCEPTED for orderId=${rawOrderId}:`, err);
                    throw err;
                }
            }

            // If job is completed, update order status
            if (updateData.status === JobStatus.COMPLETED) {
                const job = await jobModel.findById(new mongoose.Types.ObjectId(jobId));
                logger.debug(`Found job for COMPLETED flow: ${JSON.stringify(job)}`);
                const rawOrderId: any = (job as any).orderId?._id ?? (job as any).orderId;
                logger.info(`Attempting to update linked order status after job completion for orderId=${rawOrderId}`);
                try {
                    if (job.jobType === JobType.STORAGE) {
                        const orderUpdateResult = await orderModel.update(new mongoose.Types.ObjectId(rawOrderId), { status: OrderStatus.IN_STORAGE });
                        logger.info(`Order ${rawOrderId} update result (IN_STORAGE): ${JSON.stringify(orderUpdateResult)}`);
                    } else if (job.jobType === JobType.RETURN) {
                        const orderUpdateResult = await orderModel.update(new mongoose.Types.ObjectId(rawOrderId), { status: OrderStatus.COMPLETED });
                        logger.info(`Order ${rawOrderId} update result (COMPLETED): ${JSON.stringify(orderUpdateResult)}`);
                    }
                } catch (err) {
                    logger.error(`Failed to update order status after job completion for orderId=${rawOrderId}:`, err);
                    throw err;
                }
            }

            return {
                id: updatedJob._id.toString(),
                orderId: updatedJob.orderId.toString(),
                studentId: updatedJob.studentId.toString(),
                moverId: updatedJob.moverId?.toString(),
                jobType: updatedJob.jobType,
                status: updatedJob.status,
                volume: updatedJob.volume,
                price: updatedJob.price,
                pickupAddress: updatedJob.pickupAddress,
                dropoffAddress: updatedJob.dropoffAddress,
                scheduledTime: updatedJob.scheduledTime,
                createdAt: updatedJob.createdAt,
                updatedAt: updatedJob.updatedAt,
            };
        } catch (error) {
            logger.error("Error in updateJobStatus service:", error);
            throw new Error("Failed to update job status");
        }
    }
}

export const jobService = new JobService();
