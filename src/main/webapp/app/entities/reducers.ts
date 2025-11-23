import appUser from 'app/entities/app-user/app-user.reducer';
import appUserStats from 'app/entities/app-user-stats/app-user-stats.reducer';
import appUserImage from 'app/entities/app-user-image/app-user-image.reducer';
import userToken from 'app/entities/user-token/user-token.reducer';
import refreshToken from 'app/entities/refresh-token/refresh-token.reducer';
import room from 'app/entities/room/room.reducer';
import tag from 'app/entities/tag/tag.reducer';
import roomMember from 'app/entities/room-member/room-member.reducer';
import roomAccess from 'app/entities/room-access/room-access.reducer';
import roomInvitation from 'app/entities/room-invitation/room-invitation.reducer';
import chatMessage from 'app/entities/chat-message/chat-message.reducer';
import vote from 'app/entities/vote/vote.reducer';
/* jhipster-needle-add-reducer-import - JHipster will add reducer here */

const entitiesReducers = {
  appUser,
  appUserStats,
  appUserImage,
  userToken,
  refreshToken,
  room,
  tag,
  roomMember,
  roomAccess,
  roomInvitation,
  chatMessage,
  vote,
  /* jhipster-needle-add-reducer-combine - JHipster will add reducer here */
};

export default entitiesReducers;
