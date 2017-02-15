clc;
figure;
global t;
global x;
global m;
global ii;
t = [0];
m = [0];
ii = 0;
x = -100;
p = plot(t,m,'EraseMode','background','MarkerSize',5);
axis([-5 5 -5 5 -5 5]);
grid on;


try
    s=serial('com3');
catch
    error('cant serial');
end
set(s,'BaudRate', 9600,'DataBits',8,'StopBits',1,'Parity','none','FlowControl','none');
s.BytesAvailableFcnMode = 'terminator';
s.BytesAvailableFcn = {@callback,p};

fopen(s);

pause;
fclose(s);
delete(s);
clear s
close all;
clear all;

