import React from 'react';
import { Route } from 'react-router';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import RoomInvitation from './room-invitation';
import RoomInvitationDetail from './room-invitation-detail';
import RoomInvitationUpdate from './room-invitation-update';
import RoomInvitationDeleteDialog from './room-invitation-delete-dialog';

const RoomInvitationRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<RoomInvitation />} />
    <Route path="new" element={<RoomInvitationUpdate />} />
    <Route path=":id">
      <Route index element={<RoomInvitationDetail />} />
      <Route path="edit" element={<RoomInvitationUpdate />} />
      <Route path="delete" element={<RoomInvitationDeleteDialog />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default RoomInvitationRoutes;
