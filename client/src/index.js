'use strict';

import webstomp from 'webstomp-client';
import SockJS from 'sockjs-client';
import { Elm } from './elm/Main.elm';

const appPath = '/chat';
const apiPath = '/api';

const subscriptions = {};

establishWebSocketConnection('/ws/stomp', client => {
    const app = Elm.Main.init({ flags : { appPath, apiPath }});
    app.ports.refreshWebSocketSubscriptions.subscribe(endpoints => {
        refreshWebSocketSubscriptions(endpoints, subscriptions, client, (endpoint, payload) => {
            app.ports.webSocketMessageIn.send({
                endpoint: endpoint,
                payload: payload
            });
        });
    });
    app.ports.roomMessageOut.subscribe(data => {
        client.send('/app/room/' + data.roomId + '/message', data.text);
    });
    app.ports.leaveRoom.subscribe(roomId => {
        client.send('/app/room/' + roomId + '/part');
    });
}, function(errorEvent) {
    if (errorEvent['type'] === 'close' && errorEvent['code'] === 1006) {
        // Lost connection to server
        window.location.href = appPath;
    }
});

function establishWebSocketConnection(url, onConnect, onError) {
    var socket = new SockJS(url);
    var client = webstomp.over(socket, { debug: false });
    client.connect({}, () => {
        onConnect(client);
    }, onError);
}

function refreshWebSocketSubscriptions(endpoints, subs, client, onMessage) {
    const endpointSet = new Set(endpoints);
    for (const endpoint in subs) {
        if (!endpointSet.has(endpoint)) {
            unsubscribe(endpoint, subs);
        }
    }
    for (const endpoint of endpoints) {
        if (!(endpoint in subs)) {
            subscribe(endpoint, subs, client, onMessage);
        }
    }
}

function unsubscribe(endpoint, subs) {
    var subscription = subs[endpoint];
    delete subs[endpoint];
    subscription.unsubscribe();
}

function subscribe(endpoint, subs, client, onMessage) {
    subs[endpoint] = client.subscribe(endpoint, data => {
        onMessage(endpoint, JSON.parse(data.body));
    });
}
