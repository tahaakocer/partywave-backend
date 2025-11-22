import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';
import { TextFormat, Translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './room-invitation.reducer';

export const RoomInvitationDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id));
  }, []);

  const roomInvitationEntity = useAppSelector(state => state.roomInvitation.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="roomInvitationDetailsHeading">
          <Translate contentKey="partywaveApp.roomInvitation.detail.title">RoomInvitation</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="global.field.id">ID</Translate>
            </span>
          </dt>
          <dd>{roomInvitationEntity.id}</dd>
          <dt>
            <span id="token">
              <Translate contentKey="partywaveApp.roomInvitation.token">Token</Translate>
            </span>
          </dt>
          <dd>{roomInvitationEntity.token}</dd>
          <dt>
            <span id="createdAt">
              <Translate contentKey="partywaveApp.roomInvitation.createdAt">Created At</Translate>
            </span>
          </dt>
          <dd>
            {roomInvitationEntity.createdAt ? (
              <TextFormat value={roomInvitationEntity.createdAt} type="date" format={APP_DATE_FORMAT} />
            ) : null}
          </dd>
          <dt>
            <span id="expiresAt">
              <Translate contentKey="partywaveApp.roomInvitation.expiresAt">Expires At</Translate>
            </span>
          </dt>
          <dd>
            {roomInvitationEntity.expiresAt ? (
              <TextFormat value={roomInvitationEntity.expiresAt} type="date" format={APP_DATE_FORMAT} />
            ) : null}
          </dd>
          <dt>
            <span id="maxUses">
              <Translate contentKey="partywaveApp.roomInvitation.maxUses">Max Uses</Translate>
            </span>
          </dt>
          <dd>{roomInvitationEntity.maxUses}</dd>
          <dt>
            <span id="usedCount">
              <Translate contentKey="partywaveApp.roomInvitation.usedCount">Used Count</Translate>
            </span>
          </dt>
          <dd>{roomInvitationEntity.usedCount}</dd>
          <dt>
            <span id="isActive">
              <Translate contentKey="partywaveApp.roomInvitation.isActive">Is Active</Translate>
            </span>
          </dt>
          <dd>{roomInvitationEntity.isActive ? 'true' : 'false'}</dd>
          <dt>
            <Translate contentKey="partywaveApp.roomInvitation.room">Room</Translate>
          </dt>
          <dd>{roomInvitationEntity.room ? roomInvitationEntity.room.name : ''}</dd>
          <dt>
            <Translate contentKey="partywaveApp.roomInvitation.createdBy">Created By</Translate>
          </dt>
          <dd>{roomInvitationEntity.createdBy ? roomInvitationEntity.createdBy.displayName : ''}</dd>
        </dl>
        <Button tag={Link} to="/room-invitation" replace color="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button tag={Link} to={`/room-invitation/${roomInvitationEntity.id}/edit`} replace color="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default RoomInvitationDetail;
