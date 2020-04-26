'use strict';

import webstomp from 'webstomp-client';
import SockJS from 'sockjs-client';
import { Elm } from './elm/Main.elm';

const appPath = '/chat';
const apiPath = '/api';

const subscriptions = {};

establishWebSocketConnection('/ws/stomp', client => {
    const app = Elm.Main.init({ flags : { appPath, apiPath }});
    app.ports.refreshWebSocketSubscriptions.subscribe(destinations => {
        refreshWebSocketSubscriptions(destinations, subscriptions, client, (destination, payload) => {
            app.ports.webSocketMessageIn.send({
                destination: destination,
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

function refreshWebSocketSubscriptions(destinations, subs, client, onMessage) {
    const destinationSet = new Set(destinations);
    for (const destination in subs) {
        if (!destinationSet.has(destination)) {
            unsubscribe(destination, subs);
        }
    }
    for (const destination of destinations) {
        if (!(destination in subs)) {
            subscribe(destination, subs, client, onMessage);
        }
    }
}

function unsubscribe(destination, subs) {
    var subscription = subs[destination];
    delete subs[destination];
    subscription.unsubscribe();
}

function subscribe(destination, subs, client, onMessage) {
    subs[destination] = client.subscribe(destination, data => {
        onMessage(destination, JSON.parse(data.body));
    });
}
