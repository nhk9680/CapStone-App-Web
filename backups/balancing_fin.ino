#include <Wire.h>
#include <SoftwareSerial.h>
#define MPU_ADDR                  0x68        //- MPU6050 I2C주소
#define PWM_ADDR                  0x6B        //- MPU6050 PWM_MGMT 주소
#define ACX_ADDR                  0x3B        //- 6축 센서 값 시작 주소

#define ENL  10   //모터핀
#define IN1  9 
#define IN2  8
#define IN3  7
#define IN4  6
#define ENR  5

#define BT_RXD 13 //블루투스
#define BT_TXD 12
SoftwareSerial bluetooth(BT_TXD,BT_RXD);

#define GRAVITY_VALUE             16384      //- 1g 중력값
#define R2D        180 / PI   //- 가속도 Radian -> Degree  변환
#define GRYO_TO_DEGREE_PER_SEC    131        //- 자이로 초당 Degree 변환 값 

float       ALPHA=0.994;                                             //- 자이로 & 가속도 상보필터 보정값 
int16_t     AcX, AcY, AcZ, Tmp, GyX, GyY, GyZ;                      //- RAW 데이터 저장 변수 
float       acAngleX, acAngleY, acAngleZ;                           //- 가속도 각도 저장
float       gryDegX, gryDegY, gryDegZ;                              //- 자이로 각속도 저장
float       gryAngleX, gryAngleY, gryAngleZ;                        //- 자이로 각도 저장
float       filterAngleX, filterAngleY, filterAngleZ;               //- 필터링 된 각도 저장
float       baseAcX, baseAcY, baseAcZ;                              //- 초기 가속도 3축 값 저장 변수 
float       baseGyX, baseGyY, baseGyZ;                              //- 초기 자이로 3축 값 저장 변수 
double      dt;    
long        t_now, t_prev;                                          //- 자이로 센서 시간 갭 설정 변수
int         cnt = 1;

double  cam_pitch=0.0, LPF_pitch=0.0;    //카메라를 이용해 얻은 pitch, lpf필터 통과 후
double  gyro_pitch=0.0, HPF_pitch=0.0,gyro_int=0.0;    //자이로를 이용해 얻은 pitch, 이전값, hpf필터 통과 후 pitch
double  pitch=0.0,alpha;    //융합한 pitch, 카메라 융합 차단주파수
int     cflag=0;      //카메라 데이터flag
int     arr[5], arridx=0, wflag=0;
float   cam_tmp=0.0;

double pre_err=0, err, p_con, i_con=0, d_con, con, kp=22.5, ki=-4.0, kd=11;  //pid제어 변수
int mo_pwm,mo_flag=0;   //모터 pwm
double integral=0.0;

//- 초기화 함수 -----------------------------------------------------
void setup() {
    Serial.begin(9600);
    bluetooth.begin(9600);
    pinsInit();                             // 아두이노 핀들의 pinMode 설정
    initMPU6050();
    calibrationMPU6050();
    initDT();
}

//- 기능 구현 함수 -------------------------------------------------
void loop() {
    readMPU6050();              //IMU 센서 읽어들이기
    calcDT();                   //dt계산
    calcAcclAngle();          //가속도계
    calcGyroAngle();            //자이로
    getCameraAngle();           //카메라각도
    comF();                     //각도 융합
    //sendData();
    pid();
    if(mo_flag==1) cart_con(); 
    BT_send();
}

//- 핀들의 pinMode 설정 -------------------------------------------------
void pinsInit()
{
    pinMode(ENL, OUTPUT);
    pinMode(ENR, OUTPUT);
    pinMode(IN3, OUTPUT);
    pinMode(IN4, OUTPUT);
    pinMode(IN1, OUTPUT);
    pinMode(IN2, OUTPUT);
}
//- MPU6050 초기화 함수 --------------------------------------------------
void initMPU6050(){
    Wire.begin();
    Wire.beginTransmission(MPU_ADDR);  //- MPU-6050 센서 주소 
    Wire.write(PWM_ADDR);              //- 전원 동작 모드 및 클럭 설정
    Wire.write(0);                     //- MPU-6050 Sleep Mode -> WakeUp
    Wire.endTransmission(true);        //- 종료 메시지 전달
}


