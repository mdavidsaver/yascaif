Yet Another Simple/Stupid Channel Access InterFace
==================================================

Setup
-----

Place jar(s) in matlab class path.
To make use of monitor callbacks, jars must be in matlab _static_ class path.
Basic blocking calls will work if in dynamic class path.

Basic Usage
-----------

A wrapper around CAJ (Channel Access for Java) library
with an API intended for interactive use with shells
which can call Java functions, principly MATLAB.

Entry point is ```yascaif.CA```.

```matlab
ca = yascaif.CA % use default settings
% manually set max array bytes, address list, and auto-addr-list
ca = yascaif.CA(1000000, "", true)
ca.close() % explicitly close and cleanup
```

Supports CA get/put operations.
Method names are read/write to a clashing with ```.get``` automagically added by MATLAB.

```matlab
val = ca.read('pv:name')
ca.write('pv:name', val+1)
```

Get with metadata

```matlab
M = ca.readM('pv:name') % returns value+meta-data wrapper
val = M.getValue
sevr = M.getSevr % integer alarm status
time = M.getTime % double posix timestamp
```

Subscribe for monitor updates

```matlab
mon = ca.monitor('pv:name')
mon = handle(mon, 'CallbackProperties') % needed by matlab >=2014a
set(mon, 'MonitorCallback', @(h,e)disp(e.getValue)) % e is same wrapper as readM()
```
