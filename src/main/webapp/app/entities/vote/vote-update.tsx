import React, { useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';
import { Translate, ValidatedField, ValidatedForm, translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntities as getRooms } from 'app/entities/room/room.reducer';
import { getEntities as getAppUsers } from 'app/entities/app-user/app-user.reducer';
import { VoteType } from 'app/shared/model/enumerations/vote-type.model';
import { createEntity, getEntity, reset, updateEntity } from './vote.reducer';

export const VoteUpdate = () => {
  const dispatch = useAppDispatch();

  const navigate = useNavigate();

  const { id } = useParams<'id'>();
  const isNew = id === undefined;

  const rooms = useAppSelector(state => state.room.entities);
  const appUsers = useAppSelector(state => state.appUser.entities);
  const voteEntity = useAppSelector(state => state.vote.entity);
  const loading = useAppSelector(state => state.vote.loading);
  const updating = useAppSelector(state => state.vote.updating);
  const updateSuccess = useAppSelector(state => state.vote.updateSuccess);
  const voteTypeValues = Object.keys(VoteType);

  const handleClose = () => {
    navigate(`/vote${location.search}`);
  };

  useEffect(() => {
    if (isNew) {
      dispatch(reset());
    } else {
      dispatch(getEntity(id));
    }

    dispatch(getRooms({}));
    dispatch(getAppUsers({}));
  }, []);

  useEffect(() => {
    if (updateSuccess) {
      handleClose();
    }
  }, [updateSuccess]);

  const saveEntity = values => {
    if (values.id !== undefined && typeof values.id !== 'number') {
      values.id = Number(values.id);
    }

    const entity = {
      ...voteEntity,
      ...values,
      room: rooms.find(it => it.id.toString() === values.room?.toString()),
      voter: appUsers.find(it => it.id.toString() === values.voter?.toString()),
      targetUser: appUsers.find(it => it.id.toString() === values.targetUser?.toString()),
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
          voteType: 'SKIPTRACK',
          ...voteEntity,
          room: voteEntity?.room?.id,
          voter: voteEntity?.voter?.id,
          targetUser: voteEntity?.targetUser?.id,
        };

  return (
    <div>
      <Row className="justify-content-center">
        <Col md="8">
          <h2 id="partywaveApp.vote.home.createOrEditLabel" data-cy="VoteCreateUpdateHeading">
            <Translate contentKey="partywaveApp.vote.home.createOrEditLabel">Create or edit a Vote</Translate>
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
                  id="vote-id"
                  label={translate('global.field.id')}
                  validate={{ required: true }}
                />
              ) : null}
              <ValidatedField
                label={translate('partywaveApp.vote.voteType')}
                id="vote-voteType"
                name="voteType"
                data-cy="voteType"
                type="select"
              >
                {voteTypeValues.map(voteType => (
                  <option value={voteType} key={voteType}>
                    {translate(`partywaveApp.VoteType.${voteType}`)}
                  </option>
                ))}
              </ValidatedField>
              <ValidatedField
                label={translate('partywaveApp.vote.playlistItemId')}
                id="vote-playlistItemId"
                name="playlistItemId"
                data-cy="playlistItemId"
                type="text"
              />
              <ValidatedField id="vote-room" name="room" data-cy="room" label={translate('partywaveApp.vote.room')} type="select">
                <option value="" key="0" />
                {rooms
                  ? rooms.map(otherEntity => (
                      <option value={otherEntity.id} key={otherEntity.id}>
                        {otherEntity.name}
                      </option>
                    ))
                  : null}
              </ValidatedField>
              <ValidatedField id="vote-voter" name="voter" data-cy="voter" label={translate('partywaveApp.vote.voter')} type="select">
                <option value="" key="0" />
                {appUsers
                  ? appUsers.map(otherEntity => (
                      <option value={otherEntity.id} key={otherEntity.id}>
                        {otherEntity.displayName}
                      </option>
                    ))
                  : null}
              </ValidatedField>
              <ValidatedField
                id="vote-targetUser"
                name="targetUser"
                data-cy="targetUser"
                label={translate('partywaveApp.vote.targetUser')}
                type="select"
              >
                <option value="" key="0" />
                {appUsers
                  ? appUsers.map(otherEntity => (
                      <option value={otherEntity.id} key={otherEntity.id}>
                        {otherEntity.displayName}
                      </option>
                    ))
                  : null}
              </ValidatedField>
              <Button tag={Link} id="cancel-save" data-cy="entityCreateCancelButton" to="/vote" replace color="info">
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

export default VoteUpdate;
