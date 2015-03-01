Spring Boot + CAS Authentication
========================================

## CASがとりあえず動くもの

* https://github.com/irof/run-cas-jetty と合わせて使う感じ。

## 使いかた

* sslのキーをresourcesのしたに作る。
  * `keytool -genkeypair -dname "cn=localhost" -keyalg RSA -keystore resources/spring-boot-key.jks -keypass springboot -storepass springboot`
  * 他に作るなら application.yml を変える。
* LocalCAS.crt さんの入ったcacertsを作る。
  * `keytool -importcert -trustcacerts -alias localcas -keystore ./cacerts -file ..(run-cas-jettyのとこ)../LocalCAS.crt -storepass localcas -noprompt`
* Application.java を実行する時のVMオプションでcacertsを指定する。
  * `-Djavax.net.ssl.trustStore=(プロジェクトへの絶対パス)/cacerts -Djavax.net.ssl.trustStorePassword=localcas`

とりあえず動く。はず。

