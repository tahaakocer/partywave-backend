import dayjs from 'dayjs';
import { ITag } from 'app/shared/model/tag.model';

export interface IRoom {
  id?: string;
  name?: string;
  description?: string | null;
  maxParticipants?: number;
  isPublic?: boolean;
  createdAt?: dayjs.Dayjs | null;
  updatedAt?: dayjs.Dayjs | null;
  tags?: ITag[] | null;
}

export const defaultValue: Readonly<IRoom> = {
  isPublic: false,
};
