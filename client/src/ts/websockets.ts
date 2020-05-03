import * as SockJS from 'sockjs-client';
import { Client, StompSubscription } from '@stomp/stompjs';

export function connect(url : string,
                        onConnect: (arg0: any, arg1: (endpoints: string[]) => void) => void,
                        onMessage: (arg0: string, arg1: any) => void,
                        onWebSocketClose: () => any) {
    const subscriptions = {};
    const client = new Client({
        webSocketFactory: () => new SockJS(url),
    });
    client.onConnect = () => {
        onConnect(sendMessage(client), refreshSubscriptions(subscriptions, client, onMessage));
    };
    client.onWebSocketClose = () => {
        onWebSocketClose();
    }
    client.activate();
}

function sendMessage(client : Client) {
    return (destination : string, body? : any) => {
        client.publish({ destination, body })
    }
}

function refreshSubscriptions(subscriptions: { [x: string]: any; },
                              client: Client,
                              onMessage: (arg0: string, arg1: any) => void) {
    return (endpoints : string[]) => {
        const endpointSet = new Set(endpoints);
        for (const endpoint in subscriptions) {
            if (!endpointSet.has(endpoint)) {
                unsubscribe(endpoint, subscriptions);
            }
        }
        for (const endpoint of endpoints) {
            if (!(endpoint in subscriptions)) {
                subscribe(endpoint, subscriptions, client, onMessage);
            }
        }
    }
}

function unsubscribe(endpoint : string,
                     subscriptions : { [endpoint: string]: StompSubscription }) {
    const subscription = subscriptions[endpoint];
    delete subscriptions[endpoint];
    subscription.unsubscribe();
}

function subscribe(endpoint: string,
                   subscriptions: { [endpoint: string]: StompSubscription },
                   client: Client,
                   onMessage: (arg0: string, arg1: any) => void) {
    subscriptions[endpoint] = client.subscribe(endpoint, data => {
        onMessage(endpoint, JSON.parse(data.body));
    });
}
