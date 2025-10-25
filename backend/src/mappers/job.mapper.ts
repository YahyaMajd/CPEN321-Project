import { Job, JobListItem, JobResponse } from "../types/job.type";

/**
 * JobMapper - Centralized data transformation for Job entities
 * Eliminates duplicate mapping logic across services
 */
export class JobMapper {
  /**
   * Convert Job to JobListItem (for list views)
   */
  static toJobListItem(job: any): JobListItem {
    // orderId may be either an ObjectId or a populated Order document.
    // If populated, extract the nested _id; otherwise use the value's toString().
    const orderId = job.orderId && (job.orderId as any)._id
      ? (job.orderId as any)._id.toString()
      : job.orderId?.toString?.();

    return {
      id: job._id.toString(),
      orderId: orderId,
      jobType: job.jobType,
      volume: job.volume,
      price: job.price,
      pickupAddress: job.pickupAddress,
      dropoffAddress: job.dropoffAddress,
      scheduledTime: job.scheduledTime,
      status: job.status,
    };
  }

  /**
   * Convert Job to JobResponse (for detailed view)
   */
  static toJobResponse(job: any): JobResponse {
    const orderId = job.orderId && (job.orderId as any)._id
      ? (job.orderId as any)._id.toString()
      : job.orderId?.toString?.();

    return {
      id: job._id.toString(),
      orderId: orderId,
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
  }

  /**
   * Convert array of Jobs to JobListItems
   */
  static toJobListItems(jobs: any[]): JobListItem[] {
    return jobs.map(job => this.toJobListItem(job));
  }
}
