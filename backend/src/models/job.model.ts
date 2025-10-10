import mongoose, { Schema } from "mongoose";
import { Job, JobStatus, JobType } from "../types/job.type";
import logger from "../utils/logger.util";

// Address subdocument schema (reuse from order model pattern)
const addressSubSchema = new Schema(
  {
    lat: { type: Number, required: true },
    lon: { type: Number, required: true },
    formattedAddress: { type: String, required: true, trim: true },
  },
  { _id: false }
);

// Mongoose Job schema
const jobSchema = new Schema(
  {
    orderId: {
      type: Schema.Types.ObjectId,
      ref: "Order",
      required: true,
    },
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
    jobType: {
      type: String,
      enum: Object.values(JobType),
      required: true,
    },
    status: {
      type: String,
      enum: Object.values(JobStatus),
      default: JobStatus.AVAILABLE,
      required: true,
    },
    volume: {
      type: Number,
      required: true,
    },
    price: {
      type: Number,
      required: true,
    },
    pickupAddress: { type: addressSubSchema, required: true },
    dropoffAddress: { type: addressSubSchema, required: true },
    scheduledTime: { type: Date, required: true },
  },
  {
    timestamps: true,
  }
);

// JobModel class
export class JobModel {
  private job: mongoose.Model<any>;

  constructor() {
    this.job = mongoose.model("Job", jobSchema);
  }

  async create(newJob: Job) {
    try {
      const createdJob = await this.job.create(newJob);
      return createdJob;
    } catch (error) {
      logger.error("Error creating job:", error);
      throw new Error("Failed to create job");
    }
  }

  async findById(jobId: mongoose.Types.ObjectId) {
    try {
      return await this.job.findById(jobId).populate('orderId studentId moverId');
    } catch (error) {
      logger.error("Error finding job:", error);
      throw new Error("Failed to find job");
    }
  }

  async findByOrderId(orderId: mongoose.Types.ObjectId) {
    try {
      return await this.job.find({ orderId }).populate('orderId studentId moverId');
    } catch (error) {
      logger.error("Error finding jobs by order:", error);
      throw new Error("Failed to find jobs");
    }
  }

  async findAvailableJobs() {
    try {
      return await this.job.find({ status: JobStatus.AVAILABLE }).populate('orderId studentId');
    } catch (error) {
      logger.error("Error finding available jobs:", error);
      throw new Error("Failed to find available jobs");
    }
  }

  async findAllJobs() {
    try {
      return await this.job.find({}).populate('orderId studentId moverId');
    } catch (error) {
      logger.error("Error finding all jobs:", error);
      throw new Error("Failed to find all jobs");
    }
  }

  async findByMoverId(moverId: mongoose.Types.ObjectId) {
    try {
      return await this.job.find({ moverId }).populate('orderId studentId');
    } catch (error) {
      logger.error("Error finding mover jobs:", error);
      throw new Error("Failed to find mover jobs");
    }
  }

  async update(jobId: mongoose.Types.ObjectId, updatedJob: Partial<Job>) {
    try {
      return await this.job.findByIdAndUpdate(jobId, updatedJob, { new: true });
    } catch (error) {
      logger.error("Error updating job:", error);
      throw new Error("Failed to update job");
    }
  }
}

export const jobModel = new JobModel();
