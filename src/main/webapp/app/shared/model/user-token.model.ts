import dayjs from 'dayjs';
import { IAppUser } from 'app/shared/model/app-user.model';

export interface IUserToken {
  id?: number;
  accessToken?: string;
  refreshToken?: string;
  tokenType?: string | null;
  expiresAt?: dayjs.Dayjs | null;
  scope?: string | null;
  appUser?: IAppUser | null;
}

export const defaultValue: Readonly<IUserToken> = {};
