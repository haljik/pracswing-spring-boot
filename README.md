Spring Boot Admin Server を使ってみる
=======================================

## バージョンとか

https://github.com/codecentric/spring-boot-admin/

* Spring Boot Admin Server: 1.1.2
* Spring Boot: 1.2.2

SpringBootAdminServer:1.1.2はSpringBoot:1.2.1を使っているので、使用しているSpringBootは書いておかないとバージョン不整合で変な動きする。（たとえばSpringBootActuator依存に書かなかったら1.2.1が使われてしまう。）

## 大まかに言うと

SpringBootAdminServerはSpringBootActuatorで取れる内容をそれっぽく見せるもの。
あはjolokia入ってるからJMXで好きにしてね、とゆー物体。

メトリクスがちょっとリッチなUIで見れたり、ログファイルがダウンロードできたりと便利な気はする。

ServerとClientに分かれている構成。Clientは監視したいアプリケーション。
Serverには複数のアプリケーションを登録できて、アプリケーションにClientのスターターを突っ込んでれば登録しにいく。

設定はSpringBootなので `application.yml` に書く。
内容は [この辺](https://github.com/codecentric/spring-boot-admin/blob/master/spring-boot-admin-starter-client/README.md) 参照。

## 手順

1. AdminServerを起動する。
    * [これとか](https://github.com/irof/trial-spring-boot-admin/tree/master/simple-spring-admin-server)。
2. 監視したいアプリケーション側に `spring-boot-admin-starter-client` を突っ込む。
3. `spring.boot.admin.url` にAdminServerのアドレスを書く。
4. アプリケーションを起動してしばらく待つ。
5. AdminServerから見れる。

クライアントとサーバーを同じにしたら余計なUIとか依存とかエンドポイントとか抱え込むことになるのでイマイチそう。

## 課題

SpringBootActuatorのEndpointがSpringSecutiryに守られると当然だけど使えない。
とりあえず`management.security.enabled`を`false`にすれば動くけれど……。

