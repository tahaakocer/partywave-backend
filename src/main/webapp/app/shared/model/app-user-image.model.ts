import { IAppUser } from 'app/shared/model/app-user.model';

export interface IAppUserImage {
  id?: string;
  url?: string;
  height?: number | null;
  width?: number | null;
  appUser?: IAppUser | null;
}

export const defaultValue: Readonly<IAppUserImage> = {};
