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
import { emitToRooms } from "../socket";
import logger from "../utils/logger.util";

export class JobService {
    // Helper to emit job.created for a created job
    private emitJobCreated(createdJob: any, meta?: any) {
        try {
            const payload = {
                event: 'job.created',
                job: {
                    id: createdJob._id.toString(),
                    orderId: (createdJob.orderId && (createdJob.orderId._id ?? createdJob.orderId)).toString(),
                    jobType: createdJob.jobType,
                    status: createdJob.status,
                    moverId: createdJob.moverId?.toString(),
                    pickupAddress: createdJob.pickupAddress,
                    dropoffAddress: createdJob.dropoffAddress,
                    scheduledTime: createdJob.scheduledTime,
                    createdAt: createdJob.createdAt,
                },
                meta: meta ?? { ts: new Date().toISOString() }
            };
            //`user:${createdJob.studentId.toString()}`

            try { emitToRooms([`order:${payload.job.orderId}`, `job:${payload.job.id}`], 'job.created', payload, meta); } catch (e) { logger.warn('Failed to emit job.created', e); }
        } catch (err) {
            logger.warn('Failed to emit job.created event:', err);
        }
    }
    // Helper to emit job.updated for a single job document
    private emitJobUpdated(updatedJob: any, meta?: any) {
        try {
            const payload = {
                event: 'job.updated',
                job: {
                    id: updatedJob._id.toString(),
                    orderId: (updatedJob.orderId && (updatedJob.orderId._id ?? updatedJob.orderId)).toString(),
                    status: updatedJob.status,
                    moverId: updatedJob.moverId?.toString(),
                    jobType: updatedJob.jobType,
                    updatedAt: updatedJob.updatedAt,
                },
                meta: meta ?? { ts: new Date().toISOString() }
            };

            // Emit to order room, job room, and mover if assigned
            try { emitToRooms([`order:${payload.job.orderId}`, `job:${payload.job.id}`], 'job.updated', payload, meta); } catch (e) { logger.warn('Failed to emit job.updated', e); }
            if (updatedJob.moverId) {
                try { emitToRooms([`user:${updatedJob.moverId.toString()}`], 'job.updated', payload, meta); } catch (e) { logger.warn('Failed to emit job.updated to mover', e); }
            }
        } catch (err) {
            logger.warn('Failed to emit job.updated event:', err);
        }
    }

    // Cancel (mark as CANCELLED) all jobs for a given orderId that are not already terminal
    async cancelJobsForOrder(orderId: string, actorId?: string) {
        try {
            const foundJobs: any[] = await jobModel.findByOrderId(new mongoose.Types.ObjectId(orderId));
            const toCancel = foundJobs.filter(j => j.status !== JobStatus.COMPLETED && j.status !== JobStatus.CANCELLED);

            const results: Array<{ jobId: string; prevStatus: string; newStatus: string; moverId?: string }> = [];

            for (const jobDoc of toCancel) {
                try {
                    const updatedJob = await jobModel.update(jobDoc._id, { status: JobStatus.CANCELLED, updatedAt: new Date() });
                    results.push({ jobId: updatedJob._id.toString(), prevStatus: jobDoc.status, newStatus: updatedJob.status, moverId: updatedJob.moverId?.toString() });

                    // Emit job.updated for each cancelled job
                    this.emitJobUpdated(updatedJob, { by: actorId ?? null, ts: new Date().toISOString() });
                } catch (err) {
                    logger.error(`Failed to cancel job ${jobDoc._id} for order ${orderId}:`, err);
                }
            }

            return { cancelledJobs: results };
        } catch (error) {
            logger.error('Error in cancelJobsForOrder:', error);
            throw new Error('Failed to cancel jobs for order');
        }
    }
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

            // Emit job.created so clients (movers/students) are notified in realtime
            try {
                this.emitJobCreated(createdJob, { by: reqData.studentId ?? null, ts: new Date().toISOString() });
            } catch (emitErr) {
                logger.warn('Failed to emit job.created after createJob:', emitErr);
            }

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

