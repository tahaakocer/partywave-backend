import { IRoom } from 'app/shared/model/room.model';
import { IAppUser } from 'app/shared/model/app-user.model';
import { VoteType } from 'app/shared/model/enumerations/vote-type.model';

export interface IVote {
  id?: number;
  voteType?: keyof typeof VoteType | null;
  playlistItemId?: string | null;
  room?: IRoom | null;
  voter?: IAppUser | null;
  targetUser?: IAppUser | null;
}

export const defaultValue: Readonly<IVote> = {};
