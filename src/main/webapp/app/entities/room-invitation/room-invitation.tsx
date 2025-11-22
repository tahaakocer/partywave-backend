import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Button, Table } from 'reactstrap';
import { TextFormat, Translate, getSortState } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSort, faSortDown, faSortUp } from '@fortawesome/free-solid-svg-icons';
import { APP_DATE_FORMAT } from 'app/config/constants';
import { ASC, DESC } from 'app/shared/util/pagination.constants';
import { overrideSortStateWithQueryParams } from 'app/shared/util/entity-utils';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntities } from './room-invitation.reducer';

export const RoomInvitation = () => {
  const dispatch = useAppDispatch();

  const pageLocation = useLocation();
  const navigate = useNavigate();

  const [sortState, setSortState] = useState(overrideSortStateWithQueryParams(getSortState(pageLocation, 'id'), pageLocation.search));

  const roomInvitationList = useAppSelector(state => state.roomInvitation.entities);
  const loading = useAppSelector(state => state.roomInvitation.loading);

  const getAllEntities = () => {
    dispatch(
      getEntities({
        sort: `${sortState.sort},${sortState.order}`,
      }),
    );
  };

  const sortEntities = () => {
    getAllEntities();
    const endURL = `?sort=${sortState.sort},${sortState.order}`;
    if (pageLocation.search !== endURL) {
      navigate(`${pageLocation.pathname}${endURL}`);
    }
  };

  useEffect(() => {
    sortEntities();
  }, [sortState.order, sortState.sort]);

  const sort = p => () => {
    setSortState({
      ...sortState,
      order: sortState.order === ASC ? DESC : ASC,
      sort: p,
    });
  };

  const handleSyncList = () => {
    sortEntities();
  };

  const getSortIconByFieldName = (fieldName: string) => {
    const sortFieldName = sortState.sort;
    const order = sortState.order;
    if (sortFieldName !== fieldName) {
      return faSort;
    }
    return order === ASC ? faSortUp : faSortDown;
  };

  return (
    <div>
      <h2 id="room-invitation-heading" data-cy="RoomInvitationHeading">
        <Translate contentKey="partywaveApp.roomInvitation.home.title">Room Invitations</Translate>
        <div className="d-flex justify-content-end">
          <Button className="me-2" color="info" onClick={handleSyncList} disabled={loading}>
            <FontAwesomeIcon icon="sync" spin={loading} />{' '}
            <Translate contentKey="partywaveApp.roomInvitation.home.refreshListLabel">Refresh List</Translate>
          </Button>
          <Link to="/room-invitation/new" className="btn btn-primary jh-create-entity" id="jh-create-entity" data-cy="entityCreateButton">
            <FontAwesomeIcon icon="plus" />
            &nbsp;
            <Translate contentKey="partywaveApp.roomInvitation.home.createLabel">Create new Room Invitation</Translate>
          </Link>
        </div>
      </h2>
      <div className="table-responsive">
        {roomInvitationList && roomInvitationList.length > 0 ? (
          <Table responsive>
            <thead>
              <tr>
                <th className="hand" onClick={sort('id')}>
                  <Translate contentKey="partywaveApp.roomInvitation.id">ID</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('id')} />
                </th>
                <th className="hand" onClick={sort('token')}>
                  <Translate contentKey="partywaveApp.roomInvitation.token">Token</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('token')} />
                </th>
                <th className="hand" onClick={sort('createdAt')}>
                  <Translate contentKey="partywaveApp.roomInvitation.createdAt">Created At</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('createdAt')} />
                </th>
                <th className="hand" onClick={sort('expiresAt')}>
                  <Translate contentKey="partywaveApp.roomInvitation.expiresAt">Expires At</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('expiresAt')} />
                </th>
                <th className="hand" onClick={sort('maxUses')}>
                  <Translate contentKey="partywaveApp.roomInvitation.maxUses">Max Uses</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('maxUses')} />
                </th>
                <th className="hand" onClick={sort('usedCount')}>
                  <Translate contentKey="partywaveApp.roomInvitation.usedCount">Used Count</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('usedCount')} />
                </th>
                <th className="hand" onClick={sort('isActive')}>
                  <Translate contentKey="partywaveApp.roomInvitation.isActive">Is Active</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('isActive')} />
                </th>
                <th>
                  <Translate contentKey="partywaveApp.roomInvitation.room">Room</Translate> <FontAwesomeIcon icon="sort" />
                </th>
                <th>
                  <Translate contentKey="partywaveApp.roomInvitation.createdBy">Created By</Translate> <FontAwesomeIcon icon="sort" />
                </th>
                <th />
              </tr>
            </thead>
            <tbody>
              {roomInvitationList.map((roomInvitation, i) => (
                <tr key={`entity-${i}`} data-cy="entityTable">
                  <td>
                    <Button tag={Link} to={`/room-invitation/${roomInvitation.id}`} color="link" size="sm">
                      {roomInvitation.id}
                    </Button>
                  </td>
                  <td>{roomInvitation.token}</td>
                  <td>
                    {roomInvitation.createdAt ? <TextFormat type="date" value={roomInvitation.createdAt} format={APP_DATE_FORMAT} /> : null}
                  </td>
                  <td>
                    {roomInvitation.expiresAt ? <TextFormat type="date" value={roomInvitation.expiresAt} format={APP_DATE_FORMAT} /> : null}
                  </td>
                  <td>{roomInvitation.maxUses}</td>
                  <td>{roomInvitation.usedCount}</td>
                  <td>{roomInvitation.isActive ? 'true' : 'false'}</td>
                  <td>{roomInvitation.room ? <Link to={`/room/${roomInvitation.room.id}`}>{roomInvitation.room.name}</Link> : ''}</td>
                  <td>
                    {roomInvitation.createdBy ? (
                      <Link to={`/app-user/${roomInvitation.createdBy.id}`}>{roomInvitation.createdBy.displayName}</Link>
                    ) : (
                      ''
                    )}
                  </td>
                  <td className="text-end">
                    <div className="btn-group flex-btn-group-container">
                      <Button tag={Link} to={`/room-invitation/${roomInvitation.id}`} color="info" size="sm" data-cy="entityDetailsButton">
                        <FontAwesomeIcon icon="eye" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.view">View</Translate>
                        </span>
                      </Button>
                      <Button
                        tag={Link}
                        to={`/room-invitation/${roomInvitation.id}/edit`}
                        color="primary"
                        size="sm"
                        data-cy="entityEditButton"
                      >
                        <FontAwesomeIcon icon="pencil-alt" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.edit">Edit</Translate>
                        </span>
                      </Button>
                      <Button
                        onClick={() => (window.location.href = `/room-invitation/${roomInvitation.id}/delete`)}
                        color="danger"
                        size="sm"
                        data-cy="entityDeleteButton"
                      >
                        <FontAwesomeIcon icon="trash" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.delete">Delete</Translate>
                        </span>
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        ) : (
          !loading && (
            <div className="alert alert-warning">
              <Translate contentKey="partywaveApp.roomInvitation.home.notFound">No Room Invitations found</Translate>
            </div>
          )
        )}
      </div>
    </div>
  );
};

export default RoomInvitation;
