import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';
import { TextFormat, Translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './room-member.reducer';

export const RoomMemberDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id));
  }, []);

  const roomMemberEntity = useAppSelector(state => state.roomMember.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="roomMemberDetailsHeading">
          <Translate contentKey="partywaveApp.roomMember.detail.title">RoomMember</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="partywaveApp.roomMember.id">Id</Translate>
            </span>
          </dt>
          <dd>{roomMemberEntity.id}</dd>
          <dt>
            <span id="joinedAt">
              <Translate contentKey="partywaveApp.roomMember.joinedAt">Joined At</Translate>
            </span>
          </dt>
          <dd>
            {roomMemberEntity.joinedAt ? <TextFormat value={roomMemberEntity.joinedAt} type="date" format={APP_DATE_FORMAT} /> : null}
          </dd>
          <dt>
            <span id="lastActiveAt">
              <Translate contentKey="partywaveApp.roomMember.lastActiveAt">Last Active At</Translate>
            </span>
          </dt>
          <dd>
            {roomMemberEntity.lastActiveAt ? (
              <TextFormat value={roomMemberEntity.lastActiveAt} type="date" format={APP_DATE_FORMAT} />
            ) : null}
          </dd>
          <dt>
            <span id="role">
              <Translate contentKey="partywaveApp.roomMember.role">Role</Translate>
            </span>
          </dt>
          <dd>{roomMemberEntity.role}</dd>
          <dt>
            <Translate contentKey="partywaveApp.roomMember.room">Room</Translate>
          </dt>
          <dd>{roomMemberEntity.room ? roomMemberEntity.room.name : ''}</dd>
          <dt>
            <Translate contentKey="partywaveApp.roomMember.appUser">App User</Translate>
          </dt>
          <dd>{roomMemberEntity.appUser ? roomMemberEntity.appUser.displayName : ''}</dd>
        </dl>
        <Button tag={Link} to="/room-member" replace color="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button tag={Link} to={`/room-member/${roomMemberEntity.id}/edit`} replace color="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default RoomMemberDetail;
