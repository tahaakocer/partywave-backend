import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';
import { Translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './app-user-stats.reducer';

export const AppUserStatsDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id));
  }, []);

  const appUserStatsEntity = useAppSelector(state => state.appUserStats.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="appUserStatsDetailsHeading">
          <Translate contentKey="partywaveApp.appUserStats.detail.title">AppUserStats</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="partywaveApp.appUserStats.id">Id</Translate>
            </span>
          </dt>
          <dd>{appUserStatsEntity.id}</dd>
          <dt>
            <span id="totalLike">
              <Translate contentKey="partywaveApp.appUserStats.totalLike">Total Like</Translate>
            </span>
          </dt>
          <dd>{appUserStatsEntity.totalLike}</dd>
          <dt>
            <span id="totalDislike">
              <Translate contentKey="partywaveApp.appUserStats.totalDislike">Total Dislike</Translate>
            </span>
          </dt>
          <dd>{appUserStatsEntity.totalDislike}</dd>
        </dl>
        <Button tag={Link} to="/app-user-stats" replace color="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button tag={Link} to={`/app-user-stats/${appUserStatsEntity.id}/edit`} replace color="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default AppUserStatsDetail;
