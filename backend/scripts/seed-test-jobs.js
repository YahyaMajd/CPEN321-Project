/**
 * Seed Test Jobs Script (Pure JavaScript - works in production)
 * 
 * This script populates the database with test jobs for testing the Smart Route algorithm.
 * 
 * Usage on AWS:
 *   node scripts/seed-test-jobs.js
 */

const mongoose = require('mongoose');
const dotenv = require('dotenv');

dotenv.config();

// Job Types and Statuses
const JobType = {
  STORAGE: 'STORAGE',
  RETURN: 'RETURN'
};

const JobStatus = {
  AVAILABLE: 'AVAILABLE',
  ACCEPTED: 'ACCEPTED',
  IN_PROGRESS: 'IN_PROGRESS',
  COMPLETED: 'COMPLETED',
  AWAITING_STUDENT_CONFIRMATION: 'AWAITING_STUDENT_CONFIRMATION'
};

// Define Job schema
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

// UBC Warehouse (common dropoff for STORAGE jobs)
const UBC_WAREHOUSE = {
  lat: 49.2606,
  lon: -123.2460,
  formattedAddress: "UBC Warehouse, 2329 West Mall, Vancouver, BC V6T 1Z4"
};

// Test locations around Greater Vancouver
const TEST_LOCATIONS = [
  { lat: 49.2827, lon: -123.1207, formattedAddress: "789 Granville St, Vancouver, BC V6Z 1K3", distance: "downtown" },
  { lat: 49.2819, lon: -123.1086, formattedAddress: "1234 Robson St, Vancouver, BC V6E 1B5", distance: "close" },
  { lat: 49.2734, lon: -123.1022, formattedAddress: "456 Davie St, Vancouver, BC V6B 2G3", distance: "close" },
  { lat: 49.2642, lon: -123.1387, formattedAddress: "2525 Main St, Vancouver, BC V5T 3E2", distance: "close" },
  { lat: 49.2497, lon: -123.1193, formattedAddress: "5678 Cambie St, Vancouver, BC V5Z 2Z8", distance: "close" },
  { lat: 49.2488, lon: -123.0569, formattedAddress: "3456 Fraser St, Vancouver, BC V5V 4C4", distance: "medium" },
  { lat: 49.2145, lon: -123.1034, formattedAddress: "7890 Oak St, Vancouver, BC V6P 4A5", distance: "medium" },
  { lat: 49.2631, lon: -123.2489, formattedAddress: "4567 Wesbrook Mall, Vancouver, BC V6T 2A1", distance: "medium" },
  { lat: 49.2069, lon: -122.9115, formattedAddress: "8901 Kingsway, Burnaby, BC V5H 2E6", distance: "far" },
  { lat: 49.3200, lon: -123.0716, formattedAddress: "1234 Lonsdale Ave, North Vancouver, BC V7M 2H6", distance: "far" },
];

function getTomorrowDate(hour, minute = 0) {
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  tomorrow.setHours(hour, minute, 0, 0);
  return tomorrow;
}

