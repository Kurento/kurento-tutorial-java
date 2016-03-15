[![License badge](https://img.shields.io/badge/license-LGPL-blue.svg)](http://www.gnu.org/licenses/lgpl-2.1.html)
[![Documentation badge](https://readthedocs.org/projects/fiware-orion/badge/?version=latest)](http://doc-kurento.readthedocs.org/en/latest/)
[![Docker badge](https://img.shields.io/docker/pulls/fiware/orion.svg)](https://hub.docker.com/r/fiware/stream-oriented-kurento/)
[![Support badge]( https://img.shields.io/badge/support-sof-yellowgreen.svg)](http://stackoverflow.com/questions/tagged/kurento)

${project.name}
======================

The ${project.name} shows how to build a WebRTC in loopback application with Kurento.

Installation instructions
-------------------------

By running install.sh, the jar file containing the demo will be copied into 
_/var/lib/kurento_, and the startup script will be put in _/etc/init.d/${project.artifactId}_. Once the demo is installed, it can be managed as a regular service with

```
sudo service ${project.artifactId} {start|stop|restart}
```

If you want to change the port, you can do so by editing the startup script, and setting the desired value in _APP\_PORT_.
