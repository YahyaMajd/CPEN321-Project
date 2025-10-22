/**
 * Seed Test Jobs Script
 * 
 * This script populates the database with test jobs for testing the Smart Route algorithm.
 * 
 * Jobs created:
 * - Close jobs (within 5km of downtown Vancouver)
 * - Far jobs (10-20km from downtown)
 * - High price jobs ($150-$250)
 * - Low price jobs ($50-$100)
 * - Various volumes (2-10 m³)
 * - Mix of STORAGE and RETURN types
 * 
 * All jobs scheduled for tomorrow within typical working hours.
 * 
 * Usage:
 *   npm run seed-jobs
 * or
 *   ts-node scripts/seed-test-jobs.ts
 */

import mongoose from 'mongoose';
import dotenv from 'dotenv';
import { JobType, JobStatus } from '../src/types/job.type';

dotenv.config();

// Define Job schema (same as in job.model.ts)
const addressSubSchema = new mongoose.Schema(
  {
    lat: { type: Number, required: true },
    lon: { type: Number, required: true },
    formattedAddress: { type: String, required: true, trim: true },
  },
  { _id: false }
);

const jobSchema = new mongoose.Schema(
  {
    orderId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Order",
      required: true,
    },
    studentId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
    },
    moverId: {
      type: mongoose.Schema.Types.ObjectId,
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
    verificationRequestedAt: { type: Date, required: false },
  },
  {
    timestamps: true,
  }
);

// Vancouver Downtown coordinates (reference point)
const VANCOUVER_DOWNTOWN = { lat: 49.2827, lon: -123.1207 };

// UBC Warehouse (common dropoff for STORAGE jobs)
const UBC_WAREHOUSE = {
  lat: 49.2606,
  lon: -123.2460,
  formattedAddress: "UBC Warehouse, 2329 West Mall, Vancouver, BC V6T 1Z4"
};

// Test locations around Greater Vancouver
const TEST_LOCATIONS = [
  // CLOSE locations (2-5 km from downtown)
  {
    lat: 49.2827,
    lon: -123.1207,
    formattedAddress: "789 Granville St, Vancouver, BC V6Z 1K3",
    distance: "downtown"
  },
  {
    lat: 49.2819,
    lon: -123.1086,
    formattedAddress: "1234 Robson St, Vancouver, BC V6E 1B5",
    distance: "close"
  },
  {
    lat: 49.2734,
    lon: -123.1022,
    formattedAddress: "456 Davie St, Vancouver, BC V6B 2G3",
    distance: "close"
  },
  {
    lat: 49.2642,
    lon: -123.1387,
    formattedAddress: "2525 Main St, Vancouver, BC V5T 3E2",
    distance: "close"
  },
  {
    lat: 49.2497,
    lon: -123.1193,
    formattedAddress: "5678 Cambie St, Vancouver, BC V5Z 2Z8",
    distance: "close"
  },
  
  // MEDIUM distance locations (5-10 km)
  {
    lat: 49.2488,
    lon: -123.0569,
    formattedAddress: "3456 Fraser St, Vancouver, BC V5V 4C4",
    distance: "medium"
  },
  {
    lat: 49.2145,
    lon: -123.1034,
    formattedAddress: "7890 Oak St, Vancouver, BC V6P 4A5",
    distance: "medium"
  },
  {
    lat: 49.2631,
    lon: -123.2489,
    formattedAddress: "4567 Wesbrook Mall, Vancouver, BC V6T 2A1",
    distance: "medium"
  },
  
  // FAR locations (10-20 km)
  {
    lat: 49.2069,
    lon: -122.9115,
    formattedAddress: "8901 Kingsway, Burnaby, BC V5H 2E6",
    distance: "far"
  },
  {
    lat: 49.3200,
    lon: -123.0716,
    formattedAddress: "1234 Lonsdale Ave, North Vancouver, BC V7M 2H6",
    distance: "far"
  },
  {
    lat: 49.1666,
    lon: -123.1336,
    formattedAddress: "5678 No 3 Rd, Richmond, BC V6X 2C1",
    distance: "far"
  },
  {
    lat: 49.2781,
    lon: -122.9199,
    formattedAddress: "9012 Hastings St, Burnaby, BC V5B 1R5",
    distance: "far"
  },
];

// Generate tomorrow's date at various times
function getTomorrowDate(hour: number, minute: number = 0): Date {
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  tomorrow.setHours(hour, minute, 0, 0);
  return tomorrow;
}

