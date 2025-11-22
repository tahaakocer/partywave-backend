import dayjs from 'dayjs';
import { IAppUserStats } from 'app/shared/model/app-user-stats.model';
import { AppUserStatus } from 'app/shared/model/enumerations/app-user-status.model';

export interface IAppUser {
  id?: number;
  spotifyUserId?: string;
  displayName?: string;
  email?: string;
  country?: string | null;
  href?: string | null;
  url?: string | null;
  type?: string | null;
  ipAddress?: string | null;
  lastActiveAt?: dayjs.Dayjs | null;
  status?: keyof typeof AppUserStatus | null;
  appUserStats?: IAppUserStats | null;
}

export const defaultValue: Readonly<IAppUser> = {};
