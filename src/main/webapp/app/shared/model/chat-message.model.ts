import dayjs from 'dayjs';
import { IRoom } from 'app/shared/model/room.model';
import { IAppUser } from 'app/shared/model/app-user.model';

export interface IChatMessage {
  id?: number;
  content?: string;
  sentAt?: dayjs.Dayjs | null;
  room?: IRoom | null;
  sender?: IAppUser | null;
}

export const defaultValue: Readonly<IChatMessage> = {};
