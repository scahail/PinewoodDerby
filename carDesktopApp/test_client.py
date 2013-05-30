# Echo client program
import socket

HOST = 'localhost'    # The remote host
PORT = 50007              # The same port as used by the server
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect((HOST, PORT))
s.sendall('LAT:1234 LONG:1235')
s.sendall('ACCEL:123456')
print 'Sent test message'
while 1:
   data = s.recv(1024)
   if not s.recv: break
   print data
   if data == 'ARM':
     print 'Would send test data'
s.close()
print 'Done.'
