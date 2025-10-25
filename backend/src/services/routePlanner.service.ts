import mongoose from "mongoose";
import { jobModel } from "../models/job.model";
import { userModel } from "../models/user.model";
import { Job, JobStatus } from "../types/job.type";
import { DayAvailability, TimeRange } from "../types/user.types";
import { JobInRoute, RouteMetrics } from "../types/route.types";
import logger from "../utils/logger.util";
import { ROUTE_CONFIG } from "../config/route.config";

/**
 * RoutePlannerService - Provides smart route optimization for movers
 * 
 * Uses a greedy algorithm with composite scoring to build optimized job routes
 * that balance earnings potential with travel efficiency.
 */
export class RoutePlannerService {
  /**
   * Calculate the optimal route for a mover based on available jobs and their availability
   * 
   * Algorithm Overview:
   * 1. Filter jobs by mover's availability windows
   * 2. Calculate value score for each job (price/duration)
   * 3. Greedily select next best job based on composite score
   * 4. Update time and location after each job
   * 5. Stop when maxDuration is reached (if specified)
   * 6. Return optimized route with metrics
   * 
   * @param moverId - ID of the mover
   * @param currentLocation - Mover's current location {lat, lon}
   * @param maxDuration - Maximum route duration in minutes (optional)
   * @returns Optimized route with jobs and metrics
   */
  async calculateSmartRoute(
    moverId: string,
    currentLocation: { lat: number; lon: number },
    maxDuration?: number
  ): Promise<{ route: JobInRoute[]; metrics: RouteMetrics; startLocation: { lat: number; lon: number } }> {
    try {
      // Fetch mover's availability
      const mover = await userModel.findById(new mongoose.Types.ObjectId(moverId));
      if (!mover || !mover.availability) {
        logger.warn(`Mover ${moverId} not found or has no availability`);
        return this.emptyRoute(currentLocation);
      }

      // Fetch all available jobs
      const availableJobs = await jobModel.findAvailableJobs();
      if (!availableJobs || availableJobs.length === 0) {
        logger.info("No available jobs found");
        return this.emptyRoute(currentLocation);
      }

      // Step 1: Filter jobs by availability windows
      const eligibleJobs = this.filterJobsByAvailability(
        availableJobs,
        mover.availability
      );

      if (eligibleJobs.length === 0) {
        logger.info("No jobs match mover's availability");
        return this.emptyRoute(currentLocation);
      }

      // Step 2: Calculate value scores for jobs
      const jobsWithValues = this.calculateJobValues(eligibleJobs);

      // Step 3: Build optimal route using greedy algorithm
      const route = this.buildOptimalRoute(
        jobsWithValues,
        currentLocation,
        mover.availability,
        maxDuration
      );

      // Step 4: Calculate route metrics
      const metrics = this.calculateRouteMetrics(route);
      
      logger.info(`Route complete: ${route.length} jobs, ${metrics.totalDuration} minutes total, $${metrics.totalEarnings.toFixed(2)} earnings`);

      return {
        route,
        metrics,
        startLocation: currentLocation,
      };
    } catch (error) {
      logger.error("Error calculating smart route:", error);
      throw new Error("Failed to calculate smart route");
    }
  }

  /**
   * Filter jobs that fall within mover's availability windows
   */
  private filterJobsByAvailability(
    jobs: any[],
    availability: DayAvailability
  ): any[] {
    const filtered = jobs.filter((job) => {
      const scheduledTime = new Date(job.scheduledTime);
      const dayOfWeek = this.convertToDayOfWeek(scheduledTime.getDay());
      const jobTimeString = `${scheduledTime.getHours()}:${scheduledTime.getMinutes().toString().padStart(2, '0')}`;

      // Check if this day has availability
      // Handle both Map and plain object
      const daySlots = availability instanceof Map 
        ? availability.get(dayOfWeek)
        : availability[dayOfWeek];
      
      if (!daySlots || daySlots.length === 0) {
        return false;
      }

      // Check if job time falls within any time slot
      const matches = daySlots.some((slot: TimeRange) => {
        const [startTime, endTime] = slot;
        const jobDuration = this.estimateJobDuration(job.volume);
        
        // Parse times
        const [jobHour, jobMin] = jobTimeString.split(':').map(Number);
        const [startHour, startMin] = startTime.split(':').map(Number);
        const [endHour, endMin] = endTime.split(':').map(Number);

        const jobMinutes = jobHour * 60 + jobMin;
        const startMinutes = startHour * 60 + startMin;
        const endMinutes = endHour * 60 + endMin;
        const jobEndMinutes = jobMinutes + jobDuration;

        // Job must start and end within the slot
        return jobMinutes >= startMinutes && jobEndMinutes <= endMinutes;
      });
      
      return matches;
    });
    
    return filtered;
  }

