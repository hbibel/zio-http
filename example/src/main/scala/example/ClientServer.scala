package example

import zhttp.http._
import zhttp.service.{Client, EventLoopGroup, Server}
import zio.{ZIO, ZIOAppDefault}

object ClientServer extends ZIOAppDefault {

  val app = Http.collectZIO[Request] {
    case Method.GET -> !! / "hello" =>
      ZIO.succeed(Response.text("hello"))

    case Method.GET -> !! =>
      val url = "http://localhost:8080/hello"
      Client.make[Any]().flatMap(_.request(url))
  }

  val run = {
    val clientLayers = EventLoopGroup.auto()
    Server.start(8080, app).provideLayer(clientLayers).exitCode
  }
}