            // If attempting to ACCEPT the job, perform an atomic accept to avoid races
            let updatedJob;
            if (updateData.status === JobStatus.ACCEPTED) {
                const moverObjectId = updateData.moverId ? new mongoose.Types.ObjectId(updateData.moverId) : undefined;
                updatedJob = await jobModel.tryAcceptJob(new mongoose.Types.ObjectId(jobId), moverObjectId);

                if (!updatedJob) {
                    // No document returned -> job was not AVAILABLE (already accepted or not available)
                    throw new Error("Job has already been accepted or is not available");
                }

                logger.info(`Job ${jobId} atomically accepted by mover=${updateData.moverId}`);

                // job.orderId may be populated (document) or just an ObjectId
                const rawOrderId: any = (updatedJob as any).orderId?._id ?? (updatedJob as any).orderId;
                logger.info(`Attempting to update linked order status to ACCEPTED for orderId=${rawOrderId}`);
                try {
                    const orderUpdateResult = await orderModel.update(new mongoose.Types.ObjectId(rawOrderId), { status: OrderStatus.ACCEPTED });
                    logger.info(`Order Updated in Job Service${rawOrderId} update result: ${JSON.stringify(orderUpdateResult)}`);
                    // Emit order.updated so clients watching the order room get notified
                    try {
                        const meta = { by: updateData.moverId ?? null, ts: new Date().toISOString() };
                        const orderPayload = {
                            event: 'order.updated',
                            order: {
                                id: orderUpdateResult._id.toString(),
                                studentId: orderUpdateResult.studentId.toString(),
                                moverId: orderUpdateResult.moverId?.toString(),
                                status: orderUpdateResult.status,
                                volume: orderUpdateResult.volume,
                                price: orderUpdateResult.price,
                                studentAddress: orderUpdateResult.studentAddress,
                                warehouseAddress: orderUpdateResult.warehouseAddress,
                                returnAddress: orderUpdateResult.returnAddress,
                                pickupTime: orderUpdateResult.pickupTime,
                                returnTime: orderUpdateResult.returnTime,
                                createdAt: orderUpdateResult.createdAt,
                                updatedAt: orderUpdateResult.updatedAt,
                            },
                            meta: meta
                        };
                        emitToRooms([`user:${orderUpdateResult.studentId.toString()}`, `order:${orderUpdateResult._id.toString()}`], 'order.updated', orderPayload, meta);
                    } catch (emitErr) {
                        logger.warn('Failed to emit order.updated after accept in JobService:', emitErr);
                    }
                } catch (err) {
                    logger.error(`Failed to update order status to ACCEPTED for orderId=${rawOrderId}:`, err);
                    throw err;
                }
                // Emit job.updated for the accepted job
                try {
                    this.emitJobUpdated(updatedJob, { by: updateData.moverId ?? null, ts: new Date().toISOString() });
                } catch (emitErr) {
                    logger.warn('Failed to emit job.updated after accept:', emitErr);
                }
                
            } else {
                // For non-ACCEPTED statuses, perform a simple update
                updatedJob = await jobModel.update(
                    new mongoose.Types.ObjectId(jobId),
                    updateFields
                );

                logger.info(`Job ${jobId} updated: status=${updateFields.status}`);

                if (!updatedJob) {
                    throw new Error("Job not found");
                }

                // Emit job.updated for the updated job
                try {
                    this.emitJobUpdated(updatedJob, { by: updateData.moverId ?? null, ts: new Date().toISOString() });
                } catch (emitErr) {
                    logger.warn('Failed to emit job.updated after update:', emitErr);
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
                        try {
                            const meta = { by: updatedJob.moverId?.toString() ?? null, ts: new Date().toISOString() };
                            const orderPayload = {
                                event: 'order.updated',
                                order: {
                                    id: orderUpdateResult._id.toString(),
                                    studentId: orderUpdateResult.studentId.toString(),
                                    moverId: orderUpdateResult.moverId?.toString(),
                                    status: orderUpdateResult.status,
                                    volume: orderUpdateResult.volume,
                                    price: orderUpdateResult.price,
                                    studentAddress: orderUpdateResult.studentAddress,
                                    warehouseAddress: orderUpdateResult.warehouseAddress,
                                    returnAddress: orderUpdateResult.returnAddress,
                                    pickupTime: orderUpdateResult.pickupTime,
                                    returnTime: orderUpdateResult.returnTime,
                                    createdAt: orderUpdateResult.createdAt,
                                    updatedAt: orderUpdateResult.updatedAt,
                                },
                                meta: meta
                            };
                            emitToRooms([`user:${orderUpdateResult.studentId.toString()}`, `order:${orderUpdateResult._id.toString()}`], 'order.updated', orderPayload, meta);
                        } catch (emitErr) {
                            logger.warn('Failed to emit order.updated after IN_STORAGE in JobService:', emitErr);
                        }
                    } else if (job.jobType === JobType.RETURN) {
                        const orderUpdateResult = await orderModel.update(new mongoose.Types.ObjectId(rawOrderId), { status: OrderStatus.COMPLETED });
                        logger.info(`Order ${rawOrderId} update result (COMPLETED): ${JSON.stringify(orderUpdateResult)}`);
                        try {
                            const meta = { by: updatedJob.moverId?.toString() ?? null, ts: new Date().toISOString() };
                            const orderPayload = {
                                event: 'order.updated',
                                order: {
                                    id: orderUpdateResult._id.toString(),
                                    studentId: orderUpdateResult.studentId.toString(),
                                    moverId: orderUpdateResult.moverId?.toString(),
                                    status: orderUpdateResult.status,
                                    volume: orderUpdateResult.volume,
                                    price: orderUpdateResult.price,
                                    studentAddress: orderUpdateResult.studentAddress,
                                    warehouseAddress: orderUpdateResult.warehouseAddress,
                                    returnAddress: orderUpdateResult.returnAddress,
                                    pickupTime: orderUpdateResult.pickupTime,
                                    returnTime: orderUpdateResult.returnTime,
                                    createdAt: orderUpdateResult.createdAt,
                                    updatedAt: orderUpdateResult.updatedAt,
                                },
                                meta: meta
                            };
                            emitToRooms([`user:${orderUpdateResult.studentId.toString()}`, `order:${orderUpdateResult._id.toString()}`], 'order.updated', orderPayload, meta);
                        } catch (emitErr) {
                            logger.warn('Failed to emit order.updated after COMPLETED in JobService:', emitErr);
                        }
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
