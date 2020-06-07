int in_data =0;
void setup(){
    Serial.begin(9600);
}
 
void loop(){
    if(Serial.available()){
        in_data = Serial.parseInt();
    }
    if(in_data!=0){
        Serial.println(++in_data);
        in_data=0;
    }
    delay(500);
}
