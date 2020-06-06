# main.py

import cv2
from flask import Flask, render_template, Response
# ----------------------------------
# from camera import VideoCamera

video = cv2.VideoCapture(0)
app = Flask(__name__)

# ----------------------------------

@app.route('/')
def index():
    # rendering webpage
    return render_template('index.html')
    
def gen():
    while True:
        #get camera frame
        # frame = camera.get_frame()

        _, frame = video.read()
        _, jpeg = cv2.imencode('.jpg', frame)
        jpeg_byte = jpeg.tobytes()

        yield (b'--frame\r\n'
               b'Content-Type: image/jpeg\r\n\r\n' + frame + b'\r\n\r\n')

@app.route('/video_feed')
def video_feed():
    return Response(gen(),
                    mimetype='multipart/x-mixed-replace; boundary=frame')

if __name__ == '__main__':
    # defining server ip address and port
    app.run(host='127.0.0.1',port='5000', debug=True)