//- 6축 읽어오기 --------------------------------------
void readMPU6050(){
    Wire.beginTransmission(MPU_ADDR);   //- MPU6050 호출
    Wire.write(ACX_ADDR);               //- AcX 레지스터 위치 요청
    Wire.endTransmission(false);        //- 재시작 메시지 전달
    Wire.requestFrom(MPU_ADDR,14,true); //-14byte 데이터 요청
  
    AcX=Wire.read()<<8|Wire.read();
    AcY=Wire.read()<<8|Wire.read();
    AcZ=Wire.read()<<8|Wire.read();
    
    Tmp=Wire.read()<<8|Wire.read();
    
    GyX=Wire.read()<<8|Wire.read();
    GyY=Wire.read()<<8|Wire.read();
    GyZ=Wire.read()<<8|Wire.read();
}

//- MPU6050 초기값 설정 ----------------------------------------------------
void calibrationMPU6050()
{
    float sumAcX =0, sumAcY =0, sumAcZ =0;
    float sumGyX =0, sumGyY =0, sumGyZ =0;

    readMPU6050();                           //- 안정화되기 값 

    for(int i=0; i<20; i++)
    {
        readMPU6050();                       //- 6축 값 읽기
        sumAcX += AcX;  sumAcY += AcY;  sumAcZ += AcZ; 
        sumGyX += GyX;  sumGyY += GyY;  sumGyZ += GyZ;
        delay(50);
    }

    baseAcX = sumAcX / 20;
    baseAcY = sumAcY / 20;
    baseAcZ = sumAcZ / 20;
    baseGyX = sumGyX / 20;
    baseGyY = sumGyY / 20;
    baseGyZ = sumGyZ / 20;
    
    readMPU6050();        //가속도계를 이용해 자이로 초기화
    calcAcclAngle();
    gryAngleY=acAngleY;
}


//- 가속도 각도 구하기 -----------------------------------------------------
void calcAcclAngle(){
  float   accX, accY, accZ;
  float   accXZ, accYZ;

  //- 현재 가속도 센서가 놓여진 기본 값에서 현재 값 뺀 보정값 추출
  accX = AcX - baseAcX;
  accY = AcY - baseAcY;
  accZ = AcZ - (GRAVITY_VALUE - baseAcZ);

  //- Y 각도 
  accYZ = sqrt(pow(accY, 2) + pow(accZ, 2));
  acAngleY = atan(-accX / accYZ) * R2D;
}

//- 측정 시간 초기화 설정 ----------------------------------------------------
void initDT(){
    t_prev = millis();
}


//- 측정 시간 업데이트 -----------------------------------------------------
void calcDT(){
     long temp;
     t_now = millis();
     temp = t_prev;
     t_prev = t_now;  
     dt= double((t_now - temp)/1000.0);
}


//- 자이로 각도 구하기 -----------------------------------------------------
void calcGyroAngle(){
 
  //- 각속도 변환
  gryDegY = (GyY - baseGyY) / GRYO_TO_DEGREE_PER_SEC;

  //- Y 각도 
  gryAngleY += gryDegY * dt;

}

//- 카메라에서 각도 받아오기 -----------------------------------------------------
void getCameraAngle(){
    if (Serial.available()){
        byte bdata = Serial.read();
        if((arridx==0)and(bdata==35)){  //데이터 받기 시작
          wflag=1;
          return;
        }
        if(wflag==0){
          arridx = 0;
          return;
        }
        if(bdata==38){    //38=& 데이터 받기 종료
          cam_tmp=0;
          if(arr[0]==-3){ //'-'=-3
            for(int i=1;i<arridx;i++){
              cam_tmp=cam_tmp*10+arr[i];
            }
            cam_tmp = cam_tmp*(-1);
          }
          else{
            for(int i=0;i<arridx;i++){
              cam_tmp=cam_tmp*10+arr[i];
            }
          }
          
          cam_pitch=cam_tmp/100;
          
          arridx = 0;
          cflag=1;
          wflag=0;
          mo_flag=1;
        }
        if(arridx>5){
          cflag=0;
          wflag=0;
          return;
        }
        arr[arridx++] = bdata - 48;
        
      }
}
//- 상보필터 적용하여 각도 출력하기 ----------------------------------------------------
void calcComFilterAngle(){
  
  float tmpAngleX, tmpAngleY, tmpAngleZ;

  //- 이전 보정 각도 + 자이로 센서 각도  적용
  tmpAngleY = filterAngleY + gryDegY * dt;

  //- 0.994비중의 자이로각도 + 0.006비중의 가속도 각도 적용
  filterAngleY = ALPHA * tmpAngleY + (1.0 - ALPHA) * acAngleY;
}

