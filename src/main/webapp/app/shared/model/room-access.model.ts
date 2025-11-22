import dayjs from 'dayjs';
import { IRoom } from 'app/shared/model/room.model';
import { IAppUser } from 'app/shared/model/app-user.model';

export interface IRoomAccess {
  id?: number;
  grantedAt?: dayjs.Dayjs | null;
  room?: IRoom | null;
  appUser?: IAppUser | null;
  grantedBy?: IAppUser | null;
}

export const defaultValue: Readonly<IRoomAccess> = {};