async function createTestJobs() {
  try {
    console.log('🌱 Starting job seeding...');
    
    const uri = process.env.MONGODB_URI;
    if (!uri) {
      throw new Error('MONGODB_URI environment variable is not set');
    }
    
    await mongoose.connect(uri);
    console.log('✅ Connected to MongoDB');

    const testStudentId = new mongoose.Types.ObjectId();
    const testOrderId = new mongoose.Types.ObjectId();

    console.log('📝 Test Student ID:', testStudentId.toString());
    console.log('📝 Test Order ID:', testOrderId.toString());

    const jobs = [
      // Quick jobs
      {
        orderId: new mongoose.Types.ObjectId(),
        studentId: testStudentId,
        jobType: JobType.STORAGE,
        status: JobStatus.AVAILABLE,
        volume: 2.0,
        price: 80.00,
        pickupAddress: TEST_LOCATIONS[0],
        dropoffAddress: UBC_WAREHOUSE,
        scheduledTime: getTomorrowDate(9, 0),
      },
      {
        orderId: new mongoose.Types.ObjectId(),
        studentId: testStudentId,
        jobType: JobType.STORAGE,
        status: JobStatus.AVAILABLE,
        volume: 1.5,
        price: 60.00,
        pickupAddress: TEST_LOCATIONS[1],
        dropoffAddress: UBC_WAREHOUSE,
        scheduledTime: getTomorrowDate(9, 30),
      },
      {
        orderId: new mongoose.Types.ObjectId(),
        studentId: testStudentId,
        jobType: JobType.RETURN,
        status: JobStatus.AVAILABLE,
        volume: 2.0,
        price: 75.00,
        pickupAddress: UBC_WAREHOUSE,
        dropoffAddress: TEST_LOCATIONS[2],
        scheduledTime: getTomorrowDate(10, 0),
      },
      // Medium jobs
      {
        orderId: new mongoose.Types.ObjectId(),
        studentId: testStudentId,
        jobType: JobType.STORAGE,
        status: JobStatus.AVAILABLE,
        volume: 3.0,
        price: 120.00,
        pickupAddress: TEST_LOCATIONS[3],
        dropoffAddress: UBC_WAREHOUSE,
        scheduledTime: getTomorrowDate(10, 30),
      },
      {
        orderId: new mongoose.Types.ObjectId(),
        studentId: testStudentId,
        jobType: JobType.STORAGE,
        status: JobStatus.AVAILABLE,
        volume: 4.0,
        price: 140.00,
        pickupAddress: TEST_LOCATIONS[4],
        dropoffAddress: UBC_WAREHOUSE,
        scheduledTime: getTomorrowDate(11, 0),
      },
      {
        orderId: new mongoose.Types.ObjectId(),
        studentId: testStudentId,
        jobType: JobType.STORAGE,
        status: JobStatus.AVAILABLE,
        volume: 2.0,
        price: 90.00,
        pickupAddress: TEST_LOCATIONS[5],
        dropoffAddress: UBC_WAREHOUSE,
        scheduledTime: getTomorrowDate(11, 30),
      },
      // Longer jobs
      {
        orderId: new mongoose.Types.ObjectId(),
        studentId: testStudentId,
        jobType: JobType.RETURN,
        status: JobStatus.AVAILABLE,
        volume: 6.0,
        price: 180.00,
        pickupAddress: UBC_WAREHOUSE,
        dropoffAddress: TEST_LOCATIONS[6],
        scheduledTime: getTomorrowDate(14, 0),
      },
      {
        orderId: new mongoose.Types.ObjectId(),
        studentId: testStudentId,
        jobType: JobType.STORAGE,
        status: JobStatus.AVAILABLE,
        volume: 7.0,
        price: 210.00,
        pickupAddress: TEST_LOCATIONS[7],
        dropoffAddress: UBC_WAREHOUSE,
        scheduledTime: getTomorrowDate(15, 0),
      },
      // Far jobs
      {
        orderId: new mongoose.Types.ObjectId(),
        studentId: testStudentId,
        jobType: JobType.STORAGE,
        status: JobStatus.AVAILABLE,
        volume: 2.0,
        price: 100.00,
        pickupAddress: TEST_LOCATIONS[8],
        dropoffAddress: UBC_WAREHOUSE,
        scheduledTime: getTomorrowDate(15, 30),
      },
      {
        orderId: new mongoose.Types.ObjectId(),
        studentId: testStudentId,
        jobType: JobType.STORAGE,
        status: JobStatus.AVAILABLE,
        volume: 1.0,
        price: 85.00,
        pickupAddress: TEST_LOCATIONS[9],
        dropoffAddress: UBC_WAREHOUSE,
        scheduledTime: getTomorrowDate(16, 0),
      },
    ];

    console.log(`\n📦 Creating ${jobs.length} test jobs...`);
    
    const Job = mongoose.models.Job || mongoose.model('Job', jobSchema);
    await Job.insertMany(jobs);
    
    console.log('\n✅ Successfully created test jobs!\n');
    console.log('📊 Job Distribution:');
    console.log('━'.repeat(60));
    console.log(`  Total Jobs: ${jobs.length}`);
    console.log(`  STORAGE jobs: ${jobs.filter(j => j.jobType === JobType.STORAGE).length}`);
    console.log(`  RETURN jobs: ${jobs.filter(j => j.jobType === JobType.RETURN).length}`);
    console.log('\n━'.repeat(60));
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

createTestJobs();
