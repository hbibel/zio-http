package zio.http.middleware

import zio.ZIO
import zio.http._
import zio.http.internal.HttpAppTestExtensions
import zio.http.model.{Headers, Method, Status}
import zio.test.Assertion._
import zio.test._

object AuthSpec extends ZIOSpecDefault with HttpAppTestExtensions {
  private val successBasicHeader: Headers  = Headers.basicAuthorizationHeader("user", "resu")
  private val failureBasicHeader: Headers  = Headers.basicAuthorizationHeader("user", "user")
  private val bearerToken: String          = "dummyBearerToken"
  private val successBearerHeader: Headers = Headers.bearerAuthorizationHeader(bearerToken)
  private val failureBearerHeader: Headers = Headers.bearerAuthorizationHeader(bearerToken + "SomethingElse")

  private val basicAuthM: HttpMiddleware[Any, Nothing]             = Middleware.basicAuth { c =>
    c.uname.reverse == c.upassword
  }
  private val basicAuthZIOM: HttpMiddlewareForTotal[Any, Nothing]  = Middleware.basicAuthZIO { c =>
    ZIO.succeed(c.uname.reverse == c.upassword)
  }
  private val bearerAuthM: HttpMiddleware[Any, Nothing]            = Middleware.bearerAuth { c =>
    c == bearerToken
  }
  private val bearerAuthZIOM: HttpMiddlewareForTotal[Any, Nothing] = Middleware.bearerAuthZIO { c =>
    ZIO.succeed(c == bearerToken)
  }

  def spec = suite("AuthSpec")(
    suite("basicAuth")(
      test("HttpApp is accepted if the basic authentication succeeds") {
        val app = (Http.ok @@ basicAuthM).status
        assertZIO(app(Request.get(URL.empty).copy(headers = successBasicHeader)))(equalTo(Status.Ok))
      },
      test("Uses forbidden app if the basic authentication fails") {
        val app = (Http.ok @@ basicAuthM).status
        assertZIO(app(Request.get(URL.empty).copy(headers = failureBasicHeader)))(equalTo(Status.Unauthorized))
      },
      test("Responses should have WWW-Authentication header if Basic Auth failed") {
        val app = Http.ok @@ basicAuthM header "WWW-AUTHENTICATE"
        assertZIO(app(Request.get(URL.empty).copy(headers = failureBasicHeader)))(isSome)
      },
    ),
    suite("basicAuthZIO")(
      test("HttpApp is accepted if the basic authentication succeeds") {
        val app = (Http.ok @@ basicAuthZIOM).status
        assertZIO(app(Request.get(URL.empty).copy(headers = successBasicHeader)))(equalTo(Status.Ok))
      },
      test("Uses forbidden app if the basic authentication fails") {
        val app = (Http.ok @@ basicAuthZIOM).status
        assertZIO(app(Request.get(URL.empty).copy(headers = failureBasicHeader)))(equalTo(Status.Unauthorized))
      },
      test("Responses should have WWW-Authentication header if Basic Auth failed") {
        val app = Http.ok @@ basicAuthZIOM header "WWW-AUTHENTICATE"
        assertZIO(app(Request.get(URL.empty).copy(headers = failureBasicHeader)))(isSome)
      },
    ),
    suite("bearerAuth")(
      test("HttpApp is accepted if the bearer authentication succeeds") {
        val app = (Http.ok @@ bearerAuthM).status
        assertZIO(app(Request.get(URL.empty).copy(headers = successBearerHeader)))(equalTo(Status.Ok))
      },
      test("Uses forbidden app if the bearer authentication fails") {
        val app = (Http.ok @@ bearerAuthM).status
        assertZIO(app(Request.get(URL.empty).copy(headers = failureBearerHeader)))(equalTo(Status.Unauthorized))
      },
      test("Responses should have WWW-Authentication header if bearer Auth failed") {
        val app = Http.ok @@ bearerAuthM header "WWW-AUTHENTICATE"
        assertZIO(app(Request.get(URL.empty).copy(headers = failureBearerHeader)))(isSome)
      },
      test("Does not affect fallback apps") {
        val app1 = Http.collectHttp[Request] {
          case Method.GET -> !! / "a" => Http.ok
        }
        val app2 = Http.collectHttp[Request]  {
          case Method.GET -> !! / "b" => Http.ok
        }
        val app3 = Http.collectHttp[Request]  {
          case Method.GET -> !! / "c" => Http.ok
        }
        val app = (app1 ++ app2 @@ bearerAuthM ++ app3).status
        for {
          s1 <- app(Request.get(URL(!! / "a")).copy(headers = failureBearerHeader))
          s2 <- app(Request.get(URL(!! / "b")).copy(headers = failureBearerHeader))
          s3 <- app(Request.get(URL(!! / "c")).copy(headers = failureBearerHeader))
        } yield assertTrue(
          s1 == Status.Ok && s2 == Status.Unauthorized && s3 == Status.Ok
        )
      }
    ),
    suite("bearerAuthZIO")(
      test("HttpApp is accepted if the bearer authentication succeeds") {
        val app = (Http.ok @@ bearerAuthZIOM).status
        assertZIO(app(Request.get(URL.empty).copy(headers = successBearerHeader)))(equalTo(Status.Ok))
      },
      test("Uses forbidden app if the bearer authentication fails") {
        val app = (Http.ok @@ bearerAuthZIOM).status
        assertZIO(app(Request.get(URL.empty).copy(headers = failureBearerHeader)))(equalTo(Status.Unauthorized))
      },
      test("Responses should have WWW-Authentication header if bearer Auth failed") {
        val app = Http.ok @@ bearerAuthZIOM header "WWW-AUTHENTICATE"
        assertZIO(app(Request.get(URL.empty).copy(headers = failureBearerHeader)))(isSome)
      },
    ),
  )
}
