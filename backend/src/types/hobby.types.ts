import { HOBBIES } from '../constants/hobbies';

export type GetAllHobbiesResponse = {
  message: string;
  data?: {
    hobbies: typeof HOBBIES;
  };
};