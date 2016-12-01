Yet Another Simple/Stupid Channel Access InterFace
==================================================

Beware, using yascaif in MATLAB is likely making use
of undocumented (unsupported) MATLAB features.
This is tested with 2016a, and will likely work back to ~2010 release,
but may be broken by changes in future releases.

Setup
-----

Place jar(s) in matlab class path.
To make use of monitor callbacks, jars must be in matlab _static_ class path.
Basic blocking calls will work if in dynamic class path.

Use [pre-build jars](https://github.com/mdavidsaver/yascaif/releases)
or:

```
git clone https://github.com/mdavidsaver/yascaif.git
cd yascaif
ant
```

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
% or to put and wait for server completion notification
ca.write('pv:name', val+1, true)
```

Get with metadata

```matlab
M = ca.readM('pv:name') % returns value+meta-data wrapper
val = M.getValue
sevr = M.getSevr % integer alarm status
time = M.getTime % double posix timestamp
```

Subscribe for monitor updates
Deliver as callbacks.

```matlab
mon = ca.monitor('pv:name')
mon = handle(mon, 'CallbackProperties') % needed by matlab >=2014a
set(mon, 'MonitorCallback', @(h,e)disp(e.getValue)) % e is same wrapper as readM()
```

Deliver via FIFO.

```matlab
mon = ca.monitor('pv:name')
mon.setCapacity(4)   % max. queue size (default 1)
mon.setTimeout(10.0) % or -1 to disable (default 5.0)
M = mon.waitFor() % wait for next update
```
