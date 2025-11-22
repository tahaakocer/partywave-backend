import dayjs from 'dayjs';
import { IRoom } from 'app/shared/model/room.model';
import { IAppUser } from 'app/shared/model/app-user.model';
import { RoomMemberRole } from 'app/shared/model/enumerations/room-member-role.model';

export interface IRoomMember {
  id?: number;
  joinedAt?: dayjs.Dayjs | null;
  lastActiveAt?: dayjs.Dayjs | null;
  role?: keyof typeof RoomMemberRole | null;
  room?: IRoom | null;
  appUser?: IAppUser | null;
}

export const defaultValue: Readonly<IRoomMember> = {};
