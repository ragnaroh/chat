import SockJS from 'sockjs-client';
import webstomp from 'webstomp-client';

export function connect(url, onConnect, onMessage, onError) {
    const subscriptions = {};
    const client = webstomp.over(new SockJS(url), { debug: false });
    client.connect({}, () => {
        onConnect(client.send.bind(client), refreshSubscriptions(subscriptions, client, onMessage));
    }, onError);
}

function refreshSubscriptions(subscriptions, client, onMessage) {
    return (endpoints) => {
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

function unsubscribe(endpoint, subscriptions) {
    const subscription = subscriptions[endpoint];
    delete subscriptions[endpoint];
    subscription.unsubscribe();
}

function subscribe(endpoint, subscriptions, client, onMessage) {
    subscriptions[endpoint] = client.subscribe(endpoint, data => {
        onMessage(endpoint, JSON.parse(data.body));
    });
}
