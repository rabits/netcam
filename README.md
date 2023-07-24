# NetCam

Very simple network camera application to convert your old android phone into a great IP camera.

It was prepared under impressions of IP webcam (com.pas.webcam) which is great as a peephole, but
lacks of important features and introduces a number of complications in usage. For example it has
just MJPEG video transmission, can't properly crop and save crop settings, doesn't have day/night
mode and ML motion sensor.

So to address those problems while integration my camera system into Frigate and Home Assistant I
found and prepared this little project so anyone can benifit from it.

## Features

* RTSP streaming (h264/h265 + aac audio)
* Main and Sub stream
* Lock screen view with auto-wakeup on motion (ML)
* Automatic day and night profiles switch
* Peephole mode (capture only part of the screen)

## How it's working

Initially it collects the available cameras info, then, when user set required parameters and start
the server it runs foreground service which processes the camera stream and allows to not halt in
case the screen is off. The foreground service also passes the preview back to the main activity
to allow to monitor of what's camera capturing with optional overlay of the ML debug layer.