async function createTestJobs() {
  try {
    console.log('🌱 Starting job seeding...');
    
    // Connect to MongoDB
    const uri = process.env.MONGODB_URI!;
    await mongoose.connect(uri);
    console.log('✅ Connected to MongoDB');

    // Get a test student and order (we'll create dummy ones if needed)
    const testStudentId = new mongoose.Types.ObjectId();
    const testOrderId = new mongoose.Types.ObjectId();

    console.log('📝 Test Student ID:', testStudentId.toString());
    console.log('📝 Test Order ID:', testOrderId.toString());

    // Clear existing AVAILABLE jobs (optional - uncomment if needed)
    // await jobModel.deleteMany({ status: JobStatus.AVAILABLE });
    // console.log('🗑️  Cleared existing available jobs');

    const jobs = [];

    // === QUICK JOBS (30-45 min, good for filling gaps) ===
    
    // Job 1: Quick downtown pickup, small volume (30 min total)
    jobs.push({
      orderId: new mongoose.Types.ObjectId(),
      studentId: testStudentId,
      jobType: JobType.STORAGE,
      status: JobStatus.AVAILABLE,
      volume: 2.0, // 30min base + 2*15 = 60 min total job time
      price: 80.00,
      pickupAddress: TEST_LOCATIONS[0], // Downtown
      dropoffAddress: UBC_WAREHOUSE,
      scheduledTime: getTomorrowDate(9, 0).toISOString(),
    });

    // Job 2: Quick Robson St pickup, tiny volume (30 min)
    jobs.push({
      orderId: new mongoose.Types.ObjectId(),
      studentId: testStudentId,
      jobType: JobType.STORAGE,
      status: JobStatus.AVAILABLE,
      volume: 1.5,
      price: 60.00,
      pickupAddress: TEST_LOCATIONS[1], // Robson St
      dropoffAddress: UBC_WAREHOUSE,
      scheduledTime: getTomorrowDate(9, 30).toISOString(),
    });

    // Job 3: Quick return job (30 min)
    jobs.push({
      orderId: new mongoose.Types.ObjectId(),
      studentId: testStudentId,
      jobType: JobType.RETURN,
      status: JobStatus.AVAILABLE,
      volume: 2.0,
      price: 75.00,
      pickupAddress: UBC_WAREHOUSE,
      dropoffAddress: TEST_LOCATIONS[2], // Davie St
      scheduledTime: getTomorrowDate(10, 0).toISOString(),
    });

    // === MEDIUM JOBS (60-90 min) ===
    
    // Job 4: Medium Main St pickup (75 min)
    jobs.push({
      orderId: new mongoose.Types.ObjectId(),
      studentId: testStudentId,
      jobType: JobType.STORAGE,
      status: JobStatus.AVAILABLE,
      volume: 3.0, // 30 + 45 = 75min
      price: 120.00,
      pickupAddress: TEST_LOCATIONS[3], // Main St
      dropoffAddress: UBC_WAREHOUSE,
      scheduledTime: getTomorrowDate(10, 30).toISOString(),
    });

    // Job 5: Medium Cambie pickup (90 min)
    jobs.push({
      orderId: new mongoose.Types.ObjectId(),
      studentId: testStudentId,
      jobType: JobType.STORAGE,
      status: JobStatus.AVAILABLE,
      volume: 4.0, // 30 + 60 = 90min
      price: 140.00,
      pickupAddress: TEST_LOCATIONS[4], // Cambie St
      dropoffAddress: UBC_WAREHOUSE,
      scheduledTime: getTomorrowDate(11, 0).toISOString(),
    });

    // Job 6: Medium Fraser St (60 min)
    jobs.push({
      orderId: new mongoose.Types.ObjectId(),
      studentId: testStudentId,
      jobType: JobType.STORAGE,
      status: JobStatus.AVAILABLE,
      volume: 2.0,
      price: 90.00,
      pickupAddress: TEST_LOCATIONS[5], // Fraser St
      dropoffAddress: UBC_WAREHOUSE,
      scheduledTime: getTomorrowDate(11, 30).toISOString(),
    });

    // === LONGER JOBS (120+ min, high value) ===
    
    // Job 7: Large Oak St job (120 min)
    jobs.push({
      orderId: new mongoose.Types.ObjectId(),
      studentId: testStudentId,
      jobType: JobType.RETURN,
      status: JobStatus.AVAILABLE,
      volume: 6.0, // 30 + 90 = 120min
      price: 180.00,
      pickupAddress: UBC_WAREHOUSE,
      dropoffAddress: TEST_LOCATIONS[6], // Oak St
      scheduledTime: getTomorrowDate(14, 0).toISOString(),
    });

    // Job 8: Big Wesbrook job (135 min)
    jobs.push({
      orderId: new mongoose.Types.ObjectId(),
      studentId: testStudentId,
      jobType: JobType.STORAGE,
      status: JobStatus.AVAILABLE,
      volume: 7.0, // 30 + 105 = 135min
      price: 210.00,
      pickupAddress: TEST_LOCATIONS[7], // Wesbrook
      dropoffAddress: UBC_WAREHOUSE,
      scheduledTime: getTomorrowDate(15, 0).toISOString(),
    });

    // === FAR JOBS (travel time + job time) ===
    
    // Job 9: Burnaby job (60 min job + travel)
    jobs.push({
      orderId: new mongoose.Types.ObjectId(),
      studentId: testStudentId,
      jobType: JobType.STORAGE,
      status: JobStatus.AVAILABLE,
      volume: 2.0,
      price: 100.00,
      pickupAddress: TEST_LOCATIONS[8], // Burnaby
      dropoffAddress: UBC_WAREHOUSE,
      scheduledTime: getTomorrowDate(15, 30).toISOString(),
    });

    // Job 10: North Van job (45 min job + travel)
    jobs.push({
      orderId: new mongoose.Types.ObjectId(),
      studentId: testStudentId,
      jobType: JobType.STORAGE,
      status: JobStatus.AVAILABLE,
      volume: 1.0,
      price: 85.00,
      pickupAddress: TEST_LOCATIONS[9], // North Van
      dropoffAddress: UBC_WAREHOUSE,
      scheduledTime: getTomorrowDate(16, 0).toISOString(),
    });

    // Insert all jobs
    console.log(`\n📦 Creating ${jobs.length} test jobs...`);
    
    // Register the Job model with schema
    const Job = mongoose.models.Job || mongoose.model('Job', jobSchema);
    const createdJobs = await Job.insertMany(jobs);
    
    console.log('\n✅ Successfully created test jobs!\n');
    console.log('📊 Job Distribution:');
    console.log('━'.repeat(60));
    
    // Summary by duration (30 + volume * 15)
    const estimateDuration = (vol: number) => 30 + vol * 15;
    const byDuration = {
      quick: jobs.filter(j => estimateDuration(j.volume) <= 60).length,      // 30-60 min
      medium: jobs.filter(j => estimateDuration(j.volume) > 60 && estimateDuration(j.volume) <= 90).length,  // 60-90 min
      long: jobs.filter(j => estimateDuration(j.volume) > 90).length,        // 90+ min
    };
    
    console.log(`  Quick (30-60 min):  ${byDuration.quick} jobs`);
    console.log(`  Medium (60-90 min): ${byDuration.medium} jobs`);
    console.log(`  Long (90+ min):     ${byDuration.long} jobs`);
    
    // Summary by price
    const byPrice = {
      low: jobs.filter(j => j.price < 90).length,
      medium: jobs.filter(j => j.price >= 90 && j.price < 150).length,
      high: jobs.filter(j => j.price >= 150).length,
    };
    
    console.log(`\n  Low Price (<$90):      ${byPrice.low} jobs`);
    console.log(`  Medium Price ($90-150): ${byPrice.medium} jobs`);
    console.log(`  High Price (>$150):    ${byPrice.high} jobs`);
    
    // Summary by type
    const byType = {
      storage: jobs.filter(j => j.jobType === JobType.STORAGE).length,
      return: jobs.filter(j => j.jobType === JobType.RETURN).length,
    };
    
    console.log(`\n  STORAGE jobs: ${byType.storage}`);
    console.log(`  RETURN jobs:  ${byType.return}`);
    
    console.log('\n━'.repeat(60));
    console.log('\n📍 Test from Vancouver Downtown: 49.2827, -123.1207');
    console.log('⏰ All jobs scheduled for tomorrow between 9:00 AM - 4:00 PM');
    console.log('\n🧪 Expected Algorithm Behavior with Duration Limits:');
    console.log('  ⏱️  2 hours (120 min):  Should fit 2-3 quick jobs (~90-120 min total)');
    console.log('  ⏱️  4 hours (240 min):  Should fit 3-5 mixed jobs (~180-240 min total)');
    console.log('  ⏱️  8 hours (480 min):  Should fit most/all 10 jobs');
    console.log('  ⏱️  Unlimited:          Should fit all jobs that match availability');
    console.log('\n💡 Run Smart Route API from downtown Vancouver to test!');
    console.log('   GET /api/routes/smart?currentLat=49.2827&currentLon=-123.1207&maxDuration=240\n');

  } catch (error) {
    console.error('❌ Error seeding jobs:', error);
    process.exit(1);
  } finally {
    await mongoose.connection.close();
    console.log('👋 Database connection closed');
  }
}

// Run the seeding function
createTestJobs();
