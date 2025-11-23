import React from 'react';
import { Translate } from 'react-jhipster';

import MenuItem from 'app/shared/layout/menus/menu-item';

const EntitiesMenu = () => {
  return (
    <>
      {/* prettier-ignore */}
      <MenuItem icon="asterisk" to="/app-user">
        <Translate contentKey="global.menu.entities.appUser" />
      </MenuItem>
      <MenuItem icon="asterisk" to="/app-user-stats">
        <Translate contentKey="global.menu.entities.appUserStats" />
      </MenuItem>
      <MenuItem icon="asterisk" to="/app-user-image">
        <Translate contentKey="global.menu.entities.appUserImage" />
      </MenuItem>
      <MenuItem icon="asterisk" to="/user-token">
        <Translate contentKey="global.menu.entities.userToken" />
      </MenuItem>
      <MenuItem icon="asterisk" to="/refresh-token">
        <Translate contentKey="global.menu.entities.refreshToken" />
      </MenuItem>
      <MenuItem icon="asterisk" to="/room">
        <Translate contentKey="global.menu.entities.room" />
      </MenuItem>
      <MenuItem icon="asterisk" to="/tag">
        <Translate contentKey="global.menu.entities.tag" />
      </MenuItem>
      <MenuItem icon="asterisk" to="/room-member">
        <Translate contentKey="global.menu.entities.roomMember" />
      </MenuItem>
      <MenuItem icon="asterisk" to="/room-access">
        <Translate contentKey="global.menu.entities.roomAccess" />
      </MenuItem>
      <MenuItem icon="asterisk" to="/room-invitation">
        <Translate contentKey="global.menu.entities.roomInvitation" />
      </MenuItem>
      <MenuItem icon="asterisk" to="/chat-message">
        <Translate contentKey="global.menu.entities.chatMessage" />
      </MenuItem>
      <MenuItem icon="asterisk" to="/vote">
        <Translate contentKey="global.menu.entities.vote" />
      </MenuItem>
      {/* jhipster-needle-add-entity-to-menu - JHipster will add entities to the menu here */}
    </>
  );
};

export default EntitiesMenu;
