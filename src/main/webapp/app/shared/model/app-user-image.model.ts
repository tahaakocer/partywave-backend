import { IAppUser } from 'app/shared/model/app-user.model';

export interface IAppUserImage {
  id?: number;
  url?: string;
  height?: string | null;
  width?: string | null;
  appUser?: IAppUser | null;
}

export const defaultValue: Readonly<IAppUserImage> = {};
