import React, { useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';
import { Translate, ValidatedField, ValidatedForm, translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';

import { createEntity, getEntity, reset, updateEntity } from './app-user-stats.reducer';

export const AppUserStatsUpdate = () => {
  const dispatch = useAppDispatch();

  const navigate = useNavigate();

  const { id } = useParams<'id'>();
  const isNew = id === undefined;

  const appUserStatsEntity = useAppSelector(state => state.appUserStats.entity);
  const loading = useAppSelector(state => state.appUserStats.loading);
  const updating = useAppSelector(state => state.appUserStats.updating);
  const updateSuccess = useAppSelector(state => state.appUserStats.updateSuccess);

  const handleClose = () => {
    navigate('/app-user-stats');
  };

  useEffect(() => {
    if (isNew) {
      dispatch(reset());
    } else {
      dispatch(getEntity(id));
    }
  }, []);

  useEffect(() => {
    if (updateSuccess) {
      handleClose();
    }
  }, [updateSuccess]);

  const saveEntity = values => {
    if (values.totalLike !== undefined && typeof values.totalLike !== 'number') {
      values.totalLike = Number(values.totalLike);
    }
    if (values.totalDislike !== undefined && typeof values.totalDislike !== 'number') {
      values.totalDislike = Number(values.totalDislike);
    }

    const entity = {
      ...appUserStatsEntity,
      ...values,
    };

    if (isNew) {
      dispatch(createEntity(entity));
    } else {
      dispatch(updateEntity(entity));
    }
  };

  const defaultValues = () =>
    isNew
      ? {}
      : {
          ...appUserStatsEntity,
        };

  return (
    <div>
      <Row className="justify-content-center">
        <Col md="8">
          <h2 id="partywaveApp.appUserStats.home.createOrEditLabel" data-cy="AppUserStatsCreateUpdateHeading">
            <Translate contentKey="partywaveApp.appUserStats.home.createOrEditLabel">Create or edit a AppUserStats</Translate>
          </h2>
        </Col>
      </Row>
      <Row className="justify-content-center">
        <Col md="8">
          {loading ? (
            <p>Loading...</p>
          ) : (
            <ValidatedForm defaultValues={defaultValues()} onSubmit={saveEntity}>
              {!isNew ? (
                <ValidatedField
                  name="id"
                  required
                  readOnly
                  id="app-user-stats-id"
                  label={translate('partywaveApp.appUserStats.id')}
                  validate={{ required: true }}
                />
              ) : null}
              <ValidatedField
                label={translate('partywaveApp.appUserStats.totalLike')}
                id="app-user-stats-totalLike"
                name="totalLike"
                data-cy="totalLike"
                type="text"
              />
              <ValidatedField
                label={translate('partywaveApp.appUserStats.totalDislike')}
                id="app-user-stats-totalDislike"
                name="totalDislike"
                data-cy="totalDislike"
                type="text"
              />
              <Button tag={Link} id="cancel-save" data-cy="entityCreateCancelButton" to="/app-user-stats" replace color="info">
                <FontAwesomeIcon icon="arrow-left" />
                &nbsp;
                <span className="d-none d-md-inline">
                  <Translate contentKey="entity.action.back">Back</Translate>
                </span>
              </Button>
              &nbsp;
              <Button color="primary" id="save-entity" data-cy="entityCreateSaveButton" type="submit" disabled={updating}>
                <FontAwesomeIcon icon="save" />
                &nbsp;
                <Translate contentKey="entity.action.save">Save</Translate>
              </Button>
            </ValidatedForm>
          )}
        </Col>
      </Row>
    </div>
  );
};

export default AppUserStatsUpdate;
