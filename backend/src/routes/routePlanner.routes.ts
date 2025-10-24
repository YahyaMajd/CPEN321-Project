import { Router } from 'express';
import { routeController } from '../controllers/routePlanner.controller';
import { authenticateToken } from '../middleware/auth.middleware';

const router = Router();

// GET /api/routes/smart - Calculate optimized route for mover
// Query params: currentLat, currentLon
router.get('/smart', authenticateToken, routeController.getSmartRoute.bind(routeController));

export default router;
