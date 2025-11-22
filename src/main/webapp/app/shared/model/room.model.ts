import { ITag } from 'app/shared/model/tag.model';

export interface IRoom {
  id?: number;
  name?: string;
  description?: string | null;
  maxParticipants?: number | null;
  isPublic?: boolean;
  tags?: ITag[] | null;
}

export const defaultValue: Readonly<IRoom> = {
  isPublic: false,
};
