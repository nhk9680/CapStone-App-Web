# CapStone-App-Web

## 구동방법

- 반드시 <u>**_순!서!대!로!_**</u> 실행해 주셔야 합니다.

> 웹 접속 시 나노의 프로그램이 시작하는 구조이므로, 웹 종료 시 Broken Pipe 문제로 인해 나노의 `cam2web.py`를 다시 실행해 주어야 합니다.

1. Jetson Nano
    1) `Ctrl+Alt+T` 해서 터미널 실행
    2) `cd Downloads`
    3) `./ngrok http 5000`
        - ngrok 주소(카메라주소, 알파벳으로 된 것) 잘 봐두기
    4) `Ctrl+Alt+T` 해서 터미널 실행
    5) `cd Desktop`
    6) `python cam2web.py` 

2. Web
    1) Web/socketio 로 이동
    2) `index.html` **편집**으로 열기
    3) 밑에서 두번째(156번째 줄) iframe의 `src=` 뒤 주소를 아까 봐둔 ngrok 주소(카메라주소)로 바꿔주기
        - 바꿔줄 때 http**s** 떼고 http로만
    3) 이동해서 `run.bat` 실행
    4) ngrok 주소(웹주소, 아까 그거 아니고 여기서 실행해서 생긴 거) 잘 봐두기

3. App
   - HC-06과 미리 기기 등록이 되어있어야 합니다. 기본 블루투스 메뉴에서 해주세요.

    1) `BLUETOOTH ON`
    2) `SERVER` 클릭 후 Web주소 복붙
    3) `CONNECTION` 클릭해서 `HC-06` 페어링

## 4. 웹주소 `https -> http`로 바꿔서 접속