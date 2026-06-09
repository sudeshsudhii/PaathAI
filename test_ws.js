const http = require('http');

const options = {
    port: 8080,
    hostname: '127.0.0.1',
    path: '/ws/live?token=dummy',
    headers: {
        'Connection': 'Upgrade',
        'Upgrade': 'websocket',
        'Sec-WebSocket-Key': 'dGhlIHNhbXBsZSBub25jZQ==',
        'Sec-WebSocket-Version': 13
    }
};

const req = http.request(options);
req.on('upgrade', (res, socket, upgradeHead) => {
    console.log('Got upgrade!', res.statusCode);
    socket.end();
});
req.on('response', (res) => {
    console.log('Got response (no upgrade)', res.statusCode);
});
req.on('error', (e) => {
    console.error('Request error', e);
});
req.end();
