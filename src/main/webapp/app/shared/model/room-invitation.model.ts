import dayjs from 'dayjs';
import { IRoom } from 'app/shared/model/room.model';
import { IAppUser } from 'app/shared/model/app-user.model';

export interface IRoomInvitation {
  id?: string;
  token?: string;
  createdAt?: dayjs.Dayjs;
  expiresAt?: dayjs.Dayjs | null;
  maxUses?: number | null;
  usedCount?: number;
  isActive?: boolean;
  room?: IRoom | null;
  createdBy?: IAppUser | null;
}

export const defaultValue: Readonly<IRoomInvitation> = {
  isActive: false,
};
