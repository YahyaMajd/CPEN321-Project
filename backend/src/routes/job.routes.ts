import { Router } from 'express';
import { JobController } from '../controllers/job.controller';
import { jobService } from '../services/job.service';
import { authenticateToken } from '../middleware/auth.middleware';

const router = Router();
const jobController = new JobController(jobService);

// GET /api/jobs - Get all jobs
router.get('/', jobController.getAllJobs.bind(jobController));

// GET /api/jobs/available - Get available jobs for movers to accept
router.get('/available', jobController.getAllAvailableJobs.bind(jobController));

// GET /api/jobs/mover - Get jobs ACCEPTED to the authenticated mover
router.get('/mover', jobController.getMoverJobs.bind(jobController));

// GET /api/jobs/student - Get jobs for the authenticated student
router.get('/student', jobController.getStudentJobs.bind(jobController));

// GET /api/jobs/:id - Get specific job by ID
router.get('/:id', jobController.getJobById.bind(jobController));

// POST /api/jobs - Create a new job
router.post('/', jobController.createJob.bind(jobController));

// PATCH /api/jobs/:id/status - Update job status (assign, start, complete)
router.patch('/:id/status', jobController.updateJobStatus.bind(jobController));

// POST /api/jobs/:id/arrived - mover indicates arrival and requests student confirmation
router.post('/:id/arrived', jobController.arrived.bind(jobController));

// POST /api/jobs/:id/confirm-pickup - student confirms pickup
router.post('/:id/confirm-pickup', jobController.confirmPickup.bind(jobController));

// POST /api/jobs/:id/delivered - mover indicates delivery completed and requests student confirmation (return jobs)
router.post('/:id/delivered', jobController.delivered.bind(jobController));

// POST /api/jobs/:id/confirm-delivery - student confirms delivery (return jobs)
router.post('/:id/confirm-delivery', jobController.confirmDelivery.bind(jobController));

// Apply auth middleware to routes that change state
router.use(authenticateToken);

export default router;
