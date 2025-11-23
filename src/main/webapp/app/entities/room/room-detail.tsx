import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';
import { TextFormat, Translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './room.reducer';

export const RoomDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id));
  }, []);

  const roomEntity = useAppSelector(state => state.room.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="roomDetailsHeading">
          <Translate contentKey="partywaveApp.room.detail.title">Room</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="partywaveApp.room.id">Id</Translate>
            </span>
          </dt>
          <dd>{roomEntity.id}</dd>
          <dt>
            <span id="name">
              <Translate contentKey="partywaveApp.room.name">Name</Translate>
            </span>
          </dt>
          <dd>{roomEntity.name}</dd>
          <dt>
            <span id="description">
              <Translate contentKey="partywaveApp.room.description">Description</Translate>
            </span>
          </dt>
          <dd>{roomEntity.description}</dd>
          <dt>
            <span id="maxParticipants">
              <Translate contentKey="partywaveApp.room.maxParticipants">Max Participants</Translate>
            </span>
          </dt>
          <dd>{roomEntity.maxParticipants}</dd>
          <dt>
            <span id="isPublic">
              <Translate contentKey="partywaveApp.room.isPublic">Is Public</Translate>
            </span>
          </dt>
          <dd>{roomEntity.isPublic ? 'true' : 'false'}</dd>
          <dt>
            <span id="createdAt">
              <Translate contentKey="partywaveApp.room.createdAt">Created At</Translate>
            </span>
          </dt>
          <dd>{roomEntity.createdAt ? <TextFormat value={roomEntity.createdAt} type="date" format={APP_DATE_FORMAT} /> : null}</dd>
          <dt>
            <span id="updatedAt">
              <Translate contentKey="partywaveApp.room.updatedAt">Updated At</Translate>
            </span>
          </dt>
          <dd>{roomEntity.updatedAt ? <TextFormat value={roomEntity.updatedAt} type="date" format={APP_DATE_FORMAT} /> : null}</dd>
          <dt>
            <Translate contentKey="partywaveApp.room.tags">Tags</Translate>
          </dt>
          <dd>
            {roomEntity.tags
              ? roomEntity.tags.map((val, i) => (
                  <span key={val.id}>
                    <a>{val.name}</a>
                    {roomEntity.tags && i === roomEntity.tags.length - 1 ? '' : ', '}
                  </span>
                ))
              : null}
          </dd>
        </dl>
        <Button tag={Link} to="/room" replace color="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button tag={Link} to={`/room/${roomEntity.id}/edit`} replace color="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default RoomDetail;