//- 자이로와 카메라 각도 융합 ----------------------------------------------------
void comF()    //complementary filter
{
  /*
    if(mo_flag==0){
apitch =atan2(ax,sqrt(ay*ay + az*az))/PI*180;
  lpitch=a*lpitch+(1-a)*apitch; 

      apitch =atan2(ax,sqrt(ay*ay + az*az))/PI*180;
  lpitch=a*lpitch+(1-a)*apitch; 
    }
    */

    
    if (cflag==1){
        alpha=0.2;
        //alpha=0.8;
        cflag=0;      
    }
    else{
        alpha=1;
    }
    gyro_int+=dt*gryDegY;
    gyro_pitch=pitch+dt*gryDegY;
    
    LPF_pitch=alpha*LPF_pitch+(1-alpha)*cam_pitch;
    HPF_pitch=alpha*HPF_pitch+alpha*(gyro_pitch-pitch);
    pitch=LPF_pitch+HPF_pitch;
}

//-  출력 ------------------------------------------------------------
void sendData(){
  //Serial.print(gryDegY );Serial.print(", ");
  //Serial.print(alpha);Serial.print(", ");
  //Serial.print(alpha*LPF_pitch);Serial.print(",");
  //Serial.print((1-alpha)*cam_pitch);Serial.print(", ");          
  //Serial.print(cam_pitch); Serial.print(", ");
  Serial.print("filterAngleY");Serial.print(", ");
  Serial.print(filterAngleY);Serial.print(", ");
  Serial.print("Gyro");Serial.print(", ");
  Serial.print(gryDegY);Serial.print(", ");
  Serial.print("Gyro angle");Serial.print(", ");
  Serial.println(gyro_int); 
}
//-  블루투스 통신 ------------------------------------------------------------
int bt_cn=0;
void BT_send(){
  if(bt_cn<100){
    bt_cn=bt_cn+1;
    return;
  }
  bt_cn=0;
  float r[3];
  //r[0]=mo_pwm;
  //r[1]=d_con;
  r[0]=gyro_int;
  r[1]=cam_pitch;
  r[2]=pitch;


   bluetooth.print(r[0]);
   bluetooth.print(":");
   bluetooth.print(r[1]);
   bluetooth.print(":");
   bluetooth.print(r[2]);
   bluetooth.println(":");
     
     
}
//-  PID제어 ------------------------------------------------------------
void pid()      //PID
{
  double ref=0.0;
  err=ref-pitch;
  integral+=err*dt;
  integral=constrain(integral,-40.0/ki,40.0/ki);
  p_con=kp*err;
  i_con=ki*integral;
  d_con=kd*(err-pre_err)/dt;
  //d_con=kd*((err-pre_err)/dt+1.5*err);
  pre_err=err;
  d_con=constrain(d_con,-40.0,40.0);
  con= p_con+i_con+d_con;
  mo_pwm=int(constrain(con,-200,200));
}
//-  PWM설정 ------------------------------------------------------------
void cart_con()
{
  if(mo_pwm<0)
  {  
     mo_pwm=abs(mo_pwm);
    forward();
  }
  else
  {
    mo_pwm=abs(mo_pwm);
    backward();
  }
  
}

void forward()    //전진
{
 analogWrite(ENL, mo_pwm);//오른쪽
  digitalWrite(IN1,HIGH);
  digitalWrite(IN2,LOW);
  analogWrite(ENR,mo_pwm+22);//왼쪽
  digitalWrite(IN3,HIGH);
  digitalWrite(IN4,LOW);
  
}

void backward()   //후진
{analogWrite(ENL, mo_pwm);//오른쪽
  digitalWrite(IN1,LOW);
  digitalWrite(IN2,HIGH);
  analogWrite(ENR,mo_pwm+22);//왼쪽
  digitalWrite(IN3,LOW);
  digitalWrite(IN4,HIGH); 
}
