var app = require('express')();
var http = require('http').createServer(app);
var io = require('socket.io')(http);
var SerialPort = require('serialport');
var serialPort = new SerialPort('COM4', {
    baudRate: 9600,
//    parser: SerialPort.parsers.readline('\n')
});
const Readline = SerialPort.parsers.Readline;
const parser = new Readline();

//serialPort.on('data', function (data) {
//  console.log('Data:', data[0]);
//});

//serialPort.on('readable', function() {
//  console.log('Data:', serialPort.read());
//});

serialPort.pipe(parser);
parser.on('data', function(data){
    
    var msg = data.split('\t');
    msg[2] = msg[2].split('\r')[0];
    
    console.log('Data', msg);
    
    io.emit('chat', msg);
})

var t = 0;

app.get('/', (req, res) => {
  res.sendFile(__dirname + '/index.html');
});

io.on('connection', (socket) => {
    console.log('a user connected');
    socket.on('disconnect', () => {
        console.log('user disconnected');
    });
    
    
    socket.on('chat message', (msg) => {
        var rnd = Math.floor(Math.random() * 100);
        var mymsg = {
            t: t,
            v1: rnd,
            v2: 100-rnd,
            v3: Math.random() * 5
        };
        console.log('message: ' + mymsg.t);
        io.emit('chat', mymsg); // string type send, not integer
        t++;
    });
});

http.listen(3000, () => {
  console.log('listening on *:3000');
});