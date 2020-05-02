'use strict';

import * as ws from './js/websockets.js';
import { Elm } from './elm/Main.elm';

const appPath = '/chat';
const apiPath = '/api';

const app = Elm.Main.init({ flags : { appPath, apiPath }});

function onWsConnect(sendMessage, refreshSubscriptions) {
    app.ports.connectionStatusIn.send(true);
    app.ports.refreshWebSocketSubscriptions.subscribe(endpoints => {
        refreshSubscriptions(endpoints);
    });
    app.ports.roomMessageOut.subscribe(data => {
        sendMessage('/app/room/' + data.roomId + '/message', data.text);
    });
    app.ports.leaveRoom.subscribe(roomId => {
        sendMessage('/app/room/' + roomId + '/part');
    });
}

function onWsMessage(endpoint, payload) {
    app.ports.webSocketMessageIn.send({ endpoint, payload });
}

function onWsError(errorEvent) {
    if (errorEvent['type'] === 'close' && errorEvent['code'] === 1006) {
        // Lost connection to server
        app.ports.connectionStatusIn.send(false);
    }
}

ws.connect('/ws/stomp', onWsConnect, onWsMessage, onWsError);