  /**
   * Calculate value score for each job (earnings per minute)
   */
  private calculateJobValues(jobs: any[]): Array<any & { valueScore: number }> {
    return jobs.map((job) => {
      // Convert Mongoose document to plain object
      const jobObj = job.toObject ? job.toObject() : job;
      
      const duration = this.estimateJobDuration(jobObj.volume);
      const valueScore = jobObj.price / duration; // Earnings per minute
      return { ...jobObj, valueScore };
    });
  }

  /**
   * Estimate job duration based on volume
   */
  private estimateJobDuration(volume: number): number {
    return ROUTE_CONFIG.BASE_JOB_TIME + volume * ROUTE_CONFIG.JOB_DURATION_PER_M3;
  }

  /**
   * Build optimal route respecting scheduled times
   * 
   * Algorithm:
   * 1. Sort jobs by scheduled time (earliest first) - TIME IS THE PRIMARY CONSTRAINT
   * 2. For each iteration, find all feasible jobs (can arrive before scheduled time)
   * 3. Select the earliest feasible job (respects time above all else)
   * 4. Add waiting time if arriving early
   * 5. Stop if maxDuration is reached
   * 
   * This ensures the route is always in chronological order and respects
   * the fixed scheduled times that students have set.
   */
  private buildOptimalRoute(
    jobs: Array<any & { valueScore: number }>,
    startLocation: { lat: number; lon: number },
    availability: DayAvailability,
    maxDuration?: number
  ): JobInRoute[] {
    const route: JobInRoute[] = [];
    let currentLocation = startLocation;
    let currentTime = new Date();
    let totalElapsedTime = 0;
    const remainingJobs = [...jobs];

    // STEP 1: Sort all jobs by scheduled time (earliest first)
    // This ensures we respect time constraints above all else
    remainingJobs.sort((a, b) => {
      const timeA = new Date(a.scheduledTime).getTime();
      const timeB = new Date(b.scheduledTime).getTime();
      return timeA - timeB;
    });

    // Normalize value scores for fair comparison
    const maxValue = Math.max(...jobs.map((j) => j.valueScore));
    const minValue = Math.min(...jobs.map((j) => j.valueScore));
    const valueRange = maxValue - minValue || 1;

    while (remainingJobs.length > 0) {
      // Calculate distances and feasibility for remaining jobs
      const jobsWithDistances = remainingJobs.map((job) => {
        const location = job.pickupLocation || job.pickupAddress;
        if (!location || !location.lat || !location.lon) {
          logger.warn(`Job ${job._id} missing location data`);
          return null;
        }
        
        const distance = this.calculateDistance(currentLocation, location);
        const travelTime = this.estimateTravelTime(distance);
        const arrivalTime = new Date(currentTime.getTime() + travelTime * 60000);
        const scheduledTime = new Date(job.scheduledTime);
        
        // FEASIBILITY CHECK: Can we arrive before the scheduled time?
        const isFeasible = arrivalTime.getTime() <= scheduledTime.getTime();
        
        return {
          ...job,
          distance,
          travelTime,
          arrivalTime,
          scheduledTime,
          isFeasible,
          // Calculate waiting time if we arrive early
          waitingTime: isFeasible ? Math.max(0, (scheduledTime.getTime() - arrivalTime.getTime()) / 60000) : 0
        };
      }).filter(Boolean);

      // STEP 2: Filter to only feasible jobs (can arrive on time)
      const feasibleJobs = jobsWithDistances.filter(j => j.isFeasible);
      
      if (feasibleJobs.length === 0) {
        logger.info(`No more feasible jobs (${jobsWithDistances.length} jobs remaining but can't reach any in time)`);
        break;
      }

      // STEP 3: Select the earliest feasible job
      // Since jobs are already sorted by scheduledTime, feasibleJobs[0] is the earliest
      const selectedJob = feasibleJobs[0];

      // Check if this job fits within maxDuration
      // Note: We only count active time (travel + job), not waiting time
      const jobDuration = this.estimateJobDuration(selectedJob.volume);
      const activeTimeForJob = selectedJob.travelTime + jobDuration;
      
      if (maxDuration && totalElapsedTime + activeTimeForJob > maxDuration) {
        logger.info(`Job ${selectedJob._id} would exceed maxDuration (${totalElapsedTime + activeTimeForJob}min > ${maxDuration}min)`);
        break;
      }

      // Add job to route
      const pickupLoc = selectedJob.pickupLocation || selectedJob.pickupAddress;
      const dropoffLoc = selectedJob.dropoffLocation || selectedJob.dropoffAddress;
      
      logger.info(`Adding job: scheduled=${selectedJob.scheduledTime.toISOString()}, travel=${selectedJob.travelTime.toFixed(1)}min, wait=${selectedJob.waitingTime.toFixed(1)}min, duration=${jobDuration}min`);
      
      route.push({
        jobId: selectedJob._id?.toString() || '',
        orderId: selectedJob.orderId?.toString() || '',
        studentId: selectedJob.studentId?.toString() || '',
        jobType: selectedJob.jobType,
        volume: selectedJob.volume,
        price: selectedJob.price,
        pickupAddress: pickupLoc,
        dropoffAddress: dropoffLoc,
        scheduledTime: selectedJob.scheduledTime,
        estimatedStartTime: selectedJob.scheduledTime.toISOString(), // Use actual scheduled time
        estimatedDuration: Math.round(jobDuration),
        distanceFromPrevious: Math.round(selectedJob.distance * 10) / 10,
        travelTimeFromPrevious: Math.round(selectedJob.travelTime),
      });

      // Update current location and time
      // Current time = scheduled time + job duration (mover finishes the job)
      currentLocation = dropoffLoc;
      currentTime = new Date(selectedJob.scheduledTime.getTime() + jobDuration * 60000);
      totalElapsedTime += activeTimeForJob; // Only count active work time

      // Remove selected job from remaining jobs
      const jobIndex = remainingJobs.findIndex((j) => j._id.equals(selectedJob._id));
      remainingJobs.splice(jobIndex, 1);
    }

    return route;
  }

