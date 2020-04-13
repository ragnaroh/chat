'use strict';

import webstomp from 'webstomp-client';
import SockJS from 'sockjs-client';
import { Elm } from './elm/Main.elm';

const appPath = '/chat';
const apiPath = '/api';

const subscriptions = {};

establishWebSocketConnection('/ws/stomp', client => {
    const app = Elm.Main.init({ flags : { appPath, apiPath }});
    app.ports.refreshWebSocketSubscriptions.subscribe(topics => {
        refreshWebSocketSubscriptions(topics, subscriptions, client, (topic, payload) => {
            app.ports.webSocketMessageIn.send({
                topic: topic,
                payload: payload
            });
        });
    });
    app.ports.roomMessageOut.subscribe(data => {
        client.send('/app/room/' + data.roomId + '/message', data.text);
    });
}, function(errorEvent) {
    if (errorEvent['type'] === 'close' && errorEvent['code'] === 1006) {
        // Lost connection to server
        for (const topic in subscriptions) {
            unsubscribe(topic, subscriptions);
        }
    }
});

function establishWebSocketConnection(url, onConnect, onError) {
    var socket = new SockJS(url);
    var client = webstomp.over(socket, { debug: false });
    client.connect({}, () => {
        onConnect(client);
    }, onError);
}

function refreshWebSocketSubscriptions(topics, subs, client, onMessage) {
    const topicSet = new Set(topics);
    for (const topic in subs) {
        if (!topicSet.has(topic)) {
            unsubscribe(topic, subs);
        }
    }
    for (const topic of topics) {
        if (!(topic in subs)) {
            subscribe(topic, subs, client, onMessage);
        }
    }
}

function unsubscribe(topic, subs) {
    var subscription = subs[topic];
    delete subs[topic];
    subscription.unsubscribe();
}

function subscribe(topic, subs, client, onMessage) {
    subs[topic] = client.subscribe('/topic/' + topic, data => {
        onMessage(topic, JSON.parse(data.body));
    });
}
