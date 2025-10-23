import mongoose from "mongoose";
import { jobModel } from "../models/job.model";
import { userModel } from "../models/user.model";
import { 
    CreateJobRequest, 
    CreateJobResponse, 
    Job, 
    JobStatus, 
    JobType,
    GetAllJobsResponse,
    GetJobResponse,
    UpdateJobStatusRequest,
    JobResponse,
    GetMoverJobsResponse
} from "../types/job.type";
import { notificationService } from "./notification.service";
import { Address, OrderStatus } from "../types/order.types";
import logger from "../utils/logger.util";
import { EventEmitter } from "../utils/eventEmitter.util";
import { JobMapper } from "../mappers/job.mapper";
import { PRICING } from "../config/pricing.config";
import { extractObjectId, extractObjectIdString } from "../utils/mongoose.util";
import { 
    JobNotFoundError,
    InternalServerError
} from "../utils/errors.util";

export class JobService {
    // Lazy-load orderService to avoid circular dependency at module load time
    private get orderService() {
        // Import here instead of at the top to break circular dependency
        return require("./order.service").orderService;
    }
    // Helper to add credits to mover when job is completed
    private async addCreditsToMover(job: any) {
        if (!job.moverId) {
            logger.warn('No mover assigned to job, skipping credits');
            return;
        }

        try {
            // Extract moverId using utility
            const moverObjectId = extractObjectId(job.moverId);
            if (!moverObjectId) {
                logger.warn('Invalid moverId, skipping credits');
                return;
            }
            
            const mover = await userModel.findById(moverObjectId);
            
            if (mover && mover.userRole === 'MOVER') {
                const currentCredits = mover.credits || 0;
                const newCredits = currentCredits + job.price;
                await userModel.update(moverObjectId, { credits: newCredits });
                logger.info(`Added ${job.price} credits to mover ${moverObjectId}. New balance: ${newCredits}`);
            } else {
                logger.warn(`Mover ${moverObjectId} not found or not a MOVER role`);
            }
        } catch (creditErr) {
            logger.error('Failed to add credits to mover:', creditErr);
            // Don't fail the job completion if credit update fails
        }
    }

