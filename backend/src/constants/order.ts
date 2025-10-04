import { z } from "zod";

export const orderStatusSchema = z.enum([
  "PENDING",
  "ACCEPTED",
  "PICKED_UP",
  "IN_STORAGE",
  "CANCELLED",
]);