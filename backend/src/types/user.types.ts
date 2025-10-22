import mongoose, { Document } from 'mongoose';
import z from 'zod';
import { HOBBIES } from '../constants/hobbies';

// User model
// ------------------------------------------------------------

export type UserRole = 'STUDENT' | 'MOVER';

// Mover-specific types
export type TimeRange = [string, string]; // [startTime, endTime] in "HH:mm" format, e.g., ["08:30", "12:45"]
export type DayAvailability = {
  [key: string]: TimeRange[]; // e.g., { "Mon": [["08:30", "12:00"], ["14:00", "18:30"]], "Tue": [["09:00", "17:00"]] }
};

export interface IUser extends Document {
  _id: mongoose.Types.ObjectId;
  userRole?: UserRole;  // Optional - set after signup
  googleId: string;
  email: string;
  fcmToken?: string;
  name: string;
  profilePicture?: string;
  bio?: string;
  hobbies: string[];
  // Mover-specific fields (only present when userRole is 'MOVER')
  availability?: DayAvailability;
  capacity?: number;
  carType?: string;
  plateNumber?: string;
  createdAt: Date;
  updatedAt: Date;
}

// Zod schemas
// ------------------------------------------------------------
const timeStringSchema = z.string().regex(/^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/, {
  message: 'Time must be in HH:mm format',
});

const timeRangeSchema = z.tuple([timeStringSchema, timeStringSchema])
  .refine(([start, end]) => {
    const [startHour, startMin] = start.split(':').map(Number);
    const [endHour, endMin] = end.split(':').map(Number);
    const startMinutes = startHour * 60 + startMin;
    const endMinutes = endHour * 60 + endMin;
    return startMinutes < endMinutes;
  }, {
    message: 'Start time must be before end time',
  });

const availabilitySchema = z.record(z.string(), z.array(timeRangeSchema)).optional();

export const createUserSchema = z.object({
  email: z.string().email(),
  name: z.string().min(1),
  googleId: z.string().min(1),
  profilePicture: z.string().optional(),
  bio: z.string().max(500).optional(),
  hobbies: z.array(z.string()).default([]),
});

export const updateProfileSchema = z.object({
  name: z.string().min(1).optional(),
  bio: z.string().max(500).optional(),
  fcmToken: z.string().optional(),  
  hobbies: z
    .array(z.string())
    .refine(val => val.length === 0 || val.every(v => HOBBIES.includes(v)), {
      message: 'Hobby must be in the available hobbies list',
    })
    .optional(),
  profilePicture: z.string().min(1).optional(),
  userRole: z.enum(['STUDENT', 'MOVER']).optional(),
  // Mover-specific fields
  availability: availabilitySchema,
  capacity: z.number().positive().optional(),
  carType: z.string().optional(),
  plateNumber: z.string().optional(),
});

export const selectRoleSchema = z.object({
  userRole: z.enum(['STUDENT', 'MOVER']),
});

// Request types
// ------------------------------------------------------------
export type GetProfileResponse = {
  message: string;
  data?: {
    user: IUser;
  };
};

export type UpdateProfileRequest = z.infer<typeof updateProfileSchema>;
export type SelectRoleRequest = z.infer<typeof selectRoleSchema>;

// Generic types
// ------------------------------------------------------------
export type GoogleUserInfo = {
  googleId: string;
  email: string;
  name: string;
  profilePicture?: string;
};