  /**
   * Sort jobs by composite score (best first)
   * 
   * Score = (normalizedValue × PROXIMITY_WEIGHT) - (normalizedDistance × (1 - PROXIMITY_WEIGHT))
   * 
   * This balances:
   * - Job value (higher earnings per minute = better)
   * - Proximity (shorter distance = better)
   */
  private sortJobsByScore(
    jobs: Array<any & { valueScore: number; distance: number }>,
    valueRange: number,
    minValue: number,
    distanceRange: number,
    minDistance: number
  ): Array<any & { valueScore: number; distance: number }> {
    if (jobs.length === 0) return [];

    // Calculate score for each job
    const jobsWithScores = jobs.map(job => {
      // Normalize value (0-1)
      const normalizedValue = (job.valueScore - minValue) / valueRange;

      // Normalize distance (0-1)
      const normalizedDistance = (job.distance - minDistance) / distanceRange;

      // Composite score: prioritize value, penalize distance
      const score = 
        normalizedValue * ROUTE_CONFIG.PROXIMITY_WEIGHT - 
        normalizedDistance * (1 - ROUTE_CONFIG.PROXIMITY_WEIGHT);

      return { ...job, compositeScore: score };
    });

    // Sort by score descending (best first)
    return jobsWithScores.sort((a, b) => b.compositeScore - a.compositeScore);
  }

  /**
   * Calculate distance between two points using Haversine formula
   * 
   * @returns Distance in kilometers
   */
  private calculateDistance(
    from: { lat: number; lon: number },
    to: { lat: number; lon: number }
  ): number {
    const R = 6371; // Earth's radius in km
    const dLat = this.toRadians(to.lat - from.lat);
    const dLon = this.toRadians(to.lon - from.lon);

    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(this.toRadians(from.lat)) *
        Math.cos(this.toRadians(to.lat)) *
        Math.sin(dLon / 2) *
        Math.sin(dLon / 2);

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  /**
   * Estimate travel time based on distance
   * 
   * @returns Travel time in minutes
   */
  private estimateTravelTime(distanceKm: number): number {
    return (distanceKm / ROUTE_CONFIG.AVERAGE_SPEED_KMH) * 60;
  }

  /**
   * Calculate aggregate metrics for the route
   */
  private calculateRouteMetrics(route: JobInRoute[]): RouteMetrics {
    if (route.length === 0) {
      return {
        totalEarnings: 0,
        totalJobs: 0,
        totalDistance: 0,
        totalDuration: 0,
        earningsPerHour: 0,
      };
    }

    const totalEarnings = route.reduce((sum, job) => sum + job.price, 0);
    const totalDistance = route.reduce((sum, job) => sum + job.distanceFromPrevious, 0);
    const totalDuration = route.reduce(
      (sum, job) => sum + job.estimatedDuration + job.travelTimeFromPrevious,
      0
    );
    const earningsPerHour = totalDuration > 0 ? (totalEarnings / totalDuration) * 60 : 0;

    return {
      totalEarnings: Math.round(totalEarnings * 100) / 100,
      totalJobs: route.length,
      totalDistance: Math.round(totalDistance * 100) / 100,
      totalDuration: Math.round(totalDuration),
      earningsPerHour: Math.round(earningsPerHour * 100) / 100,
    };
  }

  /**
   * Helper: Convert numeric day (0-6) to three-letter day string
   */
  private convertToDayOfWeek(day: number): string {
    const days = ["SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"];
    return days[day];
  }

  /**
   * Helper: Convert degrees to radians
   */
  private toRadians(degrees: number): number {
    return degrees * (Math.PI / 180);
  }

  /**
   * Helper: Return empty route structure
   */
  private emptyRoute(location: { lat: number; lon: number }) {
    return {
      route: [],
      metrics: {
        totalEarnings: 0,
        totalJobs: 0,
        totalDistance: 0,
        totalDuration: 0,
        earningsPerHour: 0,
      },
      startLocation: location,
    };
  }
}

export const routePlannerService = new RoutePlannerService();
