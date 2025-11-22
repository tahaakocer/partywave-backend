import React from 'react';
import { Route } from 'react-router';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import Vote from './vote';
import VoteDetail from './vote-detail';
import VoteUpdate from './vote-update';
import VoteDeleteDialog from './vote-delete-dialog';

const VoteRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<Vote />} />
    <Route path="new" element={<VoteUpdate />} />
    <Route path=":id">
      <Route index element={<VoteDetail />} />
      <Route path="edit" element={<VoteUpdate />} />
      <Route path="delete" element={<VoteDeleteDialog />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default VoteRoutes;
