import React from 'react';
import { Route } from 'react-router';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import RoomMember from './room-member';
import RoomMemberDetail from './room-member-detail';
import RoomMemberUpdate from './room-member-update';
import RoomMemberDeleteDialog from './room-member-delete-dialog';

const RoomMemberRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<RoomMember />} />
    <Route path="new" element={<RoomMemberUpdate />} />
    <Route path=":id">
      <Route index element={<RoomMemberDetail />} />
      <Route path="edit" element={<RoomMemberUpdate />} />
      <Route path="delete" element={<RoomMemberDeleteDialog />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default RoomMemberRoutes;
