import dayjs from 'dayjs';
import { IRoom } from 'app/shared/model/room.model';
import { IAppUser } from 'app/shared/model/app-user.model';

export interface IRoomInvitation {
  id?: number;
  token?: string;
  createdAt?: dayjs.Dayjs | null;
  expiresAt?: dayjs.Dayjs | null;
  maxUses?: number | null;
  usedCount?: number | null;
  isActive?: boolean | null;
  room?: IRoom | null;
  createdBy?: IAppUser | null;
}

export const defaultValue: Readonly<IRoomInvitation> = {
  isActive: false,
};