    // Cancel (mark as CANCELLED) all jobs for a given orderId that are not already terminal
    async cancelJobsForOrder(orderId: string, actorId?: string) {
        // Input validation
        if (!orderId) {
            logger.error('cancelJobsForOrder: Missing orderId');
            throw new Error('orderId is required');
        }
        
        try {
            logger.info(`cancelJobsForOrder: orderId=${orderId}, actorId=${actorId || 'system'}`);
            const foundJobs: any[] = await jobModel.findByOrderId(new mongoose.Types.ObjectId(orderId));
            const toCancel = foundJobs.filter(j => j.status !== JobStatus.COMPLETED && j.status !== JobStatus.CANCELLED);

            const results: Array<{ jobId: string; prevStatus: string; newStatus: string; moverId?: string }> = [];

            for (const jobDoc of toCancel) {
                try {
                    const updatedJob = await jobModel.update(jobDoc._id, { status: JobStatus.CANCELLED, updatedAt: new Date() });
                    results.push({ jobId: updatedJob._id.toString(), prevStatus: jobDoc.status, newStatus: updatedJob.status, moverId: updatedJob.moverId?.toString() });

                    // Emit job.updated for each cancelled job
                    EventEmitter.emitJobUpdated(updatedJob, { by: actorId ?? null, ts: new Date().toISOString() });
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
        // Input validation
        if (!reqData.orderId || !reqData.studentId) {
            logger.error('createJob: Missing required IDs', { orderId: reqData.orderId, studentId: reqData.studentId });
            throw new Error('orderId and studentId are required');
        }
        
        if (!reqData.volume || reqData.volume <= 0) {
            logger.error('createJob: Invalid volume', { volume: reqData.volume });
            throw new Error('volume must be greater than 0');
        }
        
        if (!reqData.price || reqData.price <= 0) {
            logger.error('createJob: Invalid price', { price: reqData.price });
            throw new Error('price must be greater than 0');
        }
        
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
            EventEmitter.emitJobCreated(createdJob, { by: reqData.studentId ?? null, ts: new Date().toISOString() });

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
                price: totalPrice * PRICING.STORAGE_JOB_SPLIT,
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
                price: totalPrice * PRICING.RETURN_JOB_SPLIT,
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
            return {
                message: "All jobs retrieved successfully",
                data: { jobs: JobMapper.toJobListItems(jobs) },
            };
        } catch (error) {
            logger.error("Error in getAllJobs service:", error);
            throw new InternalServerError("Failed to get all jobs", error as Error);
        }
    }

    async getAllAvailableJobs(): Promise<GetAllJobsResponse> {
        try {
            const jobs = await jobModel.findAvailableJobs();
            return {
                message: "Available jobs retrieved successfully",
                data: { jobs: JobMapper.toJobListItems(jobs) },
            };
        } catch (error) {
            logger.error("Error in getAllAvailableJobs service:", error);
            throw new InternalServerError("Failed to get available jobs", error as Error);
        }
    }

    async getMoverJobs(moverId: string): Promise<GetMoverJobsResponse> {
        try {
            const jobs = await jobModel.findByMoverId(new mongoose.Types.ObjectId(moverId));
            return {
                message: "Mover jobs retrieved successfully",
                data: { jobs: JobMapper.toJobListItems(jobs) },
            };
        } catch (error) {
            logger.error("Error in getMoverJobs service:", error);
            throw new InternalServerError("Failed to get mover jobs", error as Error);
        }
    }

    async getStudentJobs(studentId: string): Promise<GetMoverJobsResponse> {
        try {
            const jobs = await jobModel.findByStudentId(new mongoose.Types.ObjectId(studentId));
            return {
                message: "Student jobs retrieved successfully",
                data: { jobs: JobMapper.toJobListItems(jobs) },
            };
        } catch (error) {
            logger.error("Error in getStudentJobs service:", error);
            throw new InternalServerError("Failed to get student jobs", error as Error);
        }
    }

    async getJobById(jobId: string): Promise<GetJobResponse> {
        try {
            const job = await jobModel.findById(new mongoose.Types.ObjectId(jobId));
            
            if (!job) {
                throw new JobNotFoundError(jobId);
            }

            return {
                message: "Job retrieved successfully",
                data: { job: JobMapper.toJobResponse(job) },
            };
        } catch (error) {
            logger.error("Error in getJobById service:", error);
            if (error instanceof JobNotFoundError) throw error;
            throw new InternalServerError("Failed to get job", error as Error);
        }
    }

    async updateJobStatus(jobId: string, updateData: UpdateJobStatusRequest): Promise<JobResponse> {
        // Input validation
        if (!jobId) {
            logger.error('updateJobStatus: Missing jobId');
            throw new Error('jobId is required');
        }
        
        if (!updateData.status) {
            logger.error('updateJobStatus: Missing status', { jobId });
            throw new Error('status is required');
        }
        
        try {
            logger.info(`updateJobStatus: jobId=${jobId}, status=${updateData.status}, moverId=${updateData.moverId || 'none'}`);
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

                // Extract orderId using utility
                const orderObjectId = extractObjectId(updatedJob.orderId);
                if (!orderObjectId) {
                    logger.error(`Invalid orderId in job ${jobId}`);
                    throw new Error("Invalid orderId in job");
                }
                
                logger.info(`Attempting to update linked order status to ACCEPTED for orderId=${orderObjectId}`);
                try {
                    // Use orderService instead of direct orderModel access
                    await this.orderService.updateOrderStatus(orderObjectId, OrderStatus.ACCEPTED, updateData.moverId ?? undefined);
                    logger.info(`Order ${orderObjectId} updated to ACCEPTED via OrderService`);
                } catch (err) {
                    logger.error(`Failed to update order status to ACCEPTED for orderId=${orderObjectId}:`, err);
                    throw err;
                }
                // Emit job.updated for the accepted job
                try {
                    EventEmitter.emitJobUpdated(updatedJob, { by: updateData.moverId ?? null, ts: new Date().toISOString() });
                } catch (emitErr) {
                    logger.warn('Failed to emit job.updated after accept:', emitErr);
                }
                
                // Send notification to student that their job has been accepted
                await notificationService.sendJobStatusNotification(new mongoose.Types.ObjectId(jobId), JobStatus.ACCEPTED);
                
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
                    EventEmitter.emitJobUpdated(updatedJob, { by: updateData.moverId ?? null, ts: new Date().toISOString() });
                } catch (emitErr) {
                    logger.warn('Failed to emit job.updated after update:', emitErr);
                }
                
            }

            // If job is completed, update order status
            if (updateData.status === JobStatus.COMPLETED) {
                const job = await jobModel.findById(new mongoose.Types.ObjectId(jobId));
                if (!job) {
                    throw new JobNotFoundError(jobId);
                }
                
                logger.debug(`Found job for COMPLETED flow: ${JSON.stringify(job)}`);
                
                // Extract orderId using utility
                const orderObjectId = extractObjectId(job.orderId);
                if (!orderObjectId) {
                    logger.error(`Invalid orderId in job ${jobId}`);
                    throw new Error("Invalid orderId in job");
                }
                
                logger.info(`Attempting to update linked order status after job completion for orderId=${orderObjectId}`);
                
                // Add credits to mover when job is completed
                await this.addCreditsToMover(updatedJob);
                
                try {
                    if (job.jobType === JobType.STORAGE) {
                        await this.orderService.updateOrderStatus(orderObjectId, OrderStatus.IN_STORAGE, extractObjectIdString(updatedJob.moverId));
                        // notfication should not depend on socket emission success so its called after db update
                        await notificationService.sendJobStatusNotification(new mongoose.Types.ObjectId(jobId), JobStatus.COMPLETED);
                        logger.info(`Order ${orderObjectId} updated to IN_STORAGE via OrderService`);
                    } else if (job.jobType === JobType.RETURN) {
                        // For RETURN jobs, mark order as RETURNED (not COMPLETED yet)
                        // Student will need to confirm delivery before order is COMPLETED
                        await this.orderService.updateOrderStatus(orderObjectId, OrderStatus.RETURNED, extractObjectIdString(updatedJob.moverId));
                        await notificationService.sendJobStatusNotification(new mongoose.Types.ObjectId(jobId), JobStatus.COMPLETED);
                        logger.info(`Order ${orderObjectId} updated to RETURNED via OrderService`);
                    }
                } catch (err) {
                    logger.error(`Failed to update order status after job completion for orderId=${orderObjectId}:`, err);
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

    // Mover requests student confirmation when arrived at pickup (storage jobs only)
    async requestPickupConfirmation(jobId: string, moverId: string) {
        // Input validation
        if (!jobId || !moverId) {
            logger.error('requestPickupConfirmation: Missing required parameters', { jobId, moverId });
            throw new Error('jobId and moverId are required');
        }
        
        try {
            logger.info(`requestPickupConfirmation: jobId=${jobId}, moverId=${moverId}`);
            const job = await jobModel.findById(new mongoose.Types.ObjectId(jobId));
            if (!job) throw new JobNotFoundError(jobId);
            if (job.jobType !== JobType.STORAGE) throw new Error('Arrival confirmation only valid for storage jobs');
            
            // Extract and validate moverId
            const jobMoverIdStr = extractObjectIdString(job.moverId);
            if (!jobMoverIdStr || jobMoverIdStr !== moverId) {
                throw new Error('Only assigned mover can request confirmation');
            }
            
            if (job.status !== JobStatus.ACCEPTED) throw new Error('Job must be ACCEPTED to request confirmation');

            const updatedJob = await jobModel.update(job._id, { status: JobStatus.AWAITING_STUDENT_CONFIRMATION, verificationRequestedAt: new Date(), updatedAt: new Date() });

            await notificationService.sendJobStatusNotification(new mongoose.Types.ObjectId(jobId), JobStatus.AWAITING_STUDENT_CONFIRMATION);

            // Emit job.updated targeted to student and order room
            try {
                EventEmitter.emitJobUpdated(updatedJob, { by: moverId, ts: new Date().toISOString() });
            } catch (emitErr) {
                logger.warn('Failed to emit job.updated after requestPickupConfirmation:', emitErr);
            }

            return { id: updatedJob._id.toString(), status: updatedJob.status };
        } catch (err) {
            logger.error('Error in requestPickupConfirmation:', err);
            throw err;
        }
    }

    // Student confirms the mover has the items (moves to PICKED_UP and updates order)
    async confirmPickup(jobId: string, studentId: string) {
        // Input validation
        if (!jobId || !studentId) {
            logger.error('confirmPickup: Missing required parameters', { jobId, studentId });
            throw new Error('jobId and studentId are required');
        }
        
        try {
            logger.info(`confirmPickup: jobId=${jobId}, studentId=${studentId}`);
            const job = await jobModel.findById(new mongoose.Types.ObjectId(jobId));
            if (!job) throw new JobNotFoundError(jobId);
            if (job.jobType !== JobType.STORAGE) throw new Error('Confirm pickup only valid for storage jobs');
            
            // Extract and validate studentId
            const jobStudentIdStr = extractObjectIdString(job.studentId);
            logger.info(`confirmPickup: jobId=${jobId}, jobStudentId=${jobStudentIdStr}, requestStudentId=${studentId}`);
            
            if (!jobStudentIdStr || jobStudentIdStr !== studentId) {
                throw new Error('Only the student can confirm pickup');
            }
            
            if (job.status !== JobStatus.AWAITING_STUDENT_CONFIRMATION) throw new Error('Job must be awaiting student confirmation');

            const updatedJob = await jobModel.update(job._id, { status: JobStatus.PICKED_UP, updatedAt: new Date() });

            // Update order status to PICKED_UP
            try {
                const orderObjectId = extractObjectId(updatedJob.orderId);
                if (!orderObjectId) {
                    throw new Error('Invalid orderId in job');
                }
                
                await this.orderService.updateOrderStatus(orderObjectId, OrderStatus.PICKED_UP, studentId);
                logger.info(`Order ${orderObjectId} updated to PICKED_UP via OrderService`);
            } catch (err) {
                logger.error('Failed to update order status during confirmPickup:', err);
                throw err;
            }

            // Emit job.updated for the picked up job
            try {
                EventEmitter.emitJobUpdated(updatedJob, { by: studentId ?? null, ts: new Date().toISOString() });
            } catch (emitErr) {
                logger.warn('Failed to emit job.updated after confirmPickup:', emitErr);
            }

            return { id: updatedJob._id.toString(), status: updatedJob.status };
        } catch (err) {
            logger.error('Error in confirmPickup:', err);
            throw err;
        }
    }

    // Mover requests student confirmation when delivered items (return jobs only)
    async requestDeliveryConfirmation(jobId: string, moverId: string) {
        // Input validation
        if (!jobId || !moverId) {
            logger.error('requestDeliveryConfirmation: Missing required parameters', { jobId, moverId });
            throw new Error('jobId and moverId are required');
        }
        
        try {
            logger.info(`requestDeliveryConfirmation: jobId=${jobId}, moverId=${moverId}`);
            const job = await jobModel.findById(new mongoose.Types.ObjectId(jobId));
            if (!job) throw new JobNotFoundError(jobId);
            if (job.jobType !== JobType.RETURN) throw new Error('Delivery confirmation only valid for return jobs');
            
            // Extract and validate moverId
            const jobMoverIdStr = extractObjectIdString(job.moverId);
            if (!jobMoverIdStr || jobMoverIdStr !== moverId) {
                throw new Error('Only assigned mover can request confirmation');
            }
            
            if (job.status !== JobStatus.PICKED_UP) throw new Error('Job must be PICKED_UP (since its a return job) to request confirmation');

            const updatedJob = await jobModel.update(job._id, { status: JobStatus.AWAITING_STUDENT_CONFIRMATION, verificationRequestedAt: new Date(), updatedAt: new Date() });

            await notificationService.sendJobStatusNotification(new mongoose.Types.ObjectId(jobId), JobStatus.AWAITING_STUDENT_CONFIRMATION);

            // Emit job.updated targeted to student and order room
            try {
                EventEmitter.emitJobUpdated(updatedJob, { by: moverId, ts: new Date().toISOString() });
            } catch (emitErr) {
                logger.warn('Failed to emit job.updated after requestDeliveryConfirmation:', emitErr);
            }

            return { id: updatedJob._id.toString(), status: updatedJob.status };
        } catch (err) {
            logger.error('Error in requestDeliveryConfirmation:', err);
            throw err;
        }
    }

    // Student confirms the mover delivered the items (moves job to COMPLETED and order to COMPLETED)
    async confirmDelivery(jobId: string, studentId: string) {
        // Input validation
        if (!jobId || !studentId) {
            logger.error('confirmDelivery: Missing required parameters', { jobId, studentId });
            throw new Error('jobId and studentId are required');
        }
        
        try {
            logger.info(`confirmDelivery: jobId=${jobId}, studentId=${studentId}`);
            const job = await jobModel.findById(new mongoose.Types.ObjectId(jobId));
            if (!job) throw new JobNotFoundError(jobId);
            if (job.jobType !== JobType.RETURN) throw new Error('Confirm delivery only valid for return jobs');
            
            // Extract and validate studentId
            const jobStudentIdStr = extractObjectIdString(job.studentId);
            logger.info(`confirmDelivery: jobId=${jobId}, jobStudentId=${jobStudentIdStr}, requestStudentId=${studentId}`);
            
            if (!jobStudentIdStr || jobStudentIdStr !== studentId) {
                throw new Error('Only the student can confirm delivery');
            }
            
            if (job.status !== JobStatus.AWAITING_STUDENT_CONFIRMATION) throw new Error('Job must be awaiting student confirmation');

            const updatedJob = await jobModel.update(job._id, { status: JobStatus.COMPLETED, updatedAt: new Date() });

            // Add credits to mover when job is completed
            await this.addCreditsToMover(updatedJob);

            // Update order status to COMPLETED
            try {
                const orderObjectId = extractObjectId(updatedJob.orderId);
                if (!orderObjectId) {
                    throw new Error('Invalid orderId in job');
                }
                
                await this.orderService.updateOrderStatus(orderObjectId, OrderStatus.COMPLETED, studentId);
                logger.info(`Order ${orderObjectId} updated to COMPLETED via OrderService`);
            } catch (err) {
                logger.error('Failed to update order status during confirmDelivery:', err);
                throw err;
            }

            // Emit job.updated for the completed job
            try {
                EventEmitter.emitJobUpdated(updatedJob, { by: studentId ?? null, ts: new Date().toISOString() });
            } catch (emitErr) {
                logger.warn('Failed to emit job.updated after confirmDelivery:', emitErr);
            }

            return { id: updatedJob._id.toString(), status: updatedJob.status };
        } catch (err) {
            logger.error('Error in confirmDelivery:', err);
            throw err;
        }
    }
}

export const jobService = new JobService();
