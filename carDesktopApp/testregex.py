#!/usr/bin/python
import re

msg = "ACCEL:12345";

matchLatLong = re.match( r'LAT:(\d+)\sLONG:(\d+)', msg)

# Match object for acceleration messages
matchAccel = re.match(r'ACCEL:(\d+)', msg)

if matchLatLong:
    print 'Inside matchLatLong case:'
    print "matchLatLong.group()  : ", matchLatLong.group()
    print "matchLatLong.group(1) : ", matchLatLong.group(1)
    print "matchLatLong.group(2) : ", matchLatLong.group(2)
    print 'End matchLatLong case.'
elif matchAccel:
    print 'Inside matchAccel case:'
    print "matchAccel.group()  : ", matchAccel.group()
    print "matchAccel.group(1) : ", matchAccel.group(1)
    print 'End matchAccel case.'
else:
    print 'Inside no match case:'
    print "No match; message is invalid."
    print 'End no match case.'