#hotswap-java
============

Small java hot swapping agent.

## Usage Example
```sh
$ java -Dhost=localhost -Dport=9000 -Dpath=/home/dev/code/target/classes \
-jar HotSwap.jar me/benbrewer/tools/MyClass.class
```

At some point in the future, this will be updated to take its parameters from argv instead of abusing system properties.

Hello people.
