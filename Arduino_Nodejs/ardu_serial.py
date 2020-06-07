import serial
import random
import time

# arduino = serial.Serial(
#     port='COM4',
#     baudrate=9600
# )

acc = []
gyr = []
fil = []
while True:
    # if arduino.readable():
    if True:
        '''
        res = arduino.readline().decode()
        #print(res)
        val = res.split('\t')
        #print(val)
        t_acc = val[2].split(':')
        t_gyr = val[3].split(':')
        t_fil = val[4].split(':')
        #print(t_acc[1],t_gyr[1],t_fil[1])        
        acc.append(t_acc[1])
        gyr.append(t_gyr[1])
        fil.append(t_fil[1][:-2])
        '''
        #-----------------------------------------
        acc.append(str(random.random() * 5))
        gyr.append(str(random.random() * 5))
        fil.append(str(random.random() * 5))
        #-----------------------------------------
        send_acc = ' '.join(acc)
        send_gyr = ' '.join(gyr)
        send_fil = ' '.join(fil)
        # print(send_acc)
        # print(send_gyr)
        # print(send_fil)
        time.sleep(0.05)
        
        f = open('./log.txt','w')
        f.write(send_acc)
        f.write('\n')
        f.write(send_gyr)
        f.write('\n')
        f.write(send_fil)
        f.write('\n')
        f.close()
        
        if(len(acc)>=199):
            del acc[0]
            del gyr[0]
            del fil[0]
    else :
        print("fail to read")
        import pdb; pdb.set_trace()
