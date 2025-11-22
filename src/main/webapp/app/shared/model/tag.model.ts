import { IRoom } from 'app/shared/model/room.model';

export interface ITag {
  id?: number;
  name?: string;
  rooms?: IRoom[] | null;
}

export const defaultValue: Readonly<ITag> = {};
