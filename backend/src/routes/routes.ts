import { Router } from 'express';

import { authenticateToken } from '../middleware/auth.middleware';
import authRoutes from './auth.routes';
import hobbiesRoutes from './hobby.routes';
import mediaRoutes from './media.routes';
import usersRoutes from './user.routes';
import orderRoutes from './order.routes';

const router = Router();

router.use('/auth', authRoutes);

router.use('/hobbies', authenticateToken, hobbiesRoutes);

router.use('/user', authenticateToken, usersRoutes);
//TODO: why authenticateToken is called twice both here and in media.routes.ts?
router.use('/media', authenticateToken, mediaRoutes);

router.use('/order', authenticateToken, orderRoutes);

export default router;