import mongoose from "mongoose";
import { jobModel } from "../models/job.model";
import { userModel } from "../models/user.model";
import { Job, JobStatus } from "../types/job.type";
import { DayAvailability, TimeRange } from "../types/user.types";
import { JobInRoute, RouteMetrics } from "../types/route.types";
import logger from "../utils/logger.util";

// Algorithm Configuration
// ------------------------------------------------------------
const AVERAGE_SPEED_KMH = 40; // Average driving speed in Greater Vancouver
const JOB_DURATION_PER_M3 = 15; // Base time per cubic meter (minutes)
const BASE_JOB_TIME = 30; // Minimum job duration (minutes)
const PROXIMITY_WEIGHT = 0.7; // Weight for distance in scoring (0-1)

/**
 * RouteService - Provides smart route optimization for movers
 * 
 * Uses a greedy algorithm with composite scoring to build optimized job routes
 * that balance earnings potential with travel efficiency.
 */
export class RouteService {
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
    return BASE_JOB_TIME + volume * JOB_DURATION_PER_M3;
  }

  /**
   * Build optimal route using greedy algorithm with composite scoring
   * 
   * For each iteration:
   * 1. Calculate composite score for remaining jobs
   * 2. Select job with highest score (balances value and proximity)
   * 3. Update current time and location
   * 4. Stop if maxDuration is reached
   * 5. Repeat until no more jobs fit
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
    let totalElapsedTime = 0; // Track total time in minutes
    const remainingJobs = [...jobs];

    // Normalize value scores for fair comparison
    const maxValue = Math.max(...jobs.map((j) => j.valueScore));
    const minValue = Math.min(...jobs.map((j) => j.valueScore));
    const valueRange = maxValue - minValue || 1;

    while (remainingJobs.length > 0) {
      // Calculate distances for remaining jobs
      const jobsWithDistances = remainingJobs.map((job) => {
        // Use pickupLocation instead of pickupAddress
        const location = job.pickupLocation || job.pickupAddress;
        if (!location || !location.lat || !location.lon) {
          logger.warn(`Job ${job._id} missing location data`);
          return null;
        }
        
        return {
          ...job,
          distance: this.calculateDistance(currentLocation, location),
        };
      }).filter(Boolean); // Remove null entries

      // Normalize distances
      const maxDistance = Math.max(...jobsWithDistances.map((j) => j.distance));
      const minDistance = Math.min(...jobsWithDistances.map((j) => j.distance));
      const distanceRange = maxDistance - minDistance || 1;

      // Select jobs sorted by composite score
      const sortedJobs = this.sortJobsByScore(
        jobsWithDistances,
        valueRange,
        minValue,
        distanceRange,
        minDistance
      );

      // Try jobs in order until we find one that fits within maxDuration
      let selectedJob = null;
      logger.info(`Trying to find job that fits. Current elapsed: ${totalElapsedTime}min, maxDuration: ${maxDuration}min, ${sortedJobs.length} jobs available`);
      
      for (const job of sortedJobs) {
        const travelTime = this.estimateTravelTime(job.distance);
        const jobDuration = this.estimateJobDuration(job.volume);
        const totalTime = totalElapsedTime + travelTime + jobDuration;
        
        logger.info(`  Job ${job._id}: travel=${travelTime.toFixed(1)}min, duration=${jobDuration}min, total=${totalTime.toFixed(1)}min, fits=${!maxDuration || totalTime <= maxDuration}`);
        
        // Check if this job fits within maxDuration
        if (!maxDuration || totalElapsedTime + travelTime + jobDuration <= maxDuration) {
          selectedJob = job;
          logger.info(`  ✓ Selected this job`);
          break;
        }
      }

      // If no job fits, stop building route
      if (!selectedJob) {
        logger.info(`No more jobs fit within maxDuration of ${maxDuration} minutes (current elapsed: ${totalElapsedTime} min)`);
        break;
      }

      // Calculate travel time and job duration for selected job
      const travelTime = this.estimateTravelTime(selectedJob.distance);
      const jobDuration = this.estimateJobDuration(selectedJob.volume);
      const estimatedStartTime = new Date(currentTime.getTime() + travelTime * 60000);

      // Add job to route
      const pickupLoc = selectedJob.pickupLocation || selectedJob.pickupAddress;
      const dropoffLoc = selectedJob.dropoffLocation || selectedJob.dropoffAddress;
      
      logger.info(`Adding job to route: distance=${selectedJob.distance?.toFixed(2)}km, travelTime=${travelTime.toFixed(1)}min, duration=${jobDuration}min`);
      
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
        estimatedStartTime: estimatedStartTime.toISOString(),
        estimatedDuration: Math.round(jobDuration),
        distanceFromPrevious: Math.round(selectedJob.distance * 10) / 10, // Round to 1 decimal
        travelTimeFromPrevious: Math.round(travelTime),
      });

      // Update current location and time
      currentLocation = dropoffLoc;
      currentTime = new Date(estimatedStartTime.getTime() + jobDuration * 60000);
      totalElapsedTime += travelTime + jobDuration;

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
        normalizedValue * PROXIMITY_WEIGHT - 
        normalizedDistance * (1 - PROXIMITY_WEIGHT);

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
    return (distanceKm / AVERAGE_SPEED_KMH) * 60;
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

export const routeService = new RouteService();
