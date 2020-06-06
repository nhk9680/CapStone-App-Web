import time
import socketio
sio = socketio.Client()
sio.connect('http://localhost:5000')
while True:
    sio.emit('image', 'asdf')
    time.sleep(1)