import dayjs from 'dayjs';
import { IAppUser } from 'app/shared/model/app-user.model';

export interface IRefreshToken {
  id?: string;
  tokenHash?: string;
  expiresAt?: dayjs.Dayjs;
  createdAt?: dayjs.Dayjs;
  revokedAt?: dayjs.Dayjs | null;
  deviceInfo?: string | null;
  ipAddress?: string | null;
  appUser?: IAppUser | null;
}

export const defaultValue: Readonly<IRefreshToken> = {};
