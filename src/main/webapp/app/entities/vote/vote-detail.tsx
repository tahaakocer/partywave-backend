import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';
import { Translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './vote.reducer';

export const VoteDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id));
  }, []);

  const voteEntity = useAppSelector(state => state.vote.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="voteDetailsHeading">
          <Translate contentKey="partywaveApp.vote.detail.title">Vote</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="global.field.id">ID</Translate>
            </span>
          </dt>
          <dd>{voteEntity.id}</dd>
          <dt>
            <span id="voteType">
              <Translate contentKey="partywaveApp.vote.voteType">Vote Type</Translate>
            </span>
          </dt>
          <dd>{voteEntity.voteType}</dd>
          <dt>
            <span id="playlistItemId">
              <Translate contentKey="partywaveApp.vote.playlistItemId">Playlist Item Id</Translate>
            </span>
          </dt>
          <dd>{voteEntity.playlistItemId}</dd>
          <dt>
            <Translate contentKey="partywaveApp.vote.room">Room</Translate>
          </dt>
          <dd>{voteEntity.room ? voteEntity.room.name : ''}</dd>
          <dt>
            <Translate contentKey="partywaveApp.vote.voter">Voter</Translate>
          </dt>
          <dd>{voteEntity.voter ? voteEntity.voter.displayName : ''}</dd>
          <dt>
            <Translate contentKey="partywaveApp.vote.targetUser">Target User</Translate>
          </dt>
          <dd>{voteEntity.targetUser ? voteEntity.targetUser.displayName : ''}</dd>
        </dl>
        <Button tag={Link} to="/vote" replace color="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button tag={Link} to={`/vote/${voteEntity.id}/edit`} replace color="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default VoteDetail;
