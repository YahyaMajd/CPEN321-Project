import { Router } from 'express';

import { AuthController } from '../controllers/auth.controller';
import { AuthenticateUserRequest, authenticateUserSchema } from '../types/auth.types';
import { SelectRoleRequest, selectRoleSchema } from '../types/user.types';
import { validateBody } from '../middleware/validation.middleware';
import { authenticateToken } from '../middleware/auth.middleware';

const router = Router();
const authController = new AuthController();

router.post(
  '/signup',
  validateBody<AuthenticateUserRequest>(authenticateUserSchema),
  authController.signUp
);

router.post(
  '/signin',
  validateBody(authenticateUserSchema),
  authController.signIn
);

router.post(
  '/select-role',
  authenticateToken,  // Require authentication
  validateBody<SelectRoleRequest>(selectRoleSchema),
  authController.selectRole
);

export default router;