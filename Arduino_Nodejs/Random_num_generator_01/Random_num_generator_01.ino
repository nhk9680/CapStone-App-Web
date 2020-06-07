int rndNum;
void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
}

void loop() {
  // put your main code here, to run repeatedly:
  rndNum = random(1,10);
  Serial.print(rndNum);
  Serial.print(" ");
  Serial.write(rndNum);
  delay(100);
}
