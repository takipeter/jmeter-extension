jmeter-extension
================

Provide a graph visualizer showing response time and concurrent users against time + a jmeter sampler picking up random request body content from files.

Build fom source:
1. download and extract apache-jmeter-2.9 (https://jmeter.apache.org/download_jmeter.cgi)
2. change JMETER_HOME property refering to the latest jmeter home in build.properties
3. run 'ant all'
4. copy ../build/jmeter-plugin.jar to {JMETER_HOME}/lib/ext
5. Start JMeter -> Add -> Listener: TPeter - Threads and Response Times vs Times
