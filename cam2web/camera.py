import cv2
import imutils
import serial
import time
import math
import threading

# --------------------------------------------------
from flask import Flask, render_template, Response
scale = 0.2 # response image scale
app = Flask(__name__)

@app.route('/')
def index():
    # rendering webpage
    return render_template('index.html')

@app.route('/video_feed')
def video_feed():
    return Response(main(),
                    mimetype='multipart/x-mixed-replace; boundary=frame')

# --------------------------------------------------

def Communications():
     global coms
     coms = Py2ArdComs()
     coms.run()

class Py2ArdComs():
    ThreadRun = 1
    Count = 0
    try:
        arduino = serial.Serial("/dev/ttyACM0", 9600, timeout = 1)
    except:
        arduino = serial.Serial("/dev/ttyACM1", 9600, timeout = 1)
    FromArduinoData = 0
    ToArduinoData = 1

    def __init__(self):
        time.sleep(1) #give the connection a second to settle

    def stop(self):
        self.ThreadRun = 0

    def send(self,send_data):
        send_data=round(send_data, 2)
        tem="#"+str(int(send_data*100))+'&'+'\n'
        print("to arduino",send_data)
        # print("Py2ArdComs send")
        Snum=tem.encode('utf-8')
        self.arduino.write(Snum)

    def run(self):
        global degree,flag_d
       
        data = 0
        LPF_pitch=0
        HPF_pitch=0
        pitch=0
        alpha=0.8
        flag_d=0
        while (self.ThreadRun):
            data = self.arduino.readline()[:-2] #the last bit gets rid of the new-line chars
            if data:
                print("from arduino",data)
                time_s=str(time.time()-tt)
                imu_d=data.decode('utf-8')
                fimu.write(time_s+", "+imu_d+"\n")
        
        fimu.close()
        print("Exiting the coms protocol...")         



#greenLower = (29, 86, 6)
#greenUpper = (64, 255, 255)
greenLower = (52, 30, 30)
greenUpper = (69, 255, 255)


def gstreamer_pipeline(
    capture_width=640,
    capture_height=360,
    display_width=640,
    display_height=360,
    framerate=25,
    flip_method=0,
):
    return (
        "nvarguscamerasrc ! "
        "video/x-raw(memory:NVMM), "
        "width=(int)%d, height=(int)%d, "
        "format=(string)NV12, framerate=(fraction)%d/1 ! "
        "nvvidconv flip-method=%d ! "
        "video/x-raw, width=(int)%d, height=(int)%d, format=(string)BGRx ! "
        "videoconvert ! "
        "video/x-raw, format=(string)BGR ! appsink"
        % (
            capture_width,
            capture_height,
            framerate,
            flip_method,
            display_width,
            display_height,
        )
    )


def main():

    distance=60
    count=0
    flag = 0
    degree=0
    f = open("data.txt", 'w')
    global fimu
    fimu = open("imu_data.txt", 'w')
    global tt	
    tt=0
    # coms = Py2ArdComs()
    t1 = threading.Thread( target=Communications, args="" )
    t1.start()

    # To flip the image, modify the flip_method parameter (0 and 2 are the most common)
    
    print(gstreamer_pipeline())

    cap = cv2.VideoCapture(gstreamer_pipeline(), cv2.CAP_GSTREAMER)
    time.sleep(2.0)
    tt=time.time()
    if cap.isOpened():
        
        window_handle = cv2.namedWindow("CSI Camera", cv2.WINDOW_AUTOSIZE)
        # Window
        while cv2.getWindowProperty("CSI Camera", 0) >= 0:

            ret_val, frame = cap.read()
            if count<20:
                count+=1
                continue 
            blurred = cv2.GaussianBlur(frame, (11, 11), 0)
            hsv = cv2.cvtColor(blurred, cv2.COLOR_BGR2HSV)
                
            mask = cv2.inRange(hsv, greenLower, greenUpper)
            mask = cv2.erode(mask, None, iterations=2)
            mask = cv2.dilate(mask, None, iterations=2)

            cnts = cv2.findContours(mask.copy(), cv2.RETR_EXTERNAL,
            cv2.CHAIN_APPROX_SIMPLE)
            cnts = cnts[1] if imutils.is_cv2() else cnts[0]
            center = None

            # only proceed if at least one contour was found
            if len(cnts) > 0:

                c = max(cnts, key=cv2.contourArea)
                ((x, y), radius) = cv2.minEnclosingCircle(c)
                M = cv2.moments(c)
                center = (int(M["m10"] / M["m00"]), int(M["m01"] / M["m00"]))
                #l=math.sqrt(math.pow(x-x_p,2)+math.pow(y-y_p,2))
                if flag==0:
                     x_p = int(M["m10"] / M["m00"])
                     y_p = int(M["m01"] / M["m00"])
                     flag = 1
                cv2.circle(frame,(x_p,y_p),6,(255,0,0),-1)
            
 
                # only proceed if the radius meets a minimum size
                if radius > 10:
                    length=2.5/radius
            
                    # draw the circle and centroid on the frame,
                    # then update the list of tracked points
                    cv2.circle(frame, (int(x), int(y)), int(radius), (0, 255, 255), 2)

                    cv2.circle(frame, center, 5, (0, 0, 255), -1)
                    cv2.line(frame,(int(x_p),int(y_p)),(int(x),int(y)),(255,0,0),2)
                    location = (int((x_p+x)/2+10) , int((y_p+y)/2))
                    font = cv2.FONT_HERSHEY_SCRIPT_SIMPLEX
                    fontScale = 0.5
                    lent=(y-y_p)*length
                    cv2.putText(frame,str(lent),location,font,fontScale,(255,0,0),2)
                    
                    radian=math.atan2(lent,distance)
                    degree=radian*180/math.pi
                    degree_a=degree
                    degree=-0.0025*degree*degree+1.2922*degree+0.2792
                    
                    coms.send(degree)
                    # print(degree)
                    time_s=str(time.time()-tt)
                    cam_d=", %.2f\n"%degree_a
                    f.write(time_s+cam_d)

            cv2.imshow("CSI Camera", frame)
            # --------------------------------------------------
            # replace it to show in webpage
            frame = cv2.resize(frame, None, fx=scale, fy=scale)
            frame = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            _, jpeg = cv2.imencode('.jpg', frame)
            frame = jpeg.tobytes()

            yield (b'--frame\r\n'
                    b'Content-Type: image/jpeg\r\n\r\n' + frame + b'\r\n\r\n')
            # --------------------------------------------------
            

            # This also acts as
            keyCode = cv2.waitKey(1) & 0xFF
            
            # Stop the program on the ESC key
            if keyCode == 27:
                f.close()
                break

        cap.release()
        cv2.destroyAllWindows()
        coms.stop()
        #t1.stop()
        
    else:
        print("Unable to open camera")


if __name__ == "__main__":
    app.run(host='127.0.0.1',port='5000', debug=True)
    # coms = Py2ArdComs()
    main()

