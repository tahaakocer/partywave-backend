import React from 'react';
import { Route } from 'react-router';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import AppUser from './app-user';
import AppUserStats from './app-user-stats';
import AppUserImage from './app-user-image';
import UserToken from './user-token';
import RefreshToken from './refresh-token';
import Room from './room';
import Tag from './tag';
import RoomMember from './room-member';
import RoomAccess from './room-access';
import RoomInvitation from './room-invitation';
import ChatMessage from './chat-message';
import Vote from './vote';
/* jhipster-needle-add-route-import - JHipster will add routes here */

export default () => {
  return (
    <div>
      <ErrorBoundaryRoutes>
        {/* prettier-ignore */}
        <Route path="app-user/*" element={<AppUser />} />
        <Route path="app-user-stats/*" element={<AppUserStats />} />
        <Route path="app-user-image/*" element={<AppUserImage />} />
        <Route path="user-token/*" element={<UserToken />} />
        <Route path="refresh-token/*" element={<RefreshToken />} />
        <Route path="room/*" element={<Room />} />
        <Route path="tag/*" element={<Tag />} />
        <Route path="room-member/*" element={<RoomMember />} />
        <Route path="room-access/*" element={<RoomAccess />} />
        <Route path="room-invitation/*" element={<RoomInvitation />} />
        <Route path="chat-message/*" element={<ChatMessage />} />
        <Route path="vote/*" element={<Vote />} />
        {/* jhipster-needle-add-route-path - JHipster will add routes here */}
      </ErrorBoundaryRoutes>
    </div>
  );
};
