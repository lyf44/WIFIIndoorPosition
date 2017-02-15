function callback(s, BytesAvailable,p)
    
    global t;
    global x;
    global m;
    global ii;

    out = fscanf(s)    
    data = str2num(out);
    %t = [t ii];
    %m = [m data(1)];
    data = quad2eul(data);
    P1=rotz(data(1))*roty(data(2))*rotx(data(3))*[2;1;0];
    P2=rotz(data(1))*roty(data(2))*rotx(data(3))*[2;-1;0];
    set(p, 'XData',[P1(1) P2(1) -P1(1) -P2(1) P1(1)],'YData',[P1(2) P2(2) -P1(2) -P2(2) P1(2)],'ZData',[P1(3) P2(3) -P1(3) -P2(3) P1(3)]);
    
    drawnow
%    x = x + 1;
    axis([-5 5 -5 5 -5 5]);
%    ii=ii+1;
end
    