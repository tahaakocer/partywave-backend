import dayjs from 'dayjs';
import { IRoom } from 'app/shared/model/room.model';
import { IAppUser } from 'app/shared/model/app-user.model';
import { VoteType } from 'app/shared/model/enumerations/vote-type.model';

export interface IVote {
  id?: string;
  voteType?: keyof typeof VoteType;
  playlistItemId?: string | null;
  createdAt?: dayjs.Dayjs;
  room?: IRoom | null;
  voter?: IAppUser | null;
  targetUser?: IAppUser | null;
}

export const defaultValue: Readonly<IVote> = {};
