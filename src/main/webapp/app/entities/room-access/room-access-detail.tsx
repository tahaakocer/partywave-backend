import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';
import { TextFormat, Translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './room-access.reducer';

export const RoomAccessDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id));
  }, []);

  const roomAccessEntity = useAppSelector(state => state.roomAccess.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="roomAccessDetailsHeading">
          <Translate contentKey="partywaveApp.roomAccess.detail.title">RoomAccess</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="global.field.id">ID</Translate>
            </span>
          </dt>
          <dd>{roomAccessEntity.id}</dd>
          <dt>
            <span id="grantedAt">
              <Translate contentKey="partywaveApp.roomAccess.grantedAt">Granted At</Translate>
            </span>
          </dt>
          <dd>
            {roomAccessEntity.grantedAt ? <TextFormat value={roomAccessEntity.grantedAt} type="date" format={APP_DATE_FORMAT} /> : null}
          </dd>
          <dt>
            <Translate contentKey="partywaveApp.roomAccess.room">Room</Translate>
          </dt>
          <dd>{roomAccessEntity.room ? roomAccessEntity.room.name : ''}</dd>
          <dt>
            <Translate contentKey="partywaveApp.roomAccess.appUser">App User</Translate>
          </dt>
          <dd>{roomAccessEntity.appUser ? roomAccessEntity.appUser.displayName : ''}</dd>
          <dt>
            <Translate contentKey="partywaveApp.roomAccess.grantedBy">Granted By</Translate>
          </dt>
          <dd>{roomAccessEntity.grantedBy ? roomAccessEntity.grantedBy.displayName : ''}</dd>
        </dl>
        <Button tag={Link} to="/room-access" replace color="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button tag={Link} to={`/room-access/${roomAccessEntity.id}/edit`} replace color="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default RoomAccessDetail;
