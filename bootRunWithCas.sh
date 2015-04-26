./gradlew clean compileJava
keytool -importcert -trustcacerts -alias localcas -keystore ./build/cacerts -file ../run-cas-jetty/LocalCAS.crt -storepass localcas -noprompt
export _JAVA_OPTIONS="-Djavax.net.ssl.trustStore=/Users/haljik/dev/system-sekkei/pracswing-spring-boot/cacerts -Djavax.net.ssl.trustStorePassword=localcas"
./gradlew bootRun
