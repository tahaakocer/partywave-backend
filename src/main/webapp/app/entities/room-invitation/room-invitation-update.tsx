import React, { useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';
import { Translate, ValidatedField, ValidatedForm, isNumber, translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { convertDateTimeFromServer, convertDateTimeToServer, displayDefaultDateTime } from 'app/shared/util/date-utils';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntities as getRooms } from 'app/entities/room/room.reducer';
import { getEntities as getAppUsers } from 'app/entities/app-user/app-user.reducer';
import { createEntity, getEntity, reset, updateEntity } from './room-invitation.reducer';

export const RoomInvitationUpdate = () => {
  const dispatch = useAppDispatch();

  const navigate = useNavigate();

  const { id } = useParams<'id'>();
  const isNew = id === undefined;

  const rooms = useAppSelector(state => state.room.entities);
  const appUsers = useAppSelector(state => state.appUser.entities);
  const roomInvitationEntity = useAppSelector(state => state.roomInvitation.entity);
  const loading = useAppSelector(state => state.roomInvitation.loading);
  const updating = useAppSelector(state => state.roomInvitation.updating);
  const updateSuccess = useAppSelector(state => state.roomInvitation.updateSuccess);

  const handleClose = () => {
    navigate('/room-invitation');
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
    values.createdAt = convertDateTimeToServer(values.createdAt);
    values.expiresAt = convertDateTimeToServer(values.expiresAt);
    if (values.maxUses !== undefined && typeof values.maxUses !== 'number') {
      values.maxUses = Number(values.maxUses);
    }
    if (values.usedCount !== undefined && typeof values.usedCount !== 'number') {
      values.usedCount = Number(values.usedCount);
    }

    const entity = {
      ...roomInvitationEntity,
      ...values,
      room: rooms.find(it => it.id.toString() === values.room?.toString()),
      createdBy: appUsers.find(it => it.id.toString() === values.createdBy?.toString()),
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
          createdAt: displayDefaultDateTime(),
          expiresAt: displayDefaultDateTime(),
        }
      : {
          ...roomInvitationEntity,
          createdAt: convertDateTimeFromServer(roomInvitationEntity.createdAt),
          expiresAt: convertDateTimeFromServer(roomInvitationEntity.expiresAt),
          room: roomInvitationEntity?.room?.id,
          createdBy: roomInvitationEntity?.createdBy?.id,
        };

  return (
    <div>
      <Row className="justify-content-center">
        <Col md="8">
          <h2 id="partywaveApp.roomInvitation.home.createOrEditLabel" data-cy="RoomInvitationCreateUpdateHeading">
            <Translate contentKey="partywaveApp.roomInvitation.home.createOrEditLabel">Create or edit a RoomInvitation</Translate>
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
                  id="room-invitation-id"
                  label={translate('partywaveApp.roomInvitation.id')}
                  validate={{ required: true }}
                />
              ) : null}
              <ValidatedField
                label={translate('partywaveApp.roomInvitation.token')}
                id="room-invitation-token"
                name="token"
                data-cy="token"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                }}
              />
              <ValidatedField
                label={translate('partywaveApp.roomInvitation.createdAt')}
                id="room-invitation-createdAt"
                name="createdAt"
                data-cy="createdAt"
                type="datetime-local"
                placeholder="YYYY-MM-DD HH:mm"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                }}
              />
              <ValidatedField
                label={translate('partywaveApp.roomInvitation.expiresAt')}
                id="room-invitation-expiresAt"
                name="expiresAt"
                data-cy="expiresAt"
                type="datetime-local"
                placeholder="YYYY-MM-DD HH:mm"
              />
              <ValidatedField
                label={translate('partywaveApp.roomInvitation.maxUses')}
                id="room-invitation-maxUses"
                name="maxUses"
                data-cy="maxUses"
                type="text"
              />
              <ValidatedField
                label={translate('partywaveApp.roomInvitation.usedCount')}
                id="room-invitation-usedCount"
                name="usedCount"
                data-cy="usedCount"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                  validate: v => isNumber(v) || translate('entity.validation.number'),
                }}
              />
              <ValidatedField
                label={translate('partywaveApp.roomInvitation.isActive')}
                id="room-invitation-isActive"
                name="isActive"
                data-cy="isActive"
                check
                type="checkbox"
              />
              <ValidatedField
                id="room-invitation-room"
                name="room"
                data-cy="room"
                label={translate('partywaveApp.roomInvitation.room')}
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
                id="room-invitation-createdBy"
                name="createdBy"
                data-cy="createdBy"
                label={translate('partywaveApp.roomInvitation.createdBy')}
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
              <Button tag={Link} id="cancel-save" data-cy="entityCreateCancelButton" to="/room-invitation" replace color="info">
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

export default RoomInvitationUpdate;
