import React, { useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';
import { Translate, ValidatedField, ValidatedForm, translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { convertDateTimeFromServer, convertDateTimeToServer, displayDefaultDateTime } from 'app/shared/util/date-utils';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntities as getRooms } from 'app/entities/room/room.reducer';
import { getEntities as getAppUsers } from 'app/entities/app-user/app-user.reducer';
import { createEntity, getEntity, reset, updateEntity } from './room-access.reducer';

export const RoomAccessUpdate = () => {
  const dispatch = useAppDispatch();

  const navigate = useNavigate();

  const { id } = useParams<'id'>();
  const isNew = id === undefined;

  const rooms = useAppSelector(state => state.room.entities);
  const appUsers = useAppSelector(state => state.appUser.entities);
  const roomAccessEntity = useAppSelector(state => state.roomAccess.entity);
  const loading = useAppSelector(state => state.roomAccess.loading);
  const updating = useAppSelector(state => state.roomAccess.updating);
  const updateSuccess = useAppSelector(state => state.roomAccess.updateSuccess);

  const handleClose = () => {
    navigate('/room-access');
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
    values.grantedAt = convertDateTimeToServer(values.grantedAt);

    const entity = {
      ...roomAccessEntity,
      ...values,
      room: rooms.find(it => it.id.toString() === values.room?.toString()),
      appUser: appUsers.find(it => it.id.toString() === values.appUser?.toString()),
      grantedBy: appUsers.find(it => it.id.toString() === values.grantedBy?.toString()),
    };

    if (isNew) {
      dispatch(createEntity(entity));
    } else {
      dispatch(updateEntity(entity));
    }
  };

  const defaultValues = () =>
    isNew
      ? {
          grantedAt: displayDefaultDateTime(),
        }
      : {
          ...roomAccessEntity,
          grantedAt: convertDateTimeFromServer(roomAccessEntity.grantedAt),
          room: roomAccessEntity?.room?.id,
          appUser: roomAccessEntity?.appUser?.id,
          grantedBy: roomAccessEntity?.grantedBy?.id,
        };

  return (
    <div>
      <Row className="justify-content-center">
        <Col md="8">
          <h2 id="partywaveApp.roomAccess.home.createOrEditLabel" data-cy="RoomAccessCreateUpdateHeading">
            <Translate contentKey="partywaveApp.roomAccess.home.createOrEditLabel">Create or edit a RoomAccess</Translate>
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
                  id="room-access-id"
                  label={translate('global.field.id')}
                  validate={{ required: true }}
                />
              ) : null}
              <ValidatedField
                label={translate('partywaveApp.roomAccess.grantedAt')}
                id="room-access-grantedAt"
                name="grantedAt"
                data-cy="grantedAt"
                type="datetime-local"
                placeholder="YYYY-MM-DD HH:mm"
              />
              <ValidatedField
                id="room-access-room"
                name="room"
                data-cy="room"
                label={translate('partywaveApp.roomAccess.room')}
                type="select"
              >
                <option value="" key="0" />
                {rooms
                  ? rooms.map(otherEntity => (
                      <option value={otherEntity.id} key={otherEntity.id}>
                        {otherEntity.name}
                      </option>
                    ))
                  : null}
              </ValidatedField>
              <ValidatedField
                id="room-access-appUser"
                name="appUser"
                data-cy="appUser"
                label={translate('partywaveApp.roomAccess.appUser')}
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
              <ValidatedField
                id="room-access-grantedBy"
                name="grantedBy"
                data-cy="grantedBy"
                label={translate('partywaveApp.roomAccess.grantedBy')}
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
              <Button tag={Link} id="cancel-save" data-cy="entityCreateCancelButton" to="/room-access" replace color="info">
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

export default RoomAccessUpdate;
