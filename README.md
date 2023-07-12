# NetCam

Very simple network camera application to convert your old android phone into a great IP camera.

It was prepared under impressions of IP webcam (com.pas.webcam) which is great as a peephole, but
lacks of important features and introduces a number of complications in usage. For example it has
just MJPEG video transmission, can't properly crop and save crop settings, doesn't have day/night
mode and ML motion sensor.

So to address those problems while integration my camera system into Frigate and Home Assistant I
found and prepared this little project so anyone can benifit from it.

## Features

* RTSP streaming (h264/h265 + aac audio) of all available cameras
* Lock screen view with auto-wakeup on motion (ML)
* Automatic day and night mode switch
* Peephole mode (capture only part of the screen)
