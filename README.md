# ScanServer
Java Server to scan/merge/list files using various libs (to minimize fileSize) that have to be preinstalled on a unix system.

## Used libs
* pdftk
* scanimage
* tiff2pdf
* pdftops
* ps2pdf

## System used on:

RaspberryPi with os:
No LSB modules are available.
Distributor ID:	Raspbian
Description:	Raspbian GNU/Linux 9.11 (stretch)
Release:	9.11
Codename:	stretch

with java 1.8 (i'd love to use 11+, but couldn't upgrade so far):
openjdk version "1.8.0_232"
OpenJDK Runtime Environment (build 1.8.0_232-8u232-b09-1~deb9u1-b09)
OpenJDK Client VM (build 25.232-b09, mixed mode)

## Sample Clients:
A sample implementation can be found in the project itself, see [ClientTest.java](https://github.com/JanSteffes/ScanServer/blob/master/src/testClient/ClientTest.java).

Sample implementation of app-client (flutter): [scan_app](https://github.com/JanSteffes/scan_app)

## TODO
* Maybe make this as rest-service instead of socket based (or even optional?)
* refactor some code, all the commands should be easier reusable
* move specific configs (like folder to work in) to Config
* better/more specific logs